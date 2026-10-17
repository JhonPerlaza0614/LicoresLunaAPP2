# Licores Luna App 🍷

Aplicación móvil para Android desarrollada en Kotlin que permite la autenticación de usuarios conectados a un backend robusto en **Supabase**.

---

## Características Principales

- **Supabase como Backend**: Mediante el uso de Triggers, RLS (Row Level Security) e índices, Supabase controla y asegura las acciones dentro del sistema.
- **Autenticación Segura**: Inicio de sesión mediante correo electrónico y contraseña utilizando **Supabase Auth (GoTrue)**.
- **Gestión de Roles**: Verificación automática de privilegios para poder ingresar al sistema de gestión del negocio.
- **Sesiones Persistentes**: Control de sesión local para mantener al usuario conectado de forma segura.
- **Arquitectura y Vistas**: Implementación basada en Activities con **View Binding** y componentes de Material Design.

---

## Requisitos del Sistema y Tecnologías

- **Android Studio**: Versión recomendada (Jellyfish / Koala o superior).
- **JDK**: JDK 21 o superior (Java Compatibility configurado en versión 21).
- **Android SDK**:
  - `minSdk`: 24 (Android 7.0 o superior)
  - `targetSdk`: 27
  - `compileSdk`: 35
- **Lenguaje**: Kotlin 2.0.21

---

## 📦 Dependencias Principales

El proyecto utiliza las siguientes librerías y tecnologías integradas en `app/build.gradle.kts`:

- **Supabase Kotlin SDK (v2.1.0)**:
  - `postgrest-kt`
  - `gotrue-kt`
- **Ktor Client (v2.3.7)**: `ktor-client-android`
- **Kotlinx Serialization (v1.6.0)**: Para serialización de datos JSON.
- **AndroidX & Material Design**:
  - `core-ktx` (v1.12.0)
  - `appcompat` (v1.6.1)
  - `material` (v1.11.0)
  - `activity-ktx` (v1.8.2)
  - `constraintlayout` (v2.1.4)
  - `lifecycle-runtime-ktx` (v2.8.7)

---

## ⚙️ Configuración y Ejecución

1. Clona el repositorio en tu máquina local.
2. Abre el proyecto en **Android Studio**.
3. Sincroniza el proyecto con Gradle (`Sync Project with Gradle Files`).
4. Configura tus credenciales o claves de Supabase en el cliente (`SupabaseClient.kt`).
5. Ejecuta la aplicación en un emulador Android (API 24 o superior) o dispositivo físico.
