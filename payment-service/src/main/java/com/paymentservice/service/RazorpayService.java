package com.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

	@Autowired
	private RazorpayClient razorpayClient;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	public Order createRazorpayOrder(Double amount, String userId, String email) throws Exception {
		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", (int) (amount * 100)); // Amount in paise
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

		// Custom notes to track user data inside Razorpay
		JSONObject notes = new JSONObject();
		notes.put("userId", userId);
		notes.put("email", email);
		orderRequest.put("notes", notes);

		return razorpayClient.orders.create(orderRequest);
	}

	public boolean verifyPayment(String orderId, String paymentId, String signature) {
		try {
			JSONObject options = new JSONObject();
			options.put("razorpay_order_id", orderId);
			options.put("razorpay_payment_id", paymentId);
			options.put("razorpay_signature", signature);

			// Verifies the signature using the keySecret
			return Utils.verifyPaymentSignature(options, keySecret);
		} catch (Exception e) {
			return false;
		}
	}
}