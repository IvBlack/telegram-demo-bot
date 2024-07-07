package io.proj3ct.bot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.bot.config.BotConfig;
import io.proj3ct.bot.model.User;
import io.proj3ct.bot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//TelegramLongPollingBot пингует API постоянно
@Slf4j
@Component
public class TelegraMv1Bot extends TelegramLongPollingBot {
    private static final String HELP_TEXT = """
            Help command /h - description will be here.\n\n
            Type /start to start messaging \n\n or /mydata to see stored data about you.""";

    @Autowired
    private UserRepository userRepository;
    final BotConfig botConfig;

    public TelegraMv1Bot(BotConfig botConfig) {

        this.botConfig = botConfig;

        //добавление боту нескольких команд с описанием для взаимодействия с юзером
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "bot work init"));
        listOfCommands.add(new BotCommand("/mydata", "show my stored data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my stored data from table"));
        listOfCommands.add(new BotCommand("/h", "simple guide of bot commands"));
        listOfCommands.add(new BotCommand("/setting", "settings"));

        //запуск списка команд, через констуктор
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error execution command list" + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    //обработка запроса пользователя боту на площадке telegram
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (text) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId,
                                update.getMessage()
                                .getChat()
                                .getFirstName());
                break;
                case "/h": sendMessageToUser(chatId, HELP_TEXT);
                break;
                case "/register": register(chatId);
                break;
                default: sendMessageToUser(chatId, "Sorry, command is not recognized.");
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Some text about registering...");

        //классы для кнопок внутри сообщений
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes!");
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();
        noButton.setText("No!");
        noButton.setCallbackData("NO_BUTTON");

        //расположение кнопок
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);

        //собрали клавиатуру из кнопок, прикрепили к объекту сообщения
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        //отправка сообщения
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /*
        Обработка пользователя на предмет наличия в БД при команде /start.
        Если нет в БД юзера - создается новый,
        на основе данных, что вытягиваются из объекта сообщения Message (текст, фото, стикер - что угодно)
    */
    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String userName) {
        String answer = EmojiParser.parseToUnicode("Hello, " + userName + " nice to meet you! " + ":x:");
//        String answer = "Hello, " + userName + " nice to meet you! " + ":blush:";
        log.info("Start message sent to user: " + userName);
        sendMessageToUser(chatId, answer);
    }

    /*
        Виртуальная клавиатура создается добавляется к объекту SendMessage.
        Если необходимо инстанцировать на каждое сбщ свой тип клавиатуры: вынести создание в отдельный метод,
        и подавать в sendMessageToUser в качестве одного из аргументов.
    * */
    private void sendMessageToUser(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        //---------------работа с виртуальной клавиатурой---------------------
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = getKeyboardRows();

        keyboard.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboard);
        //-------------------------------------------------------------------
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<KeyboardRow> getKeyboardRows() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        //отправляет команду при нажатии кнопки
        KeyboardRow row = new KeyboardRow();

        //добавим ряд кнопок в список рядов, порядок имеет значение
        row.add("weather");
        row.add("create random joke");
        keyboardRows.add(row);

        //новый ряд кнопок
        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);
        return keyboardRows;
    }
}
