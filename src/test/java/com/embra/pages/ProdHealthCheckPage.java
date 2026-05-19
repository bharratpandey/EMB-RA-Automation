package com.embra.pages;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.RequestOptions;
import com.embra.utils.DashboardManager;

import java.util.function.Supplier;

/**
 * ProdHealthCheckPage
 *
 * Combined Browser UI + API page object for the Production Admin portal health check.
 *
 * Every step does TWO things:
 *   1. Browser action  — real Chrome navigates / clicks / verifies UI
 *   2. API call        — direct HTTP validates response + timing + status
 *
 * Production URLs:
 *   UI  : https://admin.embtalent.ai
 *   API : https://api-ra.embtalent.ai
 *
 * Full flow:
 *   Step 1  — Login
 *   Step 2  — Overview Tab
 *   Step 3  — Candidates Tab
 *   Step 4  — Clients Tab (Dashboard)
 *   Step 5  — Partners Tab
 *   Step 6  — Revenue Tab
 *   Step 7  — New Registrations
 *   Step 8  — Partner Listing  (/vendor)
 *   Step 9  — Partner Search
 *   Step 10 — Bench Listing    (/bench-details)
 *   Step 11 — Client Listing   (/clients-listing)
 *   Step 12 — Requirement Listing (/hiring-requests)
 *   Step 13 — New Requirement  (/client)
 *   Step 14 — Intervue.io      (/intervue)
 *   Step 15 — Currency Rates   (/currency-rates)
 *   Step 16 — EMB Convertor    (/jdconvertor)
 *   Step 17 — Users            (/users)
 *
 * Expected HTTP status codes:
 *   Login POST → 201 Created
 *   All others → 200 OK
 *
 * Response-time thresholds:
 *   0–300 ms   → ✅ Excellent/Instant
 *   301–800 ms → ⚠️  Acceptable
 *   > 800 ms   → 🔴 Slow
 */
public class ProdHealthCheckPage {

    // ------------------------------------------------------------------ //
    //  URL constants
    // ------------------------------------------------------------------ //

    public static final String PROD_BASE_URL = "https://api-ra.embtalent.ai";
    public static final String UAT_BASE_URL  = "https://uatapi-ra.embtalent.ai";
    public static final String PROD_ADMIN_UI = "https://admin.embtalent.ai";
    public static final String UAT_ADMIN_UI  = "https://uat-admin.embtalent.ai";

    private static final String START_DATE    = "2025-01-01";
    private static final String END_DATE      = "2026-05-18";
    private static final String CONTENT_JSON  = "application/json";
    private static final String ACCEPT_HEADER = "application/json, text/plain, */*";
    private static final String USER_AGENT    =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

    // ------------------------------------------------------------------ //
    //  Fields
    // ------------------------------------------------------------------ //

    private final Page              page;
    private final APIRequestContext request;
    private final String            baseUrl;
    private final String            adminUiUrl;
    private final String            origin;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public ProdHealthCheckPage(Page page, APIRequestContext request, boolean useUat) {
        this.page       = page;
        this.request    = request;
        this.baseUrl    = useUat ? UAT_BASE_URL  : PROD_BASE_URL;
        this.adminUiUrl = useUat ? UAT_ADMIN_UI  : PROD_ADMIN_UI;
        this.origin     = useUat ? UAT_ADMIN_UI  : PROD_ADMIN_UI;
    }

    // ------------------------------------------------------------------ //
    //  Shared helpers
    // ------------------------------------------------------------------ //

    private RequestOptions authenticated(String token) {
        return RequestOptions.create()
                .setHeader("Accept",          ACCEPT_HEADER)
                .setHeader("Accept-Language", "en-US,en;q=0.9")
                .setHeader("Content-Type",    CONTENT_JSON)
                .setHeader("Authorization",   "Bearer " + token)
                .setHeader("Origin",          origin)
                .setHeader("Referer",         origin + "/")
                .setHeader("Connection",      "keep-alive")
                .setHeader("User-Agent",      USER_AGENT);
    }

    private String analyticsBody() {
        return String.format(
                "{\"filter_type\":\"custom\",\"start_date\":\"%s\",\"end_date\":\"%s\"}",
                START_DATE, END_DATE);
    }

    /**
     * Many APIs wrap their payload inside a "data" object.
     * This helper extracts the inner object so logMetric() finds keys correctly.
     * Falls back to the full response if "data" key is not present.
     */
    private String unwrap(String json) {
        String inner = extractJsonString(json, "data");
        return (inner != null && !inner.isEmpty()) ? inner : json;
    }

    // ------------------------------------------------------------------ //
    //  Status logger
    // ------------------------------------------------------------------ //

    public void logStatus(String label, int actual, int expected) {
        if (actual == expected) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ✅ (expected %d)", label, actual, expected));
        } else if (actual >= 200 && actual < 300) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ⚠️  (expected %d — got different 2xx, still success)",
                    label, actual, expected));
        } else if (actual == 401) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ UNAUTHORIZED — token missing or expired", label, actual));
        } else if (actual == 403) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ FORBIDDEN — insufficient permissions", label, actual));
        } else if (actual == 404) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ NOT FOUND — endpoint may have changed", label, actual));
        } else if (actual == 422) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ UNPROCESSABLE — request body invalid", label, actual));
        } else if (actual == 500) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ SERVER ERROR — backend issue", label, actual));
        } else if (actual == 502 || actual == 503 || actual == 504) {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ SERVICE UNAVAILABLE — server down or overloaded",
                    label, actual));
        } else {
            DashboardManager.log(String.format(
                    "   [%s] Status : %d ❌ UNEXPECTED STATUS (expected %d)", label, actual, expected));
        }
    }

    public void logTiming(String label, long ms) {
        String bucket;
        if (ms <= 300)      bucket = "✅ Excellent/Instant (0–300 ms)";
        else if (ms <= 800) bucket = "⚠️  Acceptable (301–800 ms)";
        else                bucket = "🔴 Slow (> 800 ms)";
        DashboardManager.log(String.format(
                "   [%s] Response time : %d ms  →  %s", label, ms, bucket));
    }

    // ================================================================== //
    //  STEP 1 — LOGIN
    // ================================================================== //

    /**
     * Browser: navigate to login page, fill credentials, click Login.
     * API:     POST /api/v1/user/login → expected 201
     */
    public String loginBrowserAndApi(String email, String password) {
        DashboardManager.log("   [Browser] Navigating to " + adminUiUrl + "/login");
        page.navigate(adminUiUrl + "/login");
        page.waitForSelector("input#email, input[name='email']");
        page.fill("input#email, input[name='email']", email);
        page.fill("input#password, input[name='password']", password);
        DashboardManager.log("   [Browser] Credentials filled — clicking Login");
        page.click("button[type='submit']");
        page.waitForURL(url -> url.contains("/dashboard") || url.contains("/requirement"),
                new Page.WaitForURLOptions().setTimeout(15000));
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Dashboard visible — login confirmed in UI");

        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        long start = System.currentTimeMillis();
        APIResponse res = request.post(baseUrl + "/api/v1/user/login",
                RequestOptions.create()
                        .setHeader("Accept",          ACCEPT_HEADER)
                        .setHeader("Accept-Language", "en-US,en;q=0.9")
                        .setHeader("Content-Type",    CONTENT_JSON)
                        .setHeader("Origin",          origin)
                        .setHeader("Referer",         origin + "/")
                        .setHeader("Connection",      "keep-alive")
                        .setHeader("User-Agent",      USER_AGENT)
                        .setData(body));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("Login API", elapsed);
        logStatus("Login API", res.status(), 201);
        DashboardManager.log("   [Login API] Response : " + preview(res.text()));

        String token = extractJsonString(res.text(), "token");
        if (token == null || token.isEmpty()) token = extractJsonString(res.text(), "access_token");
        if (token != null && !token.isEmpty()) {
            DashboardManager.log("   [Login API] Token extracted (length=" + token.length() + ")");
        } else {
            DashboardManager.log("   [Login API] ❌ Token NOT found in response");
        }
        return token;
    }

    // ================================================================== //
    //  STEP 2 — OVERVIEW TAB
    // ================================================================== //

    /** Browser: click Overview tab. API: GET /overview → 200 */
    public void overviewBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Overview tab");
        page.click("button:has-text('Overview')");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Overview tab loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.get(
                baseUrl + "/api/v1/emb/vendor/profile/overview"
                        + "?filter_type=custom&start_date=" + START_DATE + "&end_date=" + END_DATE,
                authenticated(token));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("Overview API", elapsed);
        logStatus("Overview API", res.status(), 200);

        String src = unwrap(res.text());
        DashboardManager.log("   ┌────────────────────────────────────┐");
        DashboardManager.log("   │  OVERVIEW — CARD VALUES            │");
        DashboardManager.log("   ├────────────────────────────────────┤");
        logMetric(src, "total_clients",       "  Total Clients             ");
        logMetric(src, "total_candidates",    "  Total Candidates          ");
        logMetric(src, "total_partners",      "  Total Partners            ");
        logMetric(src, "total_requirements",  "  Total Requirements        ");
        logMetric(src, "active_requirements", "  Active Requirements       ");
        logMetric(src, "closed_requirements", "  Closed Requirements       ");
        logMetric(src, "deployed_candidates", "  Deployed Candidates       ");
        DashboardManager.log("   └────────────────────────────────────┘");
    }

    // ================================================================== //
    //  STEP 3 — CANDIDATES TAB
    // ================================================================== //

    /** Browser: click Candidates tab. API: POST /candidate_analytics → 200 */
    public void candidatesBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Candidates tab");
        page.click("button:has-text('Candidates')");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Candidates tab loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/vendor/bench_profile/candidate_analytics",
                authenticated(token).setData(analyticsBody()));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("CandidateAnalytics API", elapsed);
        logStatus("CandidateAnalytics API", res.status(), 200);
        DashboardManager.log("   [CandidateAnalytics] --- Data on Screen ---");
        DashboardManager.log("   " + preview(res.text()));
    }

    // ================================================================== //
    //  STEP 4 — CLIENTS TAB (Dashboard)
    // ================================================================== //

    /** Browser: click Clients tab. API: POST /client_analytics → 200 */
    public void clientsDashboardBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Clients tab");
        page.click("button:has-text('Clients')");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Clients tab loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/client/client_analytics",
                authenticated(token).setData(analyticsBody()));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("ClientAnalytics API", elapsed);
        logStatus("ClientAnalytics API", res.status(), 200);
        DashboardManager.log("   [ClientAnalytics] --- Data on Screen ---");
        DashboardManager.log("   " + preview(res.text()));
    }

    // ================================================================== //
    //  STEP 5 — PARTNERS TAB
    // ================================================================== //

    /** Browser: click Partners tab. API: POST /partner_analytics → 200 */
    public void partnersBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Partners tab");
        page.locator("button:has-text('Partners')").first().click();
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Partners tab loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/vendor/profile/partner_analytics",
                authenticated(token).setData(analyticsBody()));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("PartnerAnalytics API", elapsed);
        logStatus("PartnerAnalytics API", res.status(), 200);
        DashboardManager.log("   [PartnerAnalytics] --- Data on Screen ---");
        DashboardManager.log("   " + preview(res.text()));
    }

    // ================================================================== //
    //  STEP 6 — REVENUE TAB
    // ================================================================== //

    /** Browser: click Revenue tab. API: POST /requirement-revenue/analytics → 200 */
    public void revenueBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Revenue tab");
        page.click("button:has-text('Revenue')");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Revenue tab loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/requirement-revenue/analytics",
                authenticated(token).setData(analyticsBody()));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("RevenueAnalytics API", elapsed);
        logStatus("RevenueAnalytics API", res.status(), 200);
        DashboardManager.log("   [RevenueAnalytics] --- Data on Screen ---");
        DashboardManager.log("   " + preview(res.text()));
    }

    // ================================================================== //
    //  STEP 7 — NEW REGISTRATIONS
    // ================================================================== //

    /** Browser: click New Registrations sidebar. API: POST /pending_vendors → 200 */
    public void newRegistrationsBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking New Registrations sidebar link");
        page.click("a[href='/pending-approvals']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ New Registrations page loaded");

        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/vendor/profile/pending_vendors"
                        + "?search_term=&limit=25&page=1&report=false",
                authenticated(token)
                        .setData("{\"statusFilter\":\"\",\"startDate\":\"\",\"endDate\":\"\"}"));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("PendingVendors API", elapsed);
        logStatus("PendingVendors API", res.status(), 200);
        String rb = res.text();
        boolean hasData = rb.contains("\"data\"") && !rb.contains("\"data\":[]");
        DashboardManager.log("   [PendingVendors] Listing has data : "
                + (hasData ? "✅ YES" : "⚠️  Empty / no pending vendors"));
    }

    // ================================================================== //
    //  STEP 8 — PARTNER LISTING (/vendor)
    // ================================================================== //

    /**
     * Browser: click Partner Listing sidebar.
     * API: GET tech_skills, counts (print card values), timezones, locations,
     *      POST all-vendors, GET currency
     * Prints: All Partners, Active Partners, Rejected Partners, Suspended Partners
     */
    public void partnerListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Partner Listing sidebar link");
        page.click("a[href='/vendor']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Partner Listing page loaded");

        String vendorBody = "{\"statusFilter\":\"active\",\"engagementType\":null,"
                + "\"hiring_service\":null,\"currency\":null,"
                + "\"startDate\":\"\",\"endDate\":\"\"}";

        DashboardManager.log("   --- 8a. GET get_tech_skills ---");
        timedCall("TechSkills", () -> request.get(
                baseUrl + "/api/v1/utility/get_tech_skills", authenticated(token)), 200, false);

        DashboardManager.log("   --- 8b. GET vendor/profile/counts ---");
        APIResponse vendorCounts = timedCall("VendorCounts", () -> request.get(
                baseUrl + "/api/v1/emb/vendor/profile/counts", authenticated(token)), 200, true);
        if (vendorCounts != null) {
            String src = unwrap(vendorCounts.text());
            DashboardManager.log("   ┌────────────────────────────────────┐");
            DashboardManager.log("   │  PARTNER LISTING — CARD VALUES     │");
            DashboardManager.log("   ├────────────────────────────────────┤");
            logMetric(src, "all_partners",       "  All Partners              ");
            logMetric(src, "active_partners",    "  Active Partners           ");
            logMetric(src, "rejected_partners",  "  Rejected Partners         ");
            logMetric(src, "suspended_partners", "  Suspended Partners        ");
            logMetric(src, "all",                "  All (fallback)            ");
            logMetric(src, "active",             "  Active (fallback)         ");
            logMetric(src, "rejected",           "  Rejected (fallback)       ");
            logMetric(src, "suspended",          "  Suspended (fallback)      ");
            DashboardManager.log("   └────────────────────────────────────┘");
        }

        DashboardManager.log("   --- 8c. GET get_preferred_timezones ---");
        timedCall("Timezones", () -> request.get(
                baseUrl + "/api/v1/utility/get_preferred_timezones", authenticated(token)), 200, false);

        DashboardManager.log("   --- 8d. GET preferred_location ---");
        timedCall("Locations", () -> request.get(
                baseUrl + "/api/v1/geo/preferred_location", authenticated(token)), 200, false);

        DashboardManager.log("   --- 8e. POST vendor/profile/all (default listing) ---");
        APIResponse allVendors = timedCall("AllVendors", () -> request.post(
                baseUrl + "/api/v1/emb/vendor/profile/all?search_term=&limit=25&page=1&report=false",
                authenticated(token).setData(vendorBody)), 200, true);
        if (allVendors != null) {
            DashboardManager.log("   [AllVendors] --- Data on UI ---");
            DashboardManager.log("   " + preview(allVendors.text()));
        }

        DashboardManager.log("   --- 8f. GET currency/values ---");
        APIResponse currency = timedCall("CurrencyValues", () -> request.get(
                baseUrl + "/api/v1/emb/currency/values", authenticated(token)), 200, true);
        if (currency != null) {
            DashboardManager.log("   [CurrencyValues] --- Data on UI ---");
            DashboardManager.log("   " + preview(currency.text()));
        }
    }

    // ================================================================== //
    //  STEP 9 — PARTNER SEARCH
    // ================================================================== //

    /** Browser: type "bharat pvt ltd". API: POST /all?search_term → 200 */
    public void partnerSearchBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Opening Search & Filters panel");
        try {
            page.locator("div.font-semibold:has-text('Search')").first().click();
            page.waitForTimeout(600);
        } catch (Exception e) {
            DashboardManager.log("   [Browser] ⚠️  Search toggle not found — trying input directly");
        }
        DashboardManager.log("   [Browser] Typing: bharat pvt ltd");
        try {
            page.fill("input[placeholder*='Search by name'], input[placeholder*='Search']",
                    "bharat pvt ltd");
            page.waitForTimeout(2000);
            DashboardManager.log("   [Browser] ✅ Search results updated");
        } catch (Exception e) {
            DashboardManager.log("   [Browser] ⚠️  Search input not found: " + e.getMessage());
        }

        String body = "{\"statusFilter\":\"active\",\"engagementType\":null,"
                + "\"hiring_service\":null,\"currency\":null,"
                + "\"startDate\":\"\",\"endDate\":\"\"}";
        long start = System.currentTimeMillis();
        APIResponse res = request.post(
                baseUrl + "/api/v1/emb/vendor/profile/all"
                        + "?search_term=bharat%20pvt%20ltd&limit=25&page=1&report=false",
                authenticated(token).setData(body));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("VendorSearch API", elapsed);
        logStatus("VendorSearch API", res.status(), 200);
        DashboardManager.log("   [VendorSearch] --- Data on UI (search results) ---");
        DashboardManager.log("   " + preview(res.text()));
    }

    // ================================================================== //
    //  STEP 10 — BENCH LISTING (/bench-details)
    // ================================================================== //

    /**
     * Browser: click Bench Listing sidebar.
     * Prints: Total Resources, Available Resources, Onsite Resources, Remote Resources
     */
    public void benchListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Bench Listing sidebar link");
        page.click("a[href='/bench-details']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Bench Listing page loaded");

        DashboardManager.log("   --- 10a. GET get_tech_skills ---");
        timedCall("TechSkills", () -> request.get(
                baseUrl + "/api/v1/utility/get_tech_skills", authenticated(token)), 200, false);

        DashboardManager.log("   --- 10b. GET vendor/profile/counts ---");
        timedCall("VendorCounts", () -> request.get(
                baseUrl + "/api/v1/emb/vendor/profile/counts", authenticated(token)), 200, false);

        DashboardManager.log("   --- 10c. GET get_preferred_timezones ---");
        timedCall("Timezones", () -> request.get(
                baseUrl + "/api/v1/utility/get_preferred_timezones", authenticated(token)), 200, false);

        DashboardManager.log("   --- 10d. GET preferred_location ---");
        timedCall("Locations", () -> request.get(
                baseUrl + "/api/v1/geo/preferred_location", authenticated(token)), 200, false);

        DashboardManager.log("   --- 10e. POST vendor/profile/all ---");
        String vendorBody = "{\"statusFilter\":\"active\",\"engagementType\":null,"
                + "\"hiring_service\":null,\"currency\":null,"
                + "\"startDate\":\"\",\"endDate\":\"\"}";
        APIResponse allVendors = timedCall("AllVendors", () -> request.post(
                baseUrl + "/api/v1/emb/vendor/profile/all?search_term=&limit=25&page=1&report=false",
                authenticated(token).setData(vendorBody)), 200, true);
        if (allVendors != null) {
            DashboardManager.log("   [AllVendors] --- Data on UI ---");
            DashboardManager.log("   " + preview(allVendors.text()));
        }

        DashboardManager.log("   --- 10f. GET bench_profile/counts ---");
        APIResponse benchCounts = timedCall("BenchCounts", () -> request.get(
                baseUrl + "/api/v1/emb/vendor/bench_profile/counts", authenticated(token)), 200, true);
        if (benchCounts != null) {
            String src = unwrap(benchCounts.text());
            DashboardManager.log("   ┌────────────────────────────────────┐");
            DashboardManager.log("   │  BENCH LISTING — CARD VALUES       │");
            DashboardManager.log("   ├────────────────────────────────────┤");
            logMetric(src, "total_resources",     "  Total Resources           ");
            logMetric(src, "available_resources", "  Available Resources       ");
            logMetric(src, "onsite_resources",    "  Onsite Resources          ");
            logMetric(src, "remote_resources",    "  Remote Resources          ");
            logMetric(src, "total",               "  Total (fallback)          ");
            logMetric(src, "available",           "  Available (fallback)      ");
            logMetric(src, "onsite",              "  Onsite (fallback)         ");
            logMetric(src, "remote",              "  Remote (fallback)         ");
            DashboardManager.log("   └────────────────────────────────────┘");
        }

        DashboardManager.log("   --- 10g. POST bench_profile/all ---");
        APIResponse benchAll = timedCall("BenchAll", () -> request.post(
                baseUrl + "/api/v1/emb/vendor/bench_profile/all?search_term=&limit=25&page=1&report=false",
                authenticated(token).setData("{}")), 200, true);
        if (benchAll != null) {
            DashboardManager.log("   [BenchAll] --- Data on UI ---");
            DashboardManager.log("   " + preview(benchAll.text()));
        }
    }

    // ================================================================== //
    //  STEP 11 — CLIENT LISTING (/clients-listing)
    // ================================================================== //

    /**
     * Browser: click Client Listing sidebar.
     * Prints: All Clients, Active Clients, New Clients, Suspended Clients
     */
    public void clientListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Client Listing sidebar link");
        page.click("a[href='/clients-listing']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Client Listing page loaded");

        DashboardManager.log("   --- 11a. GET /emb/client/res/counts ---");
        APIResponse counts = timedCall("ClientCounts", () -> request.get(
                baseUrl + "/api/v1/emb/client/res/counts", authenticated(token)), 200, true);
        if (counts != null) {
            String src = unwrap(counts.text());
            DashboardManager.log("   ┌────────────────────────────────────┐");
            DashboardManager.log("   │  CLIENT LISTING — CARD VALUES      │");
            DashboardManager.log("   ├────────────────────────────────────┤");
            logMetric(src, "all_clients",       "  All Clients               ");
            logMetric(src, "active_clients",    "  Active Clients            ");
            logMetric(src, "new_clients",       "  New Clients               ");
            logMetric(src, "suspended_clients", "  Suspended Clients         ");
            logMetric(src, "total",             "  Total (fallback)          ");
            logMetric(src, "active",            "  Active (fallback)         ");
            logMetric(src, "new",               "  New (fallback)            ");
            logMetric(src, "suspended",         "  Suspended (fallback)      ");
            DashboardManager.log("   └────────────────────────────────────┘");
        }

        DashboardManager.log("   --- 11b. POST /emb/client/all ---");
        APIResponse clientAll = timedCall("ClientAll", () -> request.post(
                        baseUrl + "/api/v1/emb/client/all?search_term=&limit=25&page=1&report=false",
                        authenticated(token).setData("{\"statusFilter\":\"\",\"startDate\":\"\",\"endDate\":\"\"}")),
                200, true);
        if (clientAll != null) {
            DashboardManager.log("   [ClientAll] --- Data on UI ---");
            DashboardManager.log("   " + preview(clientAll.text()));
        }
    }

    // ================================================================== //
    //  STEP 12 — REQUIREMENT LISTING (/hiring-requests)
    // ================================================================== //

    /**
     * Browser: click Requirement Listing sidebar.
     * Prints: Total Requirements, Active Requirements, Closed Requirements, On-Hold Requirements
     *
     * Confirmed API key names from raw response:
     *   all_requirements     → Total Requirements
     *   ongoing_requirements → Active Requirements
     *   complete_requirements→ Closed Requirements
     *   onhold_requirements  → On-Hold Requirements
     */
    public void requirementListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Requirement Listing sidebar link");
        page.click("a[href='/hiring-requests']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Requirement Listing page loaded");

        DashboardManager.log("   --- 12a. POST /emb/requirement/all ---");
        APIResponse reqAll = timedCall("RequirementAll", () -> request.post(
                        baseUrl + "/api/v1/emb/requirement/all?search_term=&limit=25&page=1&report=false",
                        authenticated(token).setData("{\"statusFilter\":\"\",\"startDate\":\"\",\"endDate\":\"\"}")),
                200, true);
        if (reqAll != null) {
            DashboardManager.log("   [RequirementAll] --- Data on UI ---");
            DashboardManager.log("   " + preview(reqAll.text()));
        }

        DashboardManager.log("   --- 12b. GET get_preferred_timezones ---");
        timedCall("Timezones", () -> request.get(
                baseUrl + "/api/v1/utility/get_preferred_timezones", authenticated(token)), 200, false);

        DashboardManager.log("   --- 12c. GET preferred_location ---");
        timedCall("Locations", () -> request.get(
                baseUrl + "/api/v1/geo/preferred_location", authenticated(token)), 200, false);

        DashboardManager.log("   --- 12d. GET /emb/requirement/status-counts ---");
        APIResponse statusCounts = timedCall("RequirementStatusCounts", () -> request.get(
                baseUrl + "/api/v1/emb/requirement/status-counts", authenticated(token)), 200, true);
        if (statusCounts != null) {
            // Keys confirmed from raw API response — nested under "data"
            String src = unwrap(statusCounts.text());
            DashboardManager.log("   ┌──────────────────────────────────────┐");
            DashboardManager.log("   │  REQUIREMENT LISTING — CARD VALUES   │");
            DashboardManager.log("   ├──────────────────────────────────────┤");
            logMetric(src, "all_requirements",      "  Total Requirements        ");
            logMetric(src, "ongoing_requirements",  "  Active Requirements       ");
            logMetric(src, "complete_requirements", "  Closed Requirements       ");
            logMetric(src, "onhold_requirements",   "  On-Hold Requirements      ");
            DashboardManager.log("   └──────────────────────────────────────┘");
        }
    }

    // ================================================================== //
    //  STEP 13 — NEW REQUIREMENT (/client)
    // ================================================================== //

    /** Browser: click New Requirement sidebar. API: currency + timezones → 200 */
    public void newRequirementBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking New Requirement sidebar link");
        page.click("a[href='/client']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ New Requirement page loaded");

        DashboardManager.log("   --- 13a. GET /emb/currency/values ---");
        timedCall("CurrencyValues", () -> request.get(
                baseUrl + "/api/v1/emb/currency/values", authenticated(token)), 200, false);

        DashboardManager.log("   --- 13b. GET get_preferred_timezones ---");
        timedCall("Timezones", () -> request.get(
                baseUrl + "/api/v1/utility/get_preferred_timezones", authenticated(token)), 200, false);
    }

    // ================================================================== //
    //  STEP 14 — INTERVUE.IO (/intervue)
    // ================================================================== //

    /**
     * Browser: click Intervue.io sidebar.
     * Prints: Total Assessments, Completed, Active, Cancelled
     */
    public void intervueBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Intervue.io sidebar link");
        page.click("a[href='/intervue']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Intervue.io page loaded");

        DashboardManager.log("   --- 14a. GET /emb/intervue/intervue-overview ---");
        APIResponse overview = timedCall("IntervueOverview", () -> request.get(
                baseUrl + "/api/v1/emb/intervue/intervue-overview", authenticated(token)), 200, true);
        if (overview != null) {
            String src = unwrap(overview.text());
            DashboardManager.log("   ┌────────────────────────────────────┐");
            DashboardManager.log("   │  INTERVUE.IO — CARD VALUES         │");
            DashboardManager.log("   ├────────────────────────────────────┤");
            logMetric(src, "total_assessments",     "  Total Assessments         ");
            logMetric(src, "completed_assessments", "  Completed Assessments     ");
            logMetric(src, "active_assessments",    "  Active Assessments        ");
            logMetric(src, "cancelled_assessments", "  Cancelled Assessments     ");
            // fallback keys
            logMetric(src, "total",      "  Total (fallback)          ");
            logMetric(src, "completed",  "  Completed (fallback)      ");
            logMetric(src, "active",     "  Active (fallback)         ");
            logMetric(src, "cancelled",  "  Cancelled (fallback)      ");
            logMetric(src, "cancel",     "  Cancel (alt key)          ");
            DashboardManager.log("   └────────────────────────────────────┘");
        }

        DashboardManager.log("   --- 14b. POST /emb/intervue/all ---");
        APIResponse intervueAll = timedCall("IntervueAll", () -> request.post(
                baseUrl + "/api/v1/emb/intervue/all?search_term=&limit=25&page=1",
                authenticated(token).setData("{}")), 200, true);
        if (intervueAll != null) {
            DashboardManager.log("   [IntervueAll] --- Data on UI ---");
            DashboardManager.log("   " + preview(intervueAll.text()));
        }
    }

    // ================================================================== //
    //  STEP 15 — CURRENCY RATES (/currency-rates)
    // ================================================================== //

    /** Browser: click Currency Rates sidebar. API: currency/values + exchange-rate/all → 200 */
    public void currencyRatesBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Currency Rates sidebar link");
        page.click("a[href='/currency-rates']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Currency Rates page loaded");

        DashboardManager.log("   --- 15a. GET /emb/currency/values ---");
        timedCall("CurrencyValues", () -> request.get(
                baseUrl + "/api/v1/emb/currency/values", authenticated(token)), 200, false);

        DashboardManager.log("   --- 15b. GET /emb/exchange-rate/all ---");
        APIResponse exchangeRates = timedCall("ExchangeRates", () -> request.get(
                baseUrl + "/api/v1/emb/exchange-rate/all", authenticated(token)), 200, true);
        if (exchangeRates != null) {
            DashboardManager.log("   [ExchangeRates] --- Data on UI ---");
            DashboardManager.log("   " + preview(exchangeRates.text()));
        }
    }

    // ================================================================== //
    //  STEP 16 — EMB CONVERTOR (/jdconvertor)
    // ================================================================== //

    /** Browser: click EMB Convertor sidebar. API: page load check → 200 */
    public void embConvertorBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking EMB Convertor sidebar link");
        page.click("a[href='/jdconvertor']");
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ EMB Convertor page loaded");

        DashboardManager.log("   --- 16a. GET /jdconvertor?_rsc=ass8g (page load check) ---");
        long start = System.currentTimeMillis();
        APIResponse res = request.get(
                adminUiUrl + "/jdconvertor?_rsc=ass8g",
                RequestOptions.create()
                        .setHeader("Accept",          ACCEPT_HEADER)
                        .setHeader("Accept-Language", "en-US,en;q=0.9")
                        .setHeader("Origin",          origin)
                        .setHeader("Referer",         origin + "/")
                        .setHeader("Connection",      "keep-alive")
                        .setHeader("User-Agent",      USER_AGENT));
        long elapsed = System.currentTimeMillis() - start;
        logTiming("EMBConvertor PageLoad", elapsed);
        logStatus("EMBConvertor PageLoad", res.status(), 200);
    }

    // ================================================================== //
    //  STEP 17 — USERS (/users)
    // ================================================================== //

    /** Browser: click Users sidebar + refresh. API: GET /user/all → 200 */
    public void usersBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Users sidebar link");
        page.click("a[href='/users']");
        page.waitForTimeout(1500);
        DashboardManager.log("   [Browser] Refreshing page (as per sanity script)");
        page.reload();
        page.waitForTimeout(2000);
        DashboardManager.log("   [Browser] ✅ Users page loaded after refresh");

        DashboardManager.log("   --- 17a. GET /api/v1/user/all ---");
        APIResponse users = timedCall("UserAll", () -> request.get(
                baseUrl + "/api/v1/user/all", authenticated(token)), 200, true);
        if (users != null) {
            DashboardManager.log("   [UserAll] --- Data on UI ---");
            DashboardManager.log("   " + preview(users.text()));
        }
    }

    // ================================================================== //
    //  Internal helpers
    // ================================================================== //

    private APIResponse timedCall(String label, Supplier<APIResponse> supplier,
                                  int expectedStatus, boolean ret) {
        long start = System.currentTimeMillis();
        APIResponse res = supplier.get();
        long elapsed = System.currentTimeMillis() - start;
        logTiming(label, elapsed);
        logStatus(label, res.status(), expectedStatus);
        return ret ? res : null;
    }

    private void logMetric(String json, String key, String label) {
        String value = extractJsonString(json, key);
        if (value != null) DashboardManager.log(
                String.format("      %-32s : %s", label, value));
    }

    private String preview(String body) {
        if (body == null) return "(null)";
        return body.length() > 600 ? body.substring(0, 600) + " …[truncated]" : body;
    }

    public String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int valStart = colon + 1;
        while (valStart < json.length()
                && Character.isWhitespace(json.charAt(valStart))) valStart++;
        if (valStart >= json.length()) return null;
        if (json.charAt(valStart) == '"') {
            int valEnd = json.indexOf("\"", valStart + 1);
            return valEnd == -1 ? null : json.substring(valStart + 1, valEnd);
        }
        // Handle nested object — return the raw substring between { }
        if (json.charAt(valStart) == '{') {
            int depth = 0;
            int end = valStart;
            while (end < json.length()) {
                if (json.charAt(end) == '{') depth++;
                else if (json.charAt(end) == '}') { depth--; if (depth == 0) break; }
                end++;
            }
            return json.substring(valStart, end + 1);
        }
        int valEnd = json.indexOf(",", valStart);
        if (valEnd == -1) valEnd = json.indexOf("}", valStart);
        return valEnd == -1 ? null : json.substring(valStart, valEnd).trim();
    }
}