package com.smartlogix.bff.service;

import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.core.PayPalHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    /**
     * Crea una orden PayPal y retorna el approveUrl para redirigir al usuario.
     */
    public Order crearOrden(String monto, String moneda, String descripcion) throws IOException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName("SmartLogix")
                .landingPage("BILLING")
                .userAction("PAY_NOW");

        orderRequest.applicationContext(applicationContext);

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description(descripcion)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(moneda)
                        .value(monto));

        orderRequest.purchaseUnits(List.of(purchaseUnit));

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.prefer("return=representation");
        request.requestBody(orderRequest);

        HttpResponse<Order> response = payPalHttpClient.execute(request);
        log.info("Orden PayPal creada. ID: {}, Status: {}", response.result().id(), response.result().status());
        return response.result();
    }

    /**
     * Captura el pago de una orden PayPal ya aprobada por el usuario.
     */
    public Order capturarOrden(String orderId) throws IOException {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.prefer("return=representation");
        request.requestBody(new OrderRequest());

        HttpResponse<Order> response = payPalHttpClient.execute(request);
        log.info("Orden PayPal capturada. ID: {}, Status: {}", response.result().id(), response.result().status());
        return response.result();
    }

    /**
     * Extrae la URL de aprobación del usuario desde la lista de links de la orden.
     */
    public String obtenerApproveUrl(Order order) {
        return order.links().stream()
                .filter(link -> "approve".equals(link.rel()))
                .findFirst()
                .map(com.paypal.orders.LinkDescription::href)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el link 'approve' en la orden PayPal"));
    }
}
