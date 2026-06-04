package com.embra.tests;

import com.embra.pages.ReportGenerationCheckPage;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

public class ReportGenerationCheckTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page adminPage;

    // Store report download link to include in automation email
    private static String reportDownloadLink = "Not found";

    @BeforeAll
    static void setupBrowser() {
        DashboardManager.initReport();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        // Single context — only admin tab needed now, Gmail checked via IMAP
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(true));

        // Tab 1 — Admin portal only
        adminPage = context.newPage();
        adminPage.setDefaultTimeout(60000);
        adminPage.setDefaultNavigationTimeout(90000);
    }

    @Test
    void testReportGenerationAndEmailVerification() {
        DashboardManager.startTest("Report Generation & Email Verification");
        DashboardManager.log("[REPORT] 🚀 Starting Report Generation Check...");

        ReportGenerationCheckPage reportPage = new ReportGenerationCheckPage(adminPage);

        // ── STEP 1: Login to Admin Portal ─────────────────────────
        DashboardManager.log("\n[STEP 1] 🔑 Admin Login");
        reportPage.login();

        // ── STEP 2: Navigate to Requirement Listing ────────────────
        DashboardManager.log("\n[STEP 2] 📋 Navigate to Requirement Listing");
        reportPage.navigateToRequirementListing();

        // ── STEP 3: Open Search & Filters ─────────────────────────
        DashboardManager.log("\n[STEP 3] 🔍 Open Search & Filters");
        reportPage.openSearchFilters();

        // ── STEP 4: Click Download Reports & capture API response + time ──
        DashboardManager.log("\n[STEP 4] 📥 Click Download Reports");
        String reportClickTime = reportPage.clickDownloadReports();

        // Save admin trace after download report click
        try {
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("target/report-generation-admin-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/report-generation-admin-trace.zip");
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin trace: " + e.getMessage());
        }

        // ── STEP 5: Check email via IMAP — no browser session needed ──
        // Connects directly to Gmail IMAP, no session expiry issues
        DashboardManager.log("\n[STEP 5] 📧 Check Report Email via IMAP");
        reportDownloadLink = reportPage.openGmailAndVerifyReportEmail(null, reportClickTime);
        DashboardManager.log("   -> Report Download Link: " + reportDownloadLink);

        DashboardManager.log("\n[REPORT] ✅ Report Generation Check Completed!");
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            try {
                // Fallback trace — captures everything if test crashes mid-way
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/report-generation-fallback-trace.zip")));
                DashboardManager.log("   💾 Fallback trace saved → target/report-generation-fallback-trace.zip");
            } catch (Exception ignored) {}
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        // Flush report file before sending email
        DashboardManager.flushReport();

        // Send automation email with report download link included
        EmailSender.sendReportCheckEmail(
                "REPORT GENERATION CHECK DAILY",
                reportDownloadLink,
                "bharatpandey011@gmail.com",
                "bharat.pandey@emb.global",
                "gaurav.rauthan@emb.global",
                "Ashish.mishra@emb.global",
                "saumya.gupta@emb.global"
        );
    }
}