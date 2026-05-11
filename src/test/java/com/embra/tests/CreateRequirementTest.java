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
    private Page page;

    private static final String JD_FILE_PATH = "src/test/resources/Anurag_DesignResume (2).pdf";

    @BeforeAll
    static void setupBrowser() throws IOException {
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
        page.navigate("https://uat-admin.embtalent.ai/login");
    }

    @Test
    void createFourRequirementsAtOnce() {
        DashboardManager.startTest("E2E Full Flow Execution");
        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // ── STEP 1: Login ──────────────────────────────────────────────
        DashboardManager.log("\n[STEP 1] 🔑 Admin Login");
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        RequirementListingPage listingPage = new RequirementListingPage(page);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        // ── STEP 2: Create Requirement ────────────────────────────────
        DashboardManager.log("\n[STEP 2] 📋 Creating Requirement");
        CreateRequirementPage createPage = new CreateRequirementPage(page);
        String commonJdPath = JD_FILE_PATH;

        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", commonJdPath)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create requirements");

        // ── STEP 3: Capture Requirement Name ──────────────────────────
        DashboardManager.log("\n[STEP 3] 🔍 Capturing Requirement Name & Status");
        String firstReqName = verifyTopRequirements(1);
        DashboardManager.log("[STEP 3] ✅ Requirement to use: " + firstReqName);

        // ── STEP 3b: Navigate to Requirement ──────────────────────────
        DashboardManager.log("\n[STEP 3b] 🔎 Navigating to Requirement: " + firstReqName);
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();

        page.locator("div.bg-gray-100").filter(new Locator.FilterOptions().setHasText("Search & Filters"))
                .first().click();
        page.waitForTimeout(1000);

        page.locator("input[placeholder='Search by client name, budget, title, email ...']")
                .fill(firstReqName);
        page.waitForTimeout(2500);

        DashboardManager.log("[STEP 3b] -> Opening Requirement via eye icon...");
        page.locator("button[title='View Details']").first().click();
        page.waitForLoadState();
        page.waitForTimeout(2000);

        // ── STEP 4: Partner Shortlisting ──────────────────────────────
        DashboardManager.log("\n[STEP 4] 🤝 Partner Shortlisting for: " + firstReqName);
        PartnerShortlistingPage partnerPage = new PartnerShortlistingPage(page);
        partnerPage.verifyRequirementStatus();
        partnerPage.navigateToPartnerShortlisting();
        page.waitForTimeout(2000);
        partnerPage.shortlistVendors(List.of("bharat pvt ltd", "Vendor Eur", "Vendor AED", "Vendor USD"));
        partnerPage.clickSendHiringRequirement();
        partnerPage.fillBudgetDetails();
        partnerPage.submitShortlisting();
        partnerPage.verifySuccessToast();
        DashboardManager.log("[STEP 4] ✅ Partner Shortlisting Completed.");

        // Save admin trace checkpoint after shortlisting
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-01-shortlisting-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-01-shortlisting-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin shortlisting trace: " + e.getMessage());
        }

        // ── STEP 5: Vendor Submit Candidate ───────────────────────────
        DashboardManager.log("\n[STEP 5] 🏢 Vendor Portal — Submitting Candidate");
        BrowserContext vendorContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        vendorContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPage = vendorContext.newPage();
        vendorPage.navigate("https://uat-vendor.embtalent.ai/login");

        SubmitCandidatePage submitPage = new SubmitCandidatePage(vendorPage);
        submitPage.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234");
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();

        // ── Inline: Add New Member ─────────────────────────────────────
        DashboardManager.log("\n[STEP 5a] 👤 Adding 1 Member Inline...");
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
            vendorPage.locator("input[name='name']").fill("Candidate 1");

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
                // Locate via parent div containing "Awards" paragraph + the button
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
            DashboardManager.log("   [5a-10] ✅ Member Saved.");
            DashboardManager.log("   [5a-10] " + (saveDuration <= 500
                    ? "✅ Save API Fast → " + saveDuration + "ms"
                    : "⚠️ Save API Slow → " + saveDuration + "ms (exceeds 500ms)"));
            vendorPage.waitForTimeout(3000);

        } catch (RuntimeException re) {
            // Save vendor trace before re-throwing so crash is captured
            try {
                vendorContext.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/vendor-1-crash-trace.zip")));
                DashboardManager.log("   💾 Vendor crash trace saved → target/vendor-1-crash-trace.zip");
            } catch (Exception ignored) {}
            throw re;
        } catch (Exception e) {
            DashboardManager.log("❌ [STEP 5a] Inline Add Member Failed: " + e.getMessage());
        }

        DashboardManager.log("\n[STEP 5b] 👥 Adding Team Members (Candidate 2, 3, 4)...");
        submitPage.addMembersFromTeam(Arrays.asList("Candidate 2", "Candidate 3", "Candidate 4"));

        DashboardManager.log("\n[STEP 5c] 🚀 Submitting Candidates for Interview...");
        submitPage.submitCandidates();

        DashboardManager.log("\n[STEP 5d] 🔍 Verifying Candidate Status...");
        submitPage.verifyCandidateStatus();

        vendorContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-1-submit-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-1-submit-trace.zip");
        vendorContext.close();
        DashboardManager.log("[STEP 5] ✅ Vendor Flow Completed.");

        // ── STEP 9: Schedule Assignment ───────────────────────────────
        DashboardManager.log("\n[STEP 9] 📋 Schedule Assignment Flow");
        page.bringToFront();
        page.waitForTimeout(1000);

        ScheduleAssignmentPage assignmentPage = new ScheduleAssignmentPage(page);
        assignmentPage.navigateAndOpenRequirement(firstReqName);
        assignmentPage.openCandidateForAssignment("Candidate 1");
        assignmentPage.updateStatusToScheduleAssignment();
        assignmentPage.scheduleAssignmentAction(JD_FILE_PATH);
        assignmentPage.verifyAssignmentDetails();
        DashboardManager.log("[STEP 9] ✅ Assignment Scheduled.");

        // Save admin trace after assignment
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-02-assignment-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-02-assignment-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin assignment trace: " + e.getMessage());
        }

        // ── STEP 10: Vendor Submits Assignment Solution ────────────────
        DashboardManager.log("\n[STEP 10] 🏢 Vendor: Submitting Assignment Solution");
        BrowserContext vendorContext4 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
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
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-4-assignment-solution-trace.zip");
        vendorContext4.close();
        DashboardManager.log("[STEP 10] ✅ Vendor Assignment Solution Submitted.");

        // ── STEP 11: Admin Assignment Feedback ────────────────────────
        DashboardManager.log("\n[STEP 11] 👮 Admin: Reviewing Assignment & Submitting Feedback");
        page.bringToFront();
        page.waitForTimeout(1000);
        assignmentPage.adminSubmitAssignmentFeedback(firstReqName, "Candidate 1");
        DashboardManager.log("[STEP 11] ✅ Admin Feedback Completed.");

        // Save admin trace after assignment feedback
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-03-assignment-feedback-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-03-assignment-feedback-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin assignment feedback trace: " + e.getMessage());
        }

        // ── STEP 12: Vendor Verify Assignment Status ───────────────────
        DashboardManager.log("\n[STEP 12] 🏢 Vendor: Verifying Final Assignment Status");
        BrowserContext vendorContext5 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        vendorContext5.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPage5 = vendorContext5.newPage();

        ScheduleAssignmentPage finalVendorPage = new ScheduleAssignmentPage(vendorPage5);
        finalVendorPage.vendorVerifyFinalAssignmentStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContext5.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-5-assignment-verify-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-5-assignment-verify-trace.zip");
        vendorContext5.close();
        DashboardManager.log("[STEP 12] ✅ Vendor Assignment Verification Completed.");

        // ── STEP 13: Schedule Interview ───────────────────────────────
        DashboardManager.log("\n[STEP 13] 📅 Schedule Interview Flow");
        page.bringToFront();
        page.waitForTimeout(1000);

        ScheduleInterviewPage interviewPage = new ScheduleInterviewPage(page);
        interviewPage.navigateAndOpenRequirement(firstReqName);
        interviewPage.openCandidateForInterview("Candidate 1");
        interviewPage.updateStatusToScheduleInterview();
        interviewPage.selectInterviewTimeSlots();
        interviewPage.verifyInterviewDetails();
        DashboardManager.log("[STEP 13] ✅ Interview Time Slots Requested.");

        // Save admin trace after interview request
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-04-interview-request-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-04-interview-request-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin interview request trace: " + e.getMessage());
        }

        // ── STEP 14: Vendor Selects Interview Time ────────────────────
        DashboardManager.log("\n[STEP 14] 🏢 Vendor: Selecting Interview Time Slots");
        BrowserContext vendorContext6 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        vendorContext6.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPage6 = vendorContext6.newPage();

        UploadInterviewPage uploadInterviewPage = new UploadInterviewPage(vendorPage6);
        uploadInterviewPage.vendorSelectInterviewTime(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContext6.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-6-interview-slots-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-6-interview-slots-trace.zip");
        vendorContext6.close();
        DashboardManager.log("[STEP 14] ✅ Vendor Interview Time Slots Selected.");

        // ── STEP 15: Admin Interview Feedback ─────────────────────────
        DashboardManager.log("\n[STEP 15] 👮 Admin: Scheduling Interview & Submitting Feedback");
        page.bringToFront();
        page.waitForTimeout(1000);

        UploadInterviewPage adminUploadInterview = new UploadInterviewPage(page);
        adminUploadInterview.adminScheduleAndFeedbackInterview(firstReqName, "Candidate 1");
        DashboardManager.log("[STEP 15] ✅ Admin Interview Scheduled & Feedback Submitted.");

        // Save admin trace after interview feedback
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-05-interview-feedback-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-05-interview-feedback-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin interview feedback trace: " + e.getMessage());
        }

        // ── STEP 16: Vendor Verify Interview Status ───────────────────
        DashboardManager.log("\n[STEP 16] 🏢 Vendor: Verifying Final Interview Status");
        BrowserContext vendorContext7 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        vendorContext7.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPage7 = vendorContext7.newPage();

        UploadInterviewPage finalVerifyInterviewPage = new UploadInterviewPage(vendorPage7);
        finalVerifyInterviewPage.vendorVerifyFinalInterviewStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContext7.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-7-interview-verify-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-7-interview-verify-trace.zip");
        vendorContext7.close();
        DashboardManager.log("[STEP 16] ✅ Vendor Interview Verification Completed.");

        // ── STEP 17: Offer Job & Deploy ───────────────────────────────
        DashboardManager.log("\n[STEP 17] 💼 Offer Job & Deploy Flow");
        page.bringToFront();
        page.waitForTimeout(1000);

        OfferJobPage offerPage = new OfferJobPage(page);
        offerPage.navigateAndOpenRequirement(firstReqName);
        offerPage.openCandidateAndVerifyStatus("Candidate 1");
        offerPage.updateStatusToOfferJob();
        offerPage.deployCandidate(JD_FILE_PATH);
        DashboardManager.log("[STEP 17a] ✅ Admin Deploy Completed.");

        // Save admin trace after deploy
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-06-deploy-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-06-deploy-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin deploy trace: " + e.getMessage());
        }

        DashboardManager.log("\n[STEP 17b] 🏢 Vendor: Verifying Deployment");
        BrowserContext vendorContext8 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        vendorContext8.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPage8 = vendorContext8.newPage();

        OfferJobPage vendorVerifyDeploy = new OfferJobPage(vendorPage8);
        vendorVerifyDeploy.vendorVerifyDeployedStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContext8.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-8-deploy-verify-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-8-deploy-verify-trace.zip");
        vendorContext8.close();
        DashboardManager.log("[STEP 17] ✅ Deploy Flow Completed.");

        // ── STEP 18: Hold, Reject & Share with Client ─────────────────
        DashboardManager.log("\n[STEP 18] 🔄 Hold, Reject & Share with Client Flow");
        page.bringToFront();
        page.waitForTimeout(1000);

        HoldRejectSentClientPage postDeployPage = new HoldRejectSentClientPage(page);
        postDeployPage.processCandidatesOnAdmin(firstReqName);
        postDeployPage.printFinalSummaryAdmin();

        // Save admin trace after hold/reject
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-07-hold-reject-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-07-hold-reject-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin hold/reject trace: " + e.getMessage());
        }

        DashboardManager.log("\n[STEP 18b] 🏢 Vendor: Verifying All Final Statuses");
        BrowserContext vendorContextFinal = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        vendorContextFinal.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPageFinal = vendorContextFinal.newPage();

        HoldRejectSentClientPage vendorFinalVerify = new HoldRejectSentClientPage(vendorPageFinal);
        vendorFinalVerify.vendorVerifyFinalStatuses(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContextFinal.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-final-statuses-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-final-statuses-trace.zip");
        vendorContextFinal.close();
        DashboardManager.log("[STEP 18] ✅ Hold/Reject/Share Flow Completed.");

        // ── STEP 19: Allow Resubmission ───────────────────────────────
        DashboardManager.log("\n[STEP 19] 🔁 Allow Resubmission Flow");
        page.bringToFront();

        AllowResubmissionPage resubmitPage = new AllowResubmissionPage(page);
        resubmitPage.allowResubmissionsOnAdmin(firstReqName);

        // Save admin trace after resubmission
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-08-resubmission-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-08-resubmission-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin resubmission trace: " + e.getMessage());
        }

        BrowserContext vendorContextResubmit = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        vendorContextResubmit.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page vendorPageResubmit = vendorContextResubmit.newPage();

        AllowResubmissionPage vendorResubmit = new AllowResubmissionPage(vendorPageResubmit);
        vendorResubmit.vendorPerformResubmission(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContextResubmit.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-resubmit-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-resubmit-trace.zip");
        vendorContextResubmit.close();
        DashboardManager.log("[STEP 19] ✅ Resubmission Flow Completed.");

        // ── STEP 20: Client Shortlist & Reject ────────────────────────
        DashboardManager.log("\n[STEP 20] 🤝 Client Shortlist & Reject Flow");
        BrowserContext clientContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        clientContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page clientPage = clientContext.newPage();

        ClientShortlistPage clientFlow = new ClientShortlistPage(clientPage);

        DashboardManager.log("[STEP 20a] 🤝 Client: Login & Shortlist");
        clientFlow.loginAndShortlist("https://uat-client.embtalent.ai/login", "AutoTest@yopmail.com", "Emb@1234", firstReqName);

        // Save client trace after shortlist — before reject (crash safety)
        try {
            clientContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-20a-shortlist-trace.zip")));
            DashboardManager.log("   💾 Client trace saved → target/client-20a-shortlist-trace.zip");
            clientContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save client shortlist trace: " + e.getMessage());
        }

        DashboardManager.log("[STEP 20b] 👮 Admin: Verify Shortlist");
        page.bringToFront();
        ClientShortlistPage adminVerify = new ClientShortlistPage(page);
        adminVerify.verifyShortlistOnAdmin(firstReqName);

        // Save admin trace after shortlist verify
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-09-client-shortlist-verify-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-09-client-shortlist-verify-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin shortlist verify trace: " + e.getMessage());
        }

        DashboardManager.log("[STEP 20c] 🤝 Client: Reject Candidate");
        clientPage.bringToFront();
        clientFlow.clientRejectCandidate(firstReqName);

        // Save client trace after reject
        try {
            clientContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-20c-reject-trace.zip")));
            DashboardManager.log("   💾 Client trace saved → target/client-20c-reject-trace.zip");
            clientContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save client reject trace: " + e.getMessage());
        }

        DashboardManager.log("[STEP 20d] 👮 Admin: Verify Rejection");
        page.bringToFront();
        adminVerify.verifyRejectionOnAdmin(firstReqName);

        clientContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-portal-trace.zip")));
        DashboardManager.log("   💾 Client final trace saved → target/client-portal-trace.zip");
        clientContext.close();
        DashboardManager.log("[STEP 20] ✅ Client Shortlist & Reject Flow Completed.");

        // Save admin trace after rejection verify
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-10-rejection-verify-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-10-rejection-verify-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin rejection verify trace: " + e.getMessage());
        }

        // ── STEP 21: Requirement Completion ───────────────────────────
        DashboardManager.log("\n[STEP 21] 🏁 Requirement Completion Flow");
        RequirementCompletedPage completedFlow = new RequirementCompletedPage(page);

        DashboardManager.log("[STEP 21a] 👮 Admin: Deploy Candidate 2");
        completedFlow.adminDeployCandidate(firstReqName, "Candidate 2", "bharat pvt ltd", JD_FILE_PATH);

        // Save admin trace after completion deploy
        try {
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/admin-11-completion-deploy-trace.zip")));
            DashboardManager.log("   💾 Admin trace saved → target/admin-11-completion-deploy-trace.zip");
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not save admin completion deploy trace: " + e.getMessage());
        }

        DashboardManager.log("[STEP 21b] 🏢 Vendor: Verify Completion Status");
        BrowserContext vendorCtx = browser.newContext();
        vendorCtx.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        RequirementCompletedPage vendorVerify = new RequirementCompletedPage(vendorCtx.newPage());
        vendorVerify.verifyPortalStatus("Vendor", "https://uat-vendor.embtalent.ai/login", "bharat.pandey+1@emb.global", "Emb@1234", firstReqName);

        vendorCtx.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-completion-verify-trace.zip")));
        DashboardManager.log("   💾 Vendor trace saved → target/vendor-completion-verify-trace.zip");
        vendorCtx.close();

        DashboardManager.log("[STEP 21c] 🤝 Client: Verify Completion Status");
        BrowserContext clientCtx = browser.newContext();
        clientCtx.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        RequirementCompletedPage clientVerify = new RequirementCompletedPage(clientCtx.newPage());
        clientVerify.verifyPortalStatus("Client", "https://uat-client.embtalent.ai/login", "AutoTest@yopmail.com", "Emb@1234", firstReqName);

        clientCtx.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-completion-verify-trace.zip")));
        DashboardManager.log("   💾 Client trace saved → target/client-completion-verify-trace.zip");
        clientCtx.close();

        DashboardManager.log("\n[REPORT] ✅ Full E2E Journey Completed Successfully!");
    }

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
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/admin-trace-latest.zip")));
                DashboardManager.log("   💾 Admin fallback trace saved → target/admin-trace-latest.zip");
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

        DashboardManager.flushReport();
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
        EmailSender.sendDashboardEmail("bharat.pandey@emb.global");
        EmailSender.sendDashboardEmail("ashish.mishra@emb.global");
        EmailSender.sendDashboardEmail("saumya.gupta@emb.global");
        EmailSender.sendDashboardEmail("prakash@emb.global");
    }
}