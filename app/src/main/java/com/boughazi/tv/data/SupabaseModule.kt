package com.boughazi.tv.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Cliente único de Supabase para toda la app (Auth + Postgrest + Realtime).
 * Sustituye SUPABASE_URL y SUPABASE_ANON_KEY por los valores reales del proyecto,
 * idealmente inyectados desde local.properties / BuildConfig, nunca hardcodeados en git.
 */
object SupabaseModule {

    val client = createSupabaseClient(
        supabaseUrl = "https://TU-PROYECTO.supabase.co",
        supabaseKey = "TU-ANON-KEY"
    ) {
        install(Auth) {
            // La sesión se persiste automáticamente en disco por el SDK:
            // el usuario no tiene que volver a iniciar sesión cada vez que enciende la TV.
        }
        install(Postgrest)
        install(Realtime)
    }
}
