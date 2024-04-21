package io.proj3ct.bot.service;

import io.proj3ct.bot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

//TelegramLongPollingBot пингует API постоянно
@Slf4j
@Component
public class TelegraMv1Bot extends TelegramLongPollingBot {
    private static final String HELP_TEXT = """
            Help command /h - description will be here.\n\n
            Type /start to start messaging \n\n or /mydata to see stored data about you.""";
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
                case "/start": startCommandReceived(chatId,
                                update.getMessage()
                                .getChat()
                                .getFirstName());
                break;
                case "/h": sendMessageToUser(chatId, HELP_TEXT);
                    break;
                default: sendMessageToUser(chatId, "Sorry, command is not recognized.");
            }
        }
    }

    private void startCommandReceived(long chatId, String userName) {
        String answer = "Hello, " + userName + " nice to meet you!";
        log.info("Start message sent to user: " + userName);
        sendMessageToUser(chatId, answer);
    }

    private void sendMessageToUser(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
