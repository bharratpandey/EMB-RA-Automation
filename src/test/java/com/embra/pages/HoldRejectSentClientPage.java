package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class HoldRejectSentClientPage {

    private final Page page;

    // Admin Side Locators
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator updateStatusDropdown;
    private final Locator submitStatusBtn;
    private final Locator confirmBtn;
    private final Locator backBtn;
    private final Locator reasonTextarea;

    public HoldRejectSentClientPage(Page page) {
        this.page = page;

        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("--Select an option--"));
        this.submitStatusBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit"));
        this.confirmBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Confirm"));
        this.backBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Back"));
        this.reasonTextarea = page.locator("textarea#reason");
    }

    // ──────────────────────────────────────────────────────────────
    // 1. ADMIN ACTIONS: HOLD, REJECT, AND SHARE WITH CLIENT
    // ──────────────────────────────────────────────────────────────

    public void processCandidatesOnAdmin(String reqName) {
        DashboardManager.log("\n--- 💼 ADMIN: HOLD / REJECT / SHARE FLOW ---");
        requirementListingLink.click();
        page.waitForLoadState();

        // A. Print Requirement Listing Status
        DashboardManager.log("   -> Searching for Requirement: " + reqName);
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        String listingStatus = row.locator("td").nth(3).innerText().trim();

        if (listingStatus.equalsIgnoreCase("Active")) {
            DashboardManager.log("      ✅ Requirement Status in Listing: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Status in Listing: " + listingStatus);
        }

        // Open Requirement
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        // B. Print Details Page Status
        String detailStatus = page.locator("div.mb-10 div.px-4").innerText().trim();
        if (detailStatus.equalsIgnoreCase("Active")) {
            DashboardManager.log("      ✅ Requirement Detail Status: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Detail Status: " + detailStatus);
        }

        candidatesTab.click();
        page.waitForTimeout(1000);

        // --- C. PROCESS CANDIDATE 2: PUT ON HOLD ---
        handleStatusUpdate("Candidate 2", "Put on hold", 1);

        // --- D. PROCESS CANDIDATE 3: REJECT ---
        handleStatusUpdate("Candidate 3", "Reject Candidate", 2);

        // --- E. PROCESS CANDIDATE 4: SHARE WITH CLIENT ---
        handleClientSharing("Candidate 4", "4000000");
    }

    private void handleStatusUpdate(String candidateName, String statusOption, int arrowDownCount) {
        DashboardManager.log("   -> Opening " + candidateName + " for status update: " + statusOption);
        page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName)).locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Verify Initial Applied Status
        Locator initialBadge = page.locator("div.text-sm").filter(new Locator.FilterOptions().setHasText("Applied")).last();
        if (initialBadge.isVisible()) {
            DashboardManager.log("      ✅ Initial Status: Applied");
        } else {
            DashboardManager.log("      ❌ Initial Status mismatch");
        }

        // 1. Open the dropdown
        updateStatusDropdown.click();
        page.waitForTimeout(500); // Wait for Radix list to appear

        // 2. STABLE SELECTION: Click the option by text directly
        // This is much safer than ArrowDown because it triggers the UI state correctly
        try {
            Locator option = page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText(statusOption));
            option.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            option.click();
            DashboardManager.log("      -> Selected '" + statusOption + "' from dropdown.");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Direct click failed. Falling back to keyboard arrows...");
            for (int i = 0; i < arrowDownCount; i++) {
                page.keyboard().press("ArrowDown");
                page.waitForTimeout(100);
            }
            page.keyboard().press("Enter");
        }

        page.waitForTimeout(1000); // Allow Submit button to enable

        // 3. Click Submit (This was failing because it was disabled)
        if (submitStatusBtn.isEnabled()) {
            submitStatusBtn.click();
            DashboardManager.log("      -> Clicked Submit CTA.");
        } else {
            DashboardManager.log("      ❌ Submit button is STILL DISABLED. Force-clicking via Javascript...");
            page.evaluate("arguments[0].click();", submitStatusBtn.elementHandle());
        }

        // 4. Fill Reason & Confirm
        reasonTextarea.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        reasonTextarea.fill("This is automated reason");
        page.waitForTimeout(1000);
        confirmBtn.click();

        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Status Update Success Toast received.");
        }

        // Verify final state print
        page.waitForTimeout(1000);
        String finalStatus = page.locator("div.text-sm.font-medium.w-fit").last().innerText().trim();
        DashboardManager.log("      Candidate Final Status: [" + finalStatus + "]");

        backBtn.click();
        page.waitForTimeout(2000);
    }

    private void handleClientSharing(String candidateName, String cost) {
        DashboardManager.log("   -> Opening " + candidateName + " for Client Sharing...");
        page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName)).locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // 1. You MUST select "Share with Client" to make the cost field appear
        DashboardManager.log("      -> Selecting 'Share with Client' from dropdown...");
        updateStatusDropdown.click();
        page.waitForTimeout(500);

        // Option 7 is "Share with Client"
        try {
            Locator option = page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("Share with Client"));
            option.click();
        } catch (Exception e) {
            // Fallback to arrows if click fails (Arrow down 7 times for Share with Client)
            for (int i = 0; i < 7; i++) {
                page.keyboard().press("ArrowDown");
            }
            page.keyboard().press("Enter");
        }

        // 2. Click the first Submit to open the Cost Modal/Field
        submitStatusBtn.click();
        page.waitForTimeout(1000);

        // 3. Now fill the cost (The locator should now be available)
        DashboardManager.log("      -> Filling Client Cost: " + cost);
        Locator costInput = page.locator("input[type='number']");
        costInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        costInput.fill(cost);

        // 4. Click the Final Submit button inside the pop-up/form
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Submit$"))).last().click();

        if (waitForToast("Client budget updated successfully")) {
            DashboardManager.log("      ✅ Toast Verified: Client budget shared.");
        }

        // 5. Verify the blue message appears
        Locator clientMsg = page.locator("p").filter(new Locator.FilterOptions().setHasText("This profile has been sent to client."));
        if (clientMsg.isVisible()) {
            DashboardManager.log("      ✅ Status Verified: This profile has been sent to client.");
        }

        backBtn.click();
        page.waitForTimeout(2000);
    }

    public void printFinalSummaryAdmin() {
        DashboardManager.log("\n   🔍 FINAL ADMIN STATUS SUMMARY:");
        String[] list = {"Candidate 1", "Candidate 2", "Candidate 3", "Candidate 4"};
        for (String name : list) {
            String status = page.locator("tr").filter(new Locator.FilterOptions().setHasText(name)).locator("td").nth(4).innerText().trim().replace("\n", " ");
            DashboardManager.log("      " + name + " = " + status);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. VENDOR ACTIONS: VERIFY ALL STATUSES
    // ──────────────────────────────────────────────────────────────

    public void vendorVerifyFinalStatuses(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: FINAL STATUS VERIFICATION ---");
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        // Strip prefix for Vendor Dashboard
        String cleanName = reqName.contains("ReqTest-") ? reqName.substring(reqName.indexOf("ReqTest-")) : reqName;

        DashboardManager.log("   -> Searching for Project (Cleaned): " + cleanName);
        Locator projectCard = page.locator("a").filter(new Locator.FilterOptions().setHasText(cleanName));

        String projStatus = projectCard.locator("span.text-project-interviewing").innerText().trim();
        if (projStatus.equalsIgnoreCase("Interviewing")) {
            DashboardManager.log("      ✅ Project Status on Dashboard: Interviewing");
        } else {
            DashboardManager.log("      ❌ Project Status on Dashboard: " + projStatus);
        }

        projectCard.first().click();
        page.waitForTimeout(3000);

        DashboardManager.log("   🔍 FINAL VENDOR STATUS SUMMARY:");
        String[] list = {"Candidate 1", "Candidate 2", "Candidate 3", "Candidate 4"};
        for (String name : list) {
            Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(name));
            String status = row.locator("span[class*='status-'], span[class*='text-project-']").first().innerText().trim();
            DashboardManager.log("      " + name + " = " + status);
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
}