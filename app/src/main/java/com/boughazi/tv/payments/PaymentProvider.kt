package com.boughazi.tv.payments

/** Resultado común, sea cual sea la pasarela usada. */
sealed class PaymentResult {
    data class Success(val providerReference: String) : PaymentResult()
    data class Failure(val reason: String) : PaymentResult()
    object Cancelled : PaymentResult()
}

/**
 * Contrato común para Visa (vía Stripe) y PayPal: la pantalla de suscripción
 * no necesita saber cuál de las dos está usando el usuario.
 */
interface PaymentProvider {
    suspend fun startCheckout(amountCents: Long, currency: String): PaymentResult
}
