package com.embra.tests;

import com.embra.pages.AdminAuthPage;
import com.embra.pages.AdminUserCreationPage;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminUserCreationTest {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserCreationTest.class);
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private AdminAuthPage adminAuthPage;
    private AdminUserCreationPage userCreationPage;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(300));
    }

    @BeforeEach
    void setup() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("auth/google_state.json")));
        page = context.newPage();
        adminAuthPage = new AdminAuthPage(page);
        userCreationPage = new AdminUserCreationPage(page);
    }

    @Test
    @Order(1)
    @DisplayName("Test 3: Creating New User and Setting Password")
    void testCreateNewUser() {
        // --- STEP 1: LOGIN & CREATE USER ---
        adminAuthPage.navigate();
        adminAuthPage.login("bharat.pandey@emb.global", "Emb@1234");

        userCreationPage.navigateToUsers();
        userCreationPage.openCreateUserForm();

        String uniqueEmail = "bharat.pandey+" + System.currentTimeMillis() + "@emb.global";
        userCreationPage.fillUserDetails("Test", "User", uniqueEmail);
        userCreationPage.selectRoleAdmin();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        String requestTimeStr = LocalTime.now().format(formatter).replace(" ", " ");

        userCreationPage.submitForm();

        assertThat(page.getByText("User Created Successfully")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000));
        logger.info("✅ User created. Set password email sent to: " + uniqueEmail);

        // --- STEP 2: GMAIL FLOW ---
        Page gmailPage = context.newPage();
        gmailPage.navigate("https://mail.google.com");

        boolean emailHandled = false;
        for (int i = 0; i < 40; i++) {
            Locator emailRow = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("[EMB-RA-CRM]:Set Your Password")).first();

            if (emailRow.isVisible()) {
                String listingTime = emailRow.locator("td.xW").innerText().replace(" ", " ").trim();

                // Only open if it matches request time or is "0 minutes ago"
                if (listingTime.equals(requestTimeStr) || listingTime.contains("minutes ago")) {
                    emailRow.click();

                    // Check for internal "0 minutes ago"
                    if (gmailPage.locator("span.g3:has-text('0 minutes ago')").first().isVisible()) {

                        // Handle strict mode violation for Trimmed Content
                        Locator trimmedBtn = gmailPage.locator("div[aria-label='Show trimmed content']").last();
                        if (trimmedBtn.count() > 0 && trimmedBtn.isVisible()) {
                            trimmedBtn.click();
                            gmailPage.waitForTimeout(1000);
                        }

                        Page setPasswordPage = context.waitForPage(() -> {
                            gmailPage.locator("a:has-text('Reset Password')").last().click();
                        });

                        // --- STEP 3: SET PASSWORD & VERIFY ---
                        AdminUserCreationPage resetPage = new AdminUserCreationPage(setPasswordPage);
                        resetPage.setNewPassword("Emb@1234");

                        assertThat(setPasswordPage.getByText("Password Set Successfully")).isVisible();

                        // Final Login check
                        setPasswordPage.navigate("https://uat-admin.embtalent.ai/login");
                        AdminAuthPage finalLogin = new AdminAuthPage(setPasswordPage);
                        finalLogin.login(uniqueEmail, "Emb@1234");
                        assertThat(finalLogin.getDashboardHeading()).isVisible();

                        emailHandled = true;
                        break;
                    } else {
                        gmailPage.navigate("https://mail.google.com");
                    }
                }
            }
            gmailPage.waitForTimeout(3000);
            gmailPage.reload();
        }

        if (!emailHandled) throw new RuntimeException("❌ Failed to find or process the Set Password email.");
        gmailPage.close();
    }

    @AfterEach
    void tearDown() { context.close(); }

    @AfterAll
    void closeBrowser() { playwright.close(); }
}