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

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateMultipleRequirementTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private static final String JD_FILE_PATH = "target/Ajay_Gupta_resume_.pdf";
    private static final String SHARED_STATE_FILE = "target/current_requirement.txt";

    @BeforeAll
    void setupBrowser() throws IOException {
        DashboardManager.initReport();

        Path jdPath = Paths.get(JD_FILE_PATH);
        if (!Files.exists(jdPath.getParent())) Files.createDirectories(jdPath.getParent());
        if (!Files.exists(jdPath)) Files.write(jdPath, "Dummy PDF content".getBytes());

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(true)
        );
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

        // --- THE FIX: GLOBALLY EXTEND TIMEOUTS ---
        // This gives the DOM up to 30 seconds to render dynamic elements
        // before Playwright throws a TimeoutError.
        context.setDefaultTimeout(30000);

        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
    }

    @Test
    @Order(1)
    @DisplayName("Create Batch Requirements (Full Time, Contractual, C2H)")
    void testCreateMultipleRequirements() throws IOException {
        DashboardManager.startTest("Bit: Create Multiple Requirements");
        DashboardManager.log("[REPORT] 🚀 Starting Module: Batch Requirement Creation...");

        page.navigate("https://uat-admin.embtalent.ai/login");
        LoginPage loginPage = new LoginPage(page);
        assertTrue(loginPage.login("bharat.pandey@emb.global", "Emb@1234"), "Login failed");

        RequirementListingPage listingPage = new RequirementListingPage(page);
        assertTrue(listingPage.clickNewRequirement(), "Navigation failed");

        CreateRequirementPage createPage = new CreateRequirementPage(page);

        DashboardManager.log("[REPORT] Passing 3 Requirements into the form generator...");

        // --- ADDED A SMALL ARTIFICIAL DELAY ---
        // Sometimes React batches updates, adding a 1-second pause before
        // starting the intense loop helps stabilize the DOM.
        page.waitForTimeout(1000);

        boolean success = createPage.createMultipleRequirements(List.of(
                new CreateRequirementPage.RequirementData("Full Time", "Onsite", "JS", "React", "52106", JD_FILE_PATH),
                new CreateRequirementPage.RequirementData("Contractual", "Hybrid", "Java", "Spring", "52107", JD_FILE_PATH),
                new CreateRequirementPage.RequirementData("Contract To Hire", "Remote", "Python", "Django", "52108", JD_FILE_PATH)
        ), "Requirement generated successfully");

        assertTrue(success, "Failed to create batch requirements");

        String firstReqName = verifyTopRequirement();
        DashboardManager.log("[INFO] Top Requirement Created: " + firstReqName);

        Files.writeString(Paths.get(SHARED_STATE_FILE), firstReqName);
        DashboardManager.log("[INFO] Saved requirement name to " + SHARED_STATE_FILE);
    }

    private String verifyTopRequirement() {
        DashboardManager.log("\n[REPORT] 🔍 Fetching ID from Requirement Table...");
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