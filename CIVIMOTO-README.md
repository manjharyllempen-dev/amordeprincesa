# CiviMoto

Sistema de mototaxi con tres aplicaciones Android: Pasajero, Chofer y Administrador.

## Beta Android 0.9
- Marca CiviMoto y logo oficial amarillo/negro.
- Splash dedicado en los tres APK.
- GPS del teléfono en Pasajero y Chofer.
- Mapa dentro de la app con MapLibre + OpenFreeMap (sin API key).
- Pasajero: origen, destino, tarifa, Yape/efectivo, solicitar, compartir y cancelar.
- Chofer: online/offline, solicitud con origen/destino, aceptar/rechazar, estados del viaje, ganancias y pago semanal/mensual por Yape.
- Administrador: métricas, mapa operativo, viajes, choferes, pagos y configuración de tarifas.

## Arquitectura objetivo de producción
- Android/PWA: CiviMoto Pasajero, Chofer y Administrador.
- Backend: Supabase/PostgreSQL + RLS + Realtime.
- Geoespacial: PostGIS para choferes cercanos.
- Máquina de estados de viaje: SOLICITADO -> BUSCANDO_CHOFER -> ACEPTADO -> CHOFER_EN_CAMINO -> CHOFER_LLEGO -> EN_VIAJE -> COMPLETADO/CANCELADO.
- Pagos Pasajero->Chofer: efectivo o Yape.
- Suscripción Chofer->CiviMoto: Yape semanal o mensual con aprobación del administrador.
- Push: Firebase Cloud Messaging cuando se conecte un proyecto Firebase.
- Mapas: la beta usa OpenFreeMap; el proveedor se puede sustituir por Google Maps cuando exista una API key restringida.

## Seguridad de producción
No almacenar service-role keys, contraseñas de Yape ni secretos administrativos en el APK. Las operaciones sensibles deben validarse en servidor y por RLS.

## Compilación
GitHub Actions genera:
- CiviMoto-Pasajero.apk
- CiviMoto-Chofer.apk
- CiviMoto-Administrador.apk
