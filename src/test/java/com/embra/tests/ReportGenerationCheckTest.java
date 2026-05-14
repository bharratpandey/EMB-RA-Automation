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
    private Page gmailPage;

    @BeforeAll
    static void setupBrowser() {
        DashboardManager.initReport();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(false)
        );
    }

    @BeforeEach
    void setup() {
        // Single context with Google session — both tabs share same window
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setStorageStatePath(Paths.get("auth/google_state.json")));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(true));

        // Tab 1 — Admin portal
        adminPage = context.newPage();
        adminPage.setDefaultTimeout(60000);
        adminPage.setDefaultNavigationTimeout(90000);

        // Tab 2 — Gmail (same window, same session)
        gmailPage = context.newPage();
        gmailPage.setDefaultTimeout(60000);
        gmailPage.setDefaultNavigationTimeout(90000);
    }

    @Test
    void testReportGenerationAndEmailVerification() {
        DashboardManager.startTest("Report Generation & Email Verification");
        DashboardManager.log("[REPORT] 🚀 Starting Report Generation Check...");

        ReportGenerationCheckPage reportPage = new ReportGenerationCheckPage(adminPage);

        // ── STEP 1: Login ──────────────────────────────────────────
        DashboardManager.log("\n[STEP 1] 🔑 Admin Login");
        reportPage.login();

        // ── STEP 2: Navigate to Requirement Listing ────────────────
        DashboardManager.log("\n[STEP 2] 📋 Navigate to Requirement Listing");
        reportPage.navigateToRequirementListing();

        // ── STEP 3: Open Search & Filters ─────────────────────────
        DashboardManager.log("\n[STEP 3] 🔍 Open Search & Filters");
        reportPage.openSearchFilters();

        // ── STEP 4: Click Download Reports & capture time ──────────
        DashboardManager.log("\n[STEP 4] 📥 Click Download Reports");
        String reportClickTime = reportPage.clickDownloadReports();

        // Save trace checkpoint
        try {
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("target/report-generation-admin-trace.zip")));
            DashboardManager.log("   💾 Trace saved → target/report-generation-admin-trace.zip");
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save trace: " + e.getMessage());
        }

        // ── STEP 5: Switch to Gmail tab & verify report email ──────
        DashboardManager.log("\n[STEP 5] 📧 Open Gmail & Verify Report Email");
        reportPage.openGmailAndVerifyReportEmail(gmailPage, reportClickTime);

        DashboardManager.log("\n[REPORT] ✅ Report Generation Check Completed!");
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            try {
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/report-generation-fallback-trace.zip")));
            } catch (Exception ignored) {}
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        EmailSender.sendDashboardEmail(
                "🚀 EMB Automation: Report Generation Check",
                "bharatpandey011@gmail.com",
                "bharat.pandey@emb.global","saumya.gupta@emb.global"
        );
    }
}