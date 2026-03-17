package com.embra.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DashboardClient {
    // These come from your GitHub Action Secrets
    private static final String SB_URL = System.getenv("SUPABASE_URL");
    private static final String SB_KEY = System.getenv("SUPABASE_KEY");
    // This ID is passed from the dashboard via the GitHub Action
    private static final String RUN_ID = System.getProperty("run_id");

    public static void log(String message) {
        if (RUN_ID == null || SB_URL == null) return;

        try {
            // We use RPC or PATCH to append logs to the existing log string
            String json = "{\"logs\": \"" + message.replace("\"", "'") + "\\n\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SB_URL + "/rest/v1/test_runs?id=eq." + RUN_ID))
                    .header("apikey", SB_KEY)
                    .header("Authorization", "Bearer " + SB_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Failed to send log to Dashboard: " + e.getMessage());
        }
    }

    public static void updateStatus(String status) {
        if (RUN_ID == null) return;
        String json = "{\"status\": \"" + status + "\"}";
        sendUpdate(json);
    }

    private static void sendUpdate(String json) {
        // ... standard HttpClient PATCH logic similar to log() above ...
    }
}