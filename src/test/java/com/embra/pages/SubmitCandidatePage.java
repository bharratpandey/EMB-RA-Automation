package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class SubmitCandidatePage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // MASTER DATA: ROLES LIST
    // ──────────────────────────────────────────────────────────────
    private static final List<String> ROLES = Arrays.asList(
            "Frontend Engineer", "Backend Engineer", "Full Stack Engineer", "AI Engineer", "ML Engineer",
            "Mobile App Developer", "DevOps Engineer", "Data Scientist", "Solution Architect",
            "Security Engineer", "Database Engineer", "QA Engineer", "UI/UX Designer", "Graphic Designer",
            "Business Analyst", "Product Manager", "Program Manager", "Technical Project Manager",
            "Scrum Master", "ERP Consultant", "Data Engineer"
    );

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginBtn;
    private final Locator projectTab;
    private final Locator acceptBtn;
    private final Locator endProjectBtn;
    private final Locator addNewMemberBtn;
    private final Locator jdUploadInput;
    private final Locator importResumeBtn;
    private final Locator nameInput;
    private final Locator memberEmailInput;
    private final Locator linkedinInput;
    private final Locator portfolioInput;
    private final Locator experienceInput;
    private final Locator roleDropdown;
    private final Locator noticePeriodDropdown;
    private final Locator locationDropdown;
    private final Locator resourceTypeDropdown;
    private final Locator currentCtcInput;
    private final Locator expectedCtcInput;
    private final Locator agencyCostInput;
    private final Locator hourlyCostInput;

    // Awards & Certs
    private final Locator addAwardsBtn;
    private final Locator awardNameInput;
    private final Locator awardDescInput;
    private final Locator saveAwardBtn;

    private final Locator addCertBtn;
    private final Locator certNameInput;
    private final Locator certIdInput;
    private final Locator certDateInput;
    private final Locator certDescInput;
    private final Locator certUploadInput;
    private final Locator saveCertBtn;

    // Education Locators (NEW)
    private final Locator addEducationBtn;
    private final Locator educationDropdown;
    private final Locator degreeInput;
    private final Locator fieldOfStudyInput;
    private final Locator institutionInput;
    private final Locator passingDateInput;
    private final Locator gradeInput;
    private final Locator saveEducationBtn;

    // Final Buttons
    private final Locator availableBtn;
    private final Locator saveMemberDetailsBtn;
    private final Locator submitCandidatesBtn;

    public SubmitCandidatePage(Page page) {
        this.page = page;

        // Login
        this.emailInput = page.locator("input[name='email']");
        this.passwordInput = page.locator("input[name='password']");
        this.loginBtn = page.locator("button[type='submit']").filter(new Locator.FilterOptions().setHasText("Login"));

        // Dashboard
        this.projectTab = page.locator("a[href='/projects']");
        this.acceptBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Accept"));
        this.endProjectBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("End Project"));

        // Member Form
        this.addNewMemberBtn = page.locator("button:has-text('Add New Member')");
        this.jdUploadInput = page.locator("input[type='file']").first();
        this.importResumeBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Import from resume"));

        this.nameInput = page.locator("input[name='name']");
        this.memberEmailInput = page.locator("input[name='email']").last();
        this.linkedinInput = page.locator("input[name='linkedin']");
        this.portfolioInput = page.locator("input[name='interviewLink']");
        this.experienceInput = page.locator("input[name='experience']");

        // Dropdowns
        this.roleDropdown = page.locator("button[role='combobox']").nth(0);
        this.resourceTypeDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText(Pattern.compile("Full-time|Contractual|Both|Select")));
        this.noticePeriodDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select notice period"));
        this.locationDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select location"));

        // Financials
        this.currentCtcInput = page.locator("input[name='currentCtc']");
        this.expectedCtcInput = page.locator("input[name='expectedCtc']");
        this.agencyCostInput = page.locator("input[name='agencyCost']");
        this.hourlyCostInput = page.locator("input[name='hourly_cost_estimate']");

        // Awards
        this.addAwardsBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add Awards"));
        this.awardNameInput = page.locator("input[name='nameOfAward']");
        this.awardDescInput = page.locator("textarea[name='description']").first();
        this.saveAwardBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Award"));

        // Certs
        this.addCertBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add Certificates"));
        this.certNameInput = page.locator("input[name='nameOfCertificate']");
        this.certIdInput = page.locator("input[name='certificateId']");
        this.certDateInput = page.locator("input[name='issued_date']");
        this.certDescInput = page.locator("textarea[name='description']").last();
        this.certUploadInput = page.locator("input[type='file']").last();
        this.saveCertBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Certificate"));

        // Education (NEW)
        this.addEducationBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add Education"));
        this.educationDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select highest qualification"));

        // Mapped from your HTML
        this.degreeInput = page.locator("input[name='degree']");
        this.fieldOfStudyInput = page.locator("input[name='domain']");
        this.institutionInput = page.locator("input[name='institution']");
        this.passingDateInput = page.locator("input[name='passingYear']");
        this.gradeInput = page.locator("input[name='grade']");
        this.saveEducationBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Education"));

        // Final Buttons
        this.availableBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Available$")));
        this.saveMemberDetailsBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Member Details"));
        this.submitCandidatesBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Submit Candidates for Interview"));
    }

    // ──────────────────────────────────────────────────────────────
    // 1. HELPERS
    // ──────────────────────────────────────────────────────────────

    private void selectRole(String roleName) {
        DashboardManager.log("      🔹 Selecting Role: [" + roleName + "]");
        try {
            roleDropdown.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            roleDropdown.click();

            int index = ROLES.indexOf(roleName);
            if (index == -1) index = 0;

            for (int i = 0; i < index + 1; i++) {
                page.keyboard().press("ArrowDown");
                try { Thread.sleep(50); } catch (Exception ignored) {}
            }
            page.keyboard().press("Enter");
            DashboardManager.log("      ✅ Role Selected Successfully.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to select Role: " + e.getMessage());
        }
    }

    private void addSkills(List<String> skills) {
        DashboardManager.log("      🛠 Adding Skills: " + skills);
        try {
            Locator addSkillBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add Skills")).first();
            addSkillBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            addSkillBtn.scrollIntoViewIfNeeded();
            addSkillBtn.click();

            Locator searchInput = page.locator("input[name='search']");
            searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

            for (String skill : skills) {
                DashboardManager.log("         -> Searching for: " + skill);
                searchInput.clear();
                searchInput.fill(skill);
                page.waitForTimeout(1000);

                Locator exactText = page.getByText(skill, new Page.GetByTextOptions().setExact(true));
                Locator row = page.locator("div.flex.w-full.flex-row")
                        .filter(new Locator.FilterOptions().setHas(exactText))
                        .filter(new Locator.FilterOptions().setHas(page.locator("button[role='checkbox']")));

                if (row.count() > 0) {
                    Locator checkbox = row.first().locator("button[role='checkbox']");
                    if (!"checked".equals(checkbox.getAttribute("data-state"))) {
                        checkbox.click();
                        DashboardManager.log("            ✅ Selected: " + skill);
                    }
                } else {
                    page.locator("button[role='checkbox']").first().click();
                }
                page.waitForTimeout(300);
            }

            Locator saveBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Selection"));
            saveBtn.scrollIntoViewIfNeeded();
            saveBtn.click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(1000);
            DashboardManager.log("      ✅ Skills Saved Successfully.");

        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to add Skills: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // ⚡️ NEW: DYNAMIC EDUCATION ADDITION ⚡️
    // ──────────────────────────────────────────────────────────────
    private void addEducation(String qualification, String degree, String fieldOfStudy, String institution, String dateYyyyMm, String grade) {
        DashboardManager.log("      🎓 Adding Education: [" + qualification + "]...");
        try {
            addEducationBtn.scrollIntoViewIfNeeded();
            addEducationBtn.click();

            // Wait for dropdown
            educationDropdown.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            educationDropdown.click();

            // Select Qualification (PhD, Post-Graduation, Graduation, 12th, 10th)
            page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText(qualification)).first().click();
            page.waitForTimeout(2000);

            // LOGIC: Degree Name is NOT present for 10th or 12th
            boolean hasDegreeField = !qualification.equalsIgnoreCase("10th") && !qualification.equalsIgnoreCase("12th");

            if (hasDegreeField) {
                degreeInput.fill(degree);
            } else {
                DashboardManager.log("         (Skipping Degree Name for " + qualification + ")");
            }

            // Fill Fields present in all types (as per your HTML)
            fieldOfStudyInput.fill(fieldOfStudy);
            institutionInput.fill(institution); // Works for "University/College" OR "School"
            passingDateInput.fill(dateYyyyMm);  // Expects YYYY-MM for type="month"
            gradeInput.fill(grade);

            saveEducationBtn.click();

            page.waitForTimeout(1500);
            DashboardManager.log("      ✅ Education Details Saved.");

        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to add Education: " + e.getMessage());
        }
    }

    private void addServiceableLocations() {
        DashboardManager.log("      🌍 Selecting Serviceable Locations...");
        try {
            page.locator("button:has-text('All')").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            page.locator("button:has-text('All')").click();
            page.waitForTimeout(1000);

            Locator addLocBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add Locations")).first();
            addLocBtn.scrollIntoViewIfNeeded();
            addLocBtn.click();

            Locator searchInput = page.locator("input[placeholder='Try entering a city or state']");
            searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

            searchInput.fill("New Delhi");
            page.waitForTimeout(1500);
            page.locator("div.flex.items-center").filter(new Locator.FilterOptions().setHasText("New Delhi"))
                    .locator("button[role='checkbox']").click();

            searchInput.fill("");
            searchInput.fill("United States");
            page.waitForTimeout(1500);
            page.locator("div.flex.items-center").filter(new Locator.FilterOptions().setHasText("United States"))
                    .first().locator("button[role='checkbox']").click();

            Locator saveBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Save Selection"));
            saveBtn.scrollIntoViewIfNeeded();
            saveBtn.click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(2000);
            DashboardManager.log("      ✅ Serviceable Locations Added.");

        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to add Serviceable Locations: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void loginToVendorPortal(String email, String pass) {
        DashboardManager.log("🔑 Logging into Vendor Portal...");
        try {
            emailInput.fill(email);
            passwordInput.fill(pass);
            loginBtn.click();
            page.getByText("Login successful!", new Page.GetByTextOptions().setExact(false))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            DashboardManager.log("✅ Vendor Login Successful.");
        } catch (Exception e) {
            DashboardManager.log("❌ Login Failed / Toast missed.");
        }
    }

    public void navigateToProject(String reqTitle) {
        DashboardManager.log("📂 Navigating to Projects...");
        try {
            projectTab.click();
            page.waitForTimeout(3000);
            page.locator("h3").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            DashboardManager.log("🔎 Searching for Requirement: [" + reqTitle + "]");
            Locator reqLink = page.locator("h3").getByText(reqTitle, new Locator.GetByTextOptions().setExact(true));
            if (reqLink.isVisible()) {
                reqLink.click();
                DashboardManager.log("✅ Found & Clicked: " + reqTitle);
            } else {
                DashboardManager.log("❌ Specific Req not found. Clicking first project fallback.");
                page.locator("div.border-b a").first().click();
            }
        } catch (Exception e) {
            DashboardManager.log("❌ Navigation Failed: " + e.getMessage());
        }
    }

    public void acceptProject() {
        DashboardManager.log("👍 Checking Project Status...");
        try {
            page.locator("h1").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            Locator anyBtn = acceptBtn.or(endProjectBtn);
            anyBtn.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            if (endProjectBtn.isVisible()) {
                DashboardManager.log("ℹ️ Project is ALREADY ACCEPTED. Skipping acceptance.");
                return;
            }
            if (acceptBtn.isVisible()) {
                page.waitForTimeout(1000);
                acceptBtn.click();
                DashboardManager.log("✅ Clicked 'Accept' button.");
                page.waitForTimeout(2000);
            }
        } catch (Exception e) {
            DashboardManager.log("❌ Error checking project status: " + e.getMessage());
        }
    }

    public void addMembers(int count, String jdPath) {
        DashboardManager.log("👥 Starting to add " + count + " members...");
        for (int i = 0; i < count; i++) {
            String candidateName = "Candidate " + (i + 1);
            DashboardManager.log("\n   ➕ Adding Member #" + (i + 1) + " (" + candidateName + ")");
            try {
                Locator addBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Add New Member")).first();
                addBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                addBtn.click();
                fillMemberDetails(jdPath, candidateName);
            } catch (Exception e) {
                DashboardManager.log("❌ Add Member Failed: " + e.getMessage());
            }
        }
    }

    private void fillMemberDetails(String jdPath, String candidateName) {
        DashboardManager.log("      📤 Uploading Resume...");
        try {
            if (!Files.exists(Paths.get(jdPath))) throw new RuntimeException("Resume file not found!");
            jdUploadInput.setInputFiles(Paths.get(jdPath));
            importResumeBtn.click();
            DashboardManager.log("      ⏳ Waiting 15s for extraction...");
            page.waitForTimeout(15000);
            page.locator("div:has-text('Resume details extracted!'), div:has-text('Failed to extract')")
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(80000));
            DashboardManager.log("      ℹ️ Extraction toast detected.");
        } catch (Exception ignored) {
            DashboardManager.log("      ⚠️ Extraction toast missing or timed out.");
        }

        DashboardManager.log("      📝 Filling Basic Info...");
        try {
            nameInput.fill(candidateName);
            memberEmailInput.fill("candidate" + System.currentTimeMillis() + "@yopmail.com");
            linkedinInput.fill("https://in.linkedin.com/company/embglobal");
            portfolioInput.fill("https://github.com/emb-ai");
            experienceInput.fill("2");
            page.waitForTimeout(2000);
            DashboardManager.log("      ✅ Basic Info Filled (" + candidateName + ").");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Basic Info Failed.");
        }

        selectRole("Frontend Engineer");
        page.waitForTimeout(2000);
        addSkills(Arrays.asList("React", "Java", "Python", "javascript", "angular","HTML5","React"));

        // ──────────────────────────────────────────────────────────────
        // ⚡️ DYNAMIC EDUCATION LOGIC ADDED HERE ⚡️
        // ──────────────────────────────────────────────────────────────
        // Example: Graduation
        addEducation("Graduation", "BTech", "Computer Science", "FSIT", "2022-12", "80.02");

        // Example: You can swap this line with 10th to test the logic:
        // addEducation("10th", "", "General", "RPVV", "2020-03", "85.00");

        DashboardManager.log("      🏆 Adding Award...");
        try {
            addAwardsBtn.click();
            awardNameInput.fill("Employee of the quarter");
            page.locator("button:has-text('Select year')").click();
            page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("2025")).click();
            awardDescInput.fill("Automated Award Description");
            saveAwardBtn.click();
            page.waitForTimeout(2000);
            DashboardManager.log("      ✅ Award Added.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to add Award: " + e.getMessage());
        }

        DashboardManager.log("      📜 Adding Certificate...");
        try {
            addCertBtn.click();
            certNameInput.fill("Java Certified");
            certIdInput.fill("JVFS-1329");
            certDateInput.fill("2025-05-12");
            certDescInput.fill("Automated Cert Description");
            certUploadInput.setInputFiles(Paths.get(jdPath));
            saveCertBtn.click();
            saveCertBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
            page.waitForTimeout(2000);
            DashboardManager.log("      ✅ Certificate Added.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to add Certificate: " + e.getMessage());
        }

        DashboardManager.log("      ⚙️ Selecting Engagement Details...");
        try {
            Locator resType = page.locator("button[role='combobox']").nth(1);
            resType.scrollIntoViewIfNeeded();
            resType.click();
            page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("Both")).click();
            page.waitForTimeout(1000);

            currentCtcInput.fill("1200000");
            expectedCtcInput.fill("2000000");
            if (agencyCostInput.isVisible()) agencyCostInput.fill("95000");
            if (hourlyCostInput.isVisible()) hourlyCostInput.fill("594");
            page.waitForTimeout(1000);
            DashboardManager.log("      ✅ Engagement & Financials Filled.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed Engagement/Financials.");
        }

        DashboardManager.log("      ⏳ Selecting Notice Period...");
        try {
            noticePeriodDropdown.scrollIntoViewIfNeeded();
            noticePeriodDropdown.click();
            page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("Available Immediately")).click();
            page.waitForTimeout(1000);
            DashboardManager.log("      ✅ Notice Period Selected.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed Notice Period.");
        }

        DashboardManager.log("      📍 Selecting Location...");
        try {
            locationDropdown.click();
            page.locator("input[placeholder='Search...']").fill("New Delhi");
            page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("New Delhi, Delhi, India")).click();
            page.waitForTimeout(1000);
            DashboardManager.log("      ✅ Location Selected.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed Location.");
        }

        addServiceableLocations();

        DashboardManager.log("      🌐 Selecting Timezone...");
        try {
            page.locator("div[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select Timezones")).click();
            page.locator("input[placeholder='Search...']").fill("India");
            page.locator("div.cursor-pointer").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^India$"))).first().click();
            page.waitForTimeout(1000);
            DashboardManager.log("      ✅ Timezone Selected.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed Timezone.");
        }

        DashboardManager.log("      💾 Saving Member...");
        try {
            availableBtn.scrollIntoViewIfNeeded();
            availableBtn.click();

            saveMemberDetailsBtn.scrollIntoViewIfNeeded();
            if (!saveMemberDetailsBtn.isEnabled()) {
                DashboardManager.log("      ⚠️ Save button is disabled! Form might be incomplete.");
            }
            saveMemberDetailsBtn.click();

            page.getByText("Team member added successfully!", new Page.GetByTextOptions().setExact(false))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000));
            DashboardManager.log("      ✅ Member Added Successfully.");
            page.waitForTimeout(3000);
        } catch (Exception e) {
            DashboardManager.log("      ❌ Failed to Save Member: " + e.getMessage());
        }
    }

    public void submitCandidates() {
        DashboardManager.log("🚀 Submitting Candidates...");
        try {
            submitCandidatesBtn.click();
            page.waitForTimeout(6000);
            DashboardManager.log("✅ Candidates Submitted.");
        } catch (Exception e) {
            DashboardManager.log("❌ Failed to Submit Candidates.");
        }
        page.waitForTimeout(3000);
    }

    public void verifyCandidateStatus() {
        DashboardManager.log("🔍 Verifying Candidate Status for all members...");
        Locator rows = page.locator("tr.group");
        try {
            rows.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            int count = rows.count();
            DashboardManager.log("📊 Found " + count + " allocated resources:");
            for (int i = 0; i < count; i++) {
                Locator row = rows.nth(i);
                String name = row.locator("h3").first().innerText().trim();
                String status = row.locator("span.status-blue-text").innerText().trim();
                DashboardManager.log("   👤 Member " + (i + 1) + ": " + name + " | Status: [" + status + "]");
                if ("Applied".equalsIgnoreCase(status)) DashboardManager.log("      ✅ Verified");
                else DashboardManager.log("      ❌ Unexpected Status");
            }
        } catch (Exception e) {
            DashboardManager.log("❌ Failed to verify status: " + e.getMessage());
        }
    }
}