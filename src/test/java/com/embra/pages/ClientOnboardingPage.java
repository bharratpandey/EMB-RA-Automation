package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ClientOnboardingPage {
    private final Page page;

    // Locators
    private final Locator formHeading;
    private final Locator roleInput;
    private final Locator orgNameInput;
    private final Locator industryDropdown;
    private final Locator searchField;
    private final Locator websiteInput;
    private final Locator countryCodeButton;
    private final Locator phoneNumberInput;
    private final Locator submitButton;
    private final Locator dashboardIndicator;

    public ClientOnboardingPage(Page page) {
        this.page = page;
        this.formHeading = page.locator("h2:has-text('Organization Details')");
        this.roleInput = page.locator("input[name='role']");
        this.orgNameInput = page.locator("input[name='agencyName']");
        this.industryDropdown = page.getByRole(AriaRole.COMBOBOX).filter(new Locator.FilterOptions().setHasText("Select your industry"));
        this.searchField = page.locator("input[placeholder='Search...']");
        this.websiteInput = page.locator("input[name='websiteLink']");
        this.countryCodeButton = page.locator("button:has-text('+91')");
        this.phoneNumberInput = page.locator("input[name='contactNumber']");
        this.submitButton = page.locator("button[type='submit']:has-text('Submit')");
        this.dashboardIndicator = page.locator("span.font-semibold-important").getByText("Dashboard");
    }

    public void fillOnboardingForm(String role, String orgName, String industry, String website, String phone) {
        roleInput.fill(role);
        orgNameInput.fill(orgName);

        // Industry Selection
        industryDropdown.click();
        searchField.fill(industry);
        page.waitForTimeout(1000);
        page.locator("role=option").filter(new Locator.FilterOptions().setHasText(industry)).click();

        websiteInput.fill(website);

        // Phone Number (assuming +91 is default, if not click and search)
        phoneNumberInput.fill(phone);

        submitButton.click();
    }

    public Locator getFormHeading() { return formHeading; }
    public Locator getDashboardIndicator() { return dashboardIndicator; }
}