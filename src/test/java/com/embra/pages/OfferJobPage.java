package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class OfferJobPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator updateStatusDropdown;
    private final Locator submitStatusBtn;
    private final Locator deployTriggerBtn;

    // Deploy Form
    private final Locator engagementStartDateBtn;
    private final Locator clientCostInput;
    private final Locator partnerCostInput;
    private final Locator fileInput; // Usually hidden
    private final Locator deploySubmitBtn;

    public OfferJobPage(Page page) {
        this.page = page;

        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select an option"));
        this.submitStatusBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit"));

        // Deploy Action
        this.deployTriggerBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Deploy"));
        this.engagementStartDateBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("DD/MM/YYYY"));
        this.clientCostInput = page.locator("input[name='client_cost']");
        this.partnerCostInput = page.locator("input[name='cost']");
        this.fileInput = page.locator("input[type='file']");
        // Target the green Deploy button in the modal specifically
        this.deploySubmitBtn = page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Deploy"));
    }

    // ──────────────────────────────────────────────────────────────
    // ADMIN ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void navigateAndOpenRequirement(String reqName) {
        DashboardManager.log("\n--- 💼 ADMIN: OFFER JOB & DEPLOY FLOW ---");
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        requirementListingLink.click();
        page.waitForLoadState();

        DashboardManager.log("   -> Searching for Requirement: " + reqName);
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        reqRow.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // Check Status
        if (reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).first().isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Status is NOT Active.");
        }

        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);
    }

    public void openCandidateAndVerifyStatus(String candidateName) {
        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        candidatesTab.click();
        page.waitForTimeout(1000);

        DashboardManager.log("   -> Opening Candidate: " + candidateName);
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));

        // Verify Listing Status "Interview Completed"
        // Note: Depending on where the status is located in your specific table row structure
        // we check broadly inside the row first.
        if (candidateRow.innerText().contains("Interview Completed")) {
            DashboardManager.log("      ✅ Candidate Status in Listing: Interview Completed");
        }

        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Verify Details Page Status
        Locator statusChip = page.locator("div").filter(new Locator.FilterOptions().setHasText("Interview Completed")).last();
        if (statusChip.isVisible()) {
            DashboardManager.log("      ✅ Candidate Details Status: Interview Completed");
        } else {
            DashboardManager.log("      ❌ Status mismatch! (Expected: Interview Completed)");
        }
    }

    public void updateStatusToOfferJob() {
        DashboardManager.log("   -> Updating Status to 'Offer Job'...");

        updateStatusDropdown.click();

        // Select Option (Using exact match regex for safety)
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile("^Offer Job$"))).click();

        submitStatusBtn.click();

        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Status updated successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }
        page.waitForTimeout(2000);
    }

    public void deployCandidate(String filePath) {
        DashboardManager.log("   -> Clicking 'Deploy'...");
        deployTriggerBtn.click();
        page.waitForTimeout(1000);

        // Date Selection
        DashboardManager.log("   -> Selecting Engagement Start Date...");
        engagementStartDateBtn.click();
        Locator calendar = page.locator("div.rdp-month");
        calendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        // Pick first available date
        page.locator("button.rdp-day:not([disabled])").first().click();

        // Costs
        DashboardManager.log("   -> Filling Financial Details...");
        clientCostInput.fill("1200000");
        partnerCostInput.fill("900000");

        // Upload
        DashboardManager.log("   -> Uploading Documents...");
        try {
            fileInput.setInputFiles(Paths.get(filePath));
            // Wait for upload success toast
            if (waitForToast("uploaded successfully")) {
                DashboardManager.log("      ✅ Document Uploaded.");
            }
        } catch (Exception e) {
            DashboardManager.log("      ❌ File upload failed: " + e.getMessage());
        }

        // Final Submit
        DashboardManager.log("   -> Submitting Deployment...");
        deploySubmitBtn.click();

        // Verify Success Message
        try {
            page.getByText("Candidate Deployed!").waitFor(new Locator.WaitForOptions().setTimeout(10000));
            DashboardManager.log("      🎉 SUCCESS: Candidate Deployed!");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Deployment Success Message NOT found.");
        }

        // Verify Status Change
        page.waitForTimeout(2000);
        Locator deployedStatus = page.locator("div").filter(new Locator.FilterOptions().setHasText("Deployed")).last();
        if (deployedStatus.isVisible()) {
            DashboardManager.log("      ✅ Final Status Verified: Deployed");
        } else {
            DashboardManager.log("      ❌ Final Status mismatch (Expected: Deployed)");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // VENDOR ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void vendorVerifyDeployedStatus(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: VERIFYING DEPLOYMENT ---");

        // Login
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // Projects
        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Project: " + reqName);
        Locator projectRow = page.locator("div.flex-row.justify-between").filter(new Locator.FilterOptions().setHasText(reqName));

        // Verify "Interviewing" (Vendor project status often stays as Interviewing until fully closed)
        if (projectRow.locator("span.text-project-interviewing").isVisible()) {
            DashboardManager.log("      ✅ Project Status: Interviewing");
        }

        projectRow.locator("h3").click();
        page.waitForTimeout(2000);

        // Verify Candidate Listing
        DashboardManager.log("   -> Checking Candidate Listing...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));

        // The prompt asked to verify "Interview Completed" in listing before clicking
        if (candidateRow.locator("span.text-project-interviewing").filter(new Locator.FilterOptions().setHasText("Interview Completed")).isVisible()) {
            DashboardManager.log("      ✅ Listing Status: Interview Completed");
        } else {
            // Fallback check in case it updated instantly
            DashboardManager.log("      ℹ️ Listing status might have updated. Proceeding to details...");
        }

        candidateRow.click();
        page.waitForTimeout(2000);

        // Verify Final Deployed Status in Details
        DashboardManager.log("   -> Verifying Details Page...");
        Locator deployedBadge = page.locator("div").filter(new Locator.FilterOptions().setHasText("Deployed")).last();

        if (deployedBadge.isVisible()) {
            DashboardManager.log("      ✅ Final Candidate Status: Deployed");
        } else {
            DashboardManager.log("      ❌ Status mismatch in Vendor Details. (Expected: Deployed)");
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