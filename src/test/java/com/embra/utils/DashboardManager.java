package com.embra.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DashboardManager {

    private static ExtentReports extent;
    private static ExtentTest currentTest;
    public static final String REPORT_PATH = "target/AutomationDashboard.html";

    // Supabase Configuration from Environment Variables
    private static final String SB_URL = System.getenv("SUPABASE_URL");
    private static final String SB_KEY = System.getenv("SUPABASE_KEY");
    private static final String RUN_ID = System.getProperty("run_id");

    private static int passCount = 0;
    private static int failCount = 0;
    private static int infoCount = 0;

    public static void initReport() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("EMB Test Dashboard");
            spark.config().setReportName("E2E Automation Flow Execution");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", "UAT");
            extent.setSystemInfo("Tester", "EMB Automation");
        }
    }

    public static void startTest(String testName) {
        if (extent != null) {
            currentTest = extent.createTest(testName);
            passCount = 0;
            failCount = 0;
            infoCount = 0;
        }
    }

    public static void log(String message) {
        // 1. Console Output
        System.out.println(message);

        // 2. ExtentReports Logic (HTML)
        if (currentTest != null) {
            if (message.contains("✅") || message.contains("SUCCESS")) {
                currentTest.pass(message);
                passCount++;
            } else if (message.contains("❌") || message.contains("Failed")) {
                currentTest.fail(message);
                failCount++;
            } else if (message.contains("⚠️")) {
                currentTest.warning(message);
                infoCount++;
            } else {
                currentTest.info(message);
                infoCount++;
            }
        }

        // 3. Real-time Supabase Stream (for Dashboard)
        streamToSupabase(message);
    }

    private static void streamToSupabase(String message) {
        if (RUN_ID == null || SB_URL == null || SB_KEY == null) {
            // If running locally without GitHub/Dashboard, skip streaming
            return;
        }

        try {
            // Clean message for JSON (remove newlines and escape quotes)
            String cleanMsg = message.replace("\"", "'").replace("\n", " ").trim();

            // We use the Supabase 'rpc' approach or a PATCH to append logs.
            // Note: In standard REST, PATCH replaces the field.
            // To append, we'd usually fetch then patch, but for simplicity, we send the line.
            String json = "{\"logs\": \"" + cleanMsg + "\\n\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SB_URL + "/rest/v1/test_runs?id=eq." + RUN_ID))
                    .header("apikey", SB_KEY)
                    .header("Authorization", "Bearer " + SB_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates") // Important for some SB configs
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Fire and forget (async) so it doesn't slow down the test execution
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            // If message indicates end of test, update status
            if (message.contains("✅") || message.contains("Completed Successfully")) {
                updateStatus("pass");
            } else if (message.contains("❌") || message.contains("Failed")) {
                updateStatus("fail");
            }

        } catch (Exception e) {
            System.err.println("Dashboard streaming failed: " + e.getMessage());
        }
    }

    private static void updateStatus(String status) {
        String json = "{\"status\": \"" + status + "\"}";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SB_URL + "/rest/v1/test_runs?id=eq." + RUN_ID))
                    .header("apikey", SB_KEY)
                    .header("Authorization", "Bearer " + SB_KEY)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { /* log error */ }
    }

    public static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }

    public static int getPassCount() { return passCount; }
    public static int getFailCount() { return failCount; }
    public static int getInfoCount() { return infoCount; }
}