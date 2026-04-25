package com.paymentservice.serviceTest;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.resource.PaymentResource;
import com.paymentservice.service.RazorpayService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentResourceTest {

	private MockMvc mockMvc;

	@Mock
	private RazorpayService paymentService;

	@Mock
	private PaymentRepository repository;

	@Mock
	private PaymentMessageProducer producer;

	@Mock
	private RazorpayClient razorpayClient; // Required for the Constructor

	@InjectMocks
	private PaymentResource paymentResource;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		// Force injection of mocks into the Autowired fields to avoid
		// NullPointerException
		// caused by the custom constructor in PaymentResource
		ReflectionTestUtils.setField(paymentResource, "paymentService", paymentService);
		ReflectionTestUtils.setField(paymentResource, "repository", repository);
		ReflectionTestUtils.setField(paymentResource, "producer", producer);

		mockMvc = MockMvcBuilders.standaloneSetup(paymentResource).build();
	}

	@Test
	void testCreateOrder_Success() throws Exception {
		// Prepare Mock Razorpay Order
		JSONObject orderJson = new JSONObject();
		orderJson.put("id", "order_test_123");
		orderJson.put("status", "created");
		orderJson.put("amount", 49900);
		orderJson.put("currency", "INR");
		orderJson.put("receipt", "receipt_123");

		Order mockOrder = new Order(orderJson);

		// Mock Service call
		when(paymentService.createRazorpayOrder(anyDouble(), anyString(), anyString())).thenReturn(mockOrder);

		String requestBody = "{\"amount\": 499, \"userId\": \"user1\", \"email\": \"abhay@example.com\"}";

		mockMvc.perform(
				post("/api/v1/payments/create-order").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value("order_test_123"));
	}

	@Test
	void testVerifyPayment_Success() throws Exception {
		// Mock verification logic
		when(paymentService.verifyPayment(anyString(), anyString(), anyString())).thenReturn(true);

		// Mock repository lookup
		PaymentOrder mockOrder = new PaymentOrder();
		mockOrder.setRazorpayOrderId("order_123");
		mockOrder.setUserEmail("abhay@example.com");
		mockOrder.setAmount(499.0);

		when(repository.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(mockOrder));

		String requestBody = "{" + "\"razorpay_order_id\": \"order_123\"," + "\"razorpay_payment_id\": \"pay_123\","
				+ "\"razorpay_signature\": \"sig_123\"" + "}";

		mockMvc.perform(post("/api/v1/payments/verify").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
	}
}