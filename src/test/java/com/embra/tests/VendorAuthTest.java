package com.embra.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.jupiter.api.*;
import com.embra.pages.VendorAuthPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.embra.utils.MailService;
import java.util.regex.Pattern;

import java.nio.file.Paths;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VendorAuthTest {
    private static final Logger logger = LoggerFactory.getLogger(VendorAuthTest.class);
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private VendorAuthPage vendorAuthPage;

    @BeforeAll
    void launchBrowser() {
        DashboardClient.log("==================================================");
        DashboardClient.log(" INITIALIZING AUTOMATION SUITE");
        DashboardClient.log("==================================================");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(300));
    }

    @BeforeEach
    void setup() {
        // Injection of authenticated session state
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth/google_state.json")));

        // Start Playwright Tracing for debugging
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
        vendorAuthPage = new VendorAuthPage(page);
        vendorAuthPage.navigate();
        DashboardClient.log("✅ Browser environment ready with active session.");
    }

    //@Disabled
    @Test
    @Order(1)
    @DisplayName("Login with Email and Password")
    void testEmailLogin() {
        DashboardClient.log("--------------------------------------------------");
        DashboardClient.log(" TEST CASE 1: EMAIL & PASSWORD LOGIN");
        DashboardClient.log("--------------------------------------------------");

        vendorAuthPage.loginWithEmail("bharat.pandey+1@emb.global", "Emb@1234");

        DashboardClient.log(" Validating UI Response...");
        assertThat(vendorAuthPage.getSuccessToastTitle()).hasText("Login successful!");
        assertThat(vendorAuthPage.getWelcomeMessage()).containsText("Welcome back, bharat Vendor!");

        DashboardClient.log(" Final Check: Dashboard Visibility");
        assertThat(vendorAuthPage.getDashboardHeading()).isVisible();
        DashboardClient.log("✅ CASE 1 CLEARED: Email login verified.");
    }

    //@Disabled
    @Test
    @Order(2)
    @DisplayName("Login with Google")
    void testGoogleLogin() {
        DashboardClient.log("--------------------------------------------------");
        DashboardClient.log(" TEST CASE 2: GOOGLE SOCIAL LOGIN");
        DashboardClient.log("--------------------------------------------------");

        // Capture popup window during Google auth click
        Page popup = page.waitForPopup(() -> {
            vendorAuthPage.clickGoogleLogin();
        });

        DashboardClient.log(" Interaction with Google Identity Provider...");
        popup.locator("div[data-email='bharat.pandey@emb.global']").click();

        DashboardClient.log(" Waiting for secure redirect to Dashboard...");
        page.waitForURL("**/dashboard**", new Page.WaitForURLOptions().setTimeout(20000));

        assertThat(vendorAuthPage.getDashboardHeading()).isVisible();
        DashboardClient.log("✅ CASE 2 CLEARED: Google login verified.");
    }

    //@Disabled
    @Test
    @Order(3)
    @DisplayName("Test 3: Forgot Password Flow")
    void testForgotPasswordFlow() {
        DashboardClient.log("--------------------------------------------------");
        DashboardClient.log(" TEST CASE 3: FORGOT PASSWORD RECOVERY");
        DashboardClient.log("--------------------------------------------------");

        // 1. Define Format and capture Request Time
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);
        String requestTime = java.time.LocalTime.now().format(dtf);
        DashboardClient.log("Reset request initiated at: " + requestTime);

        // 2. Trigger reset email
        vendorAuthPage.clickForgotPassword();
        vendorAuthPage.requestReset("bharat.pandey+1@emb.global");
        assertThat(page.locator("text='Sent successfully!'")).isVisible();

        // 3. Switch context to Gmail
        DashboardClient.log(" Accessing Gmail Inbox...");
        page.waitForTimeout(2000);
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        // 4. RETRY LOOP: Find the email matching the timestamp or later
        boolean emailReceived = false; // Variable defined inside the method scope

        for (int i = 0; i < 15; i++) {
            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("Reset Your Password"))
                    .first();

            if (emailRow.isVisible()) {
                // GET and SANITIZE the listing time (Removes Gmail's narrow spaces)
                String listingTimeRaw = emailRow.locator("td.xW").innerText().trim();
                String emailTimeText = listingTimeRaw.replace("\u00a0", " ").replace(" ", " ");

                // Sanitize our baselines
                String cleanReq = requestTime.replace("\u00a0", " ").replace(" ", " ");
                String currentTime = java.time.LocalTime.now().format(dtf).replace("\u00a0", " ").replace(" ", " ");

                DashboardClient.log("Comparing - Email: [" + emailTimeText + "] vs Request: [" + cleanReq + "]");

                // Logic: Accept if relative text OR matches request time OR matches current time
                boolean isFresh = emailTimeText.contains("minute") ||
                        emailTimeText.equals(cleanReq) ||
                        emailTimeText.equals(currentTime);

                if (isFresh) {
                    DashboardClient.log("✅ Fresh email confirmed. Opening: " + emailTimeText);
                    emailRow.click();
                    emailReceived = true;
                    break;
                } else {
                    logger.warn("⚠️ Email at " + emailTimeText + " is older than " + cleanReq + ". Retrying...");
                }
            }

            DashboardClient.log(" Email not found yet (Attempt " + (i + 1) + "). Reloading...");
            gmailPage.waitForTimeout(5000);
            gmailPage.reload();
        }

        if (!emailReceived) {
            logger.error("❌ Email not received within timeframe.");
            throw new RuntimeException("Timeout waiting for Reset Email.");
        }

        // 5. Expand body and click Reset Link
        Locator trimmedContentBtn = gmailPage.locator("div[aria-label='Show trimmed content']").last();
        if (trimmedContentBtn.count() > 0 && trimmedContentBtn.isVisible()) {
            trimmedContentBtn.click();
            gmailPage.waitForTimeout(1000);
        }

        Page setPasswordPage = context.waitForPage(() -> {
            gmailPage.locator("a:has-text('Reset Password')").last().click();
        });

        // 6. Update credentials
        setPasswordPage.locator("input[name='password']").fill("Emb@1234");
        setPasswordPage.locator("input[name='confirmPassword']").fill("Emb@1234");
        setPasswordPage.locator("button:has-text('Set password')").click();

        assertThat(setPasswordPage.locator("text='Welcome back!'")).isVisible();
        DashboardClient.log("✅ CASE 3 CLEARED: Password recovery flow verified.");

        gmailPage.close();
    }

    //@Disabled
    @Test
    @Order(4)
    @DisplayName("4. Sign Up - Wrong OTP then Right OTP")
    void testSignUpThroughOtp() {
        DashboardClient.log("--------------------------------------------------");
        DashboardClient.log(" TEST CASE 4: SIGN UP & OTP VALIDATION");
        DashboardClient.log("--------------------------------------------------");

        String uniqueEmail = "bharat.pandey+" + System.currentTimeMillis() + "@emb.global";
        DashboardClient.log(" Registering unique vendor: {}", uniqueEmail);

        page.locator("a:has-text('Sign Up')").click();
        vendorAuthPage.signup("AutoVendor", uniqueEmail, "Emb@1234");

        // --- PHASE 1: NEGATIVE VALIDATION ---
        DashboardClient.log(" PHASE 1: Testing invalid OTP behavior...");
        vendorAuthPage.enterOTP("000000");

        Pattern errorPattern = Pattern.compile(".*(invalid|expired).*");
        assertThat(vendorAuthPage.getSuccessToastTitle()).hasText(errorPattern);
        assertThat(vendorAuthPage.getOtpInlineErrorLocator()).hasText(errorPattern);
        DashboardClient.log(" Negative test passed: System rejected wrong OTP correctly.");

        // --- PHASE 2: POSITIVE VALIDATION ---
        DashboardClient.log(" PHASE 2: Fetching real-time OTP from Gmail...");
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        boolean freshEmailFound = false;
        String realOtp = "";

        // Gmail Polling Logic
        for (int i = 0; i < 12; i++) {
            DashboardClient.log(" Checking inbox (Attempt {})", i + 1);

            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("Email Verification")).first();

            if (emailRow.isVisible()) {
                emailRow.click();
                gmailPage.waitForSelector(".g3", new Page.WaitForSelectorOptions().setTimeout(5000));

                // Strict freshness check: (0 minutes ago)
                Locator timeStamp = gmailPage.locator("span:has-text('0 minutes ago')").first();

                if (timeStamp.isVisible()) {
                    DashboardClient.log(" Fresh Verification Email Found!");
                    realOtp = gmailPage.locator("span[class*='otp-box']").first().textContent().trim();
                    freshEmailFound = true;
                    break;
                } else {
                    DashboardClient.log("⚠️ Email found but not fresh. Retrying...");
                    gmailPage.navigate("https://mail.google.com");
                }
            }
            gmailPage.waitForTimeout(5000);
            gmailPage.reload();
        }

        if (!freshEmailFound) {
            logger.error("❌ TIMEOUT: OTP email did not arrive within 60s.");
            throw new RuntimeException("Strict Check Failed: Did not receive a '0 minutes ago' email.");
        }

        // --- PHASE 3: FINAL VERIFICATION ---
        page.bringToFront();
        DashboardClient.log(" Entering authentic OTP: {}", realOtp);
        vendorAuthPage.enterOTP(realOtp);

        assertThat(vendorAuthPage.getSuccessToastTitle()).hasText("Verified successfully!");

        DashboardClient.log(" Checking onboarding state...");
        assertThat(vendorAuthPage.getRegistrationSuccessSubtext())
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));

        DashboardClient.log(" Verifying post-signup landing page...");
        assertThat(vendorAuthPage.getOnboardingSubtext())
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));

        DashboardClient.log("✅ CASE 4 CLEARED: Full registration and OTP corrected.");
        gmailPage.close();
    }

    //@Disabled
    @Order(5)
    @DisplayName("5. Register with Google")
    void testRegisterWithGoogle() {
        DashboardClient.log("--------------------------------------------------");
        DashboardClient.log("📌 TEST CASE 5: REGISTER WITH GOOGLE");
        DashboardClient.log("--------------------------------------------------");

        // 1. Navigate to Sign Up page
        DashboardClient.log("Clicking on 'Sign Up' link...");
        page.locator("a:has-text('Sign Up')").click();

        // 2. Capture the Google Registration Popup
        DashboardClient.log("Clicking 'Register with Google'...");
        Page popup = page.waitForPopup(() -> {
            page.locator("button:has-text('Register with Google')").click();
        });

        // 3. Select the account from the popup
        // Based on the HTML, we target the account using the email identifier
        DashboardClient.log("Selecting Google account: bharat.pandey@emb.global");
        popup.locator("div[data-identifier='bharat.pandey@emb.global']").click();

        // 4. Wait for redirect to complete
        // Google redirects can take time, so we wait for the URL to change to dashboard
        DashboardClient.log("Waiting for redirect to Dashboard...");
        page.waitForURL("**/dashboard**", new Page.WaitForURLOptions().setTimeout(20000));

        // 5. Final verification: Check if Dashboard is visible
        // We use the specific Dashboard span from your HTML
        assertThat(page.locator("span:has-text('Dashboard')").first()).isVisible();

        DashboardClient.log("✅ TEST CASE 5 CLEARED: Register with Google Successful.");
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        String tracePath = "traces/" + testInfo.getDisplayName().replace(" ", "_") + ".zip";
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracePath)));
        DashboardClient.log(" TEST COMPLETED. Trace: {}", tracePath);
        context.close();
    }

    @AfterAll
    void closeBrowser() {
        DashboardClient.log("==================================================");
        DashboardClient.log(" SUITE EXECUTION FINISHED");
        DashboardClient.log("==================================================");
        playwright.close();
    }
}