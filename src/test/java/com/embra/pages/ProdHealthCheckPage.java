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
     * Unwraps the "data" object from API responses.
     * Falls back to full JSON if no "data" wrapper exists.
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

    public String loginBrowserAndApi(String email, String password) {
        DashboardManager.log("   [Browser] Navigating to " + adminUiUrl + "/login");
        DashboardManager.log("   [URL] " + adminUiUrl + "/login");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/user/login");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void overviewBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Overview tab");
        DashboardManager.log("   [URL] " + adminUiUrl + "/dashboard");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/vendor/profile/overview"
                + "?filter_type=custom&start_date=" + START_DATE + "&end_date=" + END_DATE);
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void candidatesBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Candidates tab");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/bench_profile/candidate_analytics");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void clientsDashboardBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Clients tab");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/client/client_analytics");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void partnersBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Partners tab");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/profile/partner_analytics");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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
    //  STEP 6 — REVENUE TAB (kept for future use)
    // ================================================================== //

    public void revenueBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Revenue tab");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/requirement-revenue/analytics");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void newRegistrationsBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking New Registrations sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/pending-approvals");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/profile/pending_vendors"
                + "?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void partnerListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Partner Listing sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/vendor");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_tech_skills");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/vendor/profile/counts");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_preferred_timezones");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/geo/preferred_location");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/profile/all?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/currency/values");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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
            // DEBUG — print raw response to identify exact key names
            DashboardManager.log("   [RAW VendorCounts] " + vendorCounts.text());
            String src = unwrap(vendorCounts.text());
            DashboardManager.log("   ┌────────────────────────────────────┐");
            DashboardManager.log("   │  PARTNER LISTING — CARD VALUES     │");
            DashboardManager.log("   ├────────────────────────────────────┤");
            logMetric(src, "total_vendors",     "  All Partners              ");
            logMetric(src, "approved_vendors",  "  Active Partners           ");
            logMetric(src, "rejected_vendors",  "  Rejected Partners         ");
            logMetric(src, "suspended_vendors", "  Suspended Partners        ");
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

    public void partnerSearchBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Opening Search & Filters panel");
        DashboardManager.log("   [URL] " + adminUiUrl + "/vendor (search)");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/profile/all?search_term=bharat%20pvt%20ltd&limit=25&page=1&report=false");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void benchListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Bench Listing sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/bench-details");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_tech_skills");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/vendor/profile/counts");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_preferred_timezones");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/geo/preferred_location");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/profile/all?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/vendor/bench_profile/counts");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/vendor/bench_profile/all?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void clientListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Client Listing sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/clients-listing");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/client/res/counts");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/client/all?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void requirementListingBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Requirement Listing sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/hiring-requests");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/requirement/all?search_term=&limit=25&page=1&report=false");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_preferred_timezones");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/geo/preferred_location");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/requirement/status-counts");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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
            // Keys confirmed from raw API: nested under "data"
            // all_requirements, ongoing_requirements, complete_requirements, onhold_requirements
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

    public void newRequirementBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking New Requirement sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/client");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/currency/values");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/utility/get_preferred_timezones");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void intervueBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Intervue.io sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/intervue");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/intervue/intervue-overview");
        DashboardManager.log("   POST " + baseUrl + "/api/v1/emb/intervue/all?search_term=&limit=25&page=1");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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
            logMetric(src, "total",                 "  Total (fallback)          ");
            logMetric(src, "completed",             "  Completed (fallback)      ");
            logMetric(src, "active",                "  Active (fallback)         ");
            logMetric(src, "cancelled",             "  Cancelled (fallback)      ");
            logMetric(src, "cancel",                "  Cancel (alt key)          ");
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

    public void currencyRatesBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Currency Rates sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/currency-rates");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/currency/values");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/emb/exchange-rate/all");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void embConvertorBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking EMB Convertor sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/jdconvertor");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + adminUiUrl + "/jdconvertor?_rsc=ass8g  (Next.js RSC page load)");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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

    public void usersBrowserAndApi(String token) {
        DashboardManager.log("   [Browser] Clicking Users sidebar link");
        DashboardManager.log("   [URL] " + adminUiUrl + "/users");
        DashboardManager.log("   ─────────────────────────────────────────────────");
        DashboardManager.log("   APIs checked:");
        DashboardManager.log("   GET  " + baseUrl + "/api/v1/user/all");
        DashboardManager.log("   ─────────────────────────────────────────────────");

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
        // Handle nested object — return substring between { }
        if (json.charAt(valStart) == '{') {
            int depth = 0, end = valStart;
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