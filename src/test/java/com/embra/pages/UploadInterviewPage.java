package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class UploadInterviewPage {

    private final Page page;

    public UploadInterviewPage(Page page) {
        this.page = page;
    }

    // ──────────────────────────────────────────────────────────────
    // 1. VENDOR SELECTS INTERVIEW TIME
    // ──────────────────────────────────────────────────────────────
    public void vendorSelectInterviewTime(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: SELECTING INTERVIEW TIME ---");

        // Login
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // Go to Projects
        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Project: " + reqName);
        Locator projectRow = page.locator("div.flex-row.justify-between").filter(new Locator.FilterOptions().setHasText(reqName));

        if (projectRow.locator("span.text-project-interviewing").isVisible()) {
            DashboardManager.log("      ✅ Project Status: Interviewing");
        } else {
            DashboardManager.log("      ❌ Project Status mismatch (Expected: Interviewing)");
        }
        projectRow.locator("h3").click();
        page.waitForTimeout(2000);

        // Verify Candidate Status & Open
        DashboardManager.log("   -> Opening Candidate 1...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));

        // Print Candidate Listing Status (FIXED STRICT MODE VIOLATION HERE)
        if (candidateRow.locator("span").filter(new Locator.FilterOptions().setHasText("Interviewing")).first().isVisible() ||
                candidateRow.locator("span").filter(new Locator.FilterOptions().setHasText("Assessment Ongoing")).first().isVisible()) {
            DashboardManager.log("      ✅ Candidate Status in listing verified.");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch in listing.");
        }
        candidateRow.click();
        page.waitForTimeout(2000);

        // Print Interview details from the list (Expected: Interview Initiated)
        Locator interviewRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Interview$")));
        if (interviewRow.isVisible()) {
            DashboardManager.log("      [Interview Listing Details]: " + interviewRow.innerText().replace("\n", " | "));
            if (interviewRow.locator("span.status-blue-text").filter(new Locator.FilterOptions().setHasText("Interview Initiated")).isVisible()) {
                DashboardManager.log("      ✅ Status Verified: Interview Initiated");
            }
        }

        // Click Select Time
        DashboardManager.log("   -> Clicking 'Select Time'...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Select Time")).first().click();
        page.waitForTimeout(1000);

        // Select available date
        DashboardManager.log("   -> Selecting available date...");
        Locator availableDate = page.locator("div.w-11.h-11:not(.opacity-40):not(.cursor-not-allowed)").first();
        try {
            availableDate.click();
            page.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Could not click date. Trying fallback...");
            page.locator("div").filter(new Locator.FilterOptions().setHasText("16")).last().click();
        }

        // Select timeslots
        DashboardManager.log("   -> Selecting time slots...");
        Locator timeSlotBtns = page.locator("button.py-2.px-4.border");
        timeSlotBtns.nth(0).click();
        timeSlotBtns.nth(1).click();

        // Submit
        DashboardManager.log("   -> Submitting Time Slots...");
        page.waitForTimeout(2000); // Wait 2s before clicking submit as requested
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Time Slots")).click();

        if (waitForToast("Interview availability dates sent successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Interview availability dates sent successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }

        // Print Interview Details Card
        page.waitForTimeout(2000);
        Locator detailsCard = page.locator("div.bg-white").filter(new Locator.FilterOptions().setHasText("Interview Details"));
        if (detailsCard.isVisible()) {
            DashboardManager.log("\n      [Vendor Submitted Interview Details]");
            DashboardManager.log("      " + detailsCard.innerText().replace("\n", " | "));
            DashboardManager.log("      ------------------------------------------\n");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. ADMIN SCHEDULES INTERVIEW & SUBMITS FEEDBACK
    // ──────────────────────────────────────────────────────────────
    public void adminScheduleAndFeedbackInterview(String reqName, String candidateName) {
        DashboardManager.log("\n--- 👮 ADMIN: SCHEDULE INTERVIEW & SUBMIT FEEDBACK ---");

        page.locator("a[href='/hiring-requests']").click();
        page.waitForLoadState();

        // Open Requirement
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        if (reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        }
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        // Open Candidate
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));
        if (candidateRow.locator("div.text-white").filter(new Locator.FilterOptions().setHasText("Interviewing")).isVisible()) {
            DashboardManager.log("      ✅ Candidate Listing Status: Interviewing");
        }
        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        // Go to Interview Tab
        DashboardManager.log("   -> Switching to Interview Tab...");
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Interview")).click();
        page.waitForTimeout(1000);

        page.reload();

        // Click Schedule Interview CTA
        DashboardManager.log("   -> Filling Interview Scheduling Form...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Schedule Interview")).first().click();

        // Fill Form
        page.locator("input[name='link']").fill("https://meet.google.com/abc-xyz-def");

        page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select a time slot")).click();
        page.waitForTimeout(500);
        page.getByRole(AriaRole.OPTION).first().click(); // Select first available slot sent by vendor

        page.locator("textarea[name='description']").fill("this is automated description");

        DashboardManager.log("   -> Clicking Schedule Interview CTA...");
        page.waitForTimeout(1000); // Wait 1 sec before click as requested
        page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Schedule Interview")).click();

        if (waitForToast("Interview scheduled successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Interview scheduled successfully!");
        } else {
            DashboardManager.log("      ❌ Success Toast NOT found.");
        }
        page.waitForTimeout(2000);

        // Submit Feedback
        DashboardManager.log("   -> Clicking 'Submit Feedback'...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Feedback")).first().click();

        DashboardManager.log("   -> Filling Feedback...");
        page.locator("input[name='score']").fill("79");
        page.locator("textarea[name='feedback']").fill("this is automated Description");

        DashboardManager.log("   -> Submitting Feedback...");
        page.waitForTimeout(2000); // Wait 2s before clicking submit as requested
        page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Submit")).click();

        // Print Feedback Details
        page.waitForTimeout(2000);
        DashboardManager.log("   🔍 Capturing Submitted Feedback...");
        Locator feedbackCard = page.locator("h5").filter(new Locator.FilterOptions().setHasText("Feedback")).locator("..");
        if (feedbackCard.isVisible()) {
            DashboardManager.log("\n      [Admin Feedback Details]");
            DashboardManager.log("      " + feedbackCard.innerText().replace("\n", " | "));
            DashboardManager.log("      -------------------------\n");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. VENDOR VERIFIES FINAL INTERVIEW STATUS
    // ──────────────────────────────────────────────────────────────
    public void vendorVerifyFinalInterviewStatus(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: VERIFYING FINAL INTERVIEW STATUS ---");

        // Login
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // Go to Projects
        page.locator("a[href='/projects']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Searching for Project: " + reqName);
        Locator projectRow = page.locator("div.flex-row.justify-between").filter(new Locator.FilterOptions().setHasText(reqName));
        projectRow.locator("h3").click();
        page.waitForTimeout(2000);

        // Verify Candidate Status & Open
        DashboardManager.log("   -> Opening Candidate 1...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));

        Locator completedBadge = candidateRow.locator("span.text-project-interviewing").filter(new Locator.FilterOptions().setHasText("Interview Completed"));
        if (completedBadge.isVisible()) {
            DashboardManager.log("      ✅ Candidate Status in Listing: Interview Completed");
        } else {
            DashboardManager.log("      ❌ Candidate Status mismatch. Found: " + candidateRow.innerText().replace("\n", " "));
        }
        candidateRow.click();
        page.waitForTimeout(2000);

        // Go to Interview Tab
        DashboardManager.log("   -> Switching to Interview Tab...");
        page.locator("div.flex.items-center.gap-8 div.py-3").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Interview$"))).click();
        page.waitForTimeout(2000);

        // Print Details and Verify Final Status
        DashboardManager.log("   🔍 Verifying Final Interview Details...");
        Locator interviewDataRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Interview"));

        if (interviewDataRow.isVisible()) {
            Locator finalStatusBadge = interviewDataRow.locator("span.status-green-text").filter(new Locator.FilterOptions().setHasText("Interview Completed"));

            if (finalStatusBadge.isVisible()) {
                DashboardManager.log("      ✅ Interview Completed | Details: " + interviewDataRow.innerText().replace("\n", " | "));
            } else {
                DashboardManager.log("      ❌ Status Mismatch | Details found: " + interviewDataRow.innerText().replace("\n", " | "));
            }
        } else {
            DashboardManager.log("      ❌ Interview row not found.");
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