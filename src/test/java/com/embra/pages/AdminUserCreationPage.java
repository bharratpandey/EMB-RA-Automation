package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class AdminUserCreationPage {
    private final Page page;

    // Sidebar & Navigation
    private final Locator usersTab;
    private final Locator createUserCta;

    // Form Fields
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator emailInput;
    private final Locator roleDropDown;
    private final Locator submitCreateUserBtn;

    // Set Password Fields
    private final Locator passwordInput;
    private final Locator confirmPasswordInput;
    private final Locator setPasswordBtn;

    public AdminUserCreationPage(Page page) {
        this.page = page;

        // Navigation
        this.usersTab = page.locator("a[href='/users']");
        this.createUserCta = page.locator("button:has-text('Create User')");

        // Form
        this.firstNameInput = page.locator("input[name='first_name']");
        this.lastNameInput = page.locator("input[name='last_name']");
        this.emailInput = page.locator("input[name='email']");
        this.roleDropDown = page.locator("button[role='combobox']");
        this.submitCreateUserBtn = page.locator("button[type='submit']:has-text('Create User')");

        // Set Password
        this.passwordInput = page.locator("input[id='password']");
        this.confirmPasswordInput = page.locator("input[id='confirmPassword']");
        this.setPasswordBtn = page.locator("button[type='submit']:has-text('Set Password')");
    }

    public void navigateToUsers() {
        usersTab.click();
    }

    public void openCreateUserForm() {
        createUserCta.click();
    }

    public void fillUserDetails(String fName, String lName, String email) {
        firstNameInput.fill(fName);
        lastNameInput.fill(lName);
        emailInput.fill(email);
    }

    // Inside AdminUserCreationPage.java

    public void selectRoleAdmin() {
        // Scope the locator to the dialog/form to avoid selecting the table's pagination dropdown
        Locator dialog = page.locator("div[role='dialog']");

        // Click the combobox specifically within the dialog
        dialog.locator("button[role='combobox']").click();

        // Select the 'Admin' option from the dropdown menu that appears
        // Radix UI often renders the list outside the dialog, so we look for the role 'option'
        page.locator("div[role='option']").filter(new Locator.FilterOptions().setHasText("Admin")).click();
    }

    public void submitForm() {
        submitCreateUserBtn.click();
    }

    public void setNewPassword(String password) {
        passwordInput.fill(password);
        confirmPasswordInput.fill(password);
        setPasswordBtn.click();
    }
}