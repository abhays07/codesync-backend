package com.paymentservice.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentMessageProducer {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void sendPaymentSuccessNotification(String email, String orderId, Double amount) {
		Map<String, Object> message = Map.of("email", email, "orderId", orderId, "amount", amount, "type",
				"PAYMENT_SUCCESS", "message", "Payment of ₹" + amount + " for order " + orderId + " was successful!");
		// Ensure this queue name matches what Notification-Service listens to
		rabbitTemplate.convertAndSend("payment-notification-queue", message);
	}
}