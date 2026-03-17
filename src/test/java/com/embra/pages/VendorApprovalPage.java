package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VendorApprovalPage {
    private static final Logger logger = LoggerFactory.getLogger(VendorApprovalPage.class);
    private final Page page;

    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator newRegistrationsTab;
    private final Locator searchBarTab;
    private final Locator searchField;
    private final Locator partnerListingTab;

    public VendorApprovalPage(Page page) {
        this.page = page;
        this.emailInput = page.locator("input[name='email']");
        this.passwordInput = page.locator("input[name='password']");
        this.loginButton = page.locator("button[type='submit']:has-text('LogIn')");

        this.newRegistrationsTab = page.locator("a:has-text('New Registrations')");
        this.partnerListingTab = page.locator("a:has-text('Partner Listing')");
        this.searchBarTab = page.locator("div.full.bg-gray-100:has-text('Search & Filters')");
        this.searchField = page.locator("input[placeholder='Search by name or email...']");
    }

    public void login(String email, String password) {
        page.navigate("https://uat-admin.embtalent.ai/login");
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }

    public void navigateToNewRegistrations() {
        newRegistrationsTab.click();
    }

    public void navigateToPartnerListing() {
        partnerListingTab.click();
    }

    public void approveVendorByEmail(String email) {
        String uniqueId = email.replaceAll("[^0-9]", "");
        logger.info("Step 1: Searching for Vendor using ID: " + uniqueId);

        searchBarTab.click();
        searchField.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchField.fill(uniqueId);

        // Wait for the table to filter
        page.locator("text=Showing 1 of 1 results").waitFor();
        logger.info("Step 2: Table filtered to single result.");

        page.waitForTimeout(2000);

        // Click the triple-dot menu
        page.locator("tbody button[title='More Actions']").first().click();

        logger.info("Step 3: Clicking Approve from floating menu...");

        // FLEXIBLE LOCATOR: Find the menu item with 'Approve' regardless of parent div
        // Using getByRole or a text-based locator is often safer for Radix/Shadcn UI
        Locator approveDropdownItem = page.locator("[role='menuitem']:has-text('Approve'), button:has-text('Approve')").last();

        // Force a small wait for the animation to finish
        page.waitForTimeout(500);

        approveDropdownItem.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        approveDropdownItem.click();

        logger.info("Step 4: Clicking final Approved confirmation button...");
        // Usually, the final button is in a dialog
        Locator finalApprovedBtn = page.locator("div[role='dialog'] button:has-text('Approved'), button:has-text('Approved')").last();
        finalApprovedBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        finalApprovedBtn.click();

        logger.info("✅ Vendor Approved.");
    }

    // Renamed to match the symbol your Test Class is looking for
    public boolean isActionSuccessVisible() {
        try {
            Locator toast = page.locator("ol li[data-sonner-toast] div[data-title='Action Success']").first();
            toast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
            return true;
        } catch (Exception e) {
            logger.warn("⚠️ Success toast not detected within 3s, moving to next verification step.");
            return false;
        }
    }

    public boolean verifyVendorIsActive(String email) {
        String uniqueId = email.replaceAll("[^0-9]", "");
        navigateToPartnerListing();

        searchBarTab.click();
        searchField.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchField.fill(uniqueId);

        page.locator("text=Showing 1 of 1 results").waitFor();

        Locator statusBadge = page.locator("tbody tr").first().locator("div:has-text('Active')");
        return statusBadge.isVisible();
    }
}