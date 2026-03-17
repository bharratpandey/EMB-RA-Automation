package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class AllowResubmissionPage {

    private final Page page;

    // Admin Locators
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator backBtn;

    public AllowResubmissionPage(Page page) {
        this.page = page;

        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));
        this.backBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Back"));
    }

    // ──────────────────────────────────────────────────────────────
    // 1. ADMIN ACTIONS: ALLOW RESUBMISSION
    // ──────────────────────────────────────────────────────────────

    public void allowResubmissionsOnAdmin(String reqName) {
        DashboardManager.log("\n--- 💼 ADMIN: ALLOW RESUBMISSION FLOW ---");
        requirementListingLink.click();
        page.waitForLoadState();

        // Check Requirement Status
        DashboardManager.log("   -> Searching for Requirement: " + reqName);
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        String listingStatus = row.locator("td").nth(3).innerText().trim();
        DashboardManager.log("      Requirement Status: " + (listingStatus.equalsIgnoreCase("Active") ? "✅ Active" : "❌ " + listingStatus));

        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        candidatesTab.click();
        page.waitForTimeout(1000);

        // Process Candidate 2 and 3
        processSingleResubmitAllowance("Candidate 2");
        processSingleResubmitAllowance("Candidate 3");
    }

    private void processSingleResubmitAllowance(String candidateName) {
        DashboardManager.log("   -> Opening " + candidateName + " for Resubmission Allowance...");
        page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName)).locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Click Allow Resubmission
        Locator allowBtn = page.locator("div").filter(new Locator.FilterOptions().setHasText("Allow Resubmission")).last();
        allowBtn.click();

        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Status updated successfully!");
        }

        // Print Status
        String status = page.locator("div").filter(new Locator.FilterOptions().setHasText("Resubmit")).last().innerText().trim();
        DashboardManager.log("      " + candidateName + " Status: " + (status.equals("Resubmit") ? "✅ Resubmit" : "❌ " + status));

        backBtn.click();
        page.waitForTimeout(2000);
    }

    // ──────────────────────────────────────────────────────────────
    // 2. VENDOR ACTIONS: PERFORM RESUBMIT & VERIFY
    // ──────────────────────────────────────────────────────────────

    public void vendorPerformResubmission(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: RESUBMITTING CANDIDATES ---");
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        // Prefix Cleaning for Vendor
        String cleanName = reqName.contains("ReqTest-") ? reqName.substring(reqName.indexOf("ReqTest-")) : reqName;
        DashboardManager.log("   -> Opening Project: " + cleanName);

        Locator projectCard = page.locator("a").filter(new Locator.FilterOptions().setHasText(cleanName));
        String projStatus = projectCard.locator("span.text-project-interviewing").innerText().trim();
        DashboardManager.log("      Project Dashboard Status: " + (projStatus.equalsIgnoreCase("Interviewing") ? "✅ Interviewing" : "❌ " + projStatus));

        projectCard.first().click();
        page.waitForTimeout(3000);

        // Resubmit Candidate 3
        executeVendorResubmitAction("Candidate 3");
        // Resubmit Candidate 2
        executeVendorResubmitAction("Candidate 2");

        // Final verification for Candidate 2 & 3 in the listing
        verifyVendorFinalStatus("Candidate 2");
        verifyVendorFinalStatus("Candidate 3");
    }

    private void executeVendorResubmitAction(String candidateName) {
        DashboardManager.log("   -> Clicking Resubmit CTA for " + candidateName + "...");
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));

        // Find the specific "Resubmit" button in that row
        Locator resubmitBtn = row.locator("button").filter(new Locator.FilterOptions().setHasText("Resubmit"));
        resubmitBtn.click();

        if (waitForVendorToast("resubmitted for shortlist successfully")) {
            DashboardManager.log("      ✅ Toast Verified: Candidate resubmitted successfully.");
        }
        page.waitForTimeout(2000);
    }

    private void verifyVendorFinalStatus(String candidateName) {
        // 🚀 1. Locate the row for the specific candidate
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));

        // 🚀 2. Updated Locator: Targeting the status badge inside the cell
        // We look for the inline-flex badge that contains the status text
        Locator statusBadge = row.locator("div.inline-flex");

        if (statusBadge.isVisible()) {
            String status = statusBadge.innerText().trim();

            // 🚀 3. Validation Logic: Now checking for 'Reapplied'
            if (status.equalsIgnoreCase("Reapplied")) {
                DashboardManager.log("      ✅ Final Status for " + candidateName + ": Reapplied (Olive Badge)");
            } else {
                DashboardManager.log("      ❌ Final Status for " + candidateName + ": Mismatch! Found [" + status + "]");
            }

            // Optional: Debug the background color to ensure it matches the olive style
            String bgColor = statusBadge.getAttribute("style");
            if (bgColor != null && bgColor.contains("rgb(128, 128, 0)")) {
                DashboardManager.log("      🎨 Badge Color Verified: Olive (rgb(128, 128, 0))");
            }

        } else {
            DashboardManager.log("      ❌ Could not find status badge for candidate: " + candidateName);
        }
    }

    private boolean waitForToast(String message) {
        try {
            page.getByText(message).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForVendorToast(String partialMessage) {
        try {
            page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile(partialMessage, Pattern.CASE_INSENSITIVE)))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}