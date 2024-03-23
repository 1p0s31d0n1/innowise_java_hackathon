package com.example.tgbot.service;

import com.example.tgbot.config.JDBCConfig;
import org.json.JSONObject;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.util.*;

@EnableScheduling
@Component
public class DBupdater {
    private static DataSource dataSource = JDBCConfig.mysqlDataSource();

    @Scheduled(fixedRate = 5000)
    private void updateDB() throws IOException {
        URL url = new URL("https://api.mexc.com/api/v3/ticker/price");
        JdbcTemplate template = new JdbcTemplate(dataSource);
        Scanner scanner = new Scanner((InputStream) url.getContent());
        String result = "";
        while (scanner.hasNext()) {
            result += scanner.nextLine();
        }
        template.execute("DELETE FROM Currencies;");
        result = result.replace("[", "").replace("]","").replace("},","};");
        String[] objects = result.split(";");
        for (String i : objects) {
            JSONObject json = new JSONObject(i);
            template.execute("INSERT INTO Currencies VALUES ('" + json.get("symbol") + "','" + json.get("price") +"')");
        }
        updateUsers();
    }

    public static Map<String,String> updateUsers() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List<Map<String, Object>> list = template.queryForList("SELECT * FROM users");
        Map<String, String> result = new HashMap<>();
        for (Map<String,Object> i : list) {
            double value = Double.parseDouble((String) i.get("value"));
            String curr = (String) i.get("monitoringCurr");
            double newValue = Double.parseDouble(template.queryForObject("SELECT price FROM Currencies WHERE currency='" + curr + "'",String.class));
            double percent = Double.parseDouble((String) i.get("percent"));
            if (value * (percent/100) < Math.abs(value-newValue)) {
                String changer = value-newValue < 0 ? "UP" : "DOWN";
                result.put((String) i.get("chatId"), "Currency " + changer + "!\nPrevious value: " + value + "\nNew value: " + newValue);
            }
            template.execute("UPDATE users SET value='" + newValue + "' WHERE monitoringCurr='" + curr + "'");
        }
        return result;
    }

    public static void addUserData(long chatId, String currency, String percent) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.update("INSERT INTO users VALUES ('" + chatId + "', '" + currency + "', '" + percent + "', '" + getCurrRate(currency) + "')");
    }

    private static String getCurrRate(String currency) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        String rate = template.queryForObject("SELECT price FROM Currencies WHERE currency='" + currency + "'", String.class);
        return rate;
    }
}
