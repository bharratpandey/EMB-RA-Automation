package com.embra.tests;

import com.embra.pages.VendorAuthPage;
import com.embra.pages.VendorOnboardingPage;
import com.embra.pages.VendorApprovalPage;
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
public class VendorOnboardingTest {
    private static final Logger logger = LoggerFactory.getLogger(VendorOnboardingTest.class);
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private VendorAuthPage vendorAuthPage;
    private VendorOnboardingPage onboardingPage;
    private VendorApprovalPage adminPage;

    @BeforeAll
    void launchBrowser() {
        logger.info("--------------------------------------------------");
        logger.info("STARTING VENDOR ONBOARDING SUITE");
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

        // INITIALIZE ALL PAGE OBJECTS
        vendorAuthPage = new VendorAuthPage(page);
        onboardingPage = new VendorOnboardingPage(page);
        adminPage = new VendorApprovalPage(page);

        vendorAuthPage.navigate();
    }

    @Test
    @Order(1)
    @DisplayName("Complete Vendor Onboarding Flow")
    void testFullVendorOnboarding() {
        // --- PHASE 1: REGISTRATION & OTP (STRICT GMAIL SCRAPING) ---
        logger.info("PHASE 1: SIGN UP & OTP VALIDATION");
        String uniqueEmail = "bharat.pandey+" + System.currentTimeMillis() + "@emb.global";
        logger.info("Registering unique vendor: " + uniqueEmail);

        page.locator("a:has-text('Sign Up')").click();
        vendorAuthPage.signup("AutoVendor", uniqueEmail, "Emb@1234");

        // NEGATIVE TEST
        logger.info("Testing invalid OTP behavior...");
        vendorAuthPage.enterOTP("000000");
        page.waitForTimeout(2000);

        // FETCHING REAL OTP
        logger.info("Fetching real-time OTP from Gmail...");
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        boolean freshEmailFound = false;
        String realOtp = "";

        for (int i = 0; i < 12; i++) {
            logger.info("Checking inbox (Attempt " + (i + 1) + ")");
            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("Email Verification")).first();

            if (emailRow.isVisible()) {
                emailRow.click();
                gmailPage.waitForSelector(".g3", new Page.WaitForSelectorOptions().setTimeout(5000));
                Locator timeStamp = gmailPage.locator("span.g3:has-text('0 minutes ago')").first();

                if (timeStamp.isVisible()) {
                    logger.info("✅ Fresh Verification Email Found!");
                    realOtp = gmailPage.locator("span[class*='otp-box']").first().textContent().trim();
                    freshEmailFound = true;
                    break;
                } else {
                    logger.info("⚠️ Email found but not fresh. Retrying...");
                    gmailPage.navigate("https://mail.google.com");
                }
            }
            gmailPage.waitForTimeout(5000);
            gmailPage.reload();
        }

        if (!freshEmailFound) {
            logger.error("❌ TIMEOUT: OTP email did not arrive.");
            throw new RuntimeException("Strict Check Failed: Fresh email not received.");
        }

        page.bringToFront();
        logger.info("Entering authentic OTP: " + realOtp);
        vendorAuthPage.enterOTP(realOtp);

        // --- PHASE 2: ABOUT ORGANIZATION ---
        logger.info("PHASE 2: ABOUT ORGANIZATION");
        assertThat(onboardingPage.getAboutOrgHeading()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));

        onboardingPage.getAgencyNameInput().fill("VendorOrg" + (System.currentTimeMillis() % 10000));
        onboardingPage.getWebsiteInput().fill("https://www.example.com");

        onboardingPage.getCountryOriginDropdown().click();
        page.locator("div[role='option']").filter(new Locator.FilterOptions().setHasText("India (INR)")).click();

        onboardingPage.getPhoneInput().fill("7982886711");
        onboardingPage.getNextButton().click();

        // --- PHASE 3: ORGANIZATION DETAILS ---
        logger.info("PHASE 3: ORGANIZATION DETAILS & LOCATIONS");
        assertThat(onboardingPage.getOrgDetailsHeading()).isVisible();

        onboardingPage.getOrgSizeDropdown().click();
        page.locator("div[role='option']").filter(new Locator.FilterOptions().setHasText("1 - 10 employees")).click();

        onboardingPage.getTurnoverDropdown().click();
        page.locator("div[role='option']").filter(new Locator.FilterOptions().setHasText("Up to $100K")).click();

        onboardingPage.getHiringServicesAllBtn().click();
        onboardingPage.getEngagementModeAllBtn().click();

        // Locations Logic
        onboardingPage.getAddMoreLocationBtn().click();
        onboardingPage.getLocationSearchInput().fill("new delhi");
        page.waitForTimeout(3000);
        page.locator("div:has-text('New Delhi, Delhi, India')").first().click();

        onboardingPage.getLocationSearchInput().clear();
        onboardingPage.getLocationSearchInput().fill("United States");
        page.waitForTimeout(3000);
        page.locator("div:has-text('United States')").first().click();
        onboardingPage.getSaveSelectionBtn().click();

        // Timezones
        onboardingPage.getTimezoneDropdown().click();
        onboardingPage.selectTimezone("India");
        onboardingPage.selectTimezone("United States");
        onboardingPage.getNextButton().click();

        // --- PHASE 4: CAPABILITIES ---
        logger.info("PHASE 4: CAPABILITIES SELECTION");
        assertThat(onboardingPage.getCapabilitiesHeading()).isVisible();
        onboardingPage.getSelectCapabilitiesBtn().click();

        String[] skills = {"React", "HTML", "Java", "Python", "CSS3"};
        for (String skill : skills) {
            logger.info("Selecting Skill: " + skill);
            onboardingPage.selectSkill(skill);
        }

        onboardingPage.getSaveSelectionBtn().click();
        onboardingPage.getCompleteSetupBtn().click();

        // --- PHASE 5: FINAL ADDRESS & SUBMIT ---
        logger.info("PHASE 5: ADDRESS SUBMISSION");
        onboardingPage.getAddressLine1Input().fill("Plot No. 17, Phase-4, Maruti Udyog, Sector 18, Gurugram, HR");
        onboardingPage.getCountryInput().fill("india");
        onboardingPage.getZipCodeInput().fill("122015");

        assertThat(onboardingPage.getAutoFillToast()).isVisible();
        onboardingPage.getFinalSubmitBtn().click();

        // --- PHASE 6: ADMIN LOGIN & VENDOR APPROVAL ---
        adminPage.login("bharat.pandey@emb.global", "Emb@1234");
        adminPage.navigateToNewRegistrations();
        adminPage.approveVendorByEmail(uniqueEmail);

        // This call will now find the symbol because we renamed it in the Page Object
        if (adminPage.isActionSuccessVisible()) {
            logger.info("✅ Admin Action Success toast verified.");
        }

        // --- PHASE 7: PARTNER LISTING VERIFICATION ---
        logger.info("PHASE 7: PARTNER LISTING VERIFICATION");

        // Use JUnit Assertions for boolean values instead of Playwright's assertThat
        org.junit.jupiter.api.Assertions.assertTrue(
                adminPage.verifyVendorIsActive(uniqueEmail),
                "Vendor should be Active in Partner Listing"
        );
        logger.info("✅ Vendor is confirmed 'Active' in Partner Listing.");

        // --- PHASE 8: VENDOR DASHBOARD VERIFICATION ---
        logger.info("--------------------------------------------------");
        logger.info("PHASE 8: VENDOR PORTAL FINAL VERIFICATION");
        logger.info("--------------------------------------------------");

        page.navigate("https://uat-vendor.embtalent.ai/login");

        // 1. Conditional Login: Only log in if we aren't already at the dashboard
        if (page.locator("input[name='email']").isVisible()) {
            logger.info("Login page detected. Proceeding with login for: " + uniqueEmail);
            vendorAuthPage.loginWithEmail(uniqueEmail, "Emb@1234");
        } else {
            logger.info("Already logged in or redirecting to dashboard...");
        }

        // 2. Target the specific Green Verification Banner
        // Using the text and the unique background color from your HTML for a perfect match
        Locator verifiedBanner = page.locator("div.bg-\\[\\#DEFCE9\\]")
                .locator("span:has-text('Your organization profile is verified')");

        try {
            // Wait up to 15 seconds for the banner to appear (handling post-approval data sync)
            assertThat(verifiedBanner).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));
            logger.info("✅ Verified: 'Your organization profile is verified' banner is visible.");
        } catch (Throwable e) {
            logger.error("❌ Verification banner not found!");
            // Capture a screenshot for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("verification-fail.png")));
            throw e;
        }

        logger.info("✅ END-TO-END VENDOR ONBOARDING & APPROVAL SUCCESSFUL.");
    }

    @AfterEach
    void tearDown() { context.close(); }

    @AfterAll
    void closeBrowser() { playwright.close(); }
}