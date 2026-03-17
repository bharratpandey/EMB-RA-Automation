package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class RequirementCompletedPage {

    private final Page page;

    public RequirementCompletedPage(Page page) {
        this.page = page;
    }

    // ──────────────────────────────────────────────────────────────
    // 1. ADMIN ACTIONS: DEPLOY CANDIDATE
    // ──────────────────────────────────────────────────────────────

    public void adminDeployCandidate(String reqName, String candidateName, String vendorName, String dummyFilePath) {
        DashboardManager.log("\n--- 👮 ADMIN: DEPLOYMENT & COMPLETION FLOW ---");

        // Navigate and Open Requirement
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        // Open Candidates Tab
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        // Open specific Candidate View
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName))
                .filter(new Locator.FilterOptions().setHasText(vendorName)).first();
        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        page.reload();

        // 🚀 STATUS UPDATE (Using the working OfferJob logic style)
        DashboardManager.log("   -> Updating status to 'Offer Job'...");
        Locator dropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText(Pattern.compile("option|Applied|Resubmit|Interview|Hold", Pattern.CASE_INSENSITIVE))).first();
        dropdown.click();

        // Wait for list container
        page.locator("div[role='listbox'], [id^='radix-']").first().waitFor();
        page.waitForTimeout(500);

        for (int i = 0; i < 6; i++) {
            page.keyboard().press("ArrowDown");
            page.waitForTimeout(100);
        }
        page.keyboard().press("Enter");
        page.waitForTimeout(1000);

        // Click Submit and handle Toast
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit")).click();
        waitForToast("Status updated successfully!");

        // 🚀 DISMISS TOAST (Crucial step to unblock the Deploy button)
        if (page.locator("li[data-sonner-toast]").isVisible()) {
            page.locator("li[data-sonner-toast]").click();
            page.waitForTimeout(1000);
        }

        // 🚀 DEPLOYMENT FLOW (Mirrored from your working OfferJobPage)
        DashboardManager.log("   -> Clicking 'Deploy'...");
        // Use first() because one exists on the page and one in the portal
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Deploy$"))).first().click();
        page.waitForTimeout(1500);

        // 1. Engagement Start Date Selection
        DashboardManager.log("   -> Selecting Engagement Start Date...");
        page.locator("button[aria-haspopup='dialog']").first().click();

        // Use the OfferJob logic: wait for start calendar and click first valid day
        Locator startCalendar = page.locator("div.rdp-month").first();
        startCalendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.locator("button.rdp-day_button:not([disabled]):not(.rdp-day_outside)").first().click();
        page.waitForTimeout(500);

        // 2. Conditional Logic for Engagement End Date
        Locator endDateLabel = page.locator("label").filter(new Locator.FilterOptions().setHasText("Engagement End Date"));
        if (endDateLabel.isVisible()) {
            DashboardManager.log("   -> Engagement End Date visible. Selecting date...");
            // Click the second date button
            page.locator("button[aria-haspopup='dialog']").nth(1).click();
            page.waitForTimeout(500);

            Locator endCalendar = page.locator("div.rdp-month").last();
            endCalendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            page.locator("button.rdp-day_button:not([disabled]):not(.rdp-day_outside)").last().click();
            DashboardManager.log("      ✅ Engagement End Date Selected.");
        }

        // 3. Costs
        DashboardManager.log("   -> Filling Financial Details...");
        page.locator("input[name='client_cost']").fill("1200000");
        page.locator("input[name='cost']").fill("900000");

        // 4. Upload
        DashboardManager.log("   -> Uploading Documents...");
        page.setInputFiles("input[id='deploy-docs']", Paths.get(dummyFilePath));
        waitForToast("uploaded successfully");
        page.waitForTimeout(1000);

        // 5. Final Submit (Using .last() to target the green Deploy button in modal)
        DashboardManager.log("   -> Submitting Deployment...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Deploy$"))).last().click(new Locator.ClickOptions().setForce(true));

        // ──────────────────────────────────────────────────────────────
        // VERIFICATIONS
        // ──────────────────────────────────────────────────────────────
        try {
            page.getByText("Candidate Deployed!").waitFor(new Locator.WaitForOptions().setTimeout(10000));
            DashboardManager.log("      🎉 SUCCESS: Candidate Deployed!");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Success message not detected, checking UI status badge...");
        }

        page.waitForTimeout(2000);
        Locator deployedStatus = page.locator("div").filter(new Locator.FilterOptions().setHasText("Deployed")).last();
        if (deployedStatus.isVisible()) {
            DashboardManager.log("      ✅ Final Status Verified: Deployed");
        }

        // Back to Listing to verify "Completed" status
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Back")).click();
        page.waitForTimeout(1000);
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Back")).click();
        verifyAndPrintRequirementStatus(reqName, "Completed");
    }

    public void verifyPortalStatus(String portalName, String url, String email, String pass, String reqName) {
        DashboardManager.log("\n--- 🌐 " + portalName.toUpperCase() + ": VERIFYING COMPLETION ---");

        // 🚀 CLEANUP: Extract only the unique part (ReqTest-...)
        // This ensures "AUT131 - ReqTest-..." becomes "ReqTest-..."
        String cleanName = reqName.contains("ReqTest-") ?
                reqName.substring(reqName.indexOf("ReqTest-")) : reqName;

        DashboardManager.log("   -> Searching for Clean ID: [" + cleanName + "]");

        page.navigate(url);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(pass);
        page.locator("button[type='submit']").click();
        page.waitForLoadState();

        // Dismiss Tutorial
        try {
            Locator closeTourBtn = page.locator("button[aria-label='Close tour']");
            if (closeTourBtn.isVisible()) {
                closeTourBtn.dispatchEvent("click");
                page.locator("div.fixed.inset-0.z-\\[9999\\]").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
            }
        } catch (Exception ignored) {}

        String tabName = portalName.equalsIgnoreCase("Vendor") ? "/projects" : "/jobs";
        DashboardManager.log("   -> Navigating to: " + tabName);
        page.locator("a[href='" + tabName + "']").first().dispatchEvent("click");
        page.waitForTimeout(3000);

        // 🚀 SEARCH LOGIC: Look for the cleaned name + Completed status
        // We target the card container (div or a) that contains the unique ReqTest ID
        Locator projectCard = page.locator("a, div").filter(new Locator.FilterOptions()
                        .setHasText(cleanName))
                .filter(new Locator.FilterOptions().setHasText("Completed"))
                .last();

        if (projectCard.isVisible()) {
            String cardContent = projectCard.innerText().trim();
            if (cardContent.contains("Completed")) {
                DashboardManager.log("      ✅ " + portalName + " Status Verified: [Completed] for " + cleanName);
            } else {
                DashboardManager.log("      ❌ " + portalName + " Status Mismatch! 'Completed' text missing in card.");
            }
        } else {
            DashboardManager.log("      ❌ " + portalName + " Status Mismatch! Card for " + cleanName + " not found.");
            // Print what is actually visible to help debug
            if (page.locator("h3").count() > 0) {
                DashboardManager.log("      ℹ️ First visible card on screen: " + page.locator("h3").first().innerText());
            }
        }
    }

    private void verifyAndPrintRequirementStatus(String reqName, String expected) {
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName)).first();
        row.scrollIntoViewIfNeeded();
        String actual = row.locator("td").nth(3).innerText().trim();
        DashboardManager.log("      Requirement [" + reqName + "] Status: " + (actual.equalsIgnoreCase(expected) ? "✅ " : "❌ ") + actual);
    }

    private void waitForToast(String text) {
        try {
            page.locator("li[data-sonner-toast]").filter(new Locator.FilterOptions().setHasText(Pattern.compile(text, Pattern.CASE_INSENSITIVE)))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        } catch (Exception ignored) {}
    }
}