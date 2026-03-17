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

    private static final String JD_FILE_PATH = "target/Ajay_Gupta_resume_.pdf";

    @BeforeAll
    static void setupBrowser() throws IOException {
        // 🚀 1. INITIALIZE THE DASHBOARD
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(false)
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
    void createFourRequirementsAtOnce() {
        // 🚀 2. START A NEW TEST IN THE DASHBOARD
        DashboardManager.startTest("E2E Full Flow Execution");

        DashboardManager.log("[REPORT] 🚀 Starting E2E Journey...");

        // --- 1. Login & Navigation ---
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed"); //deev=saumyaadminqa@yopmail.com,admin@122 ,uaat=bharat.pandey@emb.global



        RequirementListingPage listingPage = new RequirementListingPage(page);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        // --- 2. Create 4 Requirements ---
        CreateRequirementPage createPage = new CreateRequirementPage(page);
        String commonJdPath = JD_FILE_PATH;

        // Change the first parameter from 'false' to the desired Engagement Type String
        // Example: "Full Time", "Contractual", or "Contract To Hire"
        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", commonJdPath)
                //,new CreateRequirementPage.RequirementData("Contractual", "Hybrid", "Java", "Spring", "52107", commonJdPath),
                //new CreateRequirementPage.RequirementData("Contract To Hire", "Remote", "Python", "Django", "52108", commonJdPath),
                //new CreateRequirementPage.RequirementData("Full Time", "Onsite", "Node", "Express", "52109", commonJdPath)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create requirements");


        // ──────────────────────────────────────────────────────────────
        // 3. CAPTURE NAME & STATUS
        // ──────────────────────────────────────────────────────────────
        String firstReqName = verifyTopRequirements(1);

        /*
        String firstReqName = "AUT93 - AI Engineer 3 yoe 66";
        DashboardManager.log("[INFO] The ID we will use for Vendor Portal is: " + firstReqName);


         */
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
        // partnerPage.openFirstRequirement();
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
        submitPage.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234"); //deev=bharat.pandey@emb.global ,uat=bharat.pandey+1@emb.global
        submitPage.navigateToProject(firstReqName);
        submitPage.acceptProject();

        submitPage.addMembers(1, JD_FILE_PATH); // Adding 1 member as per your request
        submitPage.addMembersFromTeam(Arrays.asList("Candidate 2", "Candidate 3", "Candidate 4"));

        // Then click final submit
        submitPage.submitCandidates();

        submitPage.verifyCandidateStatus();

        // Close Vendor Context
        vendorContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-1-submit-trace.zip")));
        vendorContext.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Flow Completed.");

         /* =========================================================================
           TEMPORARILY SKIPPED: ASSESSMENT FLOW (Steps 6, 7, and 8)
           (Delete the /* here and the * / below to reactivate this section)
           ========================================================================= */
        /*
        // ──────────────────────────────────────────────────────────────
        // 6. SCHEDULE ASSESSMENT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Schedule Assessment Flow...");

        // 🔥 CRITICAL FIX: Bring Admin Page to Front & Wait 🔥
        page.bringToFront();
        page.waitForTimeout(1000);

        ScheduleAssessmentPage adminSchedule = new ScheduleAssessmentPage(page);

        // --- A. Admin: Update Status to 'Schedule Assessment' ---
        DashboardManager.log("[REPORT] 👮 Admin: Updating Candidate Status...");
        adminSchedule.navigateToRequirementListing();
        adminSchedule.openRequirement(firstReqName);
        adminSchedule.clickCandidatesTab();

        adminSchedule.openCandidateAndVerify("Candidate 1", "bharat pvt ltd");
        adminSchedule.adminUpdateStatusToAssessment();

        // --- B. Vendor: Select Time Slots ---
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Selecting Time Slots...");

        BrowserContext vendorContext2 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        Page vendorPage2 = vendorContext2.newPage();
        vendorPage2.navigate("https://uat-vendor.embtalent.ai/login");

        SubmitCandidatePage vendorLogin = new SubmitCandidatePage(vendorPage2);
        vendorLogin.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234");

        ScheduleAssessmentPage vendorSchedule = new ScheduleAssessmentPage(vendorPage2);
        vendorSchedule.vendorNavigateToProjects();
        vendorSchedule.vendorOpenProjectAndVerify(firstReqName);
        vendorSchedule.vendorOpenCandidateAndVerify("Candidate 1");
        vendorSchedule.vendorSelectTimeSlots();

        vendorContext2.close();

        // --- C. Admin: Finalize Schedule ---
        DashboardManager.log("\n[REPORT] 👮 Admin: Finalizing Schedule...");

        // 🔥 Switch back to Admin again
        page.bringToFront();
        page.waitForTimeout(1000);

        adminSchedule.navigateToRequirementListing();
        adminSchedule.openRequirement(firstReqName);
        adminSchedule.clickCandidatesTab();
        adminSchedule.openCandidateAndVerify("Candidate 1", "bharat pvt ltd");

        adminSchedule.adminScheduleAssessmentAction();
        adminSchedule.captureAssessmentDetails();

        DashboardManager.log("[REPORT] 🎉 Assessment Scheduled Successfully.");

        // ──────────────────────────────────────────────────────────────
        // 7. UPLOAD ASSESSMENT RESULT
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 📝 Admin: Uploading Assessment Result...");

        adminSchedule.adminUploadAssessmentResult(JD_FILE_PATH);

        // ──────────────────────────────────────────────────────────────
        // 8. VERIFY STATUS ON VENDOR PORTAL
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying Final Status...");

        BrowserContext vendorContext3 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        Page vendorPage3 = vendorContext3.newPage();

        ScheduleAssessmentPage vendorVerify = new ScheduleAssessmentPage(vendorPage3);

        vendorVerify.verifyVendorAssessmentStatus(
                "https://deev-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",
                "Emb@1234",
                firstReqName
        );

        vendorContext3.close();
        DashboardManager.log("[REPORT] ✅ Assessment E2E Flow Completed Successfully!");
        */


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
        // (Note: It will print a mismatch here since we skipped Assessment, but won't crash)
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
                "bharat.pandey+1@emb.global", //deev=bharat.pandey@emb.global, uat=bharat.pandey+1@emb.global
                "Emb@1234",
                firstReqName,
                JD_FILE_PATH
        );

        vendorContext4.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-4-assignment-solution-trace.zip")));
        vendorContext4.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Assignment Solution Submitted.");

        // ──────────────────────────────────────────────────────────────
        // 11. ADMIN REVIEWS ASSIGNMENT & SUBMITS FEEDBACK
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 👮 Admin: Reviewing Assignment & Submitting Feedback...");

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        assignmentPage.adminSubmitAssignmentFeedback(firstReqName, "Candidate 1");

        DashboardManager.log("[REPORT] 🎉 Admin Feedback Flow Completed!");

        // ──────────────────────────────────────────────────────────────
        // 12. VENDOR VERIFIES FINAL ASSIGNMENT STATUS
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying Final Assignment Status...");

        BrowserContext vendorContext5 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        // ⭐ TRACING FOR VENDOR 5
        vendorContext5.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage5 = vendorContext5.newPage();

        ScheduleAssignmentPage finalVendorPage = new ScheduleAssignmentPage(vendorPage5);
        finalVendorPage.vendorVerifyFinalAssignmentStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global", //deev=bharat.pandey@emb.global, uat=bharat.pandey+1@emb.global
                "Emb@1234",
                firstReqName
        );

        vendorContext5.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-5-assignment-verify-trace.zip")));
        vendorContext5.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Final Verification Completed.");

        // ──────────────────────────────────────────────────────────────
        // 13. SCHEDULE INTERVIEW FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Schedule Interview Flow...");

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        // Initialize the Interview Page Object
        ScheduleInterviewPage interviewPage = new ScheduleInterviewPage(page);

        // A. Navigate & Verify Requirement Status (Active)
        interviewPage.navigateAndOpenRequirement(firstReqName);

        // B. Open Candidate & Verify Status (Assignment Completed)
        interviewPage.openCandidateForInterview("Candidate 1");

        // C. Update Status to 'Schedule Interview'
        interviewPage.updateStatusToScheduleInterview();

        // D. Select Time Slots & Submit
        interviewPage.selectInterviewTimeSlots();

        // E. Verify Interview Details
        interviewPage.verifyInterviewDetails();

        DashboardManager.log("[REPORT] 🎉 Interview Time Slots Requested Successfully!");

        // ──────────────────────────────────────────────────────────────
        // 14. VENDOR SELECTS INTERVIEW TIME
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Selecting Interview Time Slots...");

        BrowserContext vendorContext6 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        // ⭐ TRACING FOR VENDOR 6
        vendorContext6.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage6 = vendorContext6.newPage();

        UploadInterviewPage uploadInterviewPage = new UploadInterviewPage(vendorPage6);
        uploadInterviewPage.vendorSelectInterviewTime(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global", //deev=bharat.pandey@emb.global, uat=bharat.pandey+1@emb.global
                "Emb@1234",
                firstReqName
        );

        vendorContext6.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-6-interview-slots-trace.zip")));
        vendorContext6.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Time Slots Selected.");

        // ──────────────────────────────────────────────────────────────
        // 15. ADMIN SCHEDULES INTERVIEW & SUBMITS FEEDBACK
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 👮 Admin: Scheduling Interview and Submitting Feedback...");

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        UploadInterviewPage adminUploadInterview = new UploadInterviewPage(page);
        adminUploadInterview.adminScheduleAndFeedbackInterview(firstReqName, "Candidate 1");

        DashboardManager.log("[REPORT] 🎉 Admin Interview Scheduled & Feedback Submitted.");

        // ──────────────────────────────────────────────────────────────
        // 16. VENDOR VERIFIES FINAL INTERVIEW STATUS
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying Final Interview Status...");

        BrowserContext vendorContext7 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        // ⭐ TRACING FOR VENDOR 7
        vendorContext7.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage7 = vendorContext7.newPage();

        UploadInterviewPage finalVerifyInterviewPage = new UploadInterviewPage(vendorPage7);
        finalVerifyInterviewPage.vendorVerifyFinalInterviewStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",//deev=bharat.pandey@emb.global, uat=bharat.pandey+1@emb.global
                "Emb@1234",
                firstReqName
        );

        vendorContext7.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-7-interview-verify-trace.zip")));
        vendorContext7.close();
        DashboardManager.log("[REPORT] 🎉 Vendor Final Interview Verification Completed.");

        // ──────────────────────────────────────────────────────────────
        // 17. OFFER JOB & DEPLOY FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Offer Job & Deployment Flow...");

        // Bring Admin Page to Front
        page.bringToFront();
        page.waitForTimeout(1000);

        OfferJobPage offerPage = new OfferJobPage(page);

        // A. Admin: Update Status to Offer Job & Deploy
        offerPage.navigateAndOpenRequirement(firstReqName);
        offerPage.openCandidateAndVerifyStatus("Candidate 1");
        offerPage.updateStatusToOfferJob();
        offerPage.deployCandidate(JD_FILE_PATH);

        // B. Vendor: Verify Deployment
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying Deployment...");
        BrowserContext vendorContext8 = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        // ⭐ TRACING FOR VENDOR 8
        vendorContext8.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page vendorPage8 = vendorContext8.newPage();

        OfferJobPage vendorVerifyDeploy = new OfferJobPage(vendorPage8);
        vendorVerifyDeploy.vendorVerifyDeployedStatus(
                "https://uat-vendor.embtalent.ai/login",
                "bharat.pandey+1@emb.global",//deev=bharat.pandey@emb.global, uat=bharat.pandey+1@emb.global
                "Emb@1234",
                firstReqName
        );

        vendorContext8.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-8-deploy-verify-trace.zip")));
        vendorContext8.close();
        DashboardManager.log("[REPORT] ✅ Full E2E Journey Completed Successfully!");

        // ──────────────────────────────────────────────────────────────
        // 18. HOLD, REJECT & SHARE WITH CLIENT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Hold, Reject & Share with Client Flow...");

        page.bringToFront();
        page.waitForTimeout(1000);
        HoldRejectSentClientPage postDeployPage = new HoldRejectSentClientPage(page);

        // A. Admin Side Actions
        postDeployPage.processCandidatesOnAdmin(firstReqName);
        postDeployPage.printFinalSummaryAdmin();

        // B. Vendor Side Verification
        DashboardManager.log("\n[REPORT] 🏢 Vendor: Verifying All Final Statuses...");
        BrowserContext vendorContextFinal = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        // ⭐ TRACING FOR VENDOR FINAL
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
        vendorContextFinal.close();
        DashboardManager.log("[REPORT] ✅ Full E2E Multi-Candidate Journey Completed Successfully!");

        // ──────────────────────────────────────────────────────────────
        // 19. ALLOW RESUBMISSION FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Allow Resubmission Flow...");
        page.bringToFront();
        AllowResubmissionPage resubmitPage = new AllowResubmissionPage(page);

        // Admin Action
        resubmitPage.allowResubmissionsOnAdmin(firstReqName);

        // Vendor Action
        BrowserContext vendorContextResubmit = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        // ⭐ TRACING FOR VENDOR RESUBMIT
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
        vendorContextResubmit.close();

        // ──────────────────────────────────────────────────────────────
        // 20. CLIENT SHORTLIST & REJECT FLOW
        // ──────────────────────────────────────────────────────────────
        DashboardManager.log("\n[REPORT] 🚀 Starting Client Shortlist Flow...");

        // 1. Client Side: Shortlist
        BrowserContext clientContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        // ⭐ TRACING FOR CLIENT PORTAL
        clientContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        Page clientPage = clientContext.newPage();
        ClientShortlistPage clientFlow = new ClientShortlistPage(clientPage);

        clientFlow.loginAndShortlist("https://uat-client.embtalent.ai/login", "AutoTest@yopmail.com", "Emb@1234", firstReqName);

        // 2. Admin Side: Verify Shortlist
        page.bringToFront();
        ClientShortlistPage adminVerify = new ClientShortlistPage(page);
        adminVerify.verifyShortlistOnAdmin(firstReqName);

        // 3. Client Side: Reject
        clientPage.bringToFront();
        clientFlow.clientRejectCandidate(firstReqName);

        // 4. Admin Side: Verify Rejection
        page.bringToFront();
        adminVerify.verifyRejectionOnAdmin(firstReqName);

        clientContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-portal-trace.zip")));
        clientContext.close();

        // ──────────────────────────────────────────────────────────────
        // 21. REQUIREMENT COMPLETION FLOW
        // ──────────────────────────────────────────────────────────────
        RequirementCompletedPage completedFlow = new RequirementCompletedPage(page);

        // A. Admin Side: Deploy Candidate 2
        completedFlow.adminDeployCandidate(firstReqName, "Candidate 2", "bharat pvt ltd", JD_FILE_PATH);

        // B. Vendor Side: Verify Completion
        BrowserContext vendorCtx = browser.newContext();

        // ⭐ TRACING FOR VENDOR COMPLETION
        vendorCtx.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        RequirementCompletedPage vendorVerify = new RequirementCompletedPage(vendorCtx.newPage());
        vendorVerify.verifyPortalStatus("Vendor", "https://uat-vendor.embtalent.ai/login", "bharat.pandey+1@emb.global", "Emb@1234", firstReqName);

        vendorCtx.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/vendor-completion-verify-trace.zip")));

        vendorCtx.close();

        // C. Client Side: Verify Completion
        BrowserContext clientCtx = browser.newContext();

        // ⭐ TRACING FOR CLIENT COMPLETION
        clientCtx.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        RequirementCompletedPage clientVerify = new RequirementCompletedPage(clientCtx.newPage());
        clientVerify.verifyPortalStatus("Client", "https://uat-client.embtalent.ai/login", "AutoTest@yopmail.com", "Emb@1234", firstReqName);

        clientCtx.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/client-completion-verify-trace.zip")));

        clientCtx.close();
    }

    // <-- End of createFourRequirementsAtOnce method

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
    void tearDown() {
        if (context != null) {
            try {
                // Use a dynamic name or just be sure to delete the old one manually
                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("target/admin-trace-latest.zip")));
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

        // Put the email address you want to send the dashboard to here:
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
        EmailSender.sendDashboardEmail("bharat.pandey@emb.global");
        //EmailSender.sendDashboardEmail("ashish.mishra@emb.global");
    }
}