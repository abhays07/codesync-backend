package com.notificationservice.service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${FRONTEND_URL}")
	private String frontendUrl;

	@Async("emailTaskExecutor")
	public void sendHtmlEmail(List<String> recipients, String username, String projectName, String emailType) {
		if (recipients == null || recipients.isEmpty())
			return;

		// 1. Internal Logic: Subject & Message generation
		boolean isApproved = "APPROVED".equals(emailType);
		boolean isRequest = "REQUEST".equals(emailType);

		String subject;
		String statusText;
		String statusColor;
		String statusBg;
		String mainMessage;
		String actionBtn;

		if (isApproved) {
			subject = "✓ Access Granted: " + projectName;
			statusText = "Request Approved";
			statusColor = "#22c55e"; // Green
			statusBg = "#f0fdf4";
			mainMessage = "Great news! Your request to collaborate has been accepted. You now have full access to the project workspace.";
			actionBtn = "<a href='" + frontendUrl + "/dashboard' class='btn'>Go to Workspace</a>";
		} else if (isRequest) {
			subject = "👋 New Collaboration Request: " + projectName;
			statusText = "Action Required";
			statusColor = "#f59e0b"; // Amber/Orange
			statusBg = "#fffbeb";
			mainMessage = username + " has requested to join your project workspace. Please review and respond to this request.";
			actionBtn = "<a href='" + frontendUrl + "/dashboard' class='btn'>Review Request</a>";
		} else {
			subject = "✕ Update on your request: " + projectName;
			statusText = "Request Declined";
			statusColor = "#ef4444"; // Red
			statusBg = "#fef2f2";
			mainMessage = "Thank you for your interest. Unfortunately, the project owner has declined your request to join at this time.";
			actionBtn = "<a href='" + frontendUrl + "/explore' class='btn-secondary'>View Other Projects</a>";
		}

		// 2. The Professional HTML Template
		String htmlTemplate = """
				<!DOCTYPE html>
				<html>
				<head>
				    <style>
				        body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Inter', sans-serif; }
				        .container { width: 100%%; background-color: #f8fafc; padding: 40px 0; }
				        .card { background-color: #ffffff; margin: 0 auto; max-width: 550px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; border: 1px solid #e2e8f0; }
				        .header { background-color: #070F2B; padding: 30px; text-align: center; }
				        .content { padding: 40px; text-align: center; }
				        .status { display: inline-block; padding: 6px 14px; border-radius: 50px; font-size: 12px; font-weight: 700; border: 1px solid; margin-bottom: 20px; }
				        .project-name { font-size: 32px; font-weight: 800; color: #070F2B; margin: 10px 0; }
				        .msg { color: #64748b; font-size: 16px; line-height: 1.6; margin-bottom: 30px; }
				        .btn { background-color: #535C91; color: #ffffff !important; padding: 14px 28px; border-radius: 10px; text-decoration: none; font-weight: 600; display: inline-block; }
				        .btn-secondary { background-color: #f1f5f9; color: #475569 !important; padding: 14px 28px; border-radius: 10px; text-decoration: none; font-weight: 600; display: inline-block; }
				        .footer { padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; }
				    </style>
				</head>
				<body>
				    <div class='container'>
				        <div class='card'>
				            <div class='header'><h1 style='color:white; margin:0;'>CodeSync</h1></div>
				            <div class='content'>
				                <div class='status' style='background-color: %s; color: %s; border-color: %s;'>%s</div>
				                <h3 style='margin-top:0;'>Hi %s,</h3>
				                <div style='font-size:12px; color:#94a3b8; text-transform:uppercase;'>Workspace</div>
				                <div class='project-name'>%s</div>
				                <p class='msg'>%s</p>
				                %s
				            </div>
				        </div>
				        <div class='footer'>CodeSync Microservices Framework • Bhopal, India</div>
				    </div>
				</body>
				</html>
				"""
				.formatted(statusBg, statusColor, statusColor, statusText, username, projectName, mainMessage,
						actionBtn);

		// 3. Execution
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.setTo(recipients.toArray(new String[0]));
			helper.setSubject(subject);
			helper.setText(htmlTemplate, true);
			mailSender.send(mimeMessage);
		} catch (Exception e) {
			System.err.println("Error sending collaboration email: " + e.getMessage());
		}
	}
}