package com.paymentservice.serviceTest;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.resource.AdminPaymentController;
import com.paymentservice.resource.PaymentResource;
import com.paymentservice.service.RazorpayService;
import com.razorpay.Order;

class PaymentResourceTest {

	private MockMvc mockMvc;
	private MockMvc adminMockMvc;

	@Mock
	private RazorpayService paymentService;

	@Mock
	private PaymentRepository repository;

	@Mock
	private PaymentMessageProducer producer;

	@InjectMocks
	private PaymentResource paymentResource;

	@InjectMocks
	private AdminPaymentController adminPaymentController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(paymentResource).build();
		adminMockMvc = MockMvcBuilders.standaloneSetup(adminPaymentController).build();
	}

	@Test
	void testVerifyPayment_Success() throws Exception {
		when(paymentService.verifyPayment(anyString(), anyString(), anyString())).thenReturn(true);

		PaymentOrder mockOrder = PaymentOrder.builder()
				.razorpayOrderId("order_123")
				.userEmail("abhay@example.com")
				.amount(499.0)
				.build();

		when(repository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(mockOrder));

		String requestBody = """
				{
				    "razorpay_order_id": "order_123",
				    "razorpay_payment_id": "pay_123",
				    "razorpay_signature": "sig_123"
				}
				""";

		mockMvc.perform(post("/api/v1/payments/verify").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));

		verify(producer, times(1)).sendPaymentSuccessNotification(anyString(), anyString(), anyDouble());
	}

	@Test
	void testGetSubscriptionStatus_NoSubscription() throws Exception {
		when(repository.findByUserIdAndStatusOrderByCreatedAtDesc("user1", "SUCCESS"))
				.thenReturn(Collections.emptyList());

		mockMvc.perform(get("/api/v1/payments/status/user1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSubscribed").value(false));
	}

	@Test
	void testGetSubscriptionStatus_ActiveSubscription() throws Exception {
		PaymentOrder mockOrder = PaymentOrder.builder()
				.userId("user1")
				.status("SUCCESS")
				.amount(499.0)
				.createdAt(LocalDateTime.now())
				.build();

		when(repository.findByUserIdAndStatusOrderByCreatedAtDesc("user1", "SUCCESS"))
				.thenReturn(List.of(mockOrder));

		mockMvc.perform(get("/api/v1/payments/status/user1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSubscribed").value(true));
	}

	@Test
	void testAdminGetAllPayments_Success() throws Exception {
		PaymentOrder mockOrder = PaymentOrder.builder()
				.userId("user1")
				.status("SUCCESS")
				.amount(499.0)
				.build();

		when(repository.findAll()).thenReturn(List.of(mockOrder));

		adminMockMvc.perform(get("/api/v1/admin/payments"))
				.andExpect(status().isOk());
	}
}