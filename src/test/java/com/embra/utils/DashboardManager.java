package com.embra.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class DashboardManager {

    private static ExtentReports extent;
    private static ExtentTest currentTest;
    public static final String REPORT_PATH = "target/AutomationDashboard.html";

    // Initialize the Dashboard
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

    // Start a new test block in the dashboard
    public static void startTest(String testName) {
        if (extent != null) {
            currentTest = extent.createTest(testName);
        }
    }

    // Replace System.out.println with this to log to console AND dashboard
    public static void log(String message) {
        System.out.println(message); // Keeps your console logs intact
        if (currentTest != null) {
            // Check for pass/fail keywords to color-code the dashboard
            if (message.contains("✅") || message.contains("SUCCESS")) {
                currentTest.pass(message);
            } else if (message.contains("❌") || message.contains("Failed")) {
                currentTest.fail(message);
            } else if (message.contains("⚠️")) {
                currentTest.warning(message);
            } else {
                currentTest.info(message);
            }
        }
    }

    // Save the dashboard file
    public static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}