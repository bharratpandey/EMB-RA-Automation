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

public class CreateRequirementTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;

    // ── Single browser window — three tabs (admin, vendor, client) ──
    private Page adminPage;
    private Page vendorPage;
    private Page clientPage;

    private static final String JD_FILE_PATH = "src/test/resources/Anurag_DesignResume (2).pdf";

    // ── Login URLs — used only for first login ────────────────────
    private static final String ADMIN_URL  = "https://uat-admin.embtalent.ai/login";
    private static final String VENDOR_URL = "https://uat-vendor.embtalent.ai/login";
    private static final String CLIENT_URL = "https://uat-client.embtalent.ai/login";

    // ── Dashboard URLs — used for already-logged-in tabs ──────────
    private static final String VENDOR_DASHBOARD = "https://uat-vendor.embtalent.ai/projects";
    private static final String CLIENT_DASHBOARD  = "https://uat-client.embtalent.ai/jobs";

    // ── Credentials ───────────────────────────────────────────────
    private static final String ADMIN_EMAIL    = "bharat.pandey@emb.global";
    private static final String ADMIN_PASSWORD = "Emb@1234";

    private static final String VENDOR_EMAIL    = "bharat.pandey+1@emb.global";
    private static final String VENDOR_PASSWORD = "Emb@1234";

    private static final String CLIENT_EMAIL    = "bharat.pandey@emb.global";
    private static final String CLIENT_PASSWORD = "Emb@1234";

    private static final String CLIENT_EMAIL_ALT = "bharat.pandey@emb.global"; // step 21c

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
        // ── Single context — all three tabs share same browser window ──
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // ── Single tracing session — runs continuously across all steps ──
        // No stop/restart mid-test. One final trace saved in tearDown.
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        // Tab 1 — Admin portal
        adminPage = context.newPage();
        adminPage.setDefaultTimeout(60000);
        adminPage.setDefaultNavigationTimeout(90000);
        adminPage.navigate(ADMIN_URL);

        // Tab 2 — Vendor portal (opens login page, login happens in step 5)
        vendorPage = context.newPage();
        vendorPage.setDefaultTimeout(60000);
        vendorPage.setDefaultNavigationTimeout(90000);
        vendorPage.navigate(VENDOR_URL);

        // Tab 3 — Client portal (opens login page, login happens in step 20)
        clientPage = context.newPage();
        clientPage.setDefaultTimeout(60000);
        clientPage.setDefaultNavigationTimeout(90000);
        clientPage.navigate(CLIENT_URL);

        // Switch back to admin tab to start the test
        adminPage.bringToFront();
    }

    @Test
    void createFourRequirementsAtOnce() {
        DashboardManager.startTest("E2E Full Flow Execution");
        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // ── STEP 1: Admin Login ────────────────────────────────────────
        DashboardManager.log("\n[STEP 1] 🔑 Admin Login");
        adminPage.bringToFront();
        LoginPage loginPage = new LoginPage(adminPage);
        assertTrue(loginPage.login(ADMIN_EMAIL, ADMIN_PASSWORD), "Login failed");

        RequirementListingPage listingPage = new RequirementListingPage(adminPage);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        // ── STEP 2: Create Requirement ────────────────────────────────
        DashboardManager.log("\n[STEP 2] 📋 Creating Requirement");
        CreateRequirementPage createPage = new CreateRequirementPage(adminPage);

        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", JD_FILE_PATH)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create requirements");

        // ── STEP 3: Capture Requirement Name ──────────────────────────
        DashboardManager.log("\n[STEP 3] 🔍 Capturing Requirement Name & Status");
        String firstReqName = verifyTopRequirements(1);
        DashboardManager.log("[STEP 3] ✅ Requirement to use: " + firstReqName);

        // ── STEP 3b: Navigate to Requirement ──────────────────────────
        DashboardManager.log("\n[STEP 3b] 🔎 Navigating to Requirement: " + firstReqName);
        adminPage.locator("a[href='/hiring-requests']").first().click();
        adminPage.waitForLoadState();

        adminPage.locator("div.bg-gray-100").filter(new Locator.FilterOptions().setHasText("Search & Filters"))
                .first().click();
        adminPage.waitForTimeout(1000);

        adminPage.locator("input[placeholder='Search by client name, budget, title, email ...']")
                .fill(firstReqName);
        adminPage.waitForTimeout(2500);

        DashboardManager.log("[STEP 3b] -> Opening Requirement via eye icon...");
        adminPage.locator("button[title='View Details']").first().click();
        adminPage.waitForLoadState();
        adminPage.waitForTimeout(2000);

        // ── STEP 4: Partner Shortlisting ──────────────────────────────
        DashboardManager.log("\n[STEP 4] 🤝 Partner Shortlisting for: " + firstReqName);
        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(adminPage);
        partnerPage.verifyRequirementStatus();
        partnerPage.navigateToPartnerShortlisting();
        adminPage.waitForTimeout(2000);
        partnerPage.shortlistVendors(List.of("bharat pvt ltd", "Vendor Eur", "Vendor AED", "Vendor USD"));
        partnerPage.clickSendHiringRequirement();
        partnerPage.fillBudgetDetails();
        partnerPage.submitShortlisting();
        partnerPage.verifySuccessToast();
        DashboardManager.log("[STEP 4] ✅ Partner Shortlisting Completed.");

        // ── STEP 5: Vendor Login & Submit Candidate ───────────────────
        DashboardManager.log("\n[STEP 5] 🏢 Vendor Portal — Submitting Candidate");
        vendorPage.bringToFront();

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.loginToVendorPortal(VENDOR_EMAIL, VENDOR_PASSWORD);
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();

        // ── Inline: Add New Members in loop ───────────────────────────
        List<String> inlineCandidates = List.of("Candidate 1", "Candidate 2");

        for (int candidateIndex = 0; candidateIndex < inlineCandidates.size(); candidateIndex++) {
            String candidateName = inlineCandidates.get(candidateIndex);
            DashboardManager.log("\n[STEP 5a] 👤 Adding Member Inline: " + candidateName);
            try {
                Locator addBtn = vendorPage.locator("button")
                        .filter(new Locator.FilterOptions().setHasText("Add New Member")).first();
                addBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                addBtn.click();
                vendorPage.waitForTimeout(1000);

                DashboardManager.log("   [5a-1] 📤 Uploading Resume...");
                vendorPage.locator("input[type='file'][accept='.pdf']").first()
                        .setInputFiles(Paths.get(JD_FILE_PATH));
                DashboardManager.log("   [5a-1] ✅ Resume file attached.");
                vendorPage.waitForTimeout(2000);

                DashboardManager.log("   [5a-2] -> Clicking 'Import from resume'...");
                vendorPage.locator("button")
                        .filter(new Locator.FilterOptions().setHasText("Import from resume"))
                        .first()
                        .click(new Locator.ClickOptions().setForce(true));
                DashboardManager.log("   [5a-2] -> Clicked. Waiting for extraction toast (max 59s)...");

                boolean toastFound = false;
                try {
                    vendorPage.locator("span")
                            .filter(new Locator.FilterOptions().setHasText("Resume details extracted!"))
                            .first()
                            .waitFor(new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE).setTimeout(59000));
                    DashboardManager.log("   [5a-2] ✅ Toast: 'Resume details extracted!' — Extraction working fine");
                    toastFound = true;
                } catch (Exception e) {
                    DashboardManager.log("   [5a-2] ⚠️ Toast not found in 59s — waiting additional 60s (120s total)...");
                }

                if (!toastFound) {
                    try {
                        vendorPage.locator("span")
                                .filter(new Locator.FilterOptions().setHasText("Resume details extracted!"))
                                .first()
                                .waitFor(new Locator.WaitForOptions()
                                        .setState(WaitForSelectorState.VISIBLE).setTimeout(60000));
                        DashboardManager.log("   [5a-2] ✅ Toast found after extended wait.");
                    } catch (Exception e) {
                        DashboardManager.log("   [5a-2] ❌ SLOW EXTRACTION — Toast not visible after 120s. Stopping automation.");
                        throw new RuntimeException("SLOW EXTRACTION: Resume details extracted toast not visible after 120 seconds.");
                    }
                }
                vendorPage.waitForTimeout(1500);

                DashboardManager.log("   [5a-3] 📝 Filling Basic Info...");
                vendorPage.locator("input[name='name']").clear();
                vendorPage.locator("input[name='name']").fill(candidateName);

                vendorPage.locator("input[name='email']").last().clear();
                vendorPage.locator("input[name='email']").last()
                        .fill("TestMember" + System.currentTimeMillis() + "@yopmail.com");

                vendorPage.locator("input[name='linkedin']").clear();
                vendorPage.locator("input[name='linkedin']").fill("https://in.linkedin.com/company/embglobal");

                vendorPage.locator("input[name='interviewLink']").clear();
                vendorPage.locator("input[name='interviewLink']").fill("https://www.example.com/");
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   [5a-3] ✅ Basic Info Filled.");

                DashboardManager.log("   [5a-4] 🏆 Adding Award...");
                try {
                    Locator achievementsSection = vendorPage.locator("div.flex.flex-col.gap-4")
                            .filter(new Locator.FilterOptions().setHasText("Achievements"));
                    Locator addAwardsBtn = achievementsSection.locator("button.rounded-3xl")
                            .filter(new Locator.FilterOptions().setHasText("Add Awards"))
                            .first();
                    addAwardsBtn.scrollIntoViewIfNeeded();
                    vendorPage.waitForTimeout(500);
                    addAwardsBtn.click(new Locator.ClickOptions().setForce(true));
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
                    DashboardManager.log("   [5a-4] ✅ Award Added.");
                } catch (Exception e) {
                    DashboardManager.log("   [5a-4] ⚠️ Award Failed: " + e.getMessage());
                }

                DashboardManager.log("   [5a-5] ⚙️ Filling Engagement & Financials...");
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
                DashboardManager.log("   [5a-5] ✅ Engagement & Financials Filled.");

                DashboardManager.log("   [5a-6] ⏳ Selecting Notice Period...");
                vendorPage.locator("button[role='combobox']")
                        .filter(new Locator.FilterOptions().setHasText("Select notice period")).click();
                vendorPage.waitForTimeout(500);
                vendorPage.locator("div[role='option']")
                        .filter(new Locator.FilterOptions().setHasText("Available Immediately")).click();
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   [5a-6] ✅ Notice Period: Available Immediately");

                DashboardManager.log("   [5a-7] 📍 Selecting Current Location...");
                vendorPage.locator("button[role='combobox']")
                        .filter(new Locator.FilterOptions().setHasText("Select location")).click();
                vendorPage.waitForTimeout(500);
                vendorPage.locator("input[placeholder='Search...']").fill("New Delhi");
                vendorPage.waitForTimeout(1000);
                vendorPage.locator("div[role='option']")
                        .filter(new Locator.FilterOptions().setHasText("New Delhi, Delhi, India")).click();
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   [5a-7] ✅ Location: New Delhi, Delhi, India");

                DashboardManager.log("   [5a-8] 🌍 Adding Serviceable Locations...");
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
                DashboardManager.log("   [5a-8] ✅ Serviceable Locations Saved (New Delhi + United States).");

                DashboardManager.log("   [5a-9] 🌐 Selecting Timezone...");
                vendorPage.locator("div[role='combobox']")
                        .filter(new Locator.FilterOptions().setHasText("Select Timezones")).click();
                vendorPage.waitForTimeout(500);
                vendorPage.locator("input[placeholder='Search...']").fill("India");
                vendorPage.waitForTimeout(1000);
                vendorPage.locator("div.relative.flex.cursor-pointer")
                        .filter(new Locator.FilterOptions().setHasText("India")).first().click();
                vendorPage.waitForTimeout(1000);
                DashboardManager.log("   [5a-9] ✅ Timezone: India");

                DashboardManager.log("   [5a-10] 💾 Saving Member...");
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
                DashboardManager.log("   [5a-10] ✅ Member Saved: " + candidateName);
                DashboardManager.log("   [5a-10] " + (saveDuration <= 500
                        ? "✅ Save API Fast → " + saveDuration + "ms"
                        : "⚠️ Save API Slow → " + saveDuration + "ms (exceeds 500ms)"));
                vendorPage.waitForTimeout(3000);

            } catch (RuntimeException re) {
                // Save trace on crash — captures state at point of failure
                try {
                    context.tracing().stop(new Tracing.StopOptions()
                            .setPath(Paths.get("target/e2e-crash-trace.zip")));
                    DashboardManager.log("   💾 Crash trace saved → target/e2e-crash-trace.zip");
                } catch (Exception ignored) {}
                throw re;
            } catch (Exception e) {
                DashboardManager.log("❌ [STEP 5a] Inline Add Member Failed for " + candidateName + ": " + e.getMessage());
            }
        } // ← end of inline candidates loop

        DashboardManager.log("\n[STEP 5b] 👥 Adding Team Members (Candidate 3, 4)...");
        submitPage.addMembersFromTeam(Arrays.asList("Candidate 3", "Candidate 4"));

        DashboardManager.log("\n[STEP 5c] 🚀 Submitting Candidates for Interview...");
        submitPage.submitCandidates();

        DashboardManager.log("\n[STEP 5d] 🔍 Verifying Candidate Status...");
        submitPage.verifyCandidateStatus();

        DashboardManager.log("[STEP 5] ✅ Vendor Flow Completed.");

        // ── STEP 9: Schedule Assignment (Admin) ───────────────────────
        DashboardManager.log("\n[STEP 9] 📋 Schedule Assignment Flow");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        ScheduleAssignmentPage assignmentPage = new ScheduleAssignmentPage(adminPage);
        assignmentPage.navigateAndOpenRequirement(firstReqName);
        assignmentPage.openCandidateForAssignment("Candidate 1");
        assignmentPage.updateStatusToScheduleAssignment();
        assignmentPage.scheduleAssignmentAction(JD_FILE_PATH);
        assignmentPage.verifyAssignmentDetails();
        DashboardManager.log("[STEP 9] ✅ Assignment Scheduled.");

        // ── STEP 10: Vendor Submits Assignment Solution ────────────────
        DashboardManager.log("\n[STEP 10] 🏢 Vendor: Submitting Assignment Solution");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        ScheduleAssignmentPage vendorAssignmentPage = new ScheduleAssignmentPage(vendorPage);
        vendorAssignmentPage.vendorSubmitAssignmentSolution(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName, JD_FILE_PATH);

        DashboardManager.log("[STEP 10] ✅ Vendor Assignment Solution Submitted.");

        // ── STEP 11: Admin Assignment Feedback ────────────────────────
        DashboardManager.log("\n[STEP 11] 👮 Admin: Reviewing Assignment & Submitting Feedback");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);
        assignmentPage.adminSubmitAssignmentFeedback(firstReqName, "Candidate 1");
        DashboardManager.log("[STEP 11] ✅ Admin Feedback Completed.");

        // ── STEP 12: Vendor Verify Assignment Status ───────────────────
        DashboardManager.log("\n[STEP 12] 🏢 Vendor: Verifying Final Assignment Status");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        ScheduleAssignmentPage finalVendorAssignmentPage = new ScheduleAssignmentPage(vendorPage);
        finalVendorAssignmentPage.vendorVerifyFinalAssignmentStatus(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 12] ✅ Vendor Assignment Verification Completed.");

        // ── STEP 13: Schedule Interview (Admin) ───────────────────────
        DashboardManager.log("\n[STEP 13] 📅 Schedule Interview Flow");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        ScheduleInterviewPage interviewPage = new ScheduleInterviewPage(adminPage);
        interviewPage.navigateAndOpenRequirement(firstReqName);
        interviewPage.openCandidateForInterview("Candidate 1");
        interviewPage.updateStatusToScheduleInterview();
        interviewPage.selectInterviewTimeSlots();
        interviewPage.verifyInterviewDetails();
        DashboardManager.log("[STEP 13] ✅ Interview Time Slots Requested.");

        // ── STEP 14: Vendor Selects Interview Time ────────────────────
        DashboardManager.log("\n[STEP 14] 🏢 Vendor: Selecting Interview Time Slots");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        UploadInterviewPage uploadInterviewPage = new UploadInterviewPage(vendorPage);
        uploadInterviewPage.vendorSelectInterviewTime(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 14] ✅ Vendor Interview Time Slots Selected.");

        // ── STEP 15: Admin Interview Feedback ─────────────────────────
        DashboardManager.log("\n[STEP 15] 👮 Admin: Scheduling Interview & Submitting Feedback");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        UploadInterviewPage adminUploadInterview = new UploadInterviewPage(adminPage);
        adminUploadInterview.adminScheduleAndFeedbackInterview(firstReqName, "Candidate 1");
        DashboardManager.log("[STEP 15] ✅ Admin Interview Scheduled & Feedback Submitted.");

        // ── STEP 16: Vendor Verify Interview Status ───────────────────
        DashboardManager.log("\n[STEP 16] 🏢 Vendor: Verifying Final Interview Status");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        UploadInterviewPage finalVerifyInterviewPage = new UploadInterviewPage(vendorPage);
        finalVerifyInterviewPage.vendorVerifyFinalInterviewStatus(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 16] ✅ Vendor Interview Verification Completed.");

        // ── STEP 17: Offer Job & Deploy (Admin) ───────────────────────
        DashboardManager.log("\n[STEP 17] 💼 Offer Job & Deploy Flow");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        OfferJobPage offerPage = new OfferJobPage(adminPage);
        offerPage.navigateAndOpenRequirement(firstReqName);
        offerPage.openCandidateAndVerifyStatus("Candidate 1");
        offerPage.updateStatusToOfferJob();
        offerPage.deployCandidate(JD_FILE_PATH);
        DashboardManager.log("[STEP 17a] ✅ Admin Deploy Completed.");

        // ── STEP 17b: Vendor Verify Deployment ────────────────────────
        DashboardManager.log("\n[STEP 17b] 🏢 Vendor: Verifying Deployment");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        OfferJobPage vendorVerifyDeploy = new OfferJobPage(vendorPage);
        vendorVerifyDeploy.vendorVerifyDeployedStatus(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 17] ✅ Deploy Flow Completed.");

        // ── STEP 18: Hold, Reject & Share with Client (Admin) ─────────
        DashboardManager.log("\n[STEP 18] 🔄 Hold, Reject & Share with Client Flow");
        adminPage.bringToFront();
        adminPage.waitForTimeout(1000);

        HoldRejectSentClientPage postDeployPage = new HoldRejectSentClientPage(adminPage);
        postDeployPage.processCandidatesOnAdmin(firstReqName);
        postDeployPage.printFinalSummaryAdmin();

        // ── STEP 18b: Vendor Verify Final Statuses ────────────────────
        DashboardManager.log("\n[STEP 18b] 🏢 Vendor: Verifying All Final Statuses");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        HoldRejectSentClientPage vendorFinalVerify = new HoldRejectSentClientPage(vendorPage);
        vendorFinalVerify.vendorVerifyFinalStatuses(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 18] ✅ Hold/Reject/Share Flow Completed.");

        // ── STEP 19: Allow Resubmission ───────────────────────────────
        DashboardManager.log("\n[STEP 19] 🔁 Allow Resubmission Flow");
        adminPage.bringToFront();

        AllowResubmissionPage resubmitPage = new AllowResubmissionPage(adminPage);
        resubmitPage.allowResubmissionsOnAdmin(firstReqName);

        // ── STEP 19b: Vendor Perform Resubmission ─────────────────────
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        AllowResubmissionPage vendorResubmit = new AllowResubmissionPage(vendorPage);
        vendorResubmit.vendorPerformResubmission(
                VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 19] ✅ Resubmission Flow Completed.");

        // ── STEP 20: Client Shortlist & Reject ────────────────────────
        DashboardManager.log("\n[STEP 20] 🤝 Client Shortlist & Reject Flow");
        clientPage.bringToFront();

        ClientShortlistPage clientFlow = new ClientShortlistPage(clientPage);

        DashboardManager.log("[STEP 20a] 🤝 Client: Login & Shortlist");
        clientFlow.loginAndShortlist(CLIENT_URL, CLIENT_EMAIL, CLIENT_PASSWORD, firstReqName);

        DashboardManager.log("[STEP 20b] 👮 Admin: Verify Shortlist");
        adminPage.bringToFront();
        ClientShortlistPage adminVerify = new ClientShortlistPage(adminPage);
        adminVerify.verifyShortlistOnAdmin(firstReqName);

        DashboardManager.log("[STEP 20c] 🤝 Client: Reject Candidate");
        clientPage.bringToFront();
        clientFlow.clientRejectCandidate(firstReqName);

        DashboardManager.log("[STEP 20d] 👮 Admin: Verify Rejection");
        adminPage.bringToFront();
        adminVerify.verifyRejectionOnAdmin(firstReqName);

        DashboardManager.log("[STEP 20] ✅ Client Shortlist & Reject Flow Completed.");

        // ── STEP 21: Requirement Completion ───────────────────────────
        DashboardManager.log("\n[STEP 21] 🏁 Requirement Completion Flow");
        adminPage.bringToFront();

        RequirementCompletedPage completedFlow = new RequirementCompletedPage(adminPage);

        DashboardManager.log("[STEP 21a] 👮 Admin: Deploy Candidate 2");
        completedFlow.adminDeployCandidate(firstReqName, "Candidate 2", "bharat pvt ltd", JD_FILE_PATH);

        // ── STEP 21b: Vendor Verify Completion ────────────────────────
        DashboardManager.log("[STEP 21b] 🏢 Vendor: Verify Completion Status");
        vendorPage.bringToFront();
        navigateAndLoginIfNeeded(vendorPage, VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD);

        RequirementCompletedPage vendorVerify = new RequirementCompletedPage(vendorPage);
        vendorVerify.verifyPortalStatus("Vendor", VENDOR_DASHBOARD, VENDOR_EMAIL, VENDOR_PASSWORD, firstReqName);

        // ── STEP 21c: Client Verify Completion ────────────────────────
        DashboardManager.log("[STEP 21c] 🤝 Client: Verify Completion Status");
        clientPage.bringToFront();
        navigateAndLoginIfNeeded(clientPage, CLIENT_DASHBOARD, CLIENT_EMAIL_ALT, CLIENT_PASSWORD);

        RequirementCompletedPage clientVerify = new RequirementCompletedPage(clientPage);
        clientVerify.verifyPortalStatus("Client", CLIENT_DASHBOARD, CLIENT_EMAIL_ALT, CLIENT_PASSWORD, firstReqName);

        DashboardManager.log("\n[REPORT] ✅ Full E2E Journey Completed Successfully!");
    }

    // ── HELPER: Navigate to URL, login only if needed ─────────────
    private void navigateAndLoginIfNeeded(Page page, String url, String email, String password) {
        page.navigate(url);
        page.waitForTimeout(2000);
        if (page.locator("input[name='email']").isVisible()) {
            DashboardManager.log("   -> Session expired. Logging in to: " + url);
            page.locator("input[name='email']").fill(email);
            page.locator("input[name='password']").fill(password);
            page.locator("button[type='submit']").click();
            page.waitForTimeout(3000);
        } else {
            DashboardManager.log("   -> Already logged in. Skipping login.");
        }
    }

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
            String title = row.locator("td:nth-child(2) a span.cursor-pointer").innerText().trim();
            String status = row.locator("td:nth-child(4)").innerText().trim();

            if (i == 0) firstTitle = title;

            if ("New".equalsIgnoreCase(status)) {
                DashboardManager.log("[REPORT] ✅ Row " + (i + 1) + " [" + title + "]: Status is New");
            } else {
                DashboardManager.log("[REPORT] ❌ Row " + (i + 1) + " [" + title + "]: WRONG STATUS! Found: [" + status + "]");
            }
        }
        return firstTitle;
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            try {
                // ── Single combined trace — all tabs, all steps in one file ──
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/e2e-full-trace.zip")));
                DashboardManager.log("   💾 Full E2E trace saved → target/e2e-full-trace.zip");
            } catch (Exception e) {
                System.err.println("Failed to save trace: " + e.getMessage());
            }
            context.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail(
                "🚀 EMB Automation: Weekly E2E Execution Report",
                "bharatpandey011@gmail.com",
                "bharat.pandey@emb.global"
        );
    }
}