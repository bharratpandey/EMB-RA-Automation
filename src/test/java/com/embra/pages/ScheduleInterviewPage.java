package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class ScheduleInterviewPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator updateStatusDropdown;
    private final Locator submitStatusBtn;

    // Interview Modal Locators
    private final Locator selectTimeBtn;
    private final Locator sendSlotsBtn;

    public ScheduleInterviewPage(Page page) {
        this.page = page;

        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select an option"));
        this.submitStatusBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit"));

        this.selectTimeBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Select Time"));
        this.sendSlotsBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Send Slots"));
    }

    // ──────────────────────────────────────────────────────────────
    // ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void navigateAndOpenRequirement(String reqName) {
        DashboardManager.log("\n--- 📅 ADMIN: SCHEDULE INTERVIEW FLOW ---");
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        requirementListingLink.click();
        page.waitForLoadState();

        // 🚀 FIX: Strip Admin prefix if it exists (removes everything before "ReqTest")
        String cleanName = reqName.contains("ReqTest-")
                ? reqName.substring(reqName.indexOf("ReqTest-"))
                : reqName;

        DashboardManager.log("   -> Searching for Requirement (Cleaned): " + cleanName);

        // 🚀 Use cleanName to find the specific row
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(cleanName));
        reqRow.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // Check Status
        Locator statusBadge = reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active"));
        if (statusBadge.first().isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Status is NOT Active or not found.");
        }

        // Open Requirement
        page.getByText(cleanName).first().click();
        page.waitForTimeout(2000);

        // Verify Status on Details Page
        Locator detailsStatus = page.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active"));
        if (detailsStatus.first().isVisible()) {
            DashboardManager.log("      ✅ Details Page Status: Active");
        }
    }

    public void openCandidateForInterview(String candidateName) {
        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        candidatesTab.click();
        page.waitForTimeout(1000);

        DashboardManager.log("   -> Opening Candidate: " + candidateName);
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));

        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Verify Candidate Status
        Locator statusChip = page.locator("div").filter(new Locator.FilterOptions().setHasText("Assignment Completed")).last();
        if (statusChip.isVisible()) {
            DashboardManager.log("      ✅ Candidate Status: Assignment Completed");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch! (Expected: Assignment Completed)");
        }
    }

    public void updateStatusToScheduleInterview() {
        DashboardManager.log("   -> Updating Status to 'Schedule Interview'...");

        // Open Dropdown
        updateStatusDropdown.click();

        // Select Option (Using exact match to avoid clicking "Schedule Assessment" or "Schedule Assignment" by accident)
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile("^Schedule Interview$"))).click();

        // Submit
        submitStatusBtn.click();

        // Verify Toast
        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Status updated successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }

        page.waitForTimeout(2000);
    }

    public void selectInterviewTimeSlots() {
        DashboardManager.log("   -> Clicking 'Select Time'...");
        selectTimeBtn.click();
        page.waitForTimeout(1000);

        // 1. Ensure 60 min duration is checked (It's checked by default in your HTML, but good to verify/click)
        DashboardManager.log("   -> Selecting 60 min duration...");
        page.locator("label[for='duration-60']").click();

        // 2. Wait for calendar to appear
        Locator calendar = page.locator("div.rdp-month");
        calendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // 3. Select the first available non-disabled day
        DashboardManager.log("   -> Selecting an available date...");
        Locator availableDate = page.locator("button.rdp-day:not([disabled])").first();

        try {
            availableDate.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            availableDate.click();
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to click available date.");
            return;
        }
        page.waitForTimeout(1000); // Wait for timeslots to load on the right side

        // 4. Select two time slots
        DashboardManager.log("   -> Selecting two time slots...");
        // Target the checkboxes inside the time slot list
        Locator timeSlots = page.locator("button[role='checkbox']");

        try {
            timeSlots.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
            if (timeSlots.count() >= 2) {
                // Click the labels associated with the first two checkboxes
                Locator firstLabel = timeSlots.nth(0).locator("..").locator("label");
                Locator secondLabel = timeSlots.nth(1).locator("..").locator("label");

                firstLabel.click();
                DashboardManager.log("      ✅ Selected Slot 1: " + firstLabel.innerText());

                secondLabel.click();
                DashboardManager.log("      ✅ Selected Slot 2: " + secondLabel.innerText());
            } else {
                DashboardManager.log("      ❌ Not enough time slots available on this date.");
            }
        } catch (Exception e) {
            DashboardManager.log("      ❌ Time slots did not appear.");
        }

        // 5. Submit Slots
        DashboardManager.log("   -> Submitting Interview Slots...");
        sendSlotsBtn.click();

        // 6. Verify Toast
        if (waitForToast("Interview availability dates sent successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Interview availability dates sent successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }

        page.waitForTimeout(2000); // Wait for details card to appear
    }

    public void verifyInterviewDetails() {
        DashboardManager.log("   🔍 Verifying Interview Details...");

        Locator detailsCard = page.locator("div.bg-white.border").filter(new Locator.FilterOptions().setHasText("Interview Details"));

        if (detailsCard.isVisible()) {
            DashboardManager.log("\n------------------------------------------------");
            DashboardManager.log(detailsCard.innerText().replace("\n", " | "));
            DashboardManager.log("------------------------------------------------\n");
        } else {
            DashboardManager.log("   ❌ Interview Details Card NOT found.");
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