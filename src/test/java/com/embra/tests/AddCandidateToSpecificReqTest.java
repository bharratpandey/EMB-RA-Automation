package com.embra.tests;

import com.embra.pages.SubmitCandidatePage;
import com.embra.utils.DashboardManager;
import com.embra.utils.EmailSender;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddCandidateToSpecificReqTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    // 🚀 Change this to the specific requirement you want to target before running
    private static final String TARGET_REQUIREMENT = "ReqTest-1774594017948";

    private static final String JD_FILE_PATH = "target/Ajay_Gupta_resume_.pdf";

    @BeforeAll
    static void setupBrowser() throws IOException {
        DashboardManager.initReport();

        // Ensure dummy resume exists
        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")  // Use real Chrome
                .setHeadless(false)    // Watch it execute
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));

        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
    }

    @Test
    @Order(1)
    void addCandidatesToSpecificRequirement() {
        DashboardManager.startTest("Vendor: Add Candidates to Specific Requirement");
        DashboardManager.log("\n[REPORT] 🚀 Starting Direct Vendor Submission Flow for: " + TARGET_REQUIREMENT);

        // 1. Navigate to Vendor Portal
        page.navigate("https://uat-vendor.embtalent.ai/login");

        // 2. Initialize Page Object
        SubmitCandidatePage submitPage = new SubmitCandidatePage(page);

        // 3. Login with provided credentials
        submitPage.loginToVendorPortal("bharat.pandey+1@emb.global", "Emb@1234");

        // 4. Navigate directly to the Specific Project
        submitPage.navigateToProject(TARGET_REQUIREMENT);

        // 5. Accept Project (If not already accepted)
        submitPage.acceptProject();

        // 6. Add Members
        // -> Adds 1 brand new member by uploading the resume
       // submitPage.addMembers(1, JD_FILE_PATH);

        // -> Adds existing members from the bench/team
        submitPage.addMembersFromTeam(Arrays.asList("Candidate 2", "Candidate 3", "Candidate 4"));

        // 7. Submit and Verify
        submitPage.submitCandidates();
        submitPage.verifyCandidateStatus();

        DashboardManager.log("[REPORT] 🎉 Candidates successfully submitted to " + TARGET_REQUIREMENT);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (context != null) {
            try {
                String tracePath = "target/" + testInfo.getDisplayName().replace(" ", "_") + "-trace.zip";
                context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
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
        EmailSender.sendDashboardEmail("bharatpandey011@gmail.com");
    }
}