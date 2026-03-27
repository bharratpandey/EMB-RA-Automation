package com.embra.tests;

import com.embra.pages.*;
import com.embra.utils.DashboardManager;
import com.microsoft.playwright.*;
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
public class CreateSingleRequirementTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private static final String JD_FILE_PATH = "target/Ajay_Gupta_resume_.pdf";
    private static final String SHARED_STATE_FILE = "target/current_requirement.txt";

    @BeforeAll
    void setupBrowser() throws IOException {
        DashboardManager.initReport();

        // Ensure dummy PDF exists
        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        // Set headless to TRUE for GitHub Actions
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
    }

    @Test
    @Order(1)
    @DisplayName("Create a Single Full-Time Requirement")
    void testCreateRequirement() throws IOException {
        DashboardManager.startTest("Bit 1: Create Single Requirement");
        DashboardManager.log("[REPORT] 🚀 Starting Module 1: Requirement Creation...");

        // 1. Login
        page.navigate("https://uat-admin.embtalent.ai/login");
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        // 2. Navigate
        RequirementListingPage listingPage = new RequirementListingPage(page);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        // 3. Create SINGLE Requirement
        CreateRequirementPage createPage = new CreateRequirementPage(page);
        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", JD_FILE_PATH)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create requirement");

        // 4. Capture and Save the Requirement Name for the next test
        String firstReqName = verifyTopRequirement();
        DashboardManager.log("[INFO] Requirement Created: " + firstReqName);

        // 🔥 CRITICAL: Save this ID to a text file so ScheduleAssignmentTest can read it later
        Files.writeString(Paths.get(SHARED_STATE_FILE), firstReqName);
        DashboardManager.log("[INFO] Saved requirement name to " + SHARED_STATE_FILE);
    }

    private String verifyTopRequirement() {
        Locator rows = page.locator("tbody tr");
        rows.first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
        return rows.nth(0).locator("td:nth-child(2)").innerText().trim();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        String tracePath = "traces/" + testInfo.getDisplayName().replace(" ", "_") + ".zip";
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
        context.close();
    }

    @AfterAll
    void tearDownBrowser() {
        browser.close();
        playwright.close();
        DashboardManager.flushReport();
    }
}