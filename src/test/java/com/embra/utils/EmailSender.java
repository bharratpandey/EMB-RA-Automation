package com.embra.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;

import java.io.File;
import java.util.Properties;

public class EmailSender {

    public static void sendDashboardEmail(String toEmail) {
        System.out.println("\n📧 Preparing to send Dashboard via Email...");

        // 1. Setup your SMTP credentials here
        final String fromEmail = "bharat.pandeyltd@gmail.com"; // Replace with your bot email

        // 2. Safely assign the password exactly once to make it "effectively final"
        String envPassword = System.getenv("EMAIL_PASSWORD");
        final String appPassword = (envPassword == null || envPassword.isEmpty())
                ? "vbrxoolyucgujwer" // Fallback for your local laptop
                : envPassword;                 // The secret from GitHub Actions

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // 3. Create the session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // Now Java is happy because fromEmail and appPassword are final!
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✅ Automation Execution Dashboard");

            // Text Body
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Hello,\n\nThe E2E automation run has completed. Please find the attached HTML Dashboard for the detailed logs.\n\nBest,\nAutomation Bot");

            // Attachment (The HTML Dashboard)
            BodyPart attachmentPart = new MimeBodyPart();
            File file = new File(DashboardManager.REPORT_PATH);
            if(file.exists()) {
                attachmentPart.setDataHandler(new DataHandler(new FileDataSource(file)));
                attachmentPart.setFileName("AutomationDashboard.html");
            }

            // Combine text and attachment
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            if(file.exists()) multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            // Send
            Transport.send(message);
            System.out.println("✅ Email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.out.println("❌ Failed to send email: " + e.getMessage());
        }
    }
}