package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class ClientShortlistPage {

    private final Page page;

    public ClientShortlistPage(Page page) {
        this.page = page;
    }

    // ──────────────────────────────────────────────────────────────
    // 1. CLIENT ACTIONS: LOGIN & SHORTLIST
    // ──────────────────────────────────────────────────────────────

    public void loginAndShortlist(String clientUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🤝 CLIENT: SHORTLISTING FLOW ---");
        page.navigate(clientUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // 🚀 Close Tutorial Tour if present
        Locator closeTourBtn = page.locator("button[aria-label='Close tour']");
        if (closeTourBtn.isVisible()) {
            DashboardManager.log("   -> Closing tutorial tour...");
            closeTourBtn.click();
            page.waitForTimeout(500);
        }

        // Use first() to target Sidebar 'Jobs' and avoid strict mode violation with Breadcrumbs
        page.locator("a[href='/jobs']").first().click();
        page.waitForTimeout(2000);

        String cleanName = reqName.contains("ReqTest-") ? reqName.substring(reqName.indexOf("ReqTest-")) : reqName;
        DashboardManager.log("   -> Opening Job: " + cleanName);
        page.locator("h3").filter(new Locator.FilterOptions().setHasText(cleanName)).first().click();
        page.waitForTimeout(2000);

        printClientCandidateDetails("Candidate 1");
        printClientCandidateDetails("Candidate 4");

        DashboardManager.log("   -> Attempting to click 'More Actions' for Candidate 4...");

        Locator candidate4Row = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 4"));
        Locator moreActionsBtn = candidate4Row.locator("button[title='More Actions']");

        // 🚀 FIX: Use dispatchEvent to bypass row-level click interceptors
        moreActionsBtn.scrollIntoViewIfNeeded();
        moreActionsBtn.dispatchEvent("click");

        DashboardManager.log("      -> 'More Actions' triggered. Waiting for portal menu...");
        page.waitForTimeout(1000);

        // Target 'Shortlist' button inside the high z-index portal (div.fixed)
        Locator shortlistBtn = page.locator("div.fixed button").filter(new Locator.FilterOptions().setHasText("Shortlist"));

        try {
            shortlistBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            shortlistBtn.click(new Locator.ClickOptions().setForce(true));
            DashboardManager.log("      ✅ 'Shortlist' clicked.");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Standard click failed. Trying JS fallback...");
            page.evaluate("el => el.click()", shortlistBtn.elementHandle());
        }

        if (waitForClientToast("Candidate selected successfully")) {
            DashboardManager.log("      ✅ Toast Verified: Candidate shortlisted by client.");
        }

        page.waitForTimeout(2000);
        String finalStatus = candidate4Row.locator("span.status-blue-text").innerText().trim();
        DashboardManager.log("      Candidate 4 Status: " + (finalStatus.equals("Shortlisted") ? "✅ Shortlisted" : "❌ " + finalStatus));
    }

    // ──────────────────────────────────────────────────────────────
    // 2. ADMIN VERIFICATION: CHECK SHORTLISTED STATUS
    // ──────────────────────────────────────────────────────────────

    public void verifyShortlistOnAdmin(String reqName) {
        DashboardManager.log("\n--- 💼 ADMIN: VERIFYING CLIENT SHORTLIST ---");
        page.locator("a[href='/hiring-requests']").first().click();
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 4")).locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        boolean isApplied = page.locator("div").filter(new Locator.FilterOptions().setHasText("Applied")).last().isVisible();
        boolean hasShortlistMsg = page.locator("p").filter(new Locator.FilterOptions().setHasText("This profile has been Shortlisted by client.")).isVisible();

        if (isApplied && hasShortlistMsg) {
            DashboardManager.log("      ✅ Status Verified: Applied + Shortlisted message visible.");
        } else {
            DashboardManager.log("      ❌ Status Mismatch on Admin details.");
        }

        page.locator("button").filter(new Locator.FilterOptions().setHasText("Back")).click();
    }

    // ──────────────────────────────────────────────────────────────
    // 3. CLIENT ACTIONS: REJECT CANDIDATE
    // ──────────────────────────────────────────────────────────────

    public void clientRejectCandidate(String reqName) {
        DashboardManager.log("\n--- 🤝 CLIENT: REJECTION FLOW ---");

        // 🚀 1. Safety Check: If we aren't on the Job Details page, navigate there
        if (!page.url().contains("jobs/details")) {
            DashboardManager.log("   -> Not on Details page. Navigating to Job Listing...");
            page.locator("a[href='/jobs']").first().click();
            page.waitForTimeout(2000);

            String cleanName = reqName.contains("ReqTest-") ? reqName.substring(reqName.indexOf("ReqTest-")) : reqName;
            DashboardManager.log("   -> Opening Job: " + cleanName);

            // Using a broader locator for the project card link
            Locator projectCard = page.locator("a").filter(new Locator.FilterOptions().setHasText(cleanName)).first();
            projectCard.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            projectCard.click();
            page.waitForLoadState();
        }

        // 🚀 2. Flexible Project Status Check (Fixed the timeout here)
        try {
            Locator statusBadge = page.locator("span[class*='text-project-']").first();
            statusBadge.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            DashboardManager.log("      Project Status: [" + statusBadge.innerText().trim() + "]");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Could not verify project status badge, proceeding to candidate list...");
        }

        // 🚀 3. Click 'More Actions' for Candidate 4
        DashboardManager.log("   -> Rejecting Candidate 4...");

        // Use a more specific row locator to avoid multi-row matches
        Locator candidate4Row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(Pattern.compile("Candidate 4", Pattern.CASE_INSENSITIVE))).first();
        candidate4Row.scrollIntoViewIfNeeded();

        // Target the button directly using title
        Locator moreActionsBtn = candidate4Row.locator("button[title='More Actions']");

        try {
            moreActionsBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            // Force dispatch click to bypass row overlays
            moreActionsBtn.dispatchEvent("click");
            DashboardManager.log("      -> 'More Actions' triggered via Dispatch.");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Dispatch failed. Trying JS click fallback...");
            page.evaluate("el => el.click()", moreActionsBtn.elementHandle());
        }

        page.waitForTimeout(1500);

        // 🚀 4. Click 'Reject' in the portal dropdown menu (div.fixed)
        Locator rejectOption = page.locator("div.fixed button, [role='menuitem']").filter(new Locator.FilterOptions().setHasText("Reject")).first();
        try {
            rejectOption.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            rejectOption.click(new Locator.ClickOptions().setForce(true));
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Dropdown Reject click failed. Trying JS fallback...");
            page.evaluate("el => el.click()", rejectOption.elementHandle());
        }

        // 🚀 5. Fill Reason and Finalize
        page.locator("textarea[name='reason']").fill("this is automated Reason");
        // Target the specific red Reject button in the modal
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Reject$"))).click();

        if (waitForClientToast("Candidate rejected successfully")) {
            DashboardManager.log("      ✅ Toast Verified: Candidate rejected.");
        }

        page.waitForTimeout(2000);
        // Robust status check
        String finalStatus = candidate4Row.innerText();
        DashboardManager.log("      Candidate 4 Row Text: " + finalStatus);
        if (finalStatus.contains("Rejected")) {
            DashboardManager.log("      Candidate 4 Status: ✅ Rejected");
        } else {
            DashboardManager.log("      Candidate 4 Status: ❌ Status not updated in UI");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4. ADMIN VERIFICATION: CHECK REJECTED STATUS
    // ──────────────────────────────────────────────────────────────

    public void verifyRejectionOnAdmin(String reqName) {
        DashboardManager.log("\n--- 💼 ADMIN: VERIFYING CLIENT REJECTION ---");
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();
        page.getByText(reqName).first().click();

        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        String status = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 4")).locator("td").nth(4).innerText().trim();
        DashboardManager.log("      Candidate 4 Admin Status: " + (status.contains("Rejected") ? "✅ Rejected" : "❌ " + status));
    }

    private void printClientCandidateDetails(String candidateName) {
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));
        if (row.count() > 0) {
            String details = row.innerText().trim().replaceAll("\\s+", " ").replace("\n", " | ");
            DashboardManager.log("      [" + candidateName + " Details]: " + details);
        }
    }

    private boolean waitForClientToast(String partialMessage) {
        try {
            page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile(partialMessage, Pattern.CASE_INSENSITIVE)))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}