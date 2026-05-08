package com.embra.tests;

import com.embra.pages.ProdSanityPage;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProdSanityTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    // ── Credentials — loaded securely, never hardcoded ────────
    private static String VENDOR_URL;
    private static String VENDOR_EMAIL;
    private static String VENDOR_PASSWORD;
    private static String REQ_NAME;
    private static String RESUME_PATH;

    // API response time considered acceptable (ms)
    private static final long API_SLOW_THRESHOLD_MS = 200;

    // ── SETUP ─────────────────────────────────────────────────

    @BeforeAll
    static void setupAll() {
        DashboardManager.initReport();

        // Credential loading priority:
        //   1. OS environment variables  (CI/CD GitHub Secrets — always win)
        //   2. .env file                 (local dev — in .gitignore, never committed)
        //   3. .env.example fallback     (convenient local dev without copying the file)
        Dotenv dotenv = loadDotenv();

        VENDOR_URL      = get(dotenv, "UAT_VENDOR_URL",      "https://uat-vendor.embtalent.ai/login");
        VENDOR_EMAIL    = get(dotenv, "UAT_VENDOR_EMAIL",    null);
        VENDOR_PASSWORD = get(dotenv, "UAT_VENDOR_PASSWORD", null);
        REQ_NAME        = get(dotenv, "UAT_REQ_NAME",        "Test Sanity Requirement");
        RESUME_PATH     = get(dotenv, "UAT_RESUME_PATH",     "src/test/resources/Shashikant.pdf");

        // Fail fast — never proceed with missing credentials
        requireCredential("UAT_VENDOR_EMAIL",    VENDOR_EMAIL);
        requireCredential("UAT_VENDOR_PASSWORD", VENDOR_PASSWORD);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(false));
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        context.tracing().start(new Tracing.StartOptions()
                .setName("Prod-Sanity")
                .setTitle("Prod Sanity — Vendor Portal")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
    }

    // ── TEST ──────────────────────────────────────────────────

    @Test
    @Order(1)
    void testProdSanity() {
        DashboardManager.startTest("Prod Sanity — Vendor Portal");
        DashboardManager.log("[SANITY] Starting Prod Sanity Test...");

        ProdSanityPage sanityPage = new ProdSanityPage(page);

        // ── STEP 1: Navigate & Login ──────────────────────────
        DashboardManager.log("\n[STEP 1] Navigate to Vendor Login & authenticate");
        sanityPage.navigate(VENDOR_URL);
        sanityPage.fillCredentials(VENDOR_EMAIL, VENDOR_PASSWORD);

        // Intercept the first POST to the API domain after clicking Login.
        // page.waitForResponse() registers the listener BEFORE executing the
        // callback (sanityPage::clickLogin), so no response is missed.
        long loginStart = System.currentTimeMillis();
        try {
            Response loginResp = page.waitForResponse(
                    resp -> resp.url().contains("uatapi-ra.embtalent.ai")
                            && resp.request().method().equals("POST"),
                    new Page.WaitForResponseOptions().setTimeout(15000),
                    sanityPage::clickLogin   // clicks the Login button, then waits
            );
            long loginDuration = System.currentTimeMillis() - loginStart;
            logApiTiming("Login API", loginResp.url(), loginResp.status(), loginDuration);

        } catch (Exception e) {
            // The Login button WAS clicked inside the lambda — do NOT click again.
            // We simply couldn't match the API response (endpoint may differ).
            DashboardManager.log("   ⚠️ Login API timing not captured ("
                    + (System.currentTimeMillis() - loginStart) + "ms elapsed)");
        }

        // ── STEP 2: Verify Login Toast ────────────────────────
        DashboardManager.log("\n[STEP 2] Verify Login Toast");
        boolean loginOk = sanityPage.verifyLoginToast();
        assertTrue(loginOk, "Login failed — 'Login successful!' toast was not shown");

        // ── STEP 3: Click Projects Tab ────────────────────────
        DashboardManager.log("\n[STEP 3] Navigate to Projects");
        sanityPage.clickProjectsTab();

        // ── STEP 4: Search & Open Requirement ─────────────────
        DashboardManager.log("\n[STEP 4] Search for requirement: " + REQ_NAME);
        sanityPage.searchRequirement(REQ_NAME);
        sanityPage.openRequirementCard(REQ_NAME);

        // ── STEP 5: Shortlisted Candidate Tab ─────────────────
        DashboardManager.log("\n[STEP 5] Open Shortlisted Candidate tab");
        sanityPage.clickShortlistedCandidateTab();

        // ── STEP 6: Add New Member ────────────────────────────
        DashboardManager.log("\n[STEP 6] Click Add New Member");
        sanityPage.clickAddNewMember();

        // ── STEP 7: Upload Resume ─────────────────────────────
        DashboardManager.log("\n[STEP 7] Upload resume file");
        sanityPage.uploadResume(RESUME_PATH);

        // ── STEP 8: Import from Resume + measure API ──────────
        // page.waitForResponse() registers the listener BEFORE clicking the button,
        // waits up to 59 s for POST /resume/resume-data-autofill, then returns.
        DashboardManager.log("\n[STEP 8] Import from Resume — measuring extraction API (max 59s)");
        long resumeStart = System.currentTimeMillis();
        boolean resumeApiOk = false;

        try {
            Response resumeResp = page.waitForResponse(
                    resp -> resp.url().contains("/resume/resume-data-autofill"),
                    new Page.WaitForResponseOptions().setTimeout(59000),
                    sanityPage::clickImportFromResume   // clicks button, then waits
            );
            long resumeDuration = System.currentTimeMillis() - resumeStart;
            logApiTiming("Resume Autofill API", resumeResp.url(), resumeResp.status(), resumeDuration);
            resumeApiOk = (resumeResp.status() == 200);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - resumeStart;
            DashboardManager.log("   ❌ Resume Autofill API: No response after " + elapsed + "ms — API is SLOW");
            DashboardManager.log("      Expected: POST https://uatapi-ra.embtalent.ai/api/v1/resume/resume-data-autofill → 200 OK");
        }

        // ── STEP 9: Verify Extraction Toast ───────────────────
        // After waitForResponse completed (or timed out at 59s), allow 10 more
        // seconds for the UI to render the success toast.
        DashboardManager.log("\n[STEP 9] Check resume extraction toast");
        boolean toastVisible = sanityPage.waitForResumeExtractionToast(10000);

        // ── Final summary ─────────────────────────────────────
        if (resumeApiOk && toastVisible) {
            DashboardManager.log("\n[SANITY] ✅ All checks passed. Resume extraction flow is healthy.");
        } else if (!resumeApiOk) {
            DashboardManager.log("\n[SANITY] ❌ Resume Autofill API did not return 200 OK.");
        } else {
            DashboardManager.log("\n[SANITY] ⚠️ API responded but 'Resume details extracted!' toast was not shown.");
        }
    }

    // ── TEARDOWN ──────────────────────────────────────────────

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                String name = testInfo.getDisplayName().replace(" ", "_") + "-trace.zip";
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("traces/" + name)));
                DashboardManager.log("[SANITY] Trace saved → traces/" + name);
            } catch (Exception ignored) {
                // Tracing was already stopped cleanly
            }
            context.close();
        }
    }

    @AfterAll
    static void tearDownAll() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
        EmailSender.sendDashboardEmail("bharat.pandey@emb.global");
    }

    // ── HELPERS ───────────────────────────────────────────────

    /**
     * Tries .env first; if the key is still missing (file absent or key not set),
     * falls back to .env.example. OS environment variables always take precedence
     * over both files (dotenv-java checks OS env first internally).
     */
    private static Dotenv loadDotenv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        // .env found and has the required key — use it
        if (dotenv.get("UAT_VENDOR_EMAIL") != null) return dotenv;
        // Fall back to .env.example (allows running without copying the file locally)
        return Dotenv.configure().filename(".env.example").ignoreIfMissing().load();
    }

    /**
     * Reads a value from dotenv (OS env takes precedence over .env file).
     * Falls back to {@code defaultValue} when the key is absent from both sources.
     */
    private static String get(Dotenv dotenv, String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Hard-stops the suite if a required credential is absent.
     * The error message guides both local devs and CI engineers — without revealing values.
     */
    private static void requireCredential(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "[SECURITY] Missing required credential: " + name + "\n" +
                "  Local dev → add to .env file:   " + name + "=<your_value>\n" +
                "  CI/CD     → add to GitHub Repository Secrets, then inject via the env: block in prod-sanity.yml"
            );
        }
    }

    /**
     * Logs API call results. Never logs credential values.
     * Prints: label, URL, HTTP status, duration, and speed verdict vs threshold.
     */
    private void logApiTiming(String label, String url, int status, long durationMs) {
        String statusMark = (status >= 200 && status < 300) ? "✅" : "❌";
        String speedNote  = durationMs <= API_SLOW_THRESHOLD_MS
                ? "✅ Fast  → " + durationMs + "ms"
                : "⚠️ SLOW  → " + durationMs + "ms (exceeds " + API_SLOW_THRESHOLD_MS + "ms threshold)";
        DashboardManager.log("   " + statusMark + " [" + label + "] HTTP " + status + " | " + speedNote);
        DashboardManager.log("      URL: " + url);
    }
}