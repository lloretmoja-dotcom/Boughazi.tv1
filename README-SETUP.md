# Puesta en marcha

## 1. Antes de compilar, rellena estos 3 valores de ejemplo

- `app/src/main/java/com/boughazi/tv/data/SupabaseModule.kt` → `supabaseUrl` y `supabaseKey`
  (los de tu proyecto Supabase, en Project Settings → API).
- `app/src/main/java/com/boughazi/tv/BoughaziApp.kt` → `clientId` de PayPal (developer.paypal.com,
  tu app → Client ID; cambia `Environment.SANDBOX` a `Environment.LIVE` cuando quieras cobrar de verdad).
- La clave pública de Stripe se configura normalmente en tu `Application` con
  `PaymentConfiguration.init(applicationContext, "pk_live_...")` — añádela también en `BoughaziApp.kt`
  cuando tengas tu cuenta de Stripe.

Ninguna de estas claves es secreta salvo la de Stripe/PayPal en el servidor (la que usa la Edge
Function para crear el `payment intent`); esa nunca va en la app.

## 2. Ejecuta el esquema de Supabase

Copia el contenido de `supabase/schema.sql` en el SQL Editor de tu proyecto Supabase y ejecútalo.
Después marca tus 10 canales premium, por ejemplo:

```sql
update channels set is_premium = true where channel_number in (1,2,3,4,5,6,7,8,9,10);
```

## 3. Compilar por GitHub Actions

En cuanto subas este proyecto a un repositorio de GitHub (rama `main`), el workflow
`.github/workflows/build-apk.yml` se dispara solo y genera el APK. Cuando termine (pestaña
**Actions** de tu repo → la ejecución más reciente en verde), baja el archivo
`boughazi-tv-debug-apk` — es un .zip que trae el .apk dentro.

## Pendiente de completar (no bloquea la compilación, pero sí el cobro real)

- La Edge Function de Supabase que verifica el pago con Stripe/PayPal en el servidor antes de
  confiar en `is_subscribed` (ahora mismo `SubscriptionRepository` lo marca desde el propio
  cliente, válido para probar, no para producción).
- El deep link `boughazitv://reset-password` necesita declararse también como
  `intent-filter` en `LoginActivity` (o una activity dedicada) para abrir la app desde el
  enlace que llega por Gmail.
