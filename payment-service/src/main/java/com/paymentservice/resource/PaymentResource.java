package com.paymentservice.resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.dto.PaymentVerificationRequest;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.service.RazorpayService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentResource {

	private final RazorpayClient razorpayClient;

	@Autowired
	private RazorpayService paymentService;

	@Autowired
	private PaymentRepository repository;

	@Autowired
	private PaymentMessageProducer producer;

	PaymentResource(RazorpayClient razorpayClient) {
		this.razorpayClient = razorpayClient;
	}

	@PostMapping("/create-order")
	public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws Exception {
		Double amount = Double.parseDouble(data.get("amount").toString());
		String userId = data.get("userId").toString();
		String email = data.get("email").toString();

		Order order = paymentService.createRazorpayOrder(amount, userId, email);

		PaymentOrder paymentOrder = PaymentOrder.builder().razorpayOrderId(order.get("id")).amount(amount)
				.userId(userId).userEmail(email).status("PENDING").build();

		repository.save(paymentOrder);
		return ResponseEntity.ok(order.toString());
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verify(@RequestBody PaymentVerificationRequest request) {
		boolean isValid = paymentService.verifyPayment(request.getRazorpay_order_id(), request.getRazorpay_payment_id(),
				request.getRazorpay_signature());

		// Inside PaymentResource.java verify method
		if (isValid) {
			repository.findByRazorpayOrderId(request.getRazorpay_order_id()).ifPresent(p -> {
				p.setStatus("SUCCESS");
				repository.save(p);

				// TRIGGER NOTIFICATION
				producer.sendPaymentSuccessNotification(p.getUserEmail(), p.getRazorpayOrderId(), p.getAmount());
			});
			return ResponseEntity.ok(Map.of("status", "success"));
		}
		return ResponseEntity.status(400).body(Map.of("status", "failure", "message", "Invalid signature"));
	}

	@GetMapping("/status/{userId}")
	public ResponseEntity<?> getSubscriptionStatus(@PathVariable String userId) {

		// Fetch the most recent successful payment
		List<PaymentOrder> orders = repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "SUCCESS");

		if (orders.isEmpty()) {
			return ResponseEntity.ok(Map.of("isSubscribed", false));
		}
		PaymentOrder latestPayment = orders.get(0);

		// Calculate Expiry (30 days from purchase)
		LocalDateTime expiryDate = latestPayment.getCreatedAt().plusDays(30);

		// Real-life logic: Is the current date before the expiry date?
		boolean isActive = LocalDateTime.now().isBefore(expiryDate);
		return ResponseEntity.ok(Map.of("isSubscribed", isActive, "purchaseDate", latestPayment.getCreatedAt(),
				"expiryDate", expiryDate, "amount", latestPayment.getAmount()));
	}
}