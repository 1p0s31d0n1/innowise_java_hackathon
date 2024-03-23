package com.example.tgbot.service;

import com.example.tgbot.model.CurrencyModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

public class CurrencyService {

    public static CurrencyModel getCurrencyRate(String currency) throws IOException {
        CurrencyModel model = new CurrencyModel();
        URL url = new URL("https://api.mexc.com/api/v3/ticker/price");
        Scanner scanner = new Scanner((InputStream) url.getContent());
        String result = "";
        while (scanner.hasNext()) {
            result += scanner.nextLine();
        }
        result = result.replace("[", "").replace("]","").replace("},","};");
        String[] objects = result.split(";");
        ArrayList<JSONObject> jsonObjects = new ArrayList<>();
        for (String i : objects) {
            jsonObjects.add(new JSONObject(i));
        }
        Optional<JSONObject> curr = jsonObjects.stream().filter(o -> o.get("symbol").equals(currency)).findAny();
        try {
            model.setSymbol((String) curr.get().get("symbol"));
            model.setPrice((String) curr.get().get("price"));
        } catch (NoSuchElementException e) {
            System.out.println("Not found");
        }
        return model;
    }
}
