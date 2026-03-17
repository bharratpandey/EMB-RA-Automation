package com.embra.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailService {
    private static final String BASE_URL = "https://api.mail.tm";
    private final HttpClient client = HttpClient.newHttpClient();

    public String getLatestOTP(String email, String password) throws Exception {
        // 1. Get Token
        String loginJson = String.format("{\"address\":\"%s\", \"password\":\"%s\"}", email, password);
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        String tokenResponse = client.send(loginReq, HttpResponse.BodyHandlers.ofString()).body();
        String token = JsonParser.parseString(tokenResponse).getAsJsonObject().get("token").getAsString();

        // 2. Fetch Messages (Wait a few seconds for email to arrive)
        Thread.sleep(5000);
        HttpRequest msgReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/messages"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        String msgResponse = client.send(msgReq, HttpResponse.BodyHandlers.ofString()).body();
        JsonArray msgs = JsonParser.parseString(msgResponse).getAsJsonObject().get("hydra:member").getAsJsonArray();

        if (msgs.isEmpty()) throw new RuntimeException("No emails found in Mail.tm inbox!");

        // 3. Get latest message content
        String messageId = msgs.get(0).getAsJsonObject().get("id").getAsString();
        HttpRequest contentReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/messages/" + messageId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        String contentResponse = client.send(contentReq, HttpResponse.BodyHandlers.ofString()).body();
        String body = JsonParser.parseString(contentResponse).getAsJsonObject().get("intro").getAsString();

        // 4. Extract 6-digit OTP using Regex
        Pattern pattern = Pattern.compile("\\b\\d{6}\\b");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new RuntimeException("OTP not found in email body!");
    }
}