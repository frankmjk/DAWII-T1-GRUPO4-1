# DAWII T1 – Grupo 4 · PayGo Perú

Evaluación de Laboratorio T1 – **4697 Desarrollo de Aplicaciones Web II** (CIBERTEC, 2026-SET).

**Grupo 4**
- Jennyfer Mesta Wong
- _(agregar integrantes)_

La fintech **PAYGO PERÚ** emite y gestiona tarjetas prepago. Este repositorio implementa la comunicación entre las áreas de **Tarjetas**, **Recargas** y **Riesgo** con microservicios Spring Boot.

## Arquitectura

```
                 OpenFeign (síncrono)                 RabbitMQ (asíncrono)
 ms-recargas ───────────────────────► ms-tarjetas     ms-recargas ──► [Mesta_Queue] ──► ms-riesgo
   :8085      GET /tarjetas/{id}         :8084          publica          cola           consume y
                                                                                        registra en
                                                                                        tabla analisis
```

| Proyecto | Puerto | Rol | Tecnologías |
|---|---|---|---|
| `ms-tarjetas` | 8084 | Proveedor: registra y consulta tarjetas | Spring Web, JPA, MySQL, Lombok |
| `ms-recargas` | 8085 | Consumidor Feign + productor RabbitMQ | Spring Web, JPA, OpenFeign, AMQP |
| `ms-riesgo` | 8086 | Consumidor RabbitMQ + listado de análisis | Spring Web, JPA, AMQP |

Stack: Java 17 · Spring Boot 3.2.5 · Spring Cloud 2023.0.1 · Gradle 8.7 · MySQL 8 · RabbitMQ 3.

## Pregunta 1 – Comunicación síncrona (OpenFeign)

**ms-tarjetas** – tabla `tarjetas` (`id_tarjeta`, `nom_titular`, `saldo_asignado`, `saldo_disponible`)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/tarjetas` | Registrar tarjeta |
| GET | `/tarjetas` | Listar tarjetas |
| GET | `/tarjetas/{id}` | Consultar tarjeta por id (404 si no existe) |

**ms-recargas** – tabla `recargas` (`id_recarga`, `id_tarjeta`, `saldo_disponible`, `monto_recarga`, `fecha_recarga`)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/recargas` | Registrar solicitud de recarga |
| GET | `/recargas` | Listar recargas |
| GET | `/recargas/{id}` | Consultar recarga por id |

Flujo de `POST /recargas` (body: `{ "idTarjeta": 1, "montoRecarga": 500.00 }`):
1. Valida la tarjeta llamando a `ms-tarjetas` con **OpenFeign** (`TarjetaClient`). Si no existe → **404** y no se registra.
2. Registra la recarga con el **saldo disponible obtenido de ms-tarjetas** y `fecha_recarga = LocalDateTime.now()`.

## Pregunta 2 – Comunicación asíncrona (RabbitMQ)

3. Tras registrar la recarga, `ms-recargas` publica el mensaje (`RecargaEvent`, JSON) en la cola **`Mesta_Queue`**.
4. `ms-riesgo` consume la cola (`@RabbitListener`) y registra en la tabla `analisis`
   (`id_recarga`, `id_tarjeta`, `saldo_disponible`, `monto_recarga`, `fecha_recarga`, `situacion`):
   - **APROBADA**: `monto_recarga <= 70% * saldo_disponible`
   - **OBSERVADA**: `monto_recarga > 70% * saldo_disponible`
5. `GET /analisis` (ms-riesgo) lista el contenido de la tabla `analisis`.

El nombre de la cola se configura en `riesgo.queue` del `application.yml` de ms-recargas y ms-riesgo.

## Cómo ejecutar

1. Levantar infraestructura (Docker Desktop abierto):
   ```bash
   cd database && docker compose up -d                              # MySQL :5510 (appdb / app / password), phpMyAdmin :3410
   cd queue && docker compose -f docker-compose-rabbitmq.yml up -d  # RabbitMQ :5672, consola http://localhost:15672 (guest/guest)
   ```
2. Levantar los microservicios (cada uno en su carpeta), en este orden:
   ```bash
   cd ms-tarjetas && gradlew.bat bootRun
   cd ms-riesgo   && gradlew.bat bootRun
   cd ms-recargas && gradlew.bat bootRun
   ```
   `ms-tarjetas` carga 2 tarjetas de ejemplo si la tabla está vacía.
3. Importar en Postman `postman/DAWII-T1-PayGo.postman_collection.json`.

## Casos de prueba

| Caso | Request | Resultado esperado |
|---|---|---|
| Recarga aprobada | tarjeta 1 (saldo 800), monto 500 | 201 → análisis `APROBADA` (62.5 %) |
| Recarga observada | tarjeta 2 (saldo 200), monto 180 | 201 → análisis `OBSERVADA` (90 %) |
| Tarjeta inexistente | tarjeta 999 | 404 `La tarjeta 999 no existe. No se registro la recarga` |
