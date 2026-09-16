package com.boughazi.tv.payments

import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.OrderIntent
import com.paypal.checkout.createorder.UserAction
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.OrderRequest
import com.paypal.checkout.order.PurchaseUnit
import com.paypal.checkout.paymentbuttons.PayPalButton
import kotlinx.coroutines.CompletableDeferred

/**
 * Pago con PayPal usando el SDK oficial de PayPal Checkout para Android.
 * El botón (`PayPalButton`) se coloca en la pantalla de suscripción; aquí solo
 * se define qué pasa al crear la orden y al aprobarla.
 */
class PayPalPaymentProvider(
    private val button: PayPalButton
) : PaymentProvider {

    private var pendingResult: CompletableDeferred<PaymentResult>? = null

    override suspend fun startCheckout(amountCents: Long, currency: String): PaymentResult {
        val deferred = CompletableDeferred<PaymentResult>()
        pendingResult = deferred

        val amountValue = String.format("%.2f", amountCents / 100.0)

        button.setup(
            CreateOrder { createOrderActions ->
                val order = OrderRequest(
                    intent = OrderIntent.CAPTURE,
                    appContext = AppContext(userAction = UserAction.PAY_NOW),
                    purchaseUnitList = listOf(
                        PurchaseUnit(
                            amount = Amount(currencyCode = CurrencyCode.valueOf(currency), value = amountValue)
                        )
                    )
                )
                createOrderActions.create(order)
            },
            OnApprove { approval ->
                approval.orderActions.capture { captureOrderResult ->
                    pendingResult?.complete(
                        PaymentResult.Success(providerReference = captureOrderResult.orderId)
                    )
                    pendingResult = null
                }
            }
        )
        return deferred.await()
    }

    fun onCancel() {
        pendingResult?.complete(PaymentResult.Cancelled)
        pendingResult = null
    }

    fun onError(message: String) {
        pendingResult?.complete(PaymentResult.Failure(message))
        pendingResult = null
    }
}
