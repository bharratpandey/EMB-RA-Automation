package com.embra.tests;

import com.embra.pages.*;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScheduleAssessmentTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    private static final String JD_FILE_PATH = "src/test/resources/Ajay_Gupta_resume_.pdf";

    private static final String TARGET_REQUIREMENT = "ReqTest-1778055195163";

    @BeforeAll
    static void setupBrowser() throws IOException {
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(false)
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        context.tracing().start(new Tracing.StartOptions()
                .setName("Admin-Setup-Flow")
                .setTitle("Admin Dashboard - Setup & Shortlist")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
        page.navigate("https://uat-admin.embtalent.ai/login");
    }

    @Test
    @Order(1)
    void testScheduleAssessmentFlow() {
        DashboardManager.startTest("E2E Full Flow Execution - Schedule Assessment");
        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // ──────────────────────────────────────────────────────────────
        // 1. LOGIN
        // ──────────────────────────────────────────────────────────────
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        String firstReqName;

        // ──────────────────────────────────────────────────────────────
        // 2. REQUIREMENT — USE EXISTING OR CREATE NEW
        // ──────────────────────────────────────────────────────────────
        if (TARGET_REQUIREMENT != null && !TARGET_REQUIREMENT.trim().isEmpty()) {
            DashboardManager.log("[REPORT] 🎯 Target Requirement specified: " + TARGET_REQUIREMENT);
            firstReqName = TARGET_REQUIREMENT;

            DashboardManager.log("   -> Navigating to Requirement Listing...");
            page.locator("a[href='/hiring-requests']").first().click();
            page.waitForLoadState();

        } else {
            DashboardManager.log("[REPORT] 🆕 No Target Requirement specified. Creating a new one...");

            RequirementListingPage listingPage = new RequirementListingPage(page);
            assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

            CreateRequirementPage createPage = new CreateRequirementPage(page);
            boolean success = createPage.createMultipleRequirements(List.of(
                    new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", JD_FILE_PATH)
            ), "Requirement generated successfully");

            assertTrue(success, "Failed to create requirements");

            firstReqName = verifyTopRequirements(1);

            DashboardManager.log("   -> Navigating to Requirement Listing...");
            page.locator("a[href='/hiring-requests']").first().click();
            page.waitForLoadState();
        }

        // ──────────────────────────────────────────────────────────────
        // 3. SEARCH & OPEN REQUIREMENT
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("   -> Searching for Requirement: " + firstReqName);

        Locator searchFilterBtn = page.locator("div.font-semibold")
                .filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        searchFilterBtn.click();

        Locator searchInput = page.locator("input[placeholder='Search by client name, budget, title, email ...']");
        searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchInput.fill(firstReqName);
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Opening Requirement: " + firstReqName);
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(firstReqName));
        Locator viewDetailsBtn = reqRow.locator("button[title='View Details']");
        if (viewDetailsBtn.count() > 0) {
            viewDetailsBtn.first().click();
        } else {
            reqRow.locator("a").first().click();
        }
        page.waitForTimeout(2000);

        // ──────────────────────────────────────────────────────────────
        // 4. PARTNER SHORTLISTING FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Partner Shortlisting Flow for: " + firstReqName);

        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(page);

        DashboardManager.log("   -> Switching to Partner Shortlisting Tab...");
        page.locator("button[role='tab']")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("Partner Shortlisting")))
                .first().click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Expanding Shortlisted Partners Search & Filters...");
        try {
            page.locator("div.font-semibold")
                    .filter(new Locator.FilterOptions().setHasText("Search & Filters"))
                    .first().click();
            page.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Search & Filters button not found, assuming it is already expanded.");
        }

        DashboardManager.log("   -> Checking if vendor is already shortlisted...");
        Locator shortlistedSearchInput = page.locator("input[placeholder='Search shortlisted partners...']");
        shortlistedSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        shortlistedSearchInput.fill("bharat pvt ltd");
        page.waitForTimeout(2000);

        Locator noPartnersFound = page.locator("div.text-muted-foreground")
                .filter(new Locator.FilterOptions().setHasText("No Shortlisted Partners Found."));

        if (noPartnersFound.isVisible()) {
            DashboardManager.log("   ⚠️ Vendor not found. Proceeding to shortlist vendors...");
            shortlistedSearchInput.clear();
            page.waitForTimeout(1000);

            partnerPage.shortlistVendors(List.of("bharat pvt ltd"));
            partnerPage.clickSendHiringRequirement();
            partnerPage.fillBudgetDetails();
            partnerPage.submitShortlisting();
            partnerPage.verifySuccessToast();
            DashboardManager.log("[REPORT] 🎉 Partner Shortlisting Flow Completed.");
        } else {
            DashboardManager.log("   ✅ Vendor is already shortlisted. Skipping shortlisting step.");
        }

        // ⭐ TRACE: Stop Admin Setup Trace
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-1-setup-shortlist-trace.zip")));
        DashboardManager.log("[REPORT] 💾 Admin Setup trace saved → target/admin-1-setup-shortlist-trace.zip");

        // ──────────────────────────────────────────────────────────────
        // 5. VENDOR PORTAL FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🔄 Switching to Vendor Portal...");

        BrowserContext vendorContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        vendorContext.tracing().start(new Tracing.StartOptions()
                .setName("Vendor-Submit-Candidates")
                .setTitle("Vendor Portal - Candidate Submission")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        Page vendorPage = vendorContext.newPage();
        vendorPage.navigate("https://uat-vendor.embtalent.ai/login");

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234");
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();
        submitPage.addMembers(1, JD_FILE_PATH);
        submitPage.submitCandidates();
        submitPage.verifyCandidateStatus();

        vendorContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-1-submit-trace.zip")));
        DashboardManager.log("[REPORT] 💾 Vendor Submit trace saved → target/vendor-1-submit-trace.zip");
        vendorContext.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Flow Completed.");

        // ──────────────────────────────────────────────────────────────
        // 6. ADMIN: SCHEDULE & CANCEL ASSESSMENT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Schedule Assessment Flow...");

        context.tracing().start(new Tracing.StartOptions()
                .setName("Admin-Schedule-Assessment")
                .setTitle("Admin Dashboard - Schedule Assessment")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page.bringToFront();
        page.waitForTimeout(1000);

        ScheduleAssessmentPage adminSchedule = new ScheduleAssessmentPage(page);

        DashboardManager.log("[REPORT] 👮 Admin: Updating Candidate Status...");
        searchAndOpenRequirementFromTest(firstReqName);

        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        page.getByRole(AriaRole.TAB)
                .filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        adminSchedule.openCandidateAndVerify("Candidate 1", "bharat pvt ltd");
        adminSchedule.adminUpdateStatusToAssessment();

        // ⭐ TRACE: Stop Admin Schedule Assessment Trace
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-2-schedule-assessment-trace.zip")));
        DashboardManager.log("[REPORT] 💾 Admin Schedule Assessment trace saved → target/admin-2-schedule-assessment-trace.zip");

        DashboardManager.log("[REPORT] ✅ Assessment Flow Completed Successfully!");
    }

    // ──────────────────────────────────────────────────────────────
    // HELPER & TEARDOWN METHODS
    // ──────────────────────────────────────────────────────────────

    private String verifyTopRequirements(int limit) {
        DashboardManager.log("\n[REPORT] 🔍 Verifying Table Data...");
        Locator rows = page.locator("tbody tr");
        try {
            rows.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            rows.first().locator("td:nth-child(2)").getByText(Pattern.compile("Req-")).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (Exception e) {
            DashboardManager.log("[REPORT] ⚠️ Wait timeout or empty table.");
        }

        int count = rows.count();
        if (count < limit) limit = count;

        String firstTitle = "";
        for (int i = 0; i < limit; i++) {
            Locator row = rows.nth(i);
            String title = row.locator("td:nth-child(2)").innerText().trim();
            String status = row.locator("td:nth-child(4)").innerText().trim();

            if (i == 0) firstTitle = title;

            if ("Active".equalsIgnoreCase(status)) {
                DashboardManager.log("[REPORT] ✅ Row " + (i + 1) + " [" + title + "]: Status is Active");
            } else {
                DashboardManager.log("[REPORT] ❌ Row " + (i + 1) + " [" + title + "]: WRONG STATUS! Found: [" + status + "]");
            }
        }
        return firstTitle;
    }

    private void searchAndOpenRequirementFromTest(String reqName) {
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Requirement: " + reqName);
        Locator reqSearchFilterBtn = page.locator("div.font-semibold")
                .filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        reqSearchFilterBtn.click();

        Locator reqSearchInput = page.locator("input[placeholder='Search by client name, budget, title, email ...']");
        reqSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        reqSearchInput.fill(reqName);
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Opening Requirement: " + reqName);
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        Locator viewDetailsBtn = reqRow.locator("button[title='View Details']");

        if (viewDetailsBtn.count() > 0) {
            viewDetailsBtn.first().click();
        } else {
            reqRow.locator("a").first().click();
        }
        page.waitForTimeout(2000);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                String tracePath = "target/" + testInfo.getDisplayName().replace(" ", "_") + "-fallback-trace.zip";
                context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
                DashboardManager.log("[REPORT] 💾 Fallback trace saved → " + tracePath);
            } catch (Exception e) {
                // Silently catch if tracing was already cleanly stopped
            }
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
        EmailSender.sendDashboardEmail("bharat.pandey@emb.global");
    }
}