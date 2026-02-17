package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class ScheduleAssignmentPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator updateStatusDropdown;
    private final Locator submitStatusBtn;

    // Schedule Assignment Modal
    private final Locator scheduleAssignmentCta;
    private final Locator titleInput;
    private final Locator descInput;
    private final Locator fileInput;
    private final Locator datePickerTrigger;
    private final Locator submitAssignmentBtn;

    public ScheduleAssignmentPage(Page page) {
        this.page = page;

        // Navigation
        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));

        // Status Update
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select an option"));
        this.submitStatusBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit"));

        // Assignment Form
        this.scheduleAssignmentCta = page.locator("button").filter(new Locator.FilterOptions().setHasText("Schedule Assignment"));
        this.titleInput = page.locator("input[placeholder*='Enter assignment name']");
        this.descInput = page.locator("textarea[placeholder*='Enter assignment description']");

        // Usually hidden input for file upload
        this.fileInput = page.locator("input[type='file']");

        this.datePickerTrigger = page.locator("button").filter(new Locator.FilterOptions().setHasText("Select submission date"));
        this.submitAssignmentBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Assignment"));
    }

    // ──────────────────────────────────────────────────────────────
    // ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void navigateAndOpenRequirement(String reqName) {
        DashboardManager.log("\n--- 📂 ADMIN: ASSIGNMENT FLOW ---");
        DashboardManager.log("   -> Navigating to Requirement Listing...");
        requirementListingLink.click();
        page.waitForLoadState();

        DashboardManager.log("   -> Searching for Requirement: " + reqName);

        // 1. Find the specific row for this requirement
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        reqRow.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // 2. Check Status (FIXED STRICT MODE ERROR by adding .first())
        Locator statusBadge = reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).first();

        if (statusBadge.isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Status is NOT Active or not found.");
        }

        // 3. Click Requirement Name to Open
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000); // Wait for page load

        // 4. Verify Status on Details Page (FIXED STRICT MODE ERROR by adding .first())
        Locator detailsStatus = page.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).first();
        if (detailsStatus.isVisible()) {
            DashboardManager.log("      ✅ Details Page Status: Active");
        }
    }

    public void openCandidateForAssignment(String candidateName) {
        DashboardManager.log("   -> Clicking 'Candidates' Tab...");
        candidatesTab.click();
        page.waitForTimeout(1000);

        DashboardManager.log("   -> Opening Candidate: " + candidateName);
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));

        // Click the "View" eye icon in that row
        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Verify Candidate Status
        Locator statusChip = page.locator("div").filter(new Locator.FilterOptions().setHasText("Assignment Shortlisted")).last();
        if (statusChip.isVisible()) {
            DashboardManager.log("      ✅ Candidate Status: Assignment Shortlisted");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch! (Expected: Assignment Shortlisted)");
        }
    }

    public void updateStatusToScheduleAssignment() {
        DashboardManager.log("   -> Updating Status to 'Schedule Assignment'...");

        // Open Dropdown
        updateStatusDropdown.click();

        // Select Option
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("Schedule Assignment")).click();

        // Submit
        submitStatusBtn.click();

        // Verify Toast
        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Status updated successfully!");
        }

        // Verify Status again
        page.waitForTimeout(2000);
        Locator statusChip = page.locator("div").filter(new Locator.FilterOptions().setHasText("Assignment Shortlisted")).last();
        if (statusChip.isVisible()) {
            DashboardManager.log("      ✅ Current Status Verified: Assignment Shortlisted");
        } else {
            DashboardManager.log("      ❌ Current Status NOT Assignment Shortlisted.");
        }
    }

    public void scheduleAssignmentAction(String filePath) {
        DashboardManager.log("   -> Clicking 'Schedule Assignment' CTA...");
        scheduleAssignmentCta.click();

        DashboardManager.log("   -> Filling Assignment Details...");
        titleInput.fill("SSW");
        descInput.fill("TEST");

        // Upload File
        try {
            fileInput.setInputFiles(Paths.get(filePath));
            DashboardManager.log("      ✅ File Uploaded: " + filePath);
        } catch (Exception e) {
            DashboardManager.log("      ❌ File Upload Failed: " + e.getMessage());
        }

        // Handle Calendar
        DashboardManager.log("   -> Selecting Submission Date...");
        datePickerTrigger.click();

        // Wait for calendar to appear
        Locator calendar = page.locator("div.rdp-month");
        calendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // Select the next available non-disabled day (e.g., 15th)
        Locator availableDate = page.locator("button.rdp-day:not([disabled])").nth(2);

        try {
            availableDate.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            availableDate.click();
            DashboardManager.log("      ✅ Date Selected.");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Failed to select specific date, trying first available...");
            page.locator("button.rdp-day:not([disabled])").first().click();
        }

        // Submit
        submitAssignmentBtn.click();
        DashboardManager.log("   -> Submitted Assignment.");

        // Verify Toast
        if (waitForToast("Assignment sent successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Assignment sent successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }
    }

    public void verifyAssignmentDetails() {
        DashboardManager.log("   🔍 Verifying Uploaded Assignment Details...");
        page.waitForTimeout(2000); // Allow data refresh

        Locator assignmentCard = page.locator("div.border").filter(new Locator.FilterOptions().setHasText("Uploaded Assignment"));

        if (assignmentCard.isVisible()) {
            DashboardManager.log("\n------------------------------------------------");
            DashboardManager.log(assignmentCard.innerText());
            DashboardManager.log("------------------------------------------------\n");
        } else {
            DashboardManager.log("   ❌ Uploaded Assignment Card NOT found.");
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

    // ──────────────────────────────────────────────────────────────
    // 🆕 NEW: VENDOR SUBMITS ASSIGNMENT SOLUTION
    // ──────────────────────────────────────────────────────────────

    public void vendorSubmitAssignmentSolution(String vendorUrl, String email, String password, String reqName, String dummyFilePath) {
        DashboardManager.log("\n--- 🏢 VENDOR: SUBMITTING ASSIGNMENT SOLUTION ---");

        // 1. Login & Navigate
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Project: " + reqName);
        Locator projectTitle = page.locator("h3").filter(new Locator.FilterOptions().setHasText(reqName));

        // 2. Verify Project Status (Interviewing)
        Locator projectCard = projectTitle.locator("..").locator(".."); // Traverse up to row container
        Locator interviewingBadge = projectCard.locator("span.text-project-interviewing");
        if (interviewingBadge.isVisible()) {
            DashboardManager.log("      ✅ Project Status: Interviewing");
        } else {
            DashboardManager.log("      ❌ Project Status mismatch (Expected: Interviewing)");
        }

        projectTitle.click();
        page.waitForTimeout(2000);

        // 3. Verify Candidate Status & Open
        DashboardManager.log("   -> Opening Candidate 1...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));
        Locator ongoingBadge = candidateRow.locator("span.status-blue-text");
        if (ongoingBadge.isVisible() && ongoingBadge.innerText().contains("Assessment Ongoing")) {
            DashboardManager.log("      ✅ Candidate Status: Assessment Ongoing");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch. Found: " + candidateRow.innerText());
        }
        candidateRow.click();
        page.waitForTimeout(2000);

        // 4. Print Assignment Listing Details (We are already on the Assignment tab)
        Locator assignmentRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("SSW"));
        DashboardManager.log("\n      [Vendor Assignment Listing Details]");
        DashboardManager.log("      " + assignmentRow.innerText().replace("\n", " | "));
        DashboardManager.log("      -------------------------------------\n");

        // 5. Upload Solution Form
        DashboardManager.log("   -> Clicking Upload & Filling Form...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Upload")).click();

        page.locator("input[name='title']").fill("SENIOR software engineer");
        page.locator("input[type='file']").setInputFiles(Paths.get(dummyFilePath));
        page.locator("textarea[name='description']").fill("this is automated field");

        page.locator("input[name='links.0']").fill("www.example1.com");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Add new link")).click();
        page.locator("input[name='links.1']").fill("www.example2.com");

        // Submit Assignment
        page.locator("button[type='submit']").filter(new Locator.FilterOptions().setHasText("Submit Assignment")).click();

        // 6. Verify Toast & Status Change
        if (waitForToast("Assignment submitted successfully.")) {
            DashboardManager.log("      ✅ Toast Verified: Assignment submitted successfully.");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }

        DashboardManager.log("   ⏳ Waiting 4 seconds for system update...");
        page.waitForTimeout(4000);

        // Verify new status is "Assignment Submitted"
        Locator newAssignmentRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("SSW"));
        Locator submittedBadge = newAssignmentRow.locator("span.status-blue-text");
        if (submittedBadge.isVisible() && submittedBadge.innerText().contains("Assignment Submitted")) {
            DashboardManager.log("      ✅ Assignment Status Verified: Assignment Submitted");
        } else {
            DashboardManager.log("      ❌ Assignment Status mismatch. Found: " + newAssignmentRow.innerText());
        }

        // 7. Open Details Modal & Compare
        DashboardManager.log("   -> Opening Assignment Details Modal...");
        newAssignmentRow.click();
        page.waitForTimeout(1000);

        Locator detailsModal = page.locator("div.flex-col").filter(new Locator.FilterOptions().setHasText("Received on")).first();
        DashboardManager.log("\n      [Vendor Assignment Modal Details]");
        DashboardManager.log("      " + detailsModal.innerText().replace("\n", " | "));
        DashboardManager.log("      -------------------------------------\n");

        DashboardManager.log("   -> Closing Modal...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Close")).click();
    }

    // ──────────────────────────────────────────────────────────────
    // 🆕 NEW: ADMIN REVIEWS ASSIGNMENT & SUBMITS FEEDBACK
    // ──────────────────────────────────────────────────────────────

    public void adminSubmitAssignmentFeedback(String reqName, String candidateName) {
        DashboardManager.log("\n--- 👮 ADMIN: REVIEW ASSIGNMENT & SUBMIT FEEDBACK ---");

        DashboardManager.log("   -> Navigating to Requirement Listing...");
        requirementListingLink.click();
        page.waitForLoadState();

        // Check active status & open
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        if (reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        } else {
            DashboardManager.log("      ❌ Requirement Status is NOT Active.");
        }
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        // Open Candidate
        candidatesTab.click();
        page.waitForTimeout(1000);
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));
        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Verify Candidate Status
        Locator statusChip = page.locator("div").filter(new Locator.FilterOptions().setHasText("Assignment Ongoing")).last();
        if (statusChip.isVisible()) {
            DashboardManager.log("      ✅ Candidate Status: Assignment Ongoing");
        } else {
            DashboardManager.log("      ⚠️ Candidate Status mismatch (Found: " + page.locator("div.text-white").first().innerText() + ")");
        }

        // Go to Assignment Tab (Using exact ARIA Role from your HTML)
        DashboardManager.log("   -> Switching to Assignment Tab...");
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Assignment")).click();
        page.waitForTimeout(2000); // Wait for tab data to render

        // Print Assignment Solution Data (Targeting the exact wrapper class from your HTML)
        Locator solutionCard = page.locator("div.bg-white.border.border-gray-200.rounded-lg.p-4").first();
        try {
            solutionCard.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            DashboardManager.log("\n      [Admin View: Uploaded Assignment & Solution Details]");
            DashboardManager.log("      " + solutionCard.innerText().replace("\n", " | "));
            DashboardManager.log("      ------------------------------------------------------\n");
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Could not print assignment details: " + e.getMessage());
        }

        // Submit Feedback
        page.waitForTimeout(1800);
        DashboardManager.log("   -> Clicking 'Submit Feedback'...");
        Locator submitFeedbackBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Feedback")).first();
        submitFeedbackBtn.click();

        DashboardManager.log("   -> Filling Feedback Reason...");
        Locator feedbackTextarea = page.locator("textarea[placeholder='Enter your feedback reason...']");
        feedbackTextarea.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        feedbackTextarea.fill("this is automated reason");

        // Target the green submit button specifically
        DashboardManager.log("   -> Clicking Final Submit...");
        Locator finalSubmitBtn = page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Submit"));
        finalSubmitBtn.click();

        // Verify Toast
        if (waitForToast("Feedback submitted successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Feedback submitted successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }

        page.waitForTimeout(2000);

        // Print Feedback Details
        DashboardManager.log("   🔍 Capturing Submitted Feedback...");
        Locator feedbackCard = page.locator("h5").filter(new Locator.FilterOptions().setHasText("Feedback")).locator("..");
        if (feedbackCard.isVisible()) {
            DashboardManager.log("\n      [Admin Feedback Details]");
            DashboardManager.log("      " + feedbackCard.innerText().replace("\n", " | "));
            DashboardManager.log("      -------------------------\n");
        } else {
            DashboardManager.log("      ❌ Feedback details card not visible.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🆕 NEW: VENDOR VERIFIES FINAL ASSIGNMENT STATUS
    // ──────────────────────────────────────────────────────────────

    // ──────────────────────────────────────────────────────────────
    // 🆕 NEW: VENDOR VERIFIES FINAL ASSIGNMENT STATUS
    // ──────────────────────────────────────────────────────────────

    public void vendorVerifyFinalAssignmentStatus(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: VERIFYING FINAL ASSIGNMENT STATUS ---");

        // 1. Login
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // 2. Go to Projects
        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Project: " + reqName);
        Locator projectRow = page.locator("div.flex-row.justify-between").filter(new Locator.FilterOptions().setHasText(reqName));

        // Verify Project Status (Interviewing)
        Locator interviewingBadge = projectRow.locator("span.text-project-interviewing");
        if (interviewingBadge.isVisible()) {
            DashboardManager.log("      ✅ Project Status: Interviewing");
        } else {
            DashboardManager.log("      ❌ Project Status mismatch (Expected: Interviewing)");
        }

        projectRow.locator("h3").click();
        page.waitForTimeout(2000);

        // 3. Verify Candidate Status & Open (Status should now be Assignment Completed)
        DashboardManager.log("   -> Opening Candidate 1...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));

        Locator completedBadge = candidateRow.locator("span.status-green-text").filter(new Locator.FilterOptions().setHasText("Assignment Completed"));
        if (completedBadge.isVisible()) {
            DashboardManager.log("      ✅ Candidate Status: Assignment Completed");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch. Found: " + candidateRow.innerText().replace("\n", " "));
        }
        candidateRow.click();
        page.waitForTimeout(2000);

        // 4. Go to Assignment Tab (Targeting the specific tab container to avoid clicking the progress bar)
        DashboardManager.log("   -> Switching to Assignment Tab...");
        Locator assignmentTab = page.locator("div.flex.items-center.gap-8 div.py-3").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Assignment$")));
        assignmentTab.click();
        page.waitForTimeout(2000);

        // 5. Print Details and Verify Final Status
        DashboardManager.log("   🔍 Verifying Final Assignment Details...");
        Locator assignmentDataRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("SSW"));

        if (assignmentDataRow.isVisible()) {
            // Check for the green "Assignment Completed" badge
            Locator finalStatusBadge = assignmentDataRow.locator("span.status-green-text").filter(new Locator.FilterOptions().setHasText("Assignment Completed"));

            if (finalStatusBadge.isVisible()) {
                DashboardManager.log("      ✅ Assignment Completed | Details: " + assignmentDataRow.innerText().replace("\n", " | "));
            } else {
                DashboardManager.log("      ❌ Status Mismatch | Details found: " + assignmentDataRow.innerText().replace("\n", " | "));
            }
        } else {
            DashboardManager.log("      ❌ Assignment row for 'SSW' not found.");
        }
    }
}