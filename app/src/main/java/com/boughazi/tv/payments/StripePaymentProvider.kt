package com.boughazi.tv.payments

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pago con tarjeta Visa (y el resto de tarjetas que acepte Stripe) usando PaymentSheet:
 * el número de tarjeta lo tokeniza el SDK de Stripe, nunca pasa por nuestro servidor
 * ni por nuestra app en texto plano (requisito de cumplimiento PCI-DSS).
 *
 * 1) Pedimos un "payment intent" a una Edge Function de Supabase (create-payment-intent),
 *    que es la única pieza que conoce la clave secreta de Stripe.
 * 2) Abrimos PaymentSheet con el client_secret recibido.
 * 3) Si el pago se confirma, Stripe dispara un webhook a Supabase que activa la suscripción
 *    (ver supabase/schema.sql y la función `verify-subscription`).
 */
class StripePaymentProvider(
    private val paymentSheet: PaymentSheet,
    private val client = com.boughazi.tv.data.SupabaseModule.client
) : PaymentProvider {

    @Serializable
    private data class IntentResponse(val clientSecret: String, val customerId: String, val ephemeralKey: String)

    private var pendingResult: CompletableDeferred<PaymentResult>? = null

    override suspend fun startCheckout(amountCents: Long, currency: String): PaymentResult {
        val deferred = CompletableDeferred<PaymentResult>()
        pendingResult = deferred

        val response = client.functions.invoke(
            function = "create-payment-intent",
            body = mapOf("amount" to amountCents, "currency" to currency)
        )
        val intent = Json.decodeFromString<IntentResponse>(response.body?.string() ?: "{}")

        paymentSheet.presentWithPaymentIntent(
            intent.clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "Boughazi TV",
                customer = PaymentSheet.CustomerConfiguration(
                    id = intent.customerId,
                    ephemeralKeySecret = intent.ephemeralKey
                )
            )
        )
        return deferred.await()
    }

    /** Se llama desde el callback de PaymentSheet registrado en la Activity/Fragment. */
    fun onPaymentSheetResult(result: PaymentSheetResult) {
        val resolved = when (result) {
            is PaymentSheetResult.Completed -> PaymentResult.Success(providerReference = "stripe")
            is PaymentSheetResult.Canceled -> PaymentResult.Cancelled
            is PaymentSheetResult.Failed -> PaymentResult.Failure(result.error.message ?: "Pago fallido")
        }
        pendingResult?.complete(resolved)
        pendingResult = null
    }
}
