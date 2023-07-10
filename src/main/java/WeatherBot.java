import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.Root;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultLocation;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import service.ConnectionImpl;
import service.FileServiceImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WeatherBot extends TelegramLongPollingBot {
    public WeatherBot(String botToken) {
        super(botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()){
            if (update.getMessage().hasLocation()) {
                double lon = update.getMessage().getLocation().getLongitude();
                double lat = update.getMessage().getLocation().getLatitude();
                System.out.println(lon + " --> "+ lat);
                ConnectionImpl connection = new ConnectionImpl();
                String von = connection.openUrl("https://openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&units=metric&appid=439d4b804bc8187953eb36d2a8c26a02");
                Type type = new TypeToken<Root>(){}.getType();
                Gson gson = new Gson();
                Root root = gson.fromJson(von, type);
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setMessageId(update.getMessage().getMessageId());
                deleteMessage.setChatId(update.getMessage().getChatId());
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId());
                message.setText("Siz jo'natgan joylashuv buyicha obu-havo ma'lumotlari.\n\n\uD83C\uDF06 Shahar: " + root.name +"\n\uD83C\uDF21 Harorat: " + root.main.temp+" 'C"
                +" \n\uD83D\uDCA8 Shamol: " + root.wind.speed + " m/s");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }


                if (update.getMessage().getText().equals("/start")){

                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId());
                message.setText("Assalomu Aleykum\n");
                ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                markup.setResizeKeyboard(true);
                List<KeyboardRow> keyboardRowList = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setRequestLocation(true);
                keyboardButton.setText("Joylashuvni kiritish");
                row.add(keyboardButton);
                keyboardRowList.add(row);
                KeyboardRow row2 = new KeyboardRow();
                KeyboardButton keyboardButton2 = new KeyboardButton();
                keyboardButton2.setText("Shahar izlash");
                row2.add(keyboardButton2);
                keyboardRowList.add(row2);
                markup.setKeyboard(keyboardRowList);
                message.setReplyMarkup(markup);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if (update.getMessage().getText().equals("Shahar izlash")){
                    SendMessage message = new SendMessage();
                    message.setChatId(update.getMessage().getChatId());
                    message.setText("Quyidagi tugma orqali shahar nomi bilan qidiruv bo'limiga o'tishingiz mumkin!");
                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setSwitchInlineQueryCurrentChat("");
                    button.setText("Qidiruv");
                    rowInline.add(button);
                    rowsInline.add(rowInline);
                    markup.setKeyboard(rowsInline);
                    message.setReplyMarkup(markup);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }


        }else if (update.hasInlineQuery()){
            if (update.getInlineQuery().getQuery().length() > 2){
                String query = update.getInlineQuery().getQuery();
                ConnectionImpl connection = new ConnectionImpl();
                String xml = connection.openUrl("http://45e30b7f.services.gismeteo.ru/inform-service/a407a91cfcb53e52063b77e9e777f5bd/cities/?search_all=1&lat_lng=1&count=10&lang=en&startsWith="+query);
                FileServiceImpl fileService = new FileServiceImpl();
                fileService.writeFile("data.xml", xml);
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

                AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
                answerInlineQuery.setInlineQueryId(update.getInlineQuery().getId());
                answerInlineQuery.setIsPersonal(true);
                List<InlineQueryResult> list = new ArrayList<>();

                try {
                    builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    Document document = builder.parse(new File("data.xml"));
                    document.getDocumentElement().normalize();

                    NodeList nodeList = document.getElementsByTagName("item");
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        if (node.hasAttributes()){
                            Element element = (Element) node;
                            String name = element.getAttribute("n");
                            String lon = element.getAttribute("lng");
                            String lat = element.getAttribute("lat");
                            String country = element.getAttribute("country_name");
                            String id = element.getAttribute("id");
                            if (Float.valueOf(lat) < 100.00 && Float.valueOf(lon) < 100.00) {
                                System.out.println(name + " ------> " + lon + " = "+ lat);


                                InlineQueryResultLocation location = new InlineQueryResultLocation();
                                location.setId(id);
                                location.setLatitude(Float.valueOf(lat));
                                location.setLongitude(Float.valueOf(lon));
                                location.setTitle(name + " / " + country);
                                list.add(location);
                            }
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                answerInlineQuery.setResults(list);
                try {
                    execute(answerInlineQuery);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            }

        }else {

        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return "@weather2_uz_bot";
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }
}
