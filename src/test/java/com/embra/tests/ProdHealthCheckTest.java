package com.embra.tests;

import com.embra.pages.ProdHealthCheckPage;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProdHealthCheckTest
 *
 * Combined Browser UI + API sanity check — full Production Admin portal flow.
 *
 * Each step runs simultaneously:
 *   Browser — real Chrome window (setHeadless=false), watch it live
 *   API     — direct HTTP, captures status code + response time + body
 *
 * Production:
 *   UI  : https://admin.embtalent.ai
 *   API : https://api-ra.embtalent.ai
 *
 * 17-step complete flow:
 *   Step 1  — Login
 *   Step 2  — Overview Tab
 *   Step 3  — Candidates Tab
 *   Step 4  — Clients Tab (Dashboard)
 *   Step 5  — Partners Tab
 *   Step 6  — Revenue Tab
 *   Step 7  — New Registrations
 *   Step 8  — Partner Listing (/vendor)
 *   Step 9  — Partner Search
 *   Step 10 — Bench Listing (/bench-details)
 *   Step 11 — Client Listing (/clients-listing)
 *   Step 12 — Requirement Listing (/hiring-requests)
 *   Step 13 — New Requirement (/client)
 *   Step 14 — Intervue.io (/intervue)
 *   Step 15 — Currency Rates (/currency-rates)
 *   Step 16 — EMB Convertor (/jdconvertor)
 *   Step 17 — Users (/users)
 *
 * Run: mvn test -Dtest=ProdHealthCheckTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProdHealthCheckTest {

    // ------------------------------------------------------------------ //
    //  Shared state
    // ------------------------------------------------------------------ //

    private static Playwright          playwright;
    private static Browser             browser;
    private static BrowserContext      context;
    private static Page                adminPage;
    private static APIRequestContext   apiContext;
    private static ProdHealthCheckPage healthPage;

    // ------------------------------------------------------------------ //
    //  Credentials & environment
    // ------------------------------------------------------------------ //

    private static final String  ADMIN_EMAIL    = "bharat.pandey@emb.global";
    private static final String  ADMIN_PASSWORD = "Emb@1234";
    private static final boolean USE_UAT        = false;  // false = Production

    /** Extracted at Step 1 — shared by all subsequent API calls */
    private static String bearerToken;

    // ------------------------------------------------------------------ //
    //  @BeforeAll — launch Chrome + API context
    // ------------------------------------------------------------------ //

    @BeforeAll
    static void setUp() {
        DashboardManager.initReport();
        DashboardManager.startTest("ProdHealthCheckTest");

        DashboardManager.log("Environment : " + (USE_UAT ? "UAT" : "Production"));
        DashboardManager.log("Admin email : " + ADMIN_EMAIL);
        DashboardManager.log("Password    : (masked for security)");
        DashboardManager.log("UI URL      : " + (USE_UAT
                ? ProdHealthCheckPage.UAT_ADMIN_UI
                : ProdHealthCheckPage.PROD_ADMIN_UI));
        DashboardManager.log("API URL     : " + (USE_UAT
                ? ProdHealthCheckPage.UAT_BASE_URL
                : ProdHealthCheckPage.PROD_BASE_URL));

        // Launch visible Chrome — setHeadless(false) so you watch it live
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setChannel("chrome")
                        .setHeadless(false)   // ← visible Chrome window
                        .setSlowMo(300)       // ← slight slow-mo so actions are watchable
        );

        context = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(1440, 900));

        // Start Playwright trace
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(true));

        adminPage = context.newPage();
        adminPage.setDefaultTimeout(30000);
        adminPage.setDefaultNavigationTimeout(60000);

        // API context — pure HTTP, no browser needed
        apiContext = playwright.request().newContext();

        healthPage = new ProdHealthCheckPage(adminPage, apiContext, USE_UAT);
        DashboardManager.log("✅ Chrome (visible) + APIRequestContext initialised");
    }

    // ------------------------------------------------------------------ //
    //  @AfterAll — save trace, flush report, send email
    // ------------------------------------------------------------------ //

    @AfterAll
    static void tearDown() {
        try {
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("target/prod-health-check-trace.zip")));
            DashboardManager.log("💾 Trace saved → target/prod-health-check-trace.zip");
        } catch (Exception e) {
            DashboardManager.log("⚠️  Trace save failed: " + e.getMessage());
        }

        try { if (apiContext != null) apiContext.dispose(); } catch (Exception ignored) {}
        try { if (context   != null) context.close();      } catch (Exception ignored) {}
        try { if (browser   != null) browser.close();      } catch (Exception ignored) {}
        try { if (playwright!= null) playwright.close();   } catch (Exception ignored) {}

        DashboardManager.flushReport();

        try {
            EmailSender.sendDashboardEmail(
                    "PROD HEALTH CHECK — FULL FLOW",
                    "bharat.pandey@emb.global",
                    "saumya.gupta@emb.global",
                    "ashish.mishra@emb.global",
                    "prakash@emb.global"
            );
        } catch (Exception e) {
            DashboardManager.log("⚠️  Email send failed: " + e.getMessage());
        }
    }

    // ================================================================== //
    //  STEP 1 — Login
    // ================================================================== //

    @Test @Order(1)
    @DisplayName("Step 1 — Login (Browser + POST /user/login → 201)")
    void testLogin() {
        section("STEP 1 — LOGIN");
        bearerToken = healthPage.loginBrowserAndApi(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertNotNull(bearerToken, "Login failed — bearer token is null.");
        assertFalse(bearerToken.isEmpty(), "Bearer token is empty after login.");
        DashboardManager.log("✅ Step 1 PASSED");
    }

    // ================================================================== //
    //  STEP 2 — Overview Tab
    // ================================================================== //

    @Test @Order(2)
    @DisplayName("Step 2 — Overview Tab (Browser + GET /overview → 200)")
    void testOverview() {
        requireToken();
        section("STEP 2 — OVERVIEW TAB");
        healthPage.overviewBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 2 PASSED");
    }

    // ================================================================== //
    //  STEP 3 — Candidates Tab
    // ================================================================== //

    @Test @Order(3)
    @DisplayName("Step 3 — Candidates Tab (Browser + POST /candidate_analytics → 200)")
    void testCandidatesTab() {
        requireToken();
        section("STEP 3 — CANDIDATES TAB");
        healthPage.candidatesBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 3 PASSED");
    }

    // ================================================================== //
    //  STEP 4 — Clients Tab (Dashboard)
    // ================================================================== //

    @Test @Order(4)
    @DisplayName("Step 4 — Clients Tab Dashboard (Browser + POST /client_analytics → 200)")
    void testClientsDashboard() {
        requireToken();
        section("STEP 4 — CLIENTS TAB (DASHBOARD)");
        healthPage.clientsDashboardBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 4 PASSED");
    }

    // ================================================================== //
    //  STEP 5 — Partners Tab
    // ================================================================== //

    @Test @Order(5)
    @DisplayName("Step 5 — Partners Tab (Browser + POST /partner_analytics → 200)")
    void testPartnersTab() {
        requireToken();
        section("STEP 5 — PARTNERS TAB");
        healthPage.partnersBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 5 PASSED");
    }

    // ================================================================== //
    //  STEP 6 — Revenue Tab
    // ================================================================== //

    /*@Test @Order(6)
    @DisplayName("Step 6 — Revenue Tab (Browser + POST /revenue analytics → 200)")
    void testRevenueTab() {
        requireToken();
        section("STEP 6 — REVENUE TAB");
        healthPage.revenueBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 6 PASSED");
    }*/

    // ================================================================== //
    //  STEP 7 — New Registrations
    // ================================================================== //

    @Test @Order(7)
    @DisplayName("Step 7 — New Registrations (Browser + POST /pending_vendors → 200)")
    void testNewRegistrations() {
        requireToken();
        section("STEP 7 — NEW REGISTRATIONS");
        healthPage.newRegistrationsBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 7 PASSED");
    }

    // ================================================================== //
    //  STEP 8 — Partner Listing
    // ================================================================== //

    @Test @Order(8)
    @DisplayName("Step 8 — Partner Listing /vendor (Browser + 6 APIs → 200)")
    void testPartnerListing() {
        requireToken();
        section("STEP 8 — PARTNER LISTING (/vendor)");
        healthPage.partnerListingBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 8 PASSED");
    }

    // ================================================================== //
    //  STEP 9 — Partner Search
    // ================================================================== //

    @Test @Order(9)
    @DisplayName("Step 9 — Partner Search 'bharat pvt ltd' (Browser + POST /all?search_term → 200)")
    void testPartnerSearch() {
        requireToken();
        section("STEP 9 — PARTNER SEARCH");
        healthPage.partnerSearchBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 9 PASSED");
    }

    // ================================================================== //
    //  STEP 10 — Bench Listing
    // ================================================================== //

    @Test @Order(10)
    @DisplayName("Step 10 — Bench Listing /bench-details (Browser + 7 APIs → 200)")
    void testBenchListing() {
        requireToken();
        section("STEP 10 — BENCH LISTING (/bench-details)");
        healthPage.benchListingBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 10 PASSED");
    }

    // ================================================================== //
    //  STEP 11 — Client Listing
    // ================================================================== //

    @Test @Order(11)
    @DisplayName("Step 11 — Client Listing /clients-listing (Browser + 2 APIs → 200)")
    void testClientListing() {
        requireToken();
        section("STEP 11 — CLIENT LISTING (/clients-listing)");
        healthPage.clientListingBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 11 PASSED");
    }

    // ================================================================== //
    //  STEP 12 — Requirement Listing
    // ================================================================== //

    @Test @Order(12)
    @DisplayName("Step 12 — Requirement Listing /hiring-requests (Browser + 4 APIs → 200)")
    void testRequirementListing() {
        requireToken();
        section("STEP 12 — REQUIREMENT LISTING (/hiring-requests)");
        healthPage.requirementListingBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 12 PASSED");
    }

    // ================================================================== //
    //  STEP 13 — New Requirement
    // ================================================================== //

    @Test @Order(13)
    @DisplayName("Step 13 — New Requirement /client (Browser + 2 APIs → 200)")
    void testNewRequirement() {
        requireToken();
        section("STEP 13 — NEW REQUIREMENT (/client)");
        healthPage.newRequirementBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 13 PASSED");
    }

    // ================================================================== //
    //  STEP 14 — Intervue.io
    // ================================================================== //

    @Test @Order(14)
    @DisplayName("Step 14 — Intervue.io /intervue (Browser + 2 APIs → 200)")
    void testIntervue() {
        requireToken();
        section("STEP 14 — INTERVUE.IO (/intervue)");
        healthPage.intervueBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 14 PASSED");
    }

    // ================================================================== //
    //  STEP 15 — Currency Rates
    // ================================================================== //

    @Test @Order(15)
    @DisplayName("Step 15 — Currency Rates /currency-rates (Browser + 2 APIs → 200)")
    void testCurrencyRates() {
        requireToken();
        section("STEP 15 — CURRENCY RATES (/currency-rates)");
        healthPage.currencyRatesBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 15 PASSED");
    }

    // ================================================================== //
    //  STEP 16 — EMB Convertor
    // ================================================================== //

    @Test @Order(16)
    @DisplayName("Step 16 — EMB Convertor /jdconvertor (Browser + page load check → 200)")
    void testEmbConvertor() {
        requireToken();
        section("STEP 16 — EMB CONVERTOR (/jdconvertor)");
        healthPage.embConvertorBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 16 PASSED");
    }

    // ================================================================== //
    //  STEP 17 — Users
    // ================================================================== //

    @Test @Order(17)
    @DisplayName("Step 17 — Users /users (Browser refresh + GET /user/all → 200)")
    void testUsers() {
        requireToken();
        section("STEP 17 — USERS (/users)");
        healthPage.usersBrowserAndApi(bearerToken);
        DashboardManager.log("✅ Step 17 PASSED");
    }

    // ================================================================== //
    //  Helpers
    // ================================================================== //

    private void section(String title) {
        DashboardManager.log("\n==============================");
        DashboardManager.log(title);
        DashboardManager.log("==============================");
    }

    private void requireToken() {
        assertNotNull(bearerToken,
                "Bearer token is null — Step 1 (login) must have failed. Cannot proceed.");
    }
}