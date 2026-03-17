package com.embra.tests;

import com.embra.pages.ClientAuthPage;
import com.embra.pages.ClientOnboardingPage;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientOnboardingTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientOnboardingTest.class);
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private ClientOnboardingPage onboardingPage;
    private ClientAuthPage clientAuthPage;

    @BeforeAll
    void launchBrowser() {
        logger.info("--------------------------------------------------");
        logger.info("STARTING CLIENT ONBOARDING SUITE");
        logger.info("--------------------------------------------------");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(300));
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth/google_state.json")));

        page = context.newPage();
        onboardingPage = new ClientOnboardingPage(page);
        clientAuthPage = new ClientAuthPage(page);

        page.navigate("https://uat-client.embtalent.ai/login");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Full Client Registration and Onboarding")
    void testFullClientOnboarding() {
        logger.info("--------------------------------------------------");
        logger.info("TEST CASE 1: REGISTER & ONBOARD CLIENT");
        logger.info("--------------------------------------------------");

        // --- PHASE 1: REGISTRATION ---
        String uniqueEmail = "bharat.pandey+" + System.currentTimeMillis() + "@emb.global";
        logger.info("Registering unique client: " + uniqueEmail);

        clientAuthPage.clickRegister();
        clientAuthPage.signup("Client Tester", uniqueEmail, "Emb@1234");

        // --- PHASE 2: GMAIL OTP SCRAPING ---
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        boolean freshEmailFound = false;
        String realOtp = "";

        for (int i = 0; i < 15; i++) {
            logger.info("Checking inbox (Attempt " + (i + 1) + ")");
            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("EMBTalent Account Verification Code"))
                    .first();

            if (emailRow.isVisible()) {
                emailRow.click();
                gmailPage.waitForSelector("span.g3", new Page.WaitForSelectorOptions().setTimeout(5000));

                // Strict Freshness Check
                Locator freshMarker = gmailPage.locator("span.g3:has-text('0 minutes ago')").first();

                if (freshMarker.isVisible()) {
                    logger.info("✅ Fresh Verification Email Confirmed.");
                    realOtp = gmailPage.locator("span[class*='otp-box']").first().textContent().trim();
                    freshEmailFound = true;
                    break;
                } else {
                    logger.info("⚠️ Old email found. Refreshing...");
                    gmailPage.navigate("https://mail.google.com");
                }
            }
            gmailPage.waitForTimeout(5000);
            gmailPage.reload();
        }

        if (!freshEmailFound || realOtp.isEmpty()) {
            logger.error("❌ OTP Email failed to arrive.");
            throw new RuntimeException("OTP Email failed.");
        }

        page.bringToFront();

        // --- PHASE 3: OTP INTERACTION (Negative & Positive) ---
        logger.info("Entering WRONG OTP for negative test...");
        clientAuthPage.enterOTP("111111");
        page.waitForTimeout(2000);

        logger.info("Entering CORRECT OTP: " + realOtp);
        clientAuthPage.enterOTP(realOtp);

        // --- PHASE 4: ONBOARDING FORM ---
        try {
            logger.info("Waiting for redirection to Organization Details form...");
            Locator orgDetailsHeading = page.locator("h2:has-text('Organization Details')");
            orgDetailsHeading.waitFor(new Locator.WaitForOptions().setTimeout(20000));

            assertThat(orgDetailsHeading).isVisible();
            logger.info("✅ Organization Details form detected.");
        } catch (Exception e) {
            logger.error("❌ Redirection failed. Current URL: " + page.url());
            throw e;
        }

        logger.info("Filling Organization Details Form...");
        onboardingPage.fillOnboardingForm(
                "Founder",
                "TestOrg1",
                "Software Development",
                "https://www.example.com",
                "7982886711"
        );

        // --- PHASE 5: FINAL DASHBOARD VERIFICATION ---
        logger.info("Verifying final Dashboard landing...");
        assertThat(onboardingPage.getDashboardIndicator())
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000));

        gmailPage.close();

    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @AfterAll
    void closeBrowser() {
        logger.info("--------------------------------------------------");
        logger.info("SUITE EXECUTION FINISHED");
        logger.info("--------------------------------------------------");
        playwright.close();
    }
}