# Boughazi TV — App nativa Android TV / Móvil (Kotlin + Supabase)

Arquitectura para una app tipo decodificador (estilo TiviMate) centrada solo en TV en directo,
con autenticación por Gmail, 10 canales premium protegidos por suscripción (Visa/PayPal) y
mando a distancia con zapping, entrada numérica y OSD.

## 1. Paquetes

```
com.boughazi.tv
├── data/
│   ├── Models.kt              // Channel, UserProfile, SubscriptionStatus
│   ├── SupabaseModule.kt      // cliente único de Supabase (Auth + Postgrest + Realtime)
│   ├── AuthRepository.kt      // registro, login, recuperación de contraseña por Gmail
│   └── ChannelRepository.kt   // carga de canales + PlaylistEngine en memoria RAM
├── payments/
│   ├── PaymentProvider.kt     // interfaz común Visa (Stripe) / PayPal
│   ├── StripePaymentProvider.kt
│   ├── PayPalPaymentProvider.kt
│   └── SubscriptionRepository.kt // confirma el pago y desbloquea el perfil en Supabase
├── remote/
│   └── RemoteInputController.kt // D-pad, CH+/CH-, teclado numérico, foco
├── player/
│   └── PlayerManager.kt        // ExoPlayer/Media3, buffer, reconexión automática
└── ui/
    ├── OsdOverlay.kt           // banner inferior con número/logo/nombre/candado
    └── PlayerActivity.kt       // pantalla principal, conecta todo lo anterior
```

## 2. Por qué Stripe para "Visa" y no una integración directa de tarjeta

Guardar o procesar números de tarjeta directamente en la app requiere certificación PCI-DSS,
algo que ninguna app pequeña puede asumir. La forma correcta y legal de aceptar Visa es a
través de una pasarela certificada: uso **Stripe** (su `PaymentSheet` de Android tokeniza la
tarjeta sin que el número pase por tu servidor ni por tu app) junto con el **SDK de PayPal**
para la segunda opción de pago. Ambos flujos terminan en el mismo sitio: una Edge Function de
Supabase que verifica el pago con el proveedor y marca `profiles.is_subscribed = true`.

## 3. Flujo de autenticación y recuperación por Gmail

1. `AuthRepository.signUp(email, password)` → `supabase.auth.signUpWith(Email)`.
2. `AuthRepository.signIn(email, password)` → `supabase.auth.signInWith(Email)`, guarda la
   sesión (persistida automáticamente por el SDK) para no pedir login cada vez que se abre la TV.
3. `AuthRepository.sendPasswordReset(email)` → `supabase.auth.resetPasswordForEmail(email,
   redirectUrl)`. Supabase envía el correo a la cuenta de Gmail del usuario con un enlace que
   abre `boughazitv://reset-password?token=...` (deep link), donde la app pide la nueva
   contraseña y llama a `supabase.auth.modifyUser { password = nueva }`.

## 4. Verificación de canal premium

`ChannelRepository` descarga la tabla `channels` una vez al iniciar sesión y la guarda en un
`List<Channel>` en memoria (acceso instantáneo, sin ir a red en cada zapping). Al reproducir:

```
canal.is_premium == true  &&  perfil.is_subscribed == false   →  mostrar aviso de pago (candado)
en cualquier otro caso                                          →  reproducir con PlayerManager
```

`perfil.is_subscribed` se relee de Supabase (o de una escucha Realtime) para que, en cuanto la
Edge Function confirme el pago, el canal se desbloquee sin tener que reiniciar la app.

## 5. Esquema de base de datos

Ver `supabase/schema.sql`: tablas `channels` y `profiles`, con Row Level Security para que cada
usuario solo pueda leer/editar su propio perfil, y los canales sean de lectura pública.

## 6. Siguiente paso

Este paquete de código está pensado para integrarse en tu repositorio existente
(`boughazi-android`), no para sustituirlo entero — hay nombres de clase que ya usas
(`AuthRepository`, `MainActivity`) y conviene fusionar en vez de sobrescribir a ciegas. En
cuanto me des acceso al repo (token o copia manual, como hablamos), reviso tus archivos reales
y te preparo el diff exacto en vez de un paquete genérico.
