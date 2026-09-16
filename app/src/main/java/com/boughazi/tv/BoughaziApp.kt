package com.boughazi.tv

import android.app.Application
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment

/**
 * Configura el SDK de PayPal una sola vez al arrancar la app.
 * Sustituye TU-CLIENT-ID-DE-PAYPAL por el Client ID real de tu app en developer.paypal.com,
 * y cambia Environment.SANDBOX por Environment.LIVE cuando esté listo para cobrar de verdad.
 */
class BoughaziApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PayPalCheckout.setConfig(
            CheckoutConfig(
                application = this,
                clientId = "TU-CLIENT-ID-DE-PAYPAL",
                environment = Environment.SANDBOX
            )
        )
    }
}
