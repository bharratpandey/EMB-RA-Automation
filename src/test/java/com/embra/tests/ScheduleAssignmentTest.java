package com.embra.tests;

import com.embra.pages.*;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
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

    private static final String JD_FILE_PATH = "target/Ajay_Gupta_resume_.pdf";

    @BeforeAll
    static void setupBrowser() throws IOException {
        // 🚀 1. INITIALIZE THE DASHBOARD
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                //.setChannel("chrome")  // <--- THIS TELLS PLAYWRIGHT TO USE REAL CHROME
                .setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // ⭐ ADD THIS: Start tracing before creating the page
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
        page.navigate("https://uat-admin.embtalent.ai/login");
    }

    @Test
    @Order(1)
    void testScheduleAssignmentFlow() {
        // 🚀 2. START A NEW TEST IN THE DASHBOARD
        DashboardManager.startTest("Schedule Assignment Flow Execution");

        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // --- 1. Login & Navigation ---
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        RequirementListingPage listingPage = new RequirementListingPage(page);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        // --- 2. Create Requirements ---
        CreateRequirementPage createPage = new CreateRequirementPage(page);
        String commonJdPath = JD_FILE_PATH;

        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", commonJdPath)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create requirements");

        // ──────────────────────────────────────────────────────────────
        // 3. CAPTURE NAME & STATUS
        // ──────────────────────────────────────────────────────────────
        String firstReqName = verifyTopRequirements(1);

        // ──────────────────────────────────────────────────────────────
        // 🏁 NAVIGATION TO EXISTING REQUIREMENT
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();

        DashboardManager.log("   -> Opening Requirement: " + firstReqName);
        // Find the specific requirement and click it
        page.getByText(firstReqName).first().click();
        page.waitForTimeout(2000);

        // ──────────────────────────────────────────────────────────────
        // 4. PARTNER SHORTLISTING FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Partner Shortlisting Flow for: " + firstReqName);

        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(page);
        partnerPage.verifyRequirementStatus();
        partnerPage.navigateToPartnerShortlisting();
        page.waitForTimeout(2000);
        partnerPage.shortlistVendors(List.of("bharat pvt ltd", "Vendor Eur", "Vendor AED", "Vendor USD"));
        partnerPage.clickSendHiringRequirement();
        partnerPage.fillBudgetDetails();
        partnerPage.submitShortlisting();
        partnerPage.verifySuccessToast();

        DashboardManager.log("[REPORT] 🎉 Partner Shortlisting Flow Completed.");

        // ──────────────────────────────────────────────────────────────
        // 5. VENDOR PORTAL FLOW (Site B)
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🔄 Switching to Vendor Portal...");

        BrowserContext vendorContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // ⭐ TRACING FOR VENDOR 1
        vendorContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage = vendorContext.newPage();
        vendorPage.navigate("https://uat-vendor.embtalent.ai/login");

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234");
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();


        submitPage.addMembers(1, JD_FILE_PATH);
        //submitPage.addMembersFromTeam(Arrays.asList( "Candidate 3", "Candidate 4"));

        // Then click final submit
        submitPage.submitCandidates();

        submitPage.verifyCandidateStatus();

        // Close Vendor Context
        vendorContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-1-submit-trace.zip")));
        vendorContext.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Flow Completed.");

        // ──────────────────────────────────────────────────────────────
        // 9. SCHEDULE ASSIGNMENT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Schedule Assignment Flow...");

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        // Initialize the Assignment Page Object
        ScheduleAssignmentPage assignmentPage = new ScheduleAssignmentPage(page);

        // A. Navigate & Verify Requirement Status (Active)
        assignmentPage.navigateAndOpenRequirement(firstReqName);

        // B. Open Candidate & Verify Status
        assignmentPage.openCandidateForAssignment("Candidate 1");

        // C. Update Status to 'Schedule Assignment'
        assignmentPage.updateStatusToScheduleAssignment();

        // D. Fill Form & Submit Assignment
        assignmentPage.scheduleAssignmentAction(JD_FILE_PATH);

        // E. Verify "Uploaded Assignment" Details
        assignmentPage.verifyAssignmentDetails();

        DashboardManager.log("[REPORT] 🎉 Assignment Scheduled Successfully!");

        // ──────────────────────────────────────────────────────────────
        // 10. VENDOR SUBMITS ASSIGNMENT SOLUTION
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Submitting Assignment Solution...");

        BrowserContext vendorContext4 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        // ⭐ TRACING FOR VENDOR 4
        vendorContext4.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage4 = vendorContext4.newPage();

        ScheduleAssignmentPage vendorAssignmentPage = new ScheduleAssignmentPage(vendorPage4);
        vendorAssignmentPage.vendorSubmitAssignmentSolution(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName,
                JD_FILE_PATH
        );

        vendorContext4.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-4-assignment-solution-trace.zip")));
        vendorContext4.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Assignment Solution Submitted.");
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

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                String tracePath = "target/" + testInfo.getDisplayName().replace(" ", "_") + "-trace.zip";
                context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
            } catch (Exception e) {
                System.err.println("Failed to save Admin trace: " + e.getMessage());
            }
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        // 🚀 3. SAVE THE DASHBOARD & SEND EMAIL
        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
        EmailSender.sendDashboardEmail("bharat.pandey@emb.global");
    }
}