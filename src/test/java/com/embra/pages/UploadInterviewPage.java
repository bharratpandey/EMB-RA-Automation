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

        // 🚀 FIX: Strip Admin prefix if it exists (removes everything before "ReqTest")
        String cleanName = reqName.contains("ReqTest-")
                ? reqName.substring(reqName.indexOf("ReqTest-"))
                : reqName;

        DashboardManager.log("   -> Searching for Project (Cleaned): " + cleanName);

        // Locate the project title <h3> with the name
        Locator projectTitle = page.locator("h3").filter(new Locator.FilterOptions().setHasText(cleanName));

        if (projectTitle.count() == 0) {
            DashboardManager.log("      ❌ Project '" + cleanName + "' not found on Projects page!");
        } else {
            // Ensure visibility before parent selection
            projectTitle.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));

            // Go up to the full project card <a>
            Locator card = projectTitle.first().locator("//ancestor::a[1]");

            // Find the status badge inside the card
            Locator statusBadge = card.locator("span.text-project-interviewing");

            if (statusBadge.isVisible() && "Interviewing".equals(statusBadge.innerText().trim())) {
                DashboardManager.log("      ✅ Project Status: Interviewing");
            } else {
                DashboardManager.log("      ❌ Project Status mismatch (Expected: Interviewing)");
            }

            // Click the title to open project details
            projectTitle.first().click();
            page.waitForTimeout(2000);
        }

        // Verify Candidate Status & Open
        DashboardManager.log("   -> Opening Candidate 1...");
        Locator candidateNameCell = page.locator("td").filter(new Locator.FilterOptions().setHasText("Candidate 1"));
        Locator candidateRow = candidateNameCell.locator("//ancestor::tr[1]");

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

        DashboardManager.log("   -> Selecting available date...");
        Locator availableDate = page.locator("div.w-11.h-11:not(.opacity-40):not(.cursor-not-allowed)").first();
        try {
            availableDate.click();
            page.waitForTimeout(1000);
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Could not click date. Trying fallback...");
            page.locator("div").filter(new Locator.FilterOptions().setHasText("16")).last().click();
        }

        DashboardManager.log("   -> Selecting time slots...");
        Locator timeSlotBtns = page.locator("button.py-2.px-4.border");
        timeSlotBtns.nth(0).click();
        timeSlotBtns.nth(1).click();

        DashboardManager.log("   -> Submitting Time Slots...");
        page.waitForTimeout(2000);
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

        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(candidateName));
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

        // 🚀 CHANGED: Click the new 'Select' CTA button before filling feedback
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

        // 🚀 FIX: Strip Admin prefix here as well
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
        Locator candidateRow = candidateNameCell.locator("//ancestor::tr[1]");

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