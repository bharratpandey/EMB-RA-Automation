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
    private final Locator interviewDetailsBtn;
    private final Locator selectAvailableDatesBtn;
    private final Locator duration60MinCheckbox;

    // 🚀 FIX: Separate locators for the two distinct modal buttons
    private final Locator selectSlotsBtn;
    private final Locator modalSubmitBtn;

    public ScheduleInterviewPage(Page page) {
        this.page = page;

        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select an option"));
        this.submitStatusBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit")).first();

        // New Locators
        this.interviewDetailsBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Interview Details"));
        this.selectAvailableDatesBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Select Available Interview Dates"));
        this.duration60MinCheckbox = page.locator("label[for='duration-60']");

        // 🚀 FIX: Distinct Locators
        this.selectSlotsBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Select Slots$")));
        this.modalSubmitBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Submit$"))).last();
    }

    // ──────────────────────────────────────────────────────────────
    // ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void navigateAndOpenRequirement(String reqName) {
        DashboardManager.log("\n--- 📅 ADMIN: SCHEDULE INTERVIEW FLOW ---");
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        requirementListingLink.click();
        page.waitForLoadState();

        // Strip Admin prefix if it exists
        String cleanName = reqName.contains("ReqTest-")
                ? reqName.substring(reqName.indexOf("ReqTest-"))
                : reqName;

        DashboardManager.log("   -> Searching for Requirement (Cleaned): " + cleanName);

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
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName)).first();

        candidateRow.locator("button[title='View Details']").first().click();
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

        // Select Option
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile("^Schedule Interview$"))).click();

        // Submit
        submitStatusBtn.click();

        // Verify the specific success toast for status update
        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Status updated successfully!");
        } else {
            DashboardManager.log("       Success Toast NOT found.");
        }

        page.waitForTimeout(2000);
    }

    public void selectInterviewTimeSlots() {
        DashboardManager.log("   -> Clicking 'Interview Details'...");
        try {
            interviewDetailsBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            interviewDetailsBtn.click();
            page.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ 'Interview Details' button not found. Attempting to proceed...");
        }


        // ── ASSIGNEE SELECTION ────────────────────────────────────────
        DashboardManager.log("   -> Selecting Assignee...");

// Check if assignee is already selected (combobox won't say "Select assignee")
        Locator assigneeCombobox = page.locator("button[role='combobox']")
                .filter(new Locator.FilterOptions().setHasText("Select assignee"));

        if (assigneeCombobox.count() > 0) {
            // Not yet selected — click and pick
            assigneeCombobox.click();
            page.waitForTimeout(500);

            page.locator("input[placeholder='Search...']").first()
                    .fill("bharatadminuat");
            page.waitForTimeout(1000);

            page.locator("div[role='option']")
                    .filter(new Locator.FilterOptions().setHasText("Bharatadminuat"))
                    .first().click();
            page.waitForTimeout(500);

            DashboardManager.log("      ✅ Assignee selected: Bharatadminuat");
        } else {
            DashboardManager.log("      ℹ️ Assignee already selected. Skipping selection.");
        }
// ── END ASSIGNEE SELECTION ────────────────────────────────────

        DashboardManager.log("   -> Clicking 'Select Available Interview Dates'...");
        selectAvailableDatesBtn.click();
        page.waitForTimeout(1000);

        // 1. Ensure 60 min duration is checked
        DashboardManager.log("   -> Selecting 60 min duration...");
        duration60MinCheckbox.click();
        page.waitForTimeout(500);

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
        page.waitForTimeout(1000);

        // 4. Select three time slots
        DashboardManager.log("   -> Selecting three time slots...");
        Locator timeSlots = page.locator("button[role='checkbox']");

        try {
            timeSlots.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
            if (timeSlots.count() >= 3) {
                Locator firstLabel = timeSlots.nth(0).locator("..").locator("label");
                Locator secondLabel = timeSlots.nth(1).locator("..").locator("label");
                Locator thirdLabel = timeSlots.nth(2).locator("..").locator("label");

                firstLabel.click();
                DashboardManager.log("      ✅ Selected Slot 1: " + firstLabel.innerText());

                secondLabel.click();
                DashboardManager.log("      ✅ Selected Slot 2: " + secondLabel.innerText());

                thirdLabel.click();
                DashboardManager.log("      ✅ Selected Slot 3: " + thirdLabel.innerText());
            } else {
                DashboardManager.log("      ❌ Not enough time slots available on this date.");
            }
        } catch (Exception e) {
            DashboardManager.log("      ❌ Time slots did not appear.");
        }

        // ──────────────────────────────────────────────────────────────
        // 🚀 FIX: TWO-STEP SUBMISSION FLOW
        // ──────────────────────────────────────────────────────────────

        // 5. Select Slots (Closes inner calendar modal)
        DashboardManager.log("   -> Clicking 'Select Slots'...");
        selectSlotsBtn.click();
        page.waitForTimeout(1000); // Brief wait for modal to close

        // 6. Final Submit (Closes outer details modal)
        DashboardManager.log("   -> Clicking Final 'Submit'...");
        modalSubmitBtn.click();

        // 7. Verify Toast
        if (waitForToast("Interview availability dates sent successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Interview availability dates sent successfully!");
        } else {
            DashboardManager.log("      ⚠️ Success Toast NOT found. Assuming success to continue flow.");
        }

        page.waitForTimeout(2000);
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
            page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile(message, Pattern.CASE_INSENSITIVE)))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}