package com.embra.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import com.embra.pages.ClientAuthPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientAuthTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthTest.class);
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private ClientAuthPage clientAuthPage;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(300));
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions().setStorageStatePath(Paths.get("auth/google_state.json")));

        // --- START TRACING ---
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
        clientAuthPage = new ClientAuthPage(page);
        clientAuthPage.navigate();
    }

    //@Disabled
    @Test
    @Order(1)
    @DisplayName("Test 1: Email Login")
    void testEmailLogin() {
        clientAuthPage.loginWithEmail("bharat.pandey@emb.global", "Emb@1234");
        assertThat(clientAuthPage.getDashboardHeading()).isVisible();
        logger.info("✅ Case 1: Client Email login verified.");
    }

    //@Disabled
    @Test
    @Order(2)
    @DisplayName("Test 2: Login with Google")
    void testGoogleLogin() {
        Page popup = page.waitForPopup(() -> {
            clientAuthPage.clickGoogleLogin();
        });
        popup.locator("div[data-identifier='bharat.pandey@emb.global']").click();
        page.waitForURL("**/dashboard**", new Page.WaitForURLOptions().setTimeout(20000));
        assertThat(clientAuthPage.getDashboardHeading()).isVisible();
        logger.info("✅ Case 2: Client Google login verified.");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Forgot Password")
    void testForgotPasswordFlow() {
        // 1. Capture the exact request time
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);
        String requestTime = java.time.LocalTime.now().format(dtf);

        clientAuthPage.clickForgotPassword();
        page.locator("input[name='email']").fill("bharat.pandey@emb.global");
        page.keyboard().press("Enter");
        assertThat(page.locator("text='Sent successfully!'")).isVisible();

        logger.info("Reset requested at: " + requestTime);

        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        // 2. Updated Polling Logic for "no-reply" and "Reset your EMBTalent password"
        boolean emailReceived = false;
        for (int i = 0; i < 15; i++) {
            logger.info("Checking inbox (Attempt " + (i + 1) + ")");

            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("no-reply"))
                    .filter(new Locator.FilterOptions().setHasText("Reset your EMBTalent password"))
                    .first();

            if (emailRow.isVisible()) {
                String listingTimeRaw = emailRow.locator("td.xW").innerText().trim();
                String emailTimeText = listingTimeRaw.replace("\u00a0", " ").replace(" ", " ");

                String cleanReq = requestTime.replace("\u00a0", " ").replace(" ", " ");
                String currentTime = java.time.LocalTime.now().format(dtf).replace("\u00a0", " ").replace(" ", " ");

                boolean isFresh = emailTimeText.contains("minute") ||
                        emailTimeText.equals(cleanReq) ||
                        emailTimeText.equals(currentTime);

                if (isFresh) {
                    logger.info("✅ Fresh email confirmed: " + emailTimeText);
                    emailRow.click();
                    emailReceived = true;
                    break;
                } else {
                    logger.warn("⚠️ Found email at " + emailTimeText + " but it's older than " + cleanReq);
                }
            }
            gmailPage.waitForTimeout(3000);
            gmailPage.reload();
        }

        if (!emailReceived) throw new RuntimeException("❌ Reset email not found or timed out.");

        // 3. Handle Trimmed Content
        Locator trimmed = gmailPage.locator("div[aria-label='Show trimmed content']").last();
        if (trimmed.count() > 0 && trimmed.isVisible()) {
            trimmed.click();
            gmailPage.waitForTimeout(1000);
        }

        // 4. Reset Password Action
        Page setPasswordPage = context.waitForPage(() -> {
            gmailPage.locator("a:has-text('Reset Password')").last().dispatchEvent("click");
        });

        setPasswordPage.locator("input[name='password']").fill("Emb@1234");
        setPasswordPage.locator("input[name='confirmPassword']").fill("Emb@1234");
        setPasswordPage.locator("button:has-text('Set password')").click();

        assertThat(setPasswordPage.locator("text='Welcome back!'")).isVisible();
        logger.info("✅ Password updated successfully.");
        gmailPage.close();

        // 5. FINAL LOGIN VERIFICATION
        logger.info("Verifying login with the updated password...");
        // Navigate back to login (using the setPasswordPage tab or original page)
        setPasswordPage.navigate("https://uat-client.embtalent.ai/login");

        // We can reuse the ClientAuthPage object logic on the new page
        ClientAuthPage finalLoginPage = new ClientAuthPage(setPasswordPage);
        finalLoginPage.loginWithEmail("bharat.pandey@emb.global", "Emb@1234");

        assertThat(finalLoginPage.getDashboardHeading()).isVisible();
        logger.info("✅ Case 3: Recovery flow and re-login verified.");
    }

    //@Disabled
    @Test
    @Order(4)
    @DisplayName("Test 4: Register via OTP")
    void testSignUpWithOtp() {
        logger.info("--------------------------------------------------");
        logger.info("TEST CASE 4: SIGN UP & OTP VALIDATION");
        logger.info("--------------------------------------------------");

        String uniqueEmail = "bharat.pandey+" + System.currentTimeMillis() + "@emb.global";
        logger.info("Registering unique client: " + uniqueEmail);

        // 1. Initial Signup
        clientAuthPage.clickRegister();
        clientAuthPage.signup("Client Tester", uniqueEmail, "Emb@1234");

        // 2. Access Gmail
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        boolean freshEmailFound = false;
        String realOtp = "";

        // 3. OTP Polling Loop
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

        // 4. OTP INTERACTION
        logger.info("Entering WRONG OTP for negative test...");
        clientAuthPage.enterOTP("111111");
        page.waitForTimeout(2000);

        logger.info("Entering CORRECT OTP: " + realOtp);
        clientAuthPage.enterOTP(realOtp);

        // 5. REDIRECTION CHECK (Organization Details instead of Dashboard)
        try {
            logger.info("Waiting for redirection to Organization Details form...");
            // Locating the H2 heading you mentioned earlier
            Locator orgDetailsHeading = page.locator("h2:has-text('Organization Details')");
            orgDetailsHeading.waitFor(new Locator.WaitForOptions().setTimeout(20000));

            assertThat(orgDetailsHeading).isVisible();
            logger.info("✅ Organization Details form detected.");
        } catch (Exception e) {
            logger.error("❌ Redirection failed. Current URL: " + page.url());
            throw e;
        }

        logger.info("✅ TEST CASE 4 CLEARED: OTP registration verified.");
        gmailPage.close();
    }

    //@Disabled
    @Test
    @Order(5)
    @DisplayName("Test 5: Register with Google")
    void testRegisterWithGoogle() {
        clientAuthPage.clickRegister();
        Page popup = page.waitForPopup(() -> {
            page.locator("button:has-text('Register with Google')").click();
        });
        popup.locator("div[data-identifier='bharat.pandey@emb.global']").click();
        page.waitForURL("**/dashboard**", new Page.WaitForURLOptions().setTimeout(20000));
        assertThat(clientAuthPage.getDashboardHeading()).isVisible();
        logger.info("✅ Case 5: Client registration with Google verified.");
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        // --- STOP AND SAVE TRACE ---
        // Sanitizing filename for the zip file
        String traceName = "Client_" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_") + ".zip";
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("traces/" + traceName)));
        context.close();
    }

    @AfterAll
    void closeBrowser() {
        playwright.close();
    }
}