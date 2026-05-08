# EMB-RA-Automation — Codebase & Architecture Reference

> **Project:** EMB-RA-Automation  
> **Stack:** Java 17 · Playwright 1.49.0 · JUnit 5.10.1 · ExtentReports · Maven  
> **Purpose:** End-to-end test automation for the EMB Talent Recruitment & Allocation platform  
> **Last Updated:** 2026-05-06

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Full Directory Structure](#3-full-directory-structure)
4. [Dependencies](#4-dependencies)
5. [CI/CD — GitHub Actions Workflows](#5-cicd--github-actions-workflows)
6. [Credential & Secret Management](#6-credential--secret-management)
7. [Utility Classes](#7-utility-classes)
8. [Page Object Model — All Pages](#8-page-object-model--all-pages)
9. [Test Classes](#9-test-classes)
10. [End-to-End Flow Map](#10-end-to-end-flow-map)
11. [Locator Strategy](#11-locator-strategy)
12. [Reporting & Observability](#12-reporting--observability)
13. [Test Data & Resources](#13-test-data--resources)
14. [How to Run](#14-how-to-run)
15. [Quick Reference — All Java Files](#15-quick-reference--all-java-files)

---

## 1. Project Overview

EMB-RA-Automation automates the full recruitment lifecycle across **three separate web portals**:

| Portal | UAT URL | DEV URL | Role |
|--------|---------|---------|------|
| **Admin** | `https://uat-admin.embtalent.ai` | `https://dev-admin.embtalent.ai` | Internal EMB team |
| **Client** | `https://uat-client.embtalent.ai` | — | Hiring companies |
| **Vendor** | `https://uat-vendor.embtalent.ai` | `https://dev-vendor.embtalent.ai` | Staffing agencies |

### Recruitment Lifecycle (21-Step E2E)

```
Admin creates Requirement
        ↓
Admin sends to Vendors  (Partner Shortlisting)
        ↓
Vendor accepts & submits Candidates
        ↓
Admin reviews → Hold / Reject / Send to Client
        ↓
Client shortlists / rejects Candidates
        ↓
Admin schedules Assessment  →  Vendor selects slots  →  Admin uploads result
        ↓
Admin schedules Assignment  →  Vendor submits solution  →  Admin gives feedback
        ↓
Admin schedules Interview   →  Vendor selects slots  →  Admin schedules & gives feedback
        ↓
Admin offers Job  →  Deploys Candidate
        ↓
Requirement marked Completed
```

---

## 2. Architecture

### Pattern: Page Object Model (POM)

```
┌──────────────────────────────────────────────────────────┐
│                      Test Classes                        │
│  (JUnit 5 lifecycle: @BeforeAll → @Test → @AfterAll)     │
└────────────────────────┬─────────────────────────────────┘
                         │ uses
┌────────────────────────▼─────────────────────────────────┐
│                   Page Object Classes                    │
│  (Encapsulate locators + UI interactions per page/flow)  │
└────────────────────────┬─────────────────────────────────┘
                         │ uses
┌────────────────────────▼─────────────────────────────────┐
│               Playwright Java (Browser API)              │
│  Page · Locator · BrowserContext · Tracing · Response    │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                    Utility Layer                         │
│  DashboardManager  →  ExtentReports HTML + Supabase      │
│  DashboardClient   →  Async Supabase PATCH               │
│  EmailSender       →  Gmail SMTP HTML report             │
│  MailService       →  mail.tm OTP polling                │
│  GoogleSessionGenerator → One-time Google auth capture   │
└──────────────────────────────────────────────────────────┘
```

### Multi-Context Testing

Each cross-portal test creates **separate `BrowserContext` instances** — one per role — so session cookies never bleed between portals:

```java
BrowserContext adminContext  = browser.newContext(...);  // Admin session
BrowserContext vendorContext = browser.newContext(...);  // Vendor session
BrowserContext clientContext = browser.newContext(...);  // Client session
```

Each context gets its own Playwright **trace** (screenshots + DOM snapshots + network) saved to `target/*.zip`.

---

## 3. Full Directory Structure

```
EMB-RA-Automation/
│
├── pom.xml                              # Maven build & dependencies
├── CODEBASE.md                          # This file
├── .env.example                         # Credential template — safe to commit
├── .gitignore                           # Excludes .env, target/, IDE files
│
├── .github/
│   └── workflows/
│       ├── daily-run.yml                # Nightly full E2E run (cron)
│       ├── prod-sanity.yml              # Prod/UAT sanity check (cron + manual)
│       └── remote_trigger.yml           # On-demand single test via dispatch
│
├── auth/
│   └── google_state.json                # Persisted Google OAuth session (browser storageState)
│
├── screenshots/                         # Auto-captured on test failures
│
├── traces/                              # Committed Playwright trace zips (one per test run)
│   ├── testProdSanity()-trace.zip
│   ├── Admin_Schedule_Assignment_Flow_*.zip
│   └── ...
│
└── src/
    └── test/
        ├── java/com/embra/
        │   │
        │   ├── pages/                   # 22 Page Object Model classes
        │   │   ├── AdminAuthPage.java
        │   │   ├── AdminUserCreationPage.java
        │   │   ├── AllowResubmissionPage.java
        │   │   ├── ClientAuthPage.java
        │   │   ├── ClientOnboardingPage.java
        │   │   ├── ClientShortlistPage.java
        │   │   ├── CreateRequirementPage.java
        │   │   ├── HoldRejectSentClientPage.java
        │   │   ├── LoginPage.java
        │   │   ├── OfferJobPage.java
        │   │   ├── PartnerShortlistingPage.java
        │   │   ├── ProdSanityPage.java          ← NEW
        │   │   ├── RequirementCompletedPage.java
        │   │   ├── RequirementListingPage.java
        │   │   ├── ScheduleAssessmentPage.java
        │   │   ├── ScheduleAssignmentPage.java
        │   │   ├── ScheduleInterviewPage.java
        │   │   ├── SubmitCandidatePage.java
        │   │   ├── UploadInterviewPage.java
        │   │   ├── VendorApprovalPage.java
        │   │   ├── VendorAuthPage.java
        │   │   └── VendorOnboardingPage.java
        │   │
        │   ├── tests/                   # 17 JUnit 5 test classes & suites
        │   │   ├── AddCandidateToSpecificReqTest.java
        │   │   ├── AdminAuthTest.java
        │   │   ├── AdminUserCreationTest.java
        │   │   ├── AllAuthTestSuite.java
        │   │   ├── AllOnboardingTestSuite.java
        │   │   ├── ClientAuthTest.java
        │   │   ├── ClientOnboardingTest.java
        │   │   ├── CreateMultipleRequirementTest.java
        │   │   ├── CreateRequirementTest.java
        │   │   ├── CreateSingleRequirementTest.java
        │   │   ├── ProdSanityTest.java           ← NEW
        │   │   ├── ScheduleAssessmentTest.java
        │   │   ├── ScheduleAssignmentTest.java
        │   │   ├── ScheduleInterviewAssignmentAssessmentTest.java
        │   │   ├── ScheduleInterviewTest.java
        │   │   ├── VendorAuthTest.java
        │   │   └── VendorOnboardingTest.java
        │   │
        │   └── utils/                   # 5 Utility / infrastructure classes
        │       ├── DashboardClient.java
        │       ├── DashboardManager.java
        │       ├── EmailSender.java
        │       ├── GoogleSessionGenerator.java
        │       └── MailService.java
        │
        └── resources/
            ├── env.properties           # Placeholder (credentials come from env vars)
            ├── Ajay_Gupta_resume_.pdf
            ├── Anurag_DesignResume (2).pdf
            ├── Shashikant.pdf           # Used by ProdSanityTest
            ├── demo-jd.pdf
            ├── jd_backend.pdf
            └── jd_frontend.pdf
```

---

## 4. Dependencies

**Group ID:** `com.embra` | **Artifact:** `EMB-RA-Automation` | **Version:** `1.0-SNAPSHOT`

| Dependency | Version | Scope | Purpose |
|------------|---------|-------|---------|
| `com.microsoft.playwright` | **1.49.0** | test | Browser automation — Chromium, Firefox, WebKit |
| `org.junit.jupiter:junit-jupiter` | 5.10.1 | test | JUnit 5 — `@Test`, `@BeforeAll`, lifecycle |
| `org.junit.platform:junit-platform-suite` | 1.10.1 | test | `@Suite` — aggregate multiple test classes |
| `com.aventstack:extentreports` | 5.1.1 | compile | Dark-theme HTML test reports |
| `org.eclipse.angus:angus-mail` | 2.0.3 | compile | Jakarta Mail / Gmail SMTP |
| `org.slf4j:slf4j-api` | 2.0.9 | compile | Logging facade |
| `ch.qos.logback:logback-classic` | 1.4.11 | compile | SLF4J backend |
| `com.google.code.gson:gson` | 2.11.0 | compile | JSON parsing (MailService / OTP) |
| `io.github.cdimascio:dotenv-java` | **3.0.0** | test | Secure `.env` file loading for local dev |

**Maven Plugins:**

| Plugin | Version | Config |
|--------|---------|--------|
| `maven-compiler-plugin` | 3.13.0 | `<release>17</release>` |
| `maven-surefire-plugin` | 3.2.5 | `useModulePath=false` (required for JUnit 5) |

> **Note:** `pom.xml` has a `playwright.version` property of `1.41.2` but the dependency block explicitly pins `1.49.0` — the explicit version wins.

---

## 5. CI/CD — GitHub Actions Workflows

### 5.1 `daily-run.yml` — Nightly Full Suite

```
Trigger : cron "30 0 * * *"  →  06:00 AM IST / 00:30 UTC
          workflow_dispatch   →  manual button in GitHub UI
Runner  : ubuntu-latest
```

| Step | Command |
|------|---------|
| Checkout | `actions/checkout@v4` |
| Java 17 | `actions/setup-java@v4` (Temurin, Maven cache) |
| Install browsers | `mvn exec:java … -D exec.args="install --with-deps"` |
| Run all tests | `xvfb-run mvn test` |
| Upload report | `actions/upload-artifact@v4` → `target/AutomationDashboard.html` |

**Secrets required:** `EMAIL_PASSWORD`

---

### 5.2 `prod-sanity.yml` — UAT Sanity Check ← NEW

```
Trigger : cron "0 1 * * *"  →  06:30 AM IST / 01:00 UTC
          workflow_dispatch  →  manual button in GitHub UI
Runner  : ubuntu-latest
```

| Step | Command |
|------|---------|
| Checkout | `actions/checkout@v4` |
| Java 17 | `actions/setup-java@v4` (Temurin, Maven cache) |
| Install browsers | `mvn exec:java … -D exec.args="install --with-deps"` |
| Run sanity | `xvfb-run mvn test -Dtest=ProdSanityTest` |
| Upload artifacts | Report + `traces/` directory |

**Secrets required:** `UAT_VENDOR_EMAIL`, `UAT_VENDOR_PASSWORD`, `EMAIL_PASSWORD`

**Non-secret env vars (inlined in YAML):**
- `UAT_VENDOR_URL=https://uat-vendor.embtalent.ai/login`
- `UAT_REQ_NAME=Test Sanity Requirement`
- `UAT_RESUME_PATH=src/test/resources/Shashikant.pdf`

---

### 5.3 `remote_trigger.yml` — On-Demand Single Test

```
Trigger : repository_dispatch event "manual-test-trigger"
Payload : { test_name, run_id }
Runner  : ubuntu-latest
```

Runs `mvn test -Dtest=${test_name} -Drun_id=${run_id}`. The `run_id` is a system property picked up by `DashboardManager` to stream logs to the correct Supabase row.

**Secrets required:** `SUPABASE_URL`, `SUPABASE_KEY`

---

## 6. Credential & Secret Management

### Priority Order (highest → lowest)

```
1. OS Environment Variables   (GitHub Actions Secrets → always win in CI)
2. .env file                  (local dev → in .gitignore, never committed)
3. .env.example fallback      (local convenience — can run without copying .env)
```

### Files

| File | Committed? | Purpose |
|------|-----------|---------|
| `.env.example` | ✅ Yes | Template with all required keys (safe — contains no real secrets in repo) |
| `.env` | ❌ No (`.gitignore`) | Actual local credentials — never commit |

### How it works in `ProdSanityTest`

```java
// loadDotenv() tries .env first, falls back to .env.example
Dotenv dotenv = loadDotenv();

// dotenv.get() checks OS env FIRST (CI/CD wins), then the file
VENDOR_EMAIL    = dotenv.get("UAT_VENDOR_EMAIL");
VENDOR_PASSWORD = dotenv.get("UAT_VENDOR_PASSWORD");

// Hard-stop if any credential is missing — never run with nulls
requireCredential("UAT_VENDOR_EMAIL", VENDOR_EMAIL);
```

### Required Environment Variables

| Variable | Used By | Secret in CI? |
|----------|---------|--------------|
| `UAT_VENDOR_EMAIL` | `ProdSanityTest` | ✅ Yes |
| `UAT_VENDOR_PASSWORD` | `ProdSanityTest` | ✅ Yes |
| `UAT_VENDOR_URL` | `ProdSanityTest` | No (safe to inline) |
| `UAT_REQ_NAME` | `ProdSanityTest` | No |
| `UAT_RESUME_PATH` | `ProdSanityTest` | No |
| `EMAIL_PASSWORD` | `EmailSender` | ✅ Yes |
| `SUPABASE_URL` | `DashboardManager` | ✅ Yes |
| `SUPABASE_KEY` | `DashboardManager` | ✅ Yes |

### Local Setup (one-time)

```bash
cp .env.example .env
# Edit .env with real values
mvn test -Dtest=ProdSanityTest
```

### Security Rules

- **Never hardcode** credentials in any `.java` file
- **Never log** credential values — use `"(masked for security)"` in log messages
- Use **dedicated test accounts**, not real production user accounts
- Credentials are validated at `@BeforeAll` — test fails immediately with a clear message if missing

---

## 7. Utility Classes

### 7.1 `DashboardManager`
**Package:** `com.embra.utils`

Central hub for all test output. Every log goes to three destinations simultaneously:

```
DashboardManager.log("message")
        ├── System.out.println()            → Terminal / CI logs
        ├── ExtentTest.pass/fail/info()     → target/AutomationDashboard.html
        └── HTTP PATCH to Supabase          → Real-time cloud dashboard (if run_id set)
```

**Message routing logic:**

| Message contains | ExtentReports call |
|------------------|--------------------|
| `✅` or `SUCCESS` | `pass()` + passCount++ |
| `❌` or `Failed` | `fail()` + failCount++ |
| `⚠️` | `warning()` + infoCount++ |
| anything else | `info()` + infoCount++ |

**Key methods:**

| Method | Description |
|--------|-------------|
| `initReport()` | Creates dark-theme ExtentReports; idempotent |
| `startTest(name)` | Creates new ExtentTest node; resets counters |
| `log(message)` | Routes to console + HTML + Supabase |
| `flushReport()` | Writes HTML file to disk (`extent.flush()`) |
| `getPassCount()` / `getFailCount()` | For email summary stats |

**Config (from env):**
- `SUPABASE_URL` — Supabase project REST endpoint
- `SUPABASE_KEY` — Supabase `anon` API key
- `-Drun_id` — System property; identifies which DB row to stream to

---

### 7.2 `DashboardClient`
**Package:** `com.embra.utils`

Lightweight alternative to `DashboardManager` for simple async Supabase updates. Sends fire-and-forget HTTP PATCH requests using `java.net.http.HttpClient`. Used when only a status update (pass/fail) is needed without full log streaming.

---

### 7.3 `EmailSender`
**Package:** `com.embra.utils`

Sends the final HTML test report via Gmail SMTP after each run.

- **Auth:** Gmail App Password from env var `EMAIL_PASSWORD`
- **Format:** HTML email with pass/fail/info counts and IST timestamp
- **Attachment:** `target/AutomationDashboard.html`
- **Timezone:** Asia/Kolkata (IST) — dynamically computed UTC offset

Called at the end of every `@AfterAll`:
```java
EmailSender.sendDashboardEmail("recipient@example.com");
```

---

### 7.4 `MailService`
**Package:** `com.embra.utils`

Polls [mail.tm](https://mail.tm) temporary email API to extract OTP codes during signup and forgot-password flows.

- Creates a disposable email address via REST
- Polls the inbox every 3 seconds, up to 40 retries (2 minutes max)
- Extracts 6-digit OTP using regex: `\b\d{6}\b`
- Filters by `no-reply` sender and timestamp (only reads emails newer than test start)

---

### 7.5 `GoogleSessionGenerator`
**Package:** `com.embra.utils`

One-time utility to capture a Google-authenticated browser session. Run manually:

1. Launches a **visible** Chromium browser (non-headless)
2. Navigates to Google login — waits for manual sign-in (120s)
3. Saves `cookies + localStorage + sessionStorage` to `auth/google_state.json`

Tests that need Google login reuse this state:
```java
browser.newContext(new Browser.NewContextOptions()
    .setStorageStatePath(Paths.get("auth/google_state.json")));
```

---

## 8. Page Object Model — All Pages

### 8.1 Authentication Pages

#### `LoginPage`
Generic admin login used across multiple tests.

| Method | Description |
|--------|-------------|
| `login(email, password)` | Fills form + clicks submit; validates post-login URL and `text=New Requirement` |

**Locator strategy:** Multiple selectors — `input#email, input[name='email']` — for resilience.  
**Post-login check:** `page.waitForURL(url → url.contains("/dashboard") or "/requirement")`.  
**Fallback click:** 3-tier — normal → force → JavaScript.

---

#### `AdminAuthPage`
Admin portal (`uat-admin.embtalent.ai/login`) auth flows.

| Method | Description |
|--------|-------------|
| `navigate()` | Go to admin login URL |
| `login(email, password)` | Fill + submit |
| `requestReset(email)` | Click "Forgot Password?" + enter email + click send |
| `setNewPassword(password)` | Fill new + confirm password + submit |
| `getDashboardHeading()` | Returns `h1:has-text('Dashboard')` locator |
| `getToastByTitle(title)` | Returns `li[data-sonner-toast]` toast locator |

---

#### `ClientAuthPage`
Client portal (`uat-client.embtalent.ai/login`) auth + registration.

| Method | Description |
|--------|-------------|
| `navigate()` | Go to client login URL |
| `loginWithEmail(email, password)` | Email/password login |
| `clickGoogleLogin()` | Click "Login with Google" |
| `signup(name, email, password)` | Fill registration form + submit |
| `enterOTP(otp)` | Iterates OTP inputs; calls `clear()` before each fill |
| `clickForgotPassword()` | Clicks forgot password link |

---

#### `VendorAuthPage`
Vendor portal (`uat-vendor.embtalent.ai/login` or `dev-vendor`) auth + registration.

| Method | Description |
|--------|-------------|
| `navigate()` | Go to vendor login URL |
| `loginWithEmail(email, password)` | Fill + click Login |
| `signup(name, email, password)` | Fill registration form |
| `enterOTP(otp)` | Per-character input fill with clear |
| `clickForgotPassword()` | Forgot password link |
| `getOtpInlineErrorText()` | Reads and returns inline OTP error text |
| `getRegistrationSuccessSubtext()` | Returns onboarding confirmation locator |

---

### 8.2 Onboarding Pages

#### `AdminUserCreationPage`
Creates new admin user accounts via the Admin portal UI.

| Method | Description |
|--------|-------------|
| `fillUserDetails(...)` | Fills name, email, role fields |
| `selectRoleAdmin()` | Selects Admin role from dropdown |
| `setNewPassword(password)` | Sets initial password |

---

#### `ClientOnboardingPage`
Completes client company profile after first login.

| Method | Description |
|--------|-------------|
| `fillOnboardingForm(...)` | Org name, industry, website, phone, primary contact |

---

#### `VendorOnboardingPage`
4-step wizard for vendor company registration.

| Step | Fields |
|------|--------|
| Step 1 | Organization name, website, address, city, state, country, timezone |
| Step 2 | Org size, annual turnover, services offered |
| Step 3 | Skills + technology capabilities (checkbox multi-select) |
| Step 4 | Final address confirmation + submit |

---

### 8.3 Requirement Management Pages

#### `RequirementListingPage`
Admin requirement dashboard.

| Method | Description |
|--------|-------------|
| `clickNewRequirement()` | Waits for dashboard readiness then clicks "New Requirement" CTA |

**Readiness check:** Waits for sidebar link `a[href='/hiring-requests']` before clicking.

---

#### `CreateRequirementPage`
Complex multi-form requirement creation. Supports batch creation of 1–4 requirements.

**Inner class:** `RequirementData` — value object carrying per-requirement config:
```java
new RequirementData(
    "Full Time",   // engagementType
    "Remote",      // workMode
    "JS",          // role
    "React",       // skill
    "52106",       // budget
    "path/to.pdf"  // jdFilePath
)
```

| Method | Description |
|--------|-------------|
| `createMultipleRequirements(List<RequirementData>, successMsg)` | Iterates list; handles Full Time vs Contractual vs C2H conditional fields |

**Conditional logic:**
- Full Time → fills salary band, notice period
- Contractual / C2H → fills contract duration, day rate
- All types → JD upload, skill search + checkbox, role selection, engagement mode

---

### 8.4 Candidate Management Pages

#### `SubmitCandidatePage`
Comprehensive vendor candidate submission — the most complex page in the codebase.

| Method | Description |
|--------|-------------|
| `loginToVendorPortal(email, password)` | Login + wait for dashboard |
| `navigateToProject(reqName)` | Searches project list + clicks matching card |
| `acceptProject()` | Clicks Accept Project CTA if not already accepted |
| `addMembers(count, jdPath)` | Adds `count` candidates with auto-generated details |
| `addMembersFromTeam()` | Alternative path — selects from existing team members |
| `fillMemberDetails(...)` | Resume upload, skills, education, work history, awards, certificates, financials, timezone |
| `submitCandidates()` | Final submit + verify |
| `verifyCandidateStatus()` | Confirms "Submitted" status badge |

---

#### `PartnerShortlistingPage`
Admin partner/vendor shortlisting — sends requirement to selected vendors.

| Method | Description |
|--------|-------------|
| `searchAndOpenRequirement(reqName)` | Search + open from listing |
| `shortlistVendors(List<String> names)` | Multi-select vendors from Active Partners section |
| `clickSendHiringRequirement()` | Opens budget modal |
| `fillBudgetDetails()` | Multi-currency: EUR, AED, USD, INR — fills budget + billing type |
| `submitShortlisting()` | Final submit |
| `verifySuccessToast()` | Asserts success notification |

---

#### `ClientShortlistPage`
Client-side candidate review.

| Method | Description |
|--------|-------------|
| `loginAndShortlist(email, password)` | Login to client portal + shortlist first candidate |
| `clientRejectCandidate(...)` | Reject a candidate with reason |
| `verifyShortlistOnAdmin(reqName)` | Cross-portal verify on Admin side |
| `verifyRejectionOnAdmin(reqName)` | Cross-portal verify rejection on Admin side |

---

#### `HoldRejectSentClientPage`
Admin post-submission candidate actions.

| Method | Description |
|--------|-------------|
| `processCandidatesOnAdmin(...)` | Hold / reject / resend candidates to client |
| `vendorVerifyFinalStatuses(...)` | Vendor confirms final status badges |

---

#### `AllowResubmissionPage`
Re-submission workflow after assignment failure.

| Method | Description |
|--------|-------------|
| `allowResubmissionsOnAdmin(reqName, candidateName)` | Admin unlocks resubmission |
| `vendorPerformResubmission(...)` | Vendor re-uploads assignment solution |

---

#### `OfferJobPage`
Final offer and candidate deployment.

| Method | Description |
|--------|-------------|
| `deployCandidate(reqName, candidateName)` | Fills engagement dates, costs, uploads offer document; sets status to "Deployed" |

---

#### `RequirementCompletedPage`
Final status verification across all three portals.

| Method | Description |
|--------|-------------|
| `adminDeployCandidate(...)` | Admin confirms deployment |
| `verifyPortalStatus(...)` | Checks status strings on Admin, Vendor, and Client portals |

---

### 8.5 Interview, Assessment & Assignment Pages

#### `ScheduleInterviewPage`
Admin interview scheduling + time slot management.

| Method | Description |
|--------|-------------|
| `navigateAndOpenRequirement(reqName)` | Search + open requirement |
| `openCandidateForInterview(name)` | Navigate to Candidates tab + open specific candidate |
| `updateStatusToScheduleInterview()` | Change candidate status dropdown |
| `selectInterviewTimeSlots()` | Calendar date-picker: selects 3+ slots across multiple days |
| `verifyInterviewDetails()` | Assert slot details visible |

---

#### `UploadInterviewPage`
Full interview upload + feedback flow (split across Admin and Vendor actions).

| Method | Description |
|--------|-------------|
| `vendorSelectInterviewTime(url, email, pwd, reqName)` | Vendor selects from offered time slots |
| `adminScheduleAndFeedbackInterview(reqName, candidateName)` | Admin confirms slot + uploads feedback |
| `vendorVerifyFinalInterviewStatus(url, email, pwd, reqName)` | Vendor confirms "Interview Scheduled" status |

---

#### `ScheduleAssessmentPage`
Two-phase assessment workflow.

| Phase | Method | Description |
|-------|--------|-------------|
| Admin | `adminScheduleAssessmentAction(reqName, candidateName)` | Sets assessment date range |
| Vendor | `vendorSelectTimeSlots(url, email, pwd, reqName)` | Vendor picks 2 days × 2 slots |
| Admin | `adminUploadAssessmentResult(reqName, candidateName)` | Uploads result file + feedback |

---

#### `ScheduleAssignmentPage`
Coding challenge / assignment workflow.

| Phase | Method | Description |
|-------|--------|-------------|
| Admin | `scheduleAssignmentAction(reqName, candidateName)` | Sets assignment details + deadline |
| Vendor | `vendorSubmitAssignmentSolution(url, email, pwd, reqName)` | Uploads solution file |
| Admin | `adminSubmitAssignmentFeedback(reqName, candidateName)` | Reviews + submits feedback |
| Vendor | `vendorVerifyFinalAssignmentStatus(url, email, pwd, reqName)` | Confirms final status |

---

#### `VendorApprovalPage`
Vendor approval flow (admin approves or rejects vendor registration).

---

### 8.6 Prod Sanity Page ← NEW

#### `ProdSanityPage`
**Package:** `com.embra.pages`  
Lightweight page object for the UAT Vendor portal sanity check. Covers login → projects → candidate submission initiation in 9 discrete steps.

| Method | Description |
|--------|-------------|
| `navigate(url)` | Navigate to vendor login page; waits for email field |
| `fillCredentials(email, password)` | Fill form — logs `"(masked for security)"`, never the values |
| `clickLogin()` | Click submit — exposed as `Runnable` for `page.waitForResponse()` timing |
| `verifyLoginToast()` | Wait max 10s for `"Login successful!"` toast; returns boolean |
| `clickProjectsTab()` | Click `a[href='/projects']`; waits for load |
| `searchRequirement(reqName)` | Fill search input; 2s debounce wait |
| `openRequirementCard(reqName)` | Click matching `a[href*='projects/details']` card |
| `clickShortlistedCandidateTab()` | Click "Shortlisted Candidate" tab (exact text match) |
| `clickAddNewMember()` | Click "Add New Member" button |
| `uploadResume(resumePath)` | `setInputFiles()` on hidden `input[type='file'][accept='.pdf']` |
| `clickImportFromResume()` | Click "Import from resume" — exposed as `Runnable` for API timing |
| `waitForResumeExtractionToast(timeoutMs)` | Wait up to `timeoutMs` ms for `"Resume details extracted!"` toast |

**Key design note:** `clickLogin()` and `clickImportFromResume()` are `void` methods intentionally so they can be passed as `sanityPage::clickLogin` / `sanityPage::clickImportFromResume` `Runnable` references into `page.waitForResponse()` for zero-overhead API timing measurement.

---

## 9. Test Classes

### 9.1 Auth Tests

#### `AdminAuthTest`
Tests admin portal authentication flows.

| Test | Description |
|------|-------------|
| `testEmailLogin()` | Login with email/password; assert dashboard heading |
| `testForgotPassword()` | Request reset; poll Gmail for reset link (40 retries × 3s); set new password; verify login |

---

#### `ClientAuthTest`
Tests client portal auth flows.

| Test | Description |
|------|-------------|
| `testEmailLogin()` | Email/password login → dashboard |
| `testGoogleLogin()` | Google OAuth using saved `auth/google_state.json` |
| `testForgotPassword()` | Gmail polling with `no-reply` sender filter |
| `testOTPSignup()` | Fresh mail.tm email → register → OTP extraction → verify → onboarding |
| `testGoogleSignup()` | Register via Google OAuth |

---

#### `VendorAuthTest`
Mirrors `ClientAuthTest` for the vendor portal.

---

#### `AllAuthTestSuite`
JUnit 5 `@Suite` aggregating `AdminAuthTest`, `ClientAuthTest`, `VendorAuthTest`.

```java
@Suite
@SelectClasses({ AdminAuthTest.class, ClientAuthTest.class, VendorAuthTest.class })
```

---

### 9.2 Onboarding Tests

#### `AdminUserCreationTest`
Admin creates a new user account via UI.

#### `ClientOnboardingTest`
Client fills company profile after signup.

#### `VendorOnboardingTest`
Vendor completes 4-step registration wizard.

#### `AllOnboardingTestSuite`
JUnit 5 `@Suite` aggregating all three onboarding tests.

---

### 9.3 Requirement Tests

#### `CreateSingleRequirementTest`
Creates one Full-Time requirement; verifies it appears in the listing with "Active" status.

#### `CreateMultipleRequirementTest`
Creates a batch of 3 requirements (Full Time, Contractual, C2H) in one form session.

#### `CreateRequirementTest`
**Master 21-step E2E test** covering the entire recruitment lifecycle end-to-end. Uses all major page objects across all three portals. Saves individual Playwright traces per phase.

---

### 9.4 Workflow Tests

#### `ScheduleInterviewTest`
Login → (optionally create requirement) → partner shortlisting → vendor submission → **interview scheduling** (Admin request + Vendor slot selection + Admin finalize + Vendor verify).

Configurable target requirement:
```java
private static final String TARGET_REQUIREMENT = "Senior BE1234";
// Set to null to create a new one; set to a name to skip creation
```

#### `ScheduleAssessmentTest`
Standalone assessment scheduling flow: Admin schedules → Vendor selects slots → Admin uploads result.

#### `ScheduleAssignmentTest`
Standalone assignment flow: Admin schedules → Vendor submits solution → Admin feedback → Vendor verify.

#### `ScheduleInterviewAssignmentAssessmentTest`
Combined test covering interview + assignment + assessment in sequence.

#### `AddCandidateToSpecificReqTest`
Targets an **existing** requirement by name (no creation step). Vendor logs in and adds candidates to it. Useful for exploratory / debugging test runs.

---

### 9.5 Prod Sanity Test ← NEW

#### `ProdSanityTest`
**Package:** `com.embra.tests`  
Quick UAT sanity check for the Vendor portal. Runs in ~2 minutes.

**Credential loading:**
```
OS env vars (CI/CD secrets)  →  .env file  →  .env.example fallback
```

**9-step flow:**

| Step | Action | Measurement |
|------|--------|-------------|
| 1 | Navigate to `UAT_VENDOR_URL` | — |
| 2 | Fill credentials + click Login | Login API response time (POST to `uatapi-ra.embtalent.ai`) |
| 3 | Verify `"Login successful!"` toast | `assertTrue` — test fails if missing |
| 4 | Click Projects tab | — |
| 5 | Search `UAT_REQ_NAME` | — |
| 6 | Open requirement card | — |
| 7 | Click Shortlisted Candidate tab | — |
| 8 | Click Add New Member | — |
| 9 | Upload resume + click Import | Resume autofill API response time + status code |
| 10 | Verify `"Resume details extracted!"` toast | 59s timeout; logs SLOW if missed |

**API timing logic:**
```
≤ 200ms  →  ✅ Fast
> 200ms  →  ⚠️ SLOW (exceeds threshold)
```

Uses `page.waitForResponse(predicate, options, callback)` — registers the network listener **before** clicking, so no response is ever missed.

**Security:**
- `requireCredential()` throws `IllegalStateException` at `@BeforeAll` if any credential is null — suite never starts
- Credential values are **never** passed to `DashboardManager.log()`
- `logApiTiming()` logs URL + status + duration — no secrets

---

## 10. End-to-End Flow Map

```
Test Class                      Pages Used (in order)
──────────────────────────────────────────────────────────────────────────────
CreateRequirementTest           LoginPage
  (21-step master E2E)          RequirementListingPage → CreateRequirementPage
                                PartnerShortlistingPage
                                SubmitCandidatePage        [Vendor context]
                                HoldRejectSentClientPage
                                ClientShortlistPage        [Client context]
                                ScheduleAssessmentPage     [Admin + Vendor]
                                ScheduleAssignmentPage     [Admin + Vendor]
                                ScheduleInterviewPage      [Admin]
                                UploadInterviewPage        [Admin + Vendor]
                                AllowResubmissionPage
                                OfferJobPage
                                RequirementCompletedPage   [All 3 portals]

ScheduleInterviewTest           LoginPage → PartnerShortlistingPage
                                SubmitCandidatePage        [Vendor context]
                                ScheduleInterviewPage      [Admin]
                                UploadInterviewPage        [Admin + Vendor × 2]

ScheduleAssessmentTest          LoginPage → ScheduleAssessmentPage [Admin + Vendor]

ScheduleAssignmentTest          LoginPage → ScheduleAssignmentPage [Admin + Vendor × 2]

ProdSanityTest                  ProdSanityPage             [Vendor context only]
```

---

## 11. Locator Strategy

The framework uses a layered approach for robust element selection:

### Priority Order

```
1. Semantic role selectors     page.getByRole(AriaRole.BUTTON, ...)
2. Name/placeholder attributes input[name='email'] · input[placeholder='...']
3. Text content filters        .filter(new Locator.FilterOptions().setHasText("Login"))
4. Href attributes             a[href='/projects']
5. Type + accept               input[type='file'][accept='.pdf']
6. CSS class (specific only)   span.text-toast
7. XPath (last resort)         page.locator("xpath=...")
```

### Strict Mode Compliance

Playwright's strict mode throws if a locator matches multiple elements. Handled with:
```java
locator.first()          // when multiple matches are expected and first is correct
locator.nth(0)           // explicit index
.filter(hasText("..."))  // narrow down to specific element
```

### Safe Click Pattern (`LoginPage`)

```java
try {
    locator.click();
} catch (Exception e1) {
    try {
        locator.click(new Locator.ClickOptions().setForce(true));
    } catch (Exception e2) {
        page.evaluate("el => el.click()", locator);  // JS fallback
    }
}
```

### Hidden File Input Upload

```java
// Works even if input has opacity-0 / display:none
page.locator("input[type='file'][accept='.pdf']").setInputFiles(Paths.get(path));
```

---

## 12. Reporting & Observability

### ExtentReports HTML Dashboard

- **Path:** `target/AutomationDashboard.html`
- **Theme:** Dark (Spark)
- **Generated by:** `DashboardManager.flushReport()` in `@AfterAll`
- **Content:** Per-test timelines with pass/fail/info steps, timestamps, screenshots on failure

### Playwright Traces

Each test/phase saves a `.zip` trace to `target/` or `traces/`:

```bash
npx playwright show-trace target/admin-02-request-interview-trace.zip
```

Trace contains: DOM snapshots, network requests/responses, screenshots, action log, source code.

**Naming convention:**
- `target/admin-01-setup-shortlist-trace.zip` — admin phase 1
- `target/vendor-01-submit-candidate-trace.zip` — vendor phase 1
- `traces/testProdSanity()-trace.zip` — prod sanity run

### Email Report

Sent via Gmail SMTP in `@AfterAll`:
```java
EmailSender.sendDashboardEmail("recipient@example.com");
```
Contains pass/fail counts + IST timestamp + HTML report as attachment.

### Supabase Real-Time Streaming

When `run_id` system property is set (by `remote_trigger.yml`):
- Every `DashboardManager.log()` call PATCHes the `logs` field in `test_runs` table
- Final status (`"pass"` / `"fail"`) PATCHed on completion
- Fire-and-forget async HTTP — doesn't slow down test execution

---

## 13. Test Data & Resources

### Test Accounts

| Account | Portal | Credentials |
|---------|--------|-------------|
| `bharat.pandey@emb.global` | Admin + Vendor (UAT) | Env var / `.env` |
| `bharat.pandey+1@emb.global` | Vendor (UAT) | Env var / `.env` |
| `AutoTest@yopmail.com` | Client | Env var / `.env` |

### Resource Files (`src/test/resources/`)

| File | Used By |
|------|---------|
| `Shashikant.pdf` | `ProdSanityTest` — resume upload |
| `Anurag_DesignResume (2).pdf` | `SubmitCandidatePage` |
| `Ajay_Gupta_resume_.pdf` | `CreateRequirementTest`, `ScheduleInterviewTest` |
| `demo-jd.pdf` | Fallback JD upload |
| `jd_backend.pdf` / `jd_frontend.pdf` | Requirement creation tests |

### Dynamic Test Data

- Candidate names: `Candidate 1`, auto-generated
- Requirement titles: `"Senior BE" + randomSuffix` or fixed `TARGET_REQUIREMENT`
- Emails for OTP tests: Generated via `MailService` using mail.tm API

---

## 14. How to Run

### Prerequisites

```bash
# Install Playwright browsers (one-time)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"
```

### Local Setup

```bash
cp .env.example .env
# Fill in UAT_VENDOR_EMAIL, UAT_VENDOR_PASSWORD, EMAIL_PASSWORD in .env
```

### Run Commands

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=ProdSanityTest
mvn test -Dtest=ScheduleInterviewTest
mvn test -Dtest=CreateRequirementTest

# Run a test suite
mvn test -Dtest=AllAuthTestSuite

# Run with Supabase dashboard streaming
mvn test -Dtest=ProdSanityTest -Drun_id=12345

# View a Playwright trace
npx playwright show-trace target/admin-02-request-interview-trace.zip
npx playwright show-trace traces/testProdSanity\(\)-trace.zip
```

### View Reports

```bash
open target/AutomationDashboard.html
```

---

## 15. Quick Reference — All Java Files

### Pages (22 files)

| File | Portal | Responsibility |
|------|--------|----------------|
| `AdminAuthPage` | Admin | Login, forgot password, set new password |
| `AdminUserCreationPage` | Admin | Create new admin user accounts |
| `AllowResubmissionPage` | Admin + Vendor | Unlock + perform assignment re-submission |
| `ClientAuthPage` | Client | Login, Google login, OTP signup, forgot password |
| `ClientOnboardingPage` | Client | Company profile setup (onboarding wizard) |
| `ClientShortlistPage` | Client + Admin | Shortlist/reject candidates; cross-portal verify |
| `CreateRequirementPage` | Admin | Batch requirement creation (Full Time / C2H / Contract) |
| `HoldRejectSentClientPage` | Admin + Vendor | Hold/reject/resend candidates; vendor status verify |
| `LoginPage` | Admin | Generic login with multi-selector + safe click |
| `OfferJobPage` | Admin | Make offer, fill deployment details, deploy candidate |
| `PartnerShortlistingPage` | Admin | Shortlist vendors, fill budget (multi-currency), submit |
| `ProdSanityPage` | Vendor | Login → projects → resume import (9-step sanity) |
| `RequirementCompletedPage` | All 3 | Verify final completion status across portals |
| `RequirementListingPage` | Admin | Navigate to requirement listing; click New Requirement |
| `ScheduleAssessmentPage` | Admin + Vendor | Assessment scheduling, slot selection, result upload |
| `ScheduleAssignmentPage` | Admin + Vendor | Assignment scheduling, solution submission, feedback |
| `ScheduleInterviewPage` | Admin | Interview request, calendar time-slot selection |
| `SubmitCandidatePage` | Vendor | Full candidate profile: resume, skills, financials, submit |
| `UploadInterviewPage` | Admin + Vendor | Interview time selection, scheduling, feedback |
| `VendorApprovalPage` | Admin | Vendor approval/rejection workflow |
| `VendorAuthPage` | Vendor | Login, Google login, OTP signup, forgot password |
| `VendorOnboardingPage` | Vendor | 4-step onboarding wizard (org details → skills → address) |

### Tests (17 files)

| File | Type | Description |
|------|------|-------------|
| `AddCandidateToSpecificReqTest` | Test | Add candidates to a pre-existing named requirement |
| `AdminAuthTest` | Test | Admin email login + forgot password |
| `AdminUserCreationTest` | Test | Admin creates new user |
| `AllAuthTestSuite` | Suite | Aggregates Admin + Client + Vendor auth tests |
| `AllOnboardingTestSuite` | Suite | Aggregates all onboarding tests |
| `ClientAuthTest` | Test | Client email login, Google, OTP signup, forgot password |
| `ClientOnboardingTest` | Test | Client onboarding form |
| `CreateMultipleRequirementTest` | Test | Batch create 3 requirements |
| `CreateRequirementTest` | Test | **21-step master E2E — full recruitment lifecycle** |
| `CreateSingleRequirementTest` | Test | Create one requirement, verify Active status |
| `ProdSanityTest` | Test | **9-step UAT sanity — login + API timing + resume extraction** |
| `ScheduleAssessmentTest` | Test | Standalone assessment scheduling flow |
| `ScheduleAssignmentTest` | Test | Standalone assignment flow |
| `ScheduleInterviewAssignmentAssessmentTest` | Test | Combined interview + assignment + assessment |
| `ScheduleInterviewTest` | Test | Standalone interview scheduling flow |
| `VendorAuthTest` | Test | Vendor email login, OTP signup, forgot password |
| `VendorOnboardingTest` | Test | Vendor 4-step onboarding wizard |

### Utils (5 files)

| File | Responsibility |
|------|----------------|
| `DashboardManager` | Console + HTML report + Supabase real-time log streaming |
| `DashboardClient` | Lightweight async Supabase status updater |
| `EmailSender` | Gmail SMTP HTML report delivery with IST timestamp |
| `MailService` | mail.tm API — create disposable email + extract OTP |
| `GoogleSessionGenerator` | One-time Google OAuth session capture → `auth/google_state.json` |

---

*Generated automatically from codebase state as of 2026-05-06.*