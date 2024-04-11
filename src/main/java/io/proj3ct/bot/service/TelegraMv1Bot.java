package io.proj3ct.bot.service;

import io.proj3ct.bot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

//TelegramLongPollingBot пингует API постоянно
@Component
public class TelegraMv1Bot extends TelegramLongPollingBot {
    final BotConfig botConfig;

    public TelegraMv1Bot(BotConfig botConfig) {
        this.botConfig = botConfig;
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
                default: sendMessageToUser(chatId, "Sorry, command is not recognized.");
            }
        }
    }

    private void startCommandReceived(long chatId, String userName) {
        String answer = "Hello, " + userName + " nice to meet you!";
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
