package com.smartlogix.bff.service;

import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.core.PayPalHttpClient;
import com.smartlogix.bff.client.InventarioClient;
import com.smartlogix.bff.model.ItemCarritoDTO;
import com.smartlogix.bff.model.ProductoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;
    private final InventarioClient inventarioClient;

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    /**
     * Crea una orden PayPal con el desglose real del carrito (un PayPal Item por SKU)
     * y retorna el approveUrl para redirigir al usuario.
     * El precio de cada producto se vuelve a consultar al inventario (nunca se confía
     * en un monto que venga del frontend).
     */
    public Order crearOrden(List<ItemCarritoDTO> itemsCarrito, String moneda) throws IOException {
        if (itemsCarrito == null || itemsCarrito.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        List<Item> paypalItems = new ArrayList<>();
        BigDecimal totalCarrito = BigDecimal.ZERO;

        for (ItemCarritoDTO itemCarrito : itemsCarrito) {
            ProductoDTO producto = inventarioClient.obtenerPorSku(itemCarrito.getSkuProducto());

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(itemCarrito.getCantidad()));
            totalCarrito = totalCarrito.add(subtotal);

            paypalItems.add(new Item()
                    .name(producto.getNombre())
                    .sku(producto.getSku())
                    .unitAmount(new Money().currencyCode(moneda).value(producto.getPrecio().toPlainString()))
                    .quantity(String.valueOf(itemCarrito.getCantidad())));
        }

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
                .description("Compra SmartLogix (" + itemsCarrito.size() + " producto(s))")
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(moneda)
                        .value(totalCarrito.toPlainString())
                        .amountBreakdown(new AmountBreakdown()
                                .itemTotal(new Money().currencyCode(moneda).value(totalCarrito.toPlainString()))))
                .items(paypalItems);

        orderRequest.purchaseUnits(List.of(purchaseUnit));

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.prefer("return=representation");
        request.requestBody(orderRequest);

        HttpResponse<Order> response = payPalHttpClient.execute(request);
        log.info("Orden PayPal creada. ID: {}, Status: {}, Total: {}",
                response.result().id(), response.result().status(), totalCarrito);
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