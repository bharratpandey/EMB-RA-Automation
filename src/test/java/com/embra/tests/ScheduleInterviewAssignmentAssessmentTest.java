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
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScheduleInterviewAssignmentAssessmentTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;

    // ── Single browser window — two tabs (admin, vendor) ──
    private Page adminPage;
    private Page vendorPage;

    private static final String JD_FILE_PATH = "src/test/resources/Anurag_DesignResume (2).pdf";
    private static final String TARGET_REQUIREMENT = "ReqTest-1778055195163";

    // ── URLs ──────────────────────────────────────────────
    private static final String ADMIN_URL      = "https://uat-admin.embtalent.ai/login";
    private static final String VENDOR_URL     = "https://uat-vendor.embtalent.ai/login";
    private static final String VENDOR_DASHBOARD = "https://uat-vendor.embtalent.ai/projects";

    // ── Credentials ───────────────────────────────────────
    private static final String VENDOR_EMAIL = "bharat.pandey@emb.global";
    private static final String VENDOR_PASS  = "Emb@1234";

    // ── Trace counter kept for logging only — no actual stop/start ──
    private int adminTraceCounter = 0;

    // ── No-op trace methods — single continuous trace runs from setup() to tearDown() ──
    private void startAdminTrace(String name, String title) {
        DashboardManager.log("   -> [Trace] Phase: " + title);
    }

    private void stopAdminTrace(String label) {
        adminTraceCounter++;
        DashboardManager.log("   -> [Trace] Checkpoint " + adminTraceCounter + ": " + label);
    }

    @BeforeAll
    static void setupBrowser() throws IOException {
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath)) {
            throw new RuntimeException("\n\nCRITICAL ERROR: Real PDF not found!\n\n");
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        adminTraceCounter = 0;

        // ── Single context — both tabs share same browser window ──
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // ── Single tracing session — runs continuously, one trace saved in tearDown ──
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(true));

        // Tab 1 — Admin portal
        adminPage = context.newPage();
        adminPage.setDefaultTimeout(60000);
        adminPage.setDefaultNavigationTimeout(90000);
        adminPage.navigate(ADMIN_URL);

        // Tab 2 — Vendor portal (login happens on first vendor step)
        vendorPage = context.newPage();
        vendorPage.setDefaultTimeout(60000);
        vendorPage.setDefaultNavigationTimeout(90000);
        vendorPage.navigate(VENDOR_URL);

        // Switch back to admin tab to start
        adminPage.bringToFront();
    }

    @Test
    @Order(1)
    void testMasterE2EFlow() {
        DashboardManager.startTest("MASTER E2E: Interview -> Assignment -> Assessment");
        DashboardManager.log("[REPORT] 🚀 Starting Master E2E Journey...");

        // =====================================================================================
        // PHASE 1: ADMIN SETUP & VENDOR SHORTLISTING
        // =====================================================================================
        startAdminTrace("Phase1-Setup", "Admin - Setup & Shortlist");

        LoginPage loginPage = new LoginPage(adminPage);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        String firstReqName;

        if (TARGET_REQUIREMENT != null && !TARGET_REQUIREMENT.trim().isEmpty()) {
            DashboardManager.log("[REPORT] 🎯 Target Requirement specified: " + TARGET_REQUIREMENT);
            firstReqName = TARGET_REQUIREMENT;
            adminPage.locator("a[href='/hiring-requests']").first().click();
            adminPage.waitForLoadState();
        } else {
            DashboardManager.log("[REPORT] 🆕 No Target Requirement specified. Creating a new one...");
            RequirementListingPage listingPage = new RequirementListingPage(adminPage);
            assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

            CreateRequirementPage createPage = new CreateRequirementPage(adminPage);
            boolean success = createPage.createMultipleRequirements(List.of(
                    new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", JD_FILE_PATH)
            ), "Requirement generated successfully");
            assertTrue(success, "Failed to create requirements");

            firstReqName = verifyTopRequirements(1);
            adminPage.locator("a[href='/hiring-requests']").first().click();
            adminPage.waitForLoadState();
        }

        searchAndOpenRequirementFromTest(firstReqName);

        DashboardManager.log("\n[REPORT] 🚀 Starting Partner Shortlisting Flow for: " + firstReqName);
        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(adminPage);
        adminPage.locator("button[role='tab']").filter(new Locator.FilterOptions().setHasText(Pattern.compile("Partner Shortlisting"))).first().click();
        adminPage.waitForTimeout(2000);

        try {
            adminPage.locator("div.font-semibold").filter(new Locator.FilterOptions().setHasText("Search & Filters")).first().click();
            adminPage.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Search & Filters button not found.");
        }

        Locator shortlistedSearchInput = adminPage.locator("input[placeholder='Search shortlisted partners...']");
        shortlistedSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        shortlistedSearchInput.fill("bharat pvt ltd");
        adminPage.waitForTimeout(2000);

        if (adminPage.locator("div.text-muted-foreground").filter(new Locator.FilterOptions().setHasText("No Shortlisted Partners Found.")).isVisible()) {
            shortlistedSearchInput.clear();
            adminPage.waitForTimeout(1000);
            partnerPage.shortlistVendors(List.of("bharat pvt ltd"));
            partnerPage.clickSendHiringRequirement();
            partnerPage.fillBudgetDetails();
            partnerPage.submitShortlisting();
            partnerPage.verifySuccessToast();
            DashboardManager.log("[REPORT] 🎉 Partner Shortlisting Flow Completed.");
        } else {
            DashboardManager.log("   ✅ Vendor is already shortlisted. Skipping shortlisting step.");
        }

        stopAdminTrace("setup-shortlist");

        // =====================================================================================
        // PHASE 2: VENDOR SUBMITS CANDIDATE
        // =====================================================================================
        DashboardManager.log("\n[REPORT] 🔄 Switching to Vendor Portal to Add Candidate...");

        // Switch to vendor tab — first time, login needed
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_URL, VENDOR_EMAIL, VENDOR_PASS);

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();

        // ── Inline: Add New Member with full flow ──────────────────────
        DashboardManager.log("👥 Adding 1 member inline...");
        try {
            Locator addBtn = vendorPage.locator("button")
                    .filter(new Locator.FilterOptions().setHasText("Add New Member")).first();
            addBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            addBtn.click();
            vendorPage.waitForTimeout(1000);

            DashboardManager.log("   📤 Uploading Resume...");
            vendorPage.locator("input[type='file'][accept='.pdf']").first()
                    .setInputFiles(Paths.get(JD_FILE_PATH));
            DashboardManager.log("   ✅ Resume file attached.");
            vendorPage.waitForTimeout(2000);

            DashboardManager.log("   -> Clicking 'Import from resume'...");
            vendorPage.locator("button")
                    .filter(new Locator.FilterOptions().setHasText("Import from resume"))
                    .first()
                    .click(new Locator.ClickOptions().setForce(true));
            DashboardManager.log("   -> Clicked. Waiting for toast (max 59s)...");

            boolean toastFound = false;
            try {
                vendorPage.locator("span")
                        .filter(new Locator.FilterOptions().setHasText("Resume details extracted!"))
                        .first()
                        .waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE).setTimeout(59000));
                DashboardManager.log("   ✅ Toast: 'Resume details extracted!' — Extraction working fine");
                toastFound = true;
            } catch (Exception e) {
                DashboardManager.log("   ⚠️ Toast not found in 59s — waiting additional 60s (120s total)...");
            }

            if (!toastFound) {
                try {
                    vendorPage.locator("span")
                            .filter(new Locator.FilterOptions().setHasText("Resume details extracted!"))
                            .first()
                            .waitFor(new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE).setTimeout(60000));
                    DashboardManager.log("   ✅ Toast found after extended wait.");
                } catch (Exception e) {
                    DashboardManager.log("   ❌ SLOW EXTRACTION — Toast not visible after 120s. Stopping automation.");
                    throw new RuntimeException("SLOW EXTRACTION: Resume details extracted toast not visible after 120 seconds.");
                }
            }
            vendorPage.waitForTimeout(1500);

            DashboardManager.log("   📝 Filling Basic Info...");
            vendorPage.locator("input[name='name']").clear();
            vendorPage.locator("input[name='name']").fill("candidate 1");

            vendorPage.locator("input[name='email']").last().clear();
            vendorPage.locator("input[name='email']").last()
                    .fill("TestMember" + System.currentTimeMillis() + "@yopmail.com");

            vendorPage.locator("input[name='linkedin']").clear();
            vendorPage.locator("input[name='linkedin']").fill("https://in.linkedin.com/company/embglobal");

            vendorPage.locator("input[name='interviewLink']").clear();
            vendorPage.locator("input[name='interviewLink']").fill("https://www.example.com/");
            vendorPage.waitForTimeout(1000);
            DashboardManager.log("   ✅ Basic Info Filled.");

            DashboardManager.log("   🏆 Adding Award...");
            vendorPage.locator("button")
                    .filter(new Locator.FilterOptions().setHasText("Add Awards")).click();
            vendorPage.waitForTimeout(500);

            vendorPage.locator("input[name='nameOfAward']").clear();
            vendorPage.locator("input[name='nameOfAward']").fill("EOY");

            vendorPage.locator("button[role='combobox']")
                    .filter(new Locator.FilterOptions().setHasText("Select year")).click();
            vendorPage.waitForTimeout(500);
            vendorPage.locator("div[role='option']")
                    .filter(new Locator.FilterOptions().setHasText("2024")).click();
            vendorPage.waitForTimeout(500);

            vendorPage.locator("textarea[name='description']").first().clear();
            vendorPage.locator("textarea[name='description']").first()
                    .fill("this is the Automated Description box");

            vendorPage.locator("button[type='submit']")
                    .filter(new Locator.FilterOptions().setHasText("Save Award")).click();
            vendorPage.waitForTimeout(2000);
            DashboardManager.log("   ✅ Award Added.");

            DashboardManager.log("   ⚙️ Filling Engagement & Financials...");
            vendorPage.locator("button[role='combobox']").nth(1).click();
            vendorPage.waitForTimeout(500);
            vendorPage.locator("div[role='option']")
                    .filter(new Locator.FilterOptions().setHasText("Both")).click();
            vendorPage.waitForTimeout(1000);

            vendorPage.locator("input[name='currentCtc']").clear();
            vendorPage.locator("input[name='currentCtc']").fill("1200000");

            vendorPage.locator("input[name='expectedCtc']").clear();
            vendorPage.locator("input[name='expectedCtc']").fill("2000000");

            Locator agencyCost = vendorPage.locator("input[name='agencyCost']");
            if (agencyCost.isVisible()) { agencyCost.clear(); agencyCost.fill("95000"); }

            Locator hourlyCost = vendorPage.locator("input[name='hourly_cost_estimate']");
            if (hourlyCost.isVisible()) { hourlyCost.clear(); hourlyCost.fill("594"); }

            vendorPage.waitForTimeout(1000);
            DashboardManager.log("   ✅ Engagement & Financials Filled.");

            DashboardManager.log("   ⏳ Selecting Notice Period...");
            try {
                vendorPage.locator("button[role='combobox']")
                        .filter(new Locator.FilterOptions().setHasText("Select notice period"))
                        .click();
                vendorPage.waitForTimeout(500);
                vendorPage.locator("div[role='option']")
                        .filter(new Locator.FilterOptions().setHasText("Available Immediately"))
                        .click();
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   ✅ Notice Period: Available Immediately");
            } catch (Exception e) {
                DashboardManager.log("   ❌ Notice Period Failed: " + e.getMessage());
            }

            DashboardManager.log("   📍 Selecting Location...");
            vendorPage.locator("button[role='combobox']")
                    .filter(new Locator.FilterOptions().setHasText("Select location")).click();
            vendorPage.waitForTimeout(500);
            vendorPage.locator("input[placeholder='Search...']").fill("New Delhi");
            vendorPage.waitForTimeout(1000);
            vendorPage.locator("div[role='option']")
                    .filter(new Locator.FilterOptions().setHasText("New Delhi, Delhi, India")).click();
            vendorPage.waitForTimeout(1000);
            DashboardManager.log("   ✅ Location: New Delhi, Delhi, India");

            DashboardManager.log("   🌍 Adding Serviceable Locations...");
            try {
                vendorPage.locator("button").filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^All$"))).first().click();
                vendorPage.waitForTimeout(1000);

                vendorPage.locator("button").filter(new Locator.FilterOptions()
                                .setHasText("Add Locations")).first()
                        .click(new Locator.ClickOptions().setForce(true));
                vendorPage.waitForTimeout(1000);

                Locator locSearch = vendorPage.locator("input[placeholder='Try entering a city or state']");
                locSearch.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                locSearch.fill("New Delhi");
                vendorPage.waitForTimeout(1500);

                vendorPage.locator("div.flex.items-center.justify-between")
                        .filter(new Locator.FilterOptions().setHasText("New Delhi, Delhi, India"))
                        .locator("button[role='checkbox']").click();
                vendorPage.waitForTimeout(500);

                locSearch.clear();
                locSearch.fill("United States");
                vendorPage.waitForTimeout(1500);

                vendorPage.locator("div.flex.items-center.justify-between")
                        .filter(new Locator.FilterOptions().setHasText("United States"))
                        .locator("button[role='checkbox']").first().click();
                vendorPage.waitForTimeout(500);

                vendorPage.locator("button").filter(new Locator.FilterOptions()
                        .setHasText("Save Selection")).click();
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   ✅ Serviceable Locations Saved.");
            } catch (Exception e) {
                DashboardManager.log("   ❌ Serviceable Locations Failed: " + e.getMessage());
            }

            DashboardManager.log("   🌐 Selecting Timezone...");
            vendorPage.locator("div[role='combobox']")
                    .filter(new Locator.FilterOptions().setHasText("Select Timezones")).click();
            vendorPage.waitForTimeout(500);
            vendorPage.locator("input[placeholder='Search...']").fill("India");
            vendorPage.waitForTimeout(1000);
            vendorPage.locator("div.relative.flex.cursor-pointer")
                    .filter(new Locator.FilterOptions().setHasText("India")).first().click();
            vendorPage.waitForTimeout(1000);
            DashboardManager.log("   ✅ Timezone: India");

            DashboardManager.log("   💾 Saving Member...");
            vendorPage.locator("button")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Available$"))).first()
                    .click();
            vendorPage.waitForTimeout(500);

            long saveStart = System.currentTimeMillis();
            vendorPage.locator("button")
                    .filter(new Locator.FilterOptions().setHasText("Save Member Details")).click();

            vendorPage.locator("span")
                    .filter(new Locator.FilterOptions().setHasText("Team member added successfully!"))
                    .first()
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE).setTimeout(20000));

            long saveDuration = System.currentTimeMillis() - saveStart;
            DashboardManager.log("   ✅ Member Saved.");
            DashboardManager.log("   " + (saveDuration <= 500
                    ? "✅ Save API Fast → " + saveDuration + "ms"
                    : "⚠️ Save API Slow → " + saveDuration + "ms (exceeds 500ms)"));
            vendorPage.waitForTimeout(3000);

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            DashboardManager.log("❌ Inline Add Member Failed: " + e.getMessage());
        }

        submitPage.submitCandidates();
        submitPage.submitCandidates();

        DashboardManager.log("🔍 Verifying submitted candidate status inline...");
        try {
            vendorPage.locator("tr.group")
                    .filter(new Locator.FilterOptions().setHasText("Applied"))
                    .first()
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE).setTimeout(30000));

            Locator candidate1Row = vendorPage.locator("tr.group")
                    .filter(new Locator.FilterOptions().setHasText("Candidate 1")).first();

            if (candidate1Row.count() > 0) {
                String name = candidate1Row.locator("h3").first().innerText().trim();
                String email = candidate1Row.locator("p.text-text-tertiary").first().innerText().trim();
                String status = candidate1Row.locator("span.status-blue-text").first().innerText().trim();
                DashboardManager.log("   👤 Candidate: " + name);
                DashboardManager.log("   📧 Email: " + email);
                DashboardManager.log("   📊 Status: [" + status + "]");
                if ("Applied".equalsIgnoreCase(status)) {
                    DashboardManager.log("   ✅ Candidate successfully submitted — Status: Applied");
                } else {
                    DashboardManager.log("   ❌ Unexpected status: " + status);
                }
            } else {
                DashboardManager.log("   ⚠️ Candidate 1 row not found in table.");
            }
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Status verification skipped: " + e.getMessage());
        }

        DashboardManager.log("[REPORT] 🎉 Candidate Submitted Successfully.");

        // =====================================================================================
        // PHASE 3: INTERVIEW FLOW
        // =====================================================================================
        DashboardManager.log("\n========================================================");
        DashboardManager.log("                 PHASE 3: INTERVIEW FLOW                  ");
        DashboardManager.log("========================================================\n");

        // 3.1 Admin Requests Interview Slots
        startAdminTrace("Phase3-Admin-Interview-Request", "Admin - Request Interview Slots");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        ScheduleInterviewPage interviewPage = new ScheduleInterviewPage(adminPage);
        searchAndOpenRequirementFromTest(firstReqName);

        DashboardManager.log("   -> Opening Candidate 1...");
        adminPage.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        adminPage.waitForTimeout(1000);
        adminPage.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1")).first()
                .locator("button[title='View Details']").first().click();
        adminPage.waitForTimeout(2000);

        interviewPage.updateStatusToScheduleInterview();
        interviewPage.selectInterviewTimeSlots();
        interviewPage.verifyInterviewDetails();

        stopAdminTrace("interview-request-slots");

        // 3.2 Vendor Selects Interview Time — reuse vendor tab
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS);

        UploadInterviewPage uploadInterviewPage = new UploadInterviewPage(vendorPage);
        uploadInterviewPage.vendorSelectInterviewTime(VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS, firstReqName);

        // 3.3 Admin Schedules & Gives Feedback
        startAdminTrace("Phase3-Admin-Interview-Feedback", "Admin - Interview Feedback");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        UploadInterviewPage adminFeedbackPage = new UploadInterviewPage(adminPage);
        adminFeedbackPage.adminScheduleAndFeedbackInterview(firstReqName, "Candidate 1");

        stopAdminTrace("interview-feedback");

        // 3.4 Vendor Verifies Final Interview Status — reuse vendor tab
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS);

        UploadInterviewPage vendorFinalVerifyPage = new UploadInterviewPage(vendorPage);
        vendorFinalVerifyPage.vendorVerifyFinalInterviewStatus(VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS, firstReqName);

        // =====================================================================================
        // PHASE 4: ASSIGNMENT FLOW
        // =====================================================================================
        DashboardManager.log("\n========================================================");
        DashboardManager.log("                 PHASE 4: ASSIGNMENT FLOW                 ");
        DashboardManager.log("========================================================\n");

        // 4.1 Admin Schedules Assignment
        startAdminTrace("Phase4-Admin-Schedule-Assignment", "Admin - Schedule Assignment");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        ScheduleAssignmentPage assignmentPage = new ScheduleAssignmentPage(adminPage);
        searchAndOpenRequirementFromTest(firstReqName);

        DashboardManager.log("   -> Opening Candidate 1...");
        adminPage.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        adminPage.waitForTimeout(1000);
        adminPage.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1")).first()
                .locator("button[title='View Details']").first().click();
        adminPage.waitForTimeout(2000);

        assignmentPage.updateStatusToScheduleAssignment();
        assignmentPage.scheduleAssignmentAction(JD_FILE_PATH);
        assignmentPage.verifyAssignmentDetails();

        stopAdminTrace("assignment-schedule");

        // 4.2 Vendor Submits Solution — reuse vendor tab
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS);

        ScheduleAssignmentPage vendorAssignmentPage = new ScheduleAssignmentPage(vendorPage);
        vendorAssignmentPage.vendorSubmitAssignmentSolution(VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS, firstReqName, JD_FILE_PATH);

        // 4.3 Admin Feedback on Assignment
        startAdminTrace("Phase4-Admin-Assignment-Feedback", "Admin - Assignment Feedback");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        assignmentPage.adminSubmitAssignmentFeedback(firstReqName, "Candidate 1");

        stopAdminTrace("assignment-feedback");

        // 4.4 Vendor Verifies Final Assignment Status — reuse vendor tab
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS);

        ScheduleAssignmentPage vendorVerifyAssignmentPage = new ScheduleAssignmentPage(vendorPage);
        vendorVerifyAssignmentPage.vendorVerifyFinalAssignmentStatus(VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASS, firstReqName);

        // =====================================================================================
        // PHASE 5: ASSESSMENT FLOW
        // =====================================================================================
        DashboardManager.log("\n========================================================");
        DashboardManager.log("                 PHASE 5: ASSESSMENT FLOW                 ");
        DashboardManager.log("========================================================\n");

        // 5.1 Admin: Schedule Assessment
        startAdminTrace("Phase5-Admin-Schedule-Assessment", "Admin - Schedule Assessment");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        ScheduleAssessmentPage adminAssessment = new ScheduleAssessmentPage(adminPage);
        searchAndOpenRequirementFromTest(firstReqName);

        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        adminPage.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        adminPage.waitForTimeout(1000);

        adminAssessment.openCandidateAndVerify("Candidate 1", "Vendor AED");
        adminAssessment.adminUpdateStatusToAssessment();

        stopAdminTrace("assessment-schedule-cancel");

        DashboardManager.log("\n[REPORT] 🎉 ALL PHASES COMPLETED SUCCESSFULLY!");
    }

    // ── HELPER: Navigate to URL, login only if needed ─────────────
    private void navigateAndLoginIfNeeded(Page page, String url, String email, String password) {
        page.navigate(url);
        page.waitForTimeout(2000);
        if (page.locator("input[name='email']").isVisible()) {
            DashboardManager.log("   -> Session expired. Logging in...");
            page.locator("input[name='email']").fill(email);
            page.locator("input[name='password']").fill(password);
            page.locator("button[type='submit']").click();
            page.waitForTimeout(3000);
        } else {
            DashboardManager.log("   -> Already logged in. Skipping login.");
        }
    }

    // ── HELPER: Verify top requirements ───────────────────────────
    private String verifyTopRequirements(int limit) {
        DashboardManager.log("\n[REPORT] 🔍 Verifying Table Data...");
        Locator rows = adminPage.locator("tbody tr");
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

    // ── HELPER: Search and open requirement ───────────────────────
    private void searchAndOpenRequirementFromTest(String reqName) {
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        adminPage.locator("a[href='/hiring-requests']").first().click();
        adminPage.waitForLoadState();
        adminPage.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Requirement: " + reqName);
        Locator reqSearchFilterBtn = adminPage.locator("div.font-semibold")
                .filter(new Locator.FilterOptions().setHasText("Search & Filters")).first();
        reqSearchFilterBtn.click();
        Locator reqSearchInput = adminPage.locator("input[placeholder='Search by client name, budget, title, email ...']");
        reqSearchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        reqSearchInput.fill(reqName);
        adminPage.waitForTimeout(2000);

        DashboardManager.log("   -> Opening Requirement: " + reqName);
        Locator reqRow = adminPage.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        Locator viewDetailsBtn = reqRow.locator("button[title='View Details']");

        if (viewDetailsBtn.count() > 0) {
            viewDetailsBtn.first().click();
        } else {
            reqRow.locator("a").first().click();
        }
        adminPage.waitForTimeout(2000);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                // ── Single combined trace — all tabs, all phases in one file ──
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/e2e-daily-full-trace.zip")));
                DashboardManager.log("   💾 Full E2E trace saved → target/e2e-daily-full-trace.zip");
            } catch (Exception ignored) {}
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail(
                "🚀 EMB Automation: Daily E2E Execution Report",
                "bharatpandey011@gmail.com",
                "bharat.pandey@emb.global",
                "ashish.mishra@emb.global",
                "saumya.gupta@emb.global"
        );
    }
}