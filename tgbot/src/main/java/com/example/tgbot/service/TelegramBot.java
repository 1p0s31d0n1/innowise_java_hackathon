package com.example.tgbot.service;

import com.example.tgbot.config.BotConfig;
import lombok.AllArgsConstructor;
import com.example.tgbot.model.CurrencyModel;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@EnableScheduling
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;


    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
       return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.equals("/start")) {
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            } else if (messageText.startsWith("/monitor")) {
                    monitorCommandReceived(chatId, messageText);
                } else if (messageText.startsWith("/stop")) {

            } else {
                try {
                    currencyCommandReceived(chatId, update.getMessage().getText());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void monitorCommandReceived(Long chatId, String msg) {
        String currency = msg.split(" ")[1].replaceAll(" ","");
        String percent = msg.split(" ")[2].replaceAll(" ","");
        if (currency.equals("") || percent.equals(""))
        try {
            Double.parseDouble(percent);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Error, try again");
        }
        DBupdater.addUserData(chatId, currency, percent);
    }

    private void currencyCommandReceived(Long chatId, String currency) throws IOException {
        CurrencyModel currencyModel = CurrencyService.getCurrencyRate(currency);
        String answer = currencyModel.getSymbol().equals(null) ? "There's no this currency in our database :(" :
                "Currency: " + currencyModel.getSymbol() + "\n" + "Price: " + currencyModel.getPrice();
        System.out.println(answer);
        sendMessage(chatId, answer);
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + "! Enter the currency name you want to check. \n If you want to start" +
                " monitoring currency, write /monitor [currency] [procent]\n" +
                "If you want to stop monitoring, write /stop [currency]";
        sendMessage(chatId, answer);
    }

    @Scheduled(fixedRate = 3000)
    private void schedule() {
        System.out.println("abc");
        Map<String, String> res = DBupdater.updateUsers();
        if (!res.isEmpty()) {
            for (String i : res.keySet()) {
                sendMessage(Long.getLong(i), res.get(i));
            }
        }
    }

    private void sendMessage(Long chatId, String msg) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(msg);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }
}
