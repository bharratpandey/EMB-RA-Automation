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
public class ScheduleAssignmentTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    private static final String JD_FILE_PATH = "src/test/resources/Ajay_Gupta_resume_.pdf";

    // 🚀 NEW: Add a configuration variable to toggle the flow
    // Set this to a specific Requirement ID (e.g., "Senior BE1234") to skip creation.
    // Set to null or "" to create a new requirement from scratch.
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

        // ⭐ TRACE: Start Admin Trace (Part 1: Setup & Shortlist)
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
    void testScheduleAssignmentFlow() {
        DashboardManager.startTest("Schedule Assignment Flow Execution");
        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // --- 1. Login & Navigation ---
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        String firstReqName;

        // 🚀 NEW: Check if we are targeting an existing requirement or creating a new one
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
        // 🏁 NAVIGATION TO REQUIREMENT (New Flow with Search)
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("   -> Searching for Requirement: " + firstReqName);

        // Search and Filter Logic
        Locator searchFilterBtn = page.locator("div.font-semibold").filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        searchFilterBtn.click();

        Locator searchInput = page.locator("input[placeholder='Search by client name, budget, title, email ...']");
        searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchInput.fill(firstReqName);
        page.waitForTimeout(2000); // Wait for filtering

        DashboardManager.log("   -> Opening Requirement: " + firstReqName);

        // Find the specific requirement row
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(firstReqName));

        // Click the eye button or the text link
        Locator viewDetailsBtn = reqRow.locator("button[title='View Details']");
        if(viewDetailsBtn.count() > 0) {
            viewDetailsBtn.first().click();
        } else {
            reqRow.locator("a").first().click();
        }
        page.waitForTimeout(2000);

        // ──────────────────────────────────────────────────────────────
        // 4. PARTNER SHORTLISTING FLOW (New Conditional Flow)
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Partner Shortlisting Flow for: " + firstReqName);

        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(page);

        DashboardManager.log("   -> Switching to Partner Shortlisting Tab...");
        Locator partnerTabBtn = page.locator("button[role='tab']").filter(new Locator.FilterOptions().setHasText(Pattern.compile("Partner Shortlisting")));
        partnerTabBtn.first().click();
        page.waitForTimeout(2000);

        // 🚀 FIX: Expand the FIRST "Search & Filters" bar specifically for Shortlisted Partners!
        DashboardManager.log("   -> Expanding Shortlisted Partners Search & Filters...");
        Locator shortlistedSearchFilterBtn = page.locator("div.font-semibold").filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        try {
            shortlistedSearchFilterBtn.click();
            page.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Search & Filters button not found, assuming it is already expanded.");
        }

        DashboardManager.log("   -> Checking if vendor 'bharat.pandey@emb.global' is already shortlisted...");
        Locator shortlistedSearchInput = page.locator("input[placeholder='Search shortlisted partners...']");

        shortlistedSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        shortlistedSearchInput.fill("bharat.pandey@emb.global");
        page.waitForTimeout(2000); // Wait for table to filter

        Locator noPartnersFound = page.locator("div.text-muted-foreground").filter(new Locator.FilterOptions().setHasText("No Shortlisted Partners Found."));

        if(noPartnersFound.isVisible()) {
            DashboardManager.log("   ⚠️ Vendor not found. Proceeding to shortlist vendors...");

            shortlistedSearchInput.clear();
            page.waitForTimeout(1000);

            partnerPage.shortlistVendors(List.of("bharat pvt ltd", "Vendor Eur", "Vendor AED", "Vendor USD"));
            partnerPage.clickSendHiringRequirement();
            partnerPage.fillBudgetDetails();
            partnerPage.submitShortlisting();
            partnerPage.verifySuccessToast();
            DashboardManager.log("[REPORT] 🎉 Partner Shortlisting Flow Completed.");
        } else {
            DashboardManager.log("   ✅ Vendor 'bharat.pandey@emb.global' is already shortlisted. Skipping shortlisting step.");
        }

        // ⭐ TRACE: Stop Admin Setup Trace before switching contexts
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-1-setup-shortlist-trace.zip")));
        DashboardManager.log("[REPORT] 🛡️ Admin Setup trace saved to: target/admin-1-setup-shortlist-trace.zip");


        // ──────────────────────────────────────────────────────────────
        // 5. VENDOR PORTAL FLOW (Site B)
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🔄 Switching to Vendor Portal...");

        BrowserContext vendorContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // ⭐ TRACE: Start Vendor Submit Trace
        vendorContext.tracing().start(new Tracing.StartOptions()
                .setName("Vendor-Submit-Candidates")
                .setTitle("Vendor Portal - Candidate Submission")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        Page vendorPage = vendorContext.newPage();
        vendorPage.navigate("https://uat-vendor.embtalent.ai/login");

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.loginToVendorPortal("bharat.pandey@emb.global", "Emb@1234"); //uat=bharat.pandey+1@emb.global  .dev=bharat.pandey@emb.global

        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();

        submitPage.addMembers(1, JD_FILE_PATH);

        submitPage.submitCandidates();
        submitPage.verifyCandidateStatus();

        // ⭐ TRACE: Stop Vendor Submit Trace
        vendorContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-1-submit-trace.zip")));
        vendorContext.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Flow Completed. Trace saved to: target/vendor-1-submit-trace.zip");


        // ──────────────────────────────────────────────────────────────
        // 9. SCHEDULE ASSIGNMENT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Schedule Assignment Flow...");

        // ⭐ TRACE: Start Admin Schedule Assignment Trace
        context.tracing().start(new Tracing.StartOptions()
                .setName("Admin-Schedule-Assignment")
                .setTitle("Admin Dashboard - Schedule Assignment")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        ScheduleAssignmentPage assignmentPage = new ScheduleAssignmentPage(page);

        // A. Search and Open Requirement using local test helper
        searchAndOpenRequirementFromTest(firstReqName);

        // 🚀 B. Open Candidate (Bypassing the Page Object to fix strict mode violation locally)
        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        DashboardManager.log("   -> Opening Candidate: Candidate 1 (Using Test-level bypass)");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1")).first();
        candidateRow.locator("button[title='View Details']").first().click();
        page.waitForTimeout(2000);

        // C. Update Status to 'Schedule Assignment'
        assignmentPage.updateStatusToScheduleAssignment();

        // D. Fill Form & Submit Assignment
        assignmentPage.scheduleAssignmentAction(JD_FILE_PATH);

        // E. Verify "Uploaded Assignment" Details
        assignmentPage.verifyAssignmentDetails();

        DashboardManager.log("[REPORT] 🎉 Assignment Scheduled Successfully!");

        // ⭐ TRACE: Stop Admin Schedule Assignment Trace
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-2-schedule-assignment-trace.zip")));
        DashboardManager.log("[REPORT] 🛡️ Admin Schedule Assignment trace saved to: target/admin-2-schedule-assignment-trace.zip");


        // ──────────────────────────────────────────────────────────────
        // 10. VENDOR SUBMITS ASSIGNMENT SOLUTION
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Submitting Assignment Solution...");

        BrowserContext vendorContext4 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));

        // ⭐ TRACE: Start Vendor Assignment Solution Trace
        vendorContext4.tracing().start(new Tracing.StartOptions()
                .setName("Vendor-Assignment-Solution")
                .setTitle("Vendor Portal - Assignment Solution")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        Page vendorPage4 = vendorContext4.newPage();

        ScheduleAssignmentPage vendorAssignmentPage = new ScheduleAssignmentPage(vendorPage4);
        vendorAssignmentPage.vendorSubmitAssignmentSolution(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey@emb.global",
                "Emb@1234",
                firstReqName,
                JD_FILE_PATH
        );

        // ⭐ TRACE: Stop Vendor Assignment Solution Trace
        vendorContext4.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-4-assignment-solution-trace.zip")));
        vendorContext4.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Assignment Solution Submitted. Trace saved to: target/vendor-4-assignment-solution-trace.zip");

        // ──────────────────────────────────────────────────────────────
        // 11. ADMIN REVIEWS ASSIGNMENT & SUBMITS FEEDBACK
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Admin Review Assignment & Submit Feedback Flow...");

        // ⭐ TRACE: Start Admin Feedback Trace
        context.tracing().start(new Tracing.StartOptions()
                .setName("Admin-Assignment-Feedback")
                .setTitle("Admin Dashboard - Assignment Feedback")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page.bringToFront();
        page.waitForTimeout(1000);

        // Call the newly added method (Ensure you pass "Candidate 1")
        assignmentPage.adminSubmitAssignmentFeedback(firstReqName, "Candidate 1");

        DashboardManager.log("[REPORT] 🎉 Admin Assignment Feedback Submitted Successfully!");

        // ⭐ TRACE: Stop Admin Feedback Trace
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-3-assignment-feedback-trace.zip")));
        DashboardManager.log("[REPORT] 🛡️ Admin Assignment Feedback trace saved to: target/admin-3-assignment-feedback-trace.zip");


        // ──────────────────────────────────────────────────────────────
        // 12. VENDOR VERIFIES FINAL ASSIGNMENT STATUS
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying Final Assignment Status...");

        BrowserContext vendorContext5 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));

        // ⭐ TRACE: Start Vendor Verification Trace
        vendorContext5.tracing().start(new Tracing.StartOptions()
                .setName("Vendor-Final-Verify")
                .setTitle("Vendor Portal - Final Verification")
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        Page vendorPage5 = vendorContext5.newPage();

        ScheduleAssignmentPage vendorVerifyPage = new ScheduleAssignmentPage(vendorPage5);
        vendorVerifyPage.vendorVerifyFinalAssignmentStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        // ⭐ TRACE: Stop Vendor Verification Trace
        vendorContext5.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-5-final-verify-trace.zip")));
        vendorContext5.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Final Assignment Status Verified. Trace saved to: target/vendor-5-final-verify-trace.zip");
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
        Locator reqSearchFilterBtn = page.locator("div.font-semibold").filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        reqSearchFilterBtn.click();

        Locator reqSearchInput = page.locator("input[placeholder='Search by client name, budget, title, email ...']");
        reqSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        reqSearchInput.fill(reqName);
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Opening Requirement: " + reqName);
        Locator interviewReqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        Locator interviewViewDetailsBtn = interviewReqRow.locator("button[title='View Details']");

        if(interviewViewDetailsBtn.count() > 0) {
            interviewViewDetailsBtn.first().click();
        } else {
            interviewReqRow.locator("a").first().click();
        }
        page.waitForTimeout(2000);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                // If the test crashes mid-way, this catches any open trace and saves it
                String tracePath = "target/" + testInfo.getDisplayName().replace(" ", "_") + "-fallback-trace.zip";
                context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
            } catch (Exception e) {
                // Silently catch if tracing was already cleanly stopped by our explicit code blocks
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