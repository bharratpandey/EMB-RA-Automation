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

        // Strip Admin prefix if it exists
        String cleanName = reqName.contains("ReqTest-")
                ? reqName.substring(reqName.indexOf("ReqTest-"))
                : reqName;

        DashboardManager.log("   -> Searching for Project (Cleaned): " + cleanName);

// Search for the project using the search field
        Locator searchField = page.locator("input[placeholder='Find a project by: Name, Current status']");
        searchField.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        searchField.fill(cleanName);
        page.waitForTimeout(2000);

// Find the project card by cleanName in h3
        Locator projectCard = page.locator("a").filter(new Locator.FilterOptions()
                .setHas(page.locator("h3").filter(new Locator.FilterOptions().setHasText(cleanName)))).first();

        if (projectCard.count() == 0) {
            DashboardManager.log("      ❌ Project '" + cleanName + "' not found on Projects page!");
        } else {
            // Check status badge inside card
            Locator statusBadge = projectCard.locator("span.text-project-interviewing");
            if (statusBadge.isVisible() && "Interviewing".equals(statusBadge.innerText().trim())) {
                DashboardManager.log("      ✅ Project Status: Interviewing");
            } else {
                DashboardManager.log("      ❌ Project Status mismatch (Expected: Interviewing)");
            }

            // Click the card to open project details
            projectCard.click();
            page.waitForTimeout(2000);
        }

        // Verify Candidate Status & Open
        DashboardManager.log("   -> Opening Candidate 1...");

        Locator candidateNameCell = page.locator("td").filter(new Locator.FilterOptions().setHasText("Candidate 1"));
        Locator candidateRow = candidateNameCell.locator("//ancestor::tr[1]").first();

        if (candidateRow.count() == 0) {
            DashboardManager.log("      ❌ Candidate row not found!");
        } else {
            Locator statusBadge = candidateRow.locator("span.text-project-interviewing");
            String actualStatus = statusBadge.isVisible() ? statusBadge.innerText().trim() : "NOT FOUND";
            DashboardManager.log("      Detected Candidate Status: " + actualStatus);

            if ("Interviewing".equals(actualStatus)) {
                DashboardManager.log("      ✅ Candidate Status in listing: Interviewing");
            } else {
                DashboardManager.log("      ❌ Candidate Status mismatch (Expected: Interviewing)");
            }

            candidateRow.click();
            page.waitForTimeout(2000);
        }

        Locator interviewRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Interview$")));
        if (interviewRow.isVisible()) {
            DashboardManager.log("      [Interview Listing Details]: " + interviewRow.innerText().replace("\n", " | "));
            if (interviewRow.locator("span.status-blue-text").filter(new Locator.FilterOptions().setHasText("Interview Initiated")).isVisible()) {
                DashboardManager.log("      ✅ Status Verified: Interview Initiated");
            }
        }

        DashboardManager.log("   -> Clicking 'Select Time'...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Select Time")).first().click();
        page.waitForTimeout(1000);

        // 🚀 FIX: Updated to match the custom calendar HTML exactly
        DashboardManager.log("   -> Selecting available date...");

        // Find a div.w-11.h-11 that contains a number but DOES NOT have cursor-not-allowed
        Locator availableDate = page.locator("div.w-11.h-11:not(.cursor-not-allowed)")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\d+$")))
                .first();
        try {
            availableDate.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            availableDate.click();
            page.waitForTimeout(1500); // Wait for slots to load below
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Could not click date on custom calendar. Trying fallback...");
            page.locator("div.w-11.h-11").filter(new Locator.FilterOptions().setHasText("16")).last().click();
            page.waitForTimeout(1500);
        }

        // 🚀 FIX: Highly resilient time slot selector
        DashboardManager.log("   -> Selecting time slots...");

        // Strategy 1: Checkbox + Label format
        Locator checkboxTimeSlots = page.locator("button[role='checkbox']");

        // Strategy 2: Standard button format
        Locator buttonTimeSlots = page.locator("button.py-2.px-4.border");

        try {
            if (checkboxTimeSlots.count() >= 2) {
                Locator firstLabel = checkboxTimeSlots.nth(0).locator("..").locator("label");
                Locator secondLabel = checkboxTimeSlots.nth(1).locator("..").locator("label");

                firstLabel.click();
                DashboardManager.log("      ✅ Selected Slot 1: " + firstLabel.innerText());

                secondLabel.click();
                DashboardManager.log("      ✅ Selected Slot 2: " + secondLabel.innerText());
            } else if (buttonTimeSlots.count() >= 2) {
                buttonTimeSlots.nth(0).click();
                DashboardManager.log("      ✅ Selected Slot 1 (Button format)");

                buttonTimeSlots.nth(1).click();
                DashboardManager.log("      ✅ Selected Slot 2 (Button format)");
            } else {
                DashboardManager.log("      ❌ Not enough time slots available on this date.");
            }
        } catch (Exception e) {
            DashboardManager.log("      ❌ Time slots did not appear or could not be clicked.");
        }

        DashboardManager.log("   -> Submitting Time Slots...");
        page.waitForTimeout(1000);
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Time Slots")).click();

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

        // Admin keeps the prefix logic as it's the source
        Locator reqRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(reqName));
        if (reqRow.locator("div.bg-primary").filter(new Locator.FilterOptions().setHasText("Active")).isVisible()) {
            DashboardManager.log("      ✅ Requirement Status: Active");
        }
        page.getByText(reqName).first().click();
        page.waitForTimeout(2000);

        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates")).click();
        page.waitForTimeout(1000);

        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName)).first();
        if (candidateRow.locator("div.text-white").filter(new Locator.FilterOptions().setHasText("Interviewing")).isVisible()) {
            DashboardManager.log("      ✅ Candidate Listing Status: Interviewing");
        }
        candidateRow.locator("button[title='View Details']").click();
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Switching to Interview Tab...");
        page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Interview")).click();
        page.waitForTimeout(1000);

        page.reload();

        DashboardManager.log("   -> Filling Interview Scheduling Form...");
        page.locator("button").filter(new Locator.FilterOptions().setHasText("Schedule Interview")).first().click();

        page.locator("input[name='link']").fill("https://meet.google.com/abc-xyz-def");
        page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select a time slot")).click();
        page.waitForTimeout(500);
        page.getByRole(AriaRole.OPTION).first().click();

        page.locator("textarea[name='description']").fill("this is automated description");

        DashboardManager.log("   -> Clicking Schedule Interview CTA...");
        page.waitForTimeout(1000);
        page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Schedule Interview")).click();

        if (waitForToast("Interview scheduled successfully!")) {
            DashboardManager.log("      ✅ Toast Verified: Interview scheduled successfully!");
        }
        page.waitForTimeout(2000);

        DashboardManager.log("   -> Clicking 'Select' CTA...");
        Locator selectCtaBtn = page.locator("button.text-green-700.border-green-400").filter(new Locator.FilterOptions().setHasText("Select")).first();
        selectCtaBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        selectCtaBtn.click();
        page.waitForTimeout(1000);

        DashboardManager.log("   -> Filling Feedback...");
        page.locator("input[name='score']").fill("79");
        page.locator("textarea[name='feedback']").fill("this is automated Description");

        DashboardManager.log("   -> Submitting Feedback...");
        page.waitForTimeout(2000);
        page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Submit")).click();

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

        String cleanName = reqName.contains("ReqTest-")
                ? reqName.substring(reqName.indexOf("ReqTest-"))
                : reqName;

        DashboardManager.log("   -> Searching for Project (Cleaned): " + cleanName);
        Locator projectRow = page.locator("div.flex-row.justify-between").filter(new Locator.FilterOptions().setHasText(cleanName));

        if (projectRow.count() > 0) {
            projectRow.locator("h3").first().click();
            page.waitForTimeout(2000);
        } else {
            DashboardManager.log("      ❌ Project Row not found for: " + cleanName);
        }

        DashboardManager.log("   -> Opening Candidate 1...");

        Locator candidateNameCell = page.locator("td").filter(new Locator.FilterOptions().setHasText("Candidate 1"));
        Locator candidateRow = candidateNameCell.locator("//ancestor::tr[1]").first();

        if (candidateRow.count() == 0) {
            DashboardManager.log("      ❌ Candidate row with 'Candidate 1' not found!");
        } else {
            String rowText = candidateRow.innerText().replaceAll("\\s+", " ").replace("\n", " | ");
            DashboardManager.log("      Candidate Row Details: " + rowText);

            Locator statusBadge = candidateRow.locator("span[class*='status-'][class*='-text']");
            if (statusBadge.isVisible()) {
                String actualStatus = statusBadge.innerText().trim();
                DashboardManager.log("      Detected Status: " + actualStatus);
                if ("Interview Completed".equals(actualStatus)) {
                    DashboardManager.log("      ✅ Candidate Status in Listing: Interview Completed");
                } else {
                    DashboardManager.log("      ❌ Status mismatch - Expected 'Interview Completed'");
                }
            }
            candidateRow.click();
            page.waitForTimeout(2000);
        }

        DashboardManager.log("   -> Switching to Interview Tab...");
        page.locator("div.flex.items-center.gap-8 div.py-3").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Interview$"))).click();
        page.waitForTimeout(2000);

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