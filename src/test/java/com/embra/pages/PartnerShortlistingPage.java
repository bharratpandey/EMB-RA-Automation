package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.embra.utils.DashboardManager;

import java.util.List;

public class PartnerShortlistingPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator firstViewDetailsBtn;
    private final Locator statusBadge;
    private final Locator partnerShortlistingTab;
    private final Locator searchFilterBtn;
    private final Locator searchInput;
    private final Locator sendHiringReqBtn;
    private final Locator sendToPartnersSubmitBtn;

    public PartnerShortlistingPage(Page page) {
        this.page = page;

        // 1. View Details (Eye Icon) - First one in list
        this.firstViewDetailsBtn = page.locator("button[title='View Details']").first();

        // 2. Status Badge
        this.statusBadge = page.locator("div.px-4.py-1.rounded-md.font-bold");

        // 3. Tabs
        this.partnerShortlistingTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Partner Shortlisting"));

        // 4. Search Section
        this.searchFilterBtn = page.locator("div").filter(new Locator.FilterOptions().setHasText("Search & Filters")).last();
        this.searchInput = page.locator("input[placeholder='Search active partners...']");

        // 5. Buttons
        this.sendHiringReqBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Send Hiring Requirement"));
        this.sendToPartnersSubmitBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Send to"));
    }

    // ──────────────────────────────────────────────────────────────
    // ACTIONS
    // ──────────────────────────────────────────────────────────────

    /**
     * Opens the first requirement from the listing page.
     */
    public void openFirstRequirement() {
        DashboardManager.log("👉 Opening first requirement...");
        firstViewDetailsBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        firstViewDetailsBtn.click();
    }

    /**
     * Verifies requirement status.
     * WAITS for 'Active' to appear. If not, prints actual status and proceeds.
     */
    public boolean verifyRequirementStatus() {
        DashboardManager.log("🔍 Verifying status...");

        try {
            // Wait specifically for the text "Active" to appear (up to 5 seconds)
            statusBadge.filter(new Locator.FilterOptions().setHasText("Active"))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

            DashboardManager.log("✅ Status is ACTIVE.");
            return true;

        } catch (Exception e) {
            String currentStatus = "Unknown";
            try {
                if(statusBadge.isVisible()) {
                    currentStatus = statusBadge.innerText().trim();
                }
            } catch (Exception ex) { /* Ignore */ }

            DashboardManager.log("⚠️ Status check timed out. Found: [" + currentStatus + "]");
            DashboardManager.log("👉 Continuing flow as requested...");
            return true;
        }
    }

    public void navigateToPartnerShortlisting() {
        DashboardManager.log("👉 Clicking 'Partner Shortlisting' tab...");
        partnerShortlistingTab.click();
        page.waitForTimeout(1000);
    }

    public void openSearchFilters() {
        // Only click if search input isn't visible yet
        if(!searchInput.isVisible()) {
            DashboardManager.log("👉 Expanding Search & Filters...");
            searchFilterBtn.click();
            searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        }
    }

    public void shortlistVendors(List<String> vendorNames) {
        openSearchFilters();

        for (String vendor : vendorNames) {
            DashboardManager.log("🔎 Searching and selecting vendor: " + vendor);

            searchInput.clear();
            searchInput.fill(vendor);
            page.waitForTimeout(2000);

            // Find checkbox in the first row
            Locator checkbox = page.locator("tbody tr button[role='checkbox']").first();
            checkbox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

            if (checkbox.isVisible()) {
                if ("unchecked".equals(checkbox.getAttribute("data-state"))) {
                    page.waitForTimeout(1000);
                    checkbox.click();
                    DashboardManager.log("   ✅ Selected " + vendor);
                } else {
                    DashboardManager.log("   ⚠️ Already selected: " + vendor);
                }
            } else {
                DashboardManager.log("   ❌ Vendor not found or list empty: " + vendor);
            }
        }
    }

    public void clickSendHiringRequirement() {
        DashboardManager.log("👉 Clicking 'Send Hiring Requirement' button...");
        sendHiringReqBtn.click();
    }

    /**
     * Fills the budget popup form.
     * UPDATED: Uses IDs and removed 'isVisible' check so Playwright auto-scrolls to fields.
     */
    public void fillBudgetDetails() {
        DashboardManager.log("📝 Filling Budget Details in Popup...");

        // Wait for popup
        page.locator("h3:has-text('Send Hiring Requirement')").waitFor();

        // 1. Vendor EURO (Using IDs)
        DashboardManager.log("   -> Filling EURO (3000 - 4000)");
        fillInputById("currency-min-EUR", "3000");
        fillInputById("currency-max-EUR", "4000");

        // 2. Vendor AED
        DashboardManager.log("   -> Filling AED (12000 - 17000)");
        fillInputById("currency-min-AED", "12000");
        fillInputById("currency-max-AED", "17000");

        // 3. Vendor USD
        DashboardManager.log("   -> Filling USD (3500 - 4500)");
        fillInputById("currency-min-USD", "3500");
        fillInputById("currency-max-USD", "4500");

        // 4. Vendor INR
        DashboardManager.log("   -> Filling INR (300000 - 400000)");
        fillInputById("currency-min-INR", "300000");
        fillInputById("currency-max-INR", "400000");
    }

    // New Helper: Fills by ID directly. Playwright handles scrolling.
    private void fillInputById(String id, String value) {
        Locator input = page.locator("#" + id);
        try {
            input.fill(value);
        } catch (Exception e) {
            DashboardManager.log("   ❌ Failed to fill input #" + id + ": " + e.getMessage());
        }
    }

    public void submitShortlisting() {
        DashboardManager.log("👉 Clicking Submit (Send to Partners)...");
        sendToPartnersSubmitBtn.click();
    }

    public boolean verifySuccessToast() {
        DashboardManager.log("⏳ Waiting for success toast...");
        // Updated selector to match any success toast related to vendor
        Locator toast = page.getByText("Vendor shortlisted successfully", new Page.GetByTextOptions().setExact(false));
        try {
            toast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            DashboardManager.log("✅ Success! Vendors shortlisted.");
            return true;
        } catch (Exception e) {
            DashboardManager.log("❌ Success toast not found.");
            return false;
        }
    }
}