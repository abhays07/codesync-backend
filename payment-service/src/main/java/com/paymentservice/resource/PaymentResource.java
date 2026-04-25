package com.paymentservice.resource;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentResource {

	@Autowired
	private RazorpayService paymentService;

	@Autowired
	private PaymentRepository repository;

	@Autowired
	private PaymentMessageProducer producer;

	@PostMapping("/create-order")
	public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws Exception {
		Double amount = Double.parseDouble(data.get("amount").toString());
		String userId = data.get("userId").toString();
		String email = data.get("email").toString();

		Order order = paymentService.createRazorpayOrder(amount, userId, email);

		// Persist the order immediately as PENDING
		PaymentOrder paymentOrder = PaymentOrder.builder().razorpayOrderId(order.get("id")).amount(amount)
				.userId(userId).userEmail(email).status("PENDING").build();

		repository.save(paymentOrder);
		return ResponseEntity.ok(order.toString());
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verify(@RequestBody PaymentVerificationRequest request) {
		boolean isValid = paymentService.verifyPayment(request.getRazorpay_order_id(), request.getRazorpay_payment_id(),
				request.getRazorpay_signature());

		if (isValid) {
			return repository.findByRazorpayOrderId(request.getRazorpay_order_id()).map(p -> {
				p.setStatus("SUCCESS");
				repository.save(p);
				// Async push to Notification-Service via RabbitMQ
				producer.sendPaymentSuccessNotification(p.getUserEmail(), p.getRazorpayOrderId(), p.getAmount());
				return ResponseEntity.ok(Map.of("status", "success"));
			}).orElse(ResponseEntity.status(404).body(Map.of("message", "Order record not found")));
		}
		return ResponseEntity.status(400).body(Map.of("status", "failure", "message", "Invalid payment signature"));
	}
}