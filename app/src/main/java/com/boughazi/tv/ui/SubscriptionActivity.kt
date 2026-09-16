package com.boughazi.tv.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.boughazi.tv.databinding.ActivitySubscriptionBinding
import com.boughazi.tv.payments.PayPalPaymentProvider
import com.boughazi.tv.payments.PaymentResult
import com.boughazi.tv.payments.StripePaymentProvider
import com.boughazi.tv.payments.SubscriptionRepository
import kotlinx.coroutines.launch

/**
 * Pantalla de suscripción: el usuario elige Visa (Stripe) o PayPal para desbloquear
 * los 10 canales premium. Precio de ejemplo: 4,99 EUR/mes (ajusta amountCents/currency
 * a tu plan real).
 */
class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private val subscriptionRepository = SubscriptionRepository()

    // PaymentSheet debe crearse antes de que la Activity llegue a STARTED.
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var stripeProvider: StripePaymentProvider
    private lateinit var payPalProvider: PayPalPaymentProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paymentSheet = PaymentSheet(this) { result -> stripeProvider.onPaymentSheetResult(result) }
        stripeProvider = StripePaymentProvider(paymentSheet)
        payPalProvider = PayPalPaymentProvider(binding.payPalButton)

        binding.stripeButton.setOnClickListener { checkout(stripeProvider, "stripe") }

        // PayPalButton arma su propio flujo (crear orden / capturar pago) en cuanto se
        // llama a setup() dentro de startCheckout(); por eso se dispara ya aquí, al abrir
        // la pantalla, y no espera a un clic nuestro — el propio botón de PayPal gestiona
        // su clic internamente.
        checkout(payPalProvider, "paypal")
    }

    private fun checkout(provider: com.boughazi.tv.payments.PaymentProvider, name: String) {
        lifecycleScope.launch {
            binding.subscriptionStatusText.text = "Procesando pago…"
            when (val result = provider.startCheckout(amountCents = 499, currency = "EUR")) {
                is PaymentResult.Success -> {
                    subscriptionRepository.activateSubscription(name, result.providerReference)
                    binding.subscriptionStatusText.text = "¡Suscripción activada!"
                    finish()
                }
                is PaymentResult.Failure -> binding.subscriptionStatusText.text = "Pago fallido: ${result.reason}"
                PaymentResult.Cancelled -> binding.subscriptionStatusText.text = "Pago cancelado"
            }
        }
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }
}
