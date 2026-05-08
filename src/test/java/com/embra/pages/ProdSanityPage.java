package com.embra.pages;

import com.embra.utils.DashboardManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;

public class ProdSanityPage {

    private final Page page;

    // ── Locators ──────────────────────────────────────────────
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator loginSuccessToast;
    private final Locator projectsTabLink;
    private final Locator projectSearchInput;
    private final Locator shortlistedCandidateTab;
    private final Locator addNewMemberButton;
    private final Locator resumeFileInput;
    private final Locator importFromResumeButton;
    private final Locator resumeExtractedToast;

    public ProdSanityPage(Page page) {
        this.page = page;

        this.emailInput            = page.locator("input[name='email']");
        this.passwordInput         = page.locator("input[name='password']");
        this.loginButton           = page.locator("button[type='submit']")
                                         .filter(new Locator.FilterOptions().setHasText("Login"));
        this.loginSuccessToast     = page.locator("span.text-toast")
                                         .filter(new Locator.FilterOptions().setHasText("Login successful!"));
        this.projectsTabLink       = page.locator("a[href='/projects']");
        this.projectSearchInput    = page.locator("input[placeholder='Find a project by: Name, Current status']");
        // Active tab has white text on blue background — text match is the most stable selector here
        this.shortlistedCandidateTab = page.getByText("Shortlisted Candidate",
                                         new Page.GetByTextOptions().setExact(true)).first();
        this.addNewMemberButton    = page.locator("button")
                                         .filter(new Locator.FilterOptions().setHasText("Add New Member")).first();
        // File input is opacity-0 but setInputFiles() works on hidden inputs in Playwright
        this.resumeFileInput       = page.locator("input[type='file'][accept='.pdf']").first();
        this.importFromResumeButton = page.locator("button[type='button']")
                                         .filter(new Locator.FilterOptions().setHasText("Import from resume")).first();
        this.resumeExtractedToast  = page.locator("span.text-toast")
                                         .filter(new Locator.FilterOptions().setHasText("Resume details extracted!"));
    }

    // ── STEP 1: Navigate & Login ───────────────────────────────

    public void navigate(String url) {
        page.navigate(url);
        emailInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        DashboardManager.log("   -> Navigated to Vendor Login page");
    }

    /** Fills credentials without logging their values (security: never print secrets). */
    public void fillCredentials(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        DashboardManager.log("   -> Credentials entered (masked for security)");
    }

    /**
     * Exposed as a Runnable so the test can call:
     *   page.waitForResponse(..., sanityPage::clickLogin)
     * This lets us measure API response time around the click.
     */
    public void clickLogin() {
        loginButton.click();
    }

    // ── STEP 2: Login Toast ────────────────────────────────────

    public boolean verifyLoginToast() {
        try {
            loginSuccessToast.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            DashboardManager.log("   ✅ Login toast verified: 'Login successful!'");
            return true;
        } catch (Exception e) {
            DashboardManager.log("   ❌ Login toast NOT found within 10s");
            return false;
        }
    }

    // ── STEP 3: Projects Tab ──────────────────────────────────

    public void clickProjectsTab() {
        projectsTabLink.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        projectsTabLink.click();
        page.waitForLoadState();
        DashboardManager.log("   -> Navigated to Projects tab");
    }

    // ── STEP 4: Search & Open Requirement ─────────────────────

    public void searchRequirement(String reqName) {
        projectSearchInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        projectSearchInput.fill(reqName);
        page.waitForTimeout(2000);
        DashboardManager.log("   -> Searched for requirement: " + reqName);
    }

    public void openRequirementCard(String reqName) {

        // Use only first 20 characters to handle frontend text truncation with "..."
        String shortName = reqName.length() > 20 ? reqName.substring(0, 20) : reqName;

        Locator card = page.locator("a[href*='projects/details']")
                .filter(new Locator.FilterOptions().setHasText(shortName)).first();
        card.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        card.click();
        page.waitForLoadState();
        page.waitForTimeout(2000);
        DashboardManager.log("   -> Opened requirement card: " + reqName);
    }

    // ── STEP 5: Shortlisted Candidate Tab ─────────────────────

    public void clickShortlistedCandidateTab() {
        shortlistedCandidateTab.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        shortlistedCandidateTab.click();
        page.waitForTimeout(1500);
        DashboardManager.log("   -> Clicked Shortlisted Candidate tab");
    }

    // ── STEP 6: Add New Member ────────────────────────────────

    public void clickAddNewMember() {
        addNewMemberButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        addNewMemberButton.click();
        page.waitForTimeout(1500);
        DashboardManager.log("   -> Clicked Add New Member");
    }

    // ── STEP 7: Upload Resume ─────────────────────────────────

    public void uploadResume(String resumePath) {
        resumeFileInput.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        resumeFileInput.setInputFiles(Paths.get(resumePath));
        page.waitForTimeout(1000);
        DashboardManager.log("   -> Resume file attached");
    }

    // ── STEP 8: Import from Resume ────────────────────────────

    /**
     * Exposed as a Runnable so the test can call:
     *   page.waitForResponse(..., sanityPage::clickImportFromResume)
     * This measures the resume-data-autofill API response time.
     */
    public void clickImportFromResume() {
        importFromResumeButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        importFromResumeButton.click();
        DashboardManager.log("   -> Clicked Import from resume");
    }

    // ── STEP 9: Resume Extraction Toast ──────────────────────

    /**
     * @param timeoutMs how long to wait for the toast (use 59000 on first call,
     *                  shorter on subsequent checks after API already waited)
     * @return true if toast appeared, false if timed out
     */
    public boolean waitForResumeExtractionToast(int timeoutMs) {
        try {
            resumeExtractedToast.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(timeoutMs));
            DashboardManager.log("   ✅ Resume extraction toast: 'Resume details extracted!'");
            return true;
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Resume extraction toast not visible within " + (timeoutMs / 1000) + "s");
            return false;
        }
    }

    public Page getPage() {
        return page;
    }
}