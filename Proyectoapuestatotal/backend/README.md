# Apuesta Total — Backend (Avance 3)

Arquitectura inicial del proyecto en **Java 17+**, aplicando **MVC, DAO, SOLID, TDD** y
consideraciones de **seguridad**. El front (`index.html`, `login.html`, `registro.html`) es la
**Vista** web; este backend es el **Modelo + Controlador** y expone una API REST en JSON.

> Todo el dinero es **simulado** (proyecto académico).

## 1. Cómo ejecutarlo

Requisitos: JDK 17 o superior y Maven 3.8+.

```bash
cd backend
mvn test                 # corre las 129 pruebas
mvn compile exec:java    # levanta la API en http://localhost:8080
```

Por defecto usa H2 en memoria (modo PostgreSQL), así que no necesitas instalar nada más.
Para crear un administrador (puede finalizar partidos):

```bash
# Linux / macOS
APUESTATOTAL_ADMIN_DNI=99999999 mvn compile exec:java
# Windows (PowerShell)
$env:APUESTATOTAL_ADMIN_DNI="99999999"; mvn compile exec:java
```

Variables de entorno disponibles (todas opcionales en desarrollo):

| Variable | Para qué sirve |
|---|---|
| `APUESTATOTAL_ENV` | `dev` (defecto) o `prod` |
| `APUESTATOTAL_PORT` | puerto HTTP (8080) |
| `APUESTATOTAL_DB_URL` / `_DB_USER` / `_DB_PASSWORD` | base de datos (ej. `jdbc:postgresql://localhost:5432/apuestatotal`) |
| `APUESTATOTAL_PEPPER` | secreto para proteger los DNI (obligatorio en `prod`, mínimo 32 caracteres) |
| `APUESTATOTAL_CORS_ORIGIN` | origen del front permitido (ej. `http://localhost:5500`) |
| `APUESTATOTAL_ADMIN_DNI` | DNI del administrador inicial |

## 2. Arquitectura (MVC + DAO en capas)

```
 Vista (HTML/JS)  ──HTTP/JSON──▶  Controlador ──▶ Servicio ──▶ DAO (interfaz) ──▶ Base de datos
 index/login/registro             com.apuestatotal.web   service     dao            H2 / PostgreSQL
                                  + vista (DTOs JSON)    (negocio)   ├─ jdbc  (real)
                                                                     └─ memoria (pruebas)
```

| Paquete | Rol | Qué contiene |
|---|---|---|
| `model` | **M**odelo | `Usuario`, `Partido`, `Apuesta`, `Seleccion` y enums (inmutables, `record`) |
| `vista` | **V**ista (API) | DTOs de entrada (`Solicitudes`) y salida (`Vistas`): nunca se expone el hash del DNI |
| `web` | **C**ontrolador | `AutenticacionController`, `BilleteraController`, `PartidoController`, `ApuestaController`, `Enrutador` |
| `service` | Lógica de negocio | interfaces + `*Impl`, estrategias de apuesta, liquidador |
| `dao` / `dao.jdbc` / `dao.memoria` | **DAO** | interfaces y dos implementaciones intercambiables |
| `seguridad` | Seguridad | hash HMAC del DNI, sesiones, límite de intentos, validadores |
| `config` | Configuración | lectura de `application.properties` y variables de entorno |
| `Aplicacion` | Raíz de composición | único lugar donde se hace `new` de las clases concretas |

### Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/registro` `{"dni":"12345678"}` | público | registro solo con DNI (+ S/ 1000 simulados) |
| `POST /api/auth/login` `{"dni":"12345678"}` | público | devuelve `token` Bearer |
| `POST /api/auth/logout` | sesión | invalida el token |
| `GET /api/partidos` | público | partidos y cuotas |
| `GET /api/billetera` | sesión | saldo simulado |
| `POST /api/billetera/recarga` `{"monto":500}` | sesión | recarga simulada (0.01 – 10000) |
| `POST /api/apuestas` `{"tipo":"MULTIPLE","monto":30,"selecciones":[{"partidoId":1,"opcion":"LOCAL"}]}` | sesión | registra el cupón y descuenta saldo |
| `GET /api/apuestas?estado=PENDIENTE` | sesión | historial del usuario (persiste entre sesiones) |
| `POST /api/admin/partidos/{id}/finalizar` `{"golesLocal":2,"golesVisita":1}` | admin | cierra el partido, liquida y paga |

Las peticiones autenticadas llevan `Authorization: Bearer <token>`.

## 3. Principios aplicados (evidencia en el código)

### SOLID
| Principio | Dónde se ve |
|---|---|
| **S** – Responsabilidad única | `LiquidadorApuestas` solo evalúa; `HasheadorDniHmac` solo protege el DNI; `Enrutador` solo despacha y aplica seguridad común; cada DAO maneja una sola tabla |
| **O** – Abierto/cerrado | `EstrategiaApuesta` (`EstrategiaSimple`, `EstrategiaMultiple`): para agregar apuestas tipo SISTEMA se crea otra clase y se registra en `Aplicacion`, sin tocar `ApuestaServiceImpl`. `Controlador` permite sumar controladores sin modificar el enrutador |
| **L** – Sustitución de Liskov | `ContratoDaoTest` es un mismo conjunto de pruebas que se ejecuta contra `DaoMemoriaTest` y `DaoJdbcTest`: ambas implementaciones son intercambiables |
| **I** – Segregación de interfaces | Interfaces pequeñas y específicas: `UsuarioDao`, `BilleteraDao`, `PartidoDao`, `ApuestaDao`, `AlmacenSesiones`, `HasheadorDni`, etc. |
| **D** – Inversión de dependencias | Los servicios dependen de interfaces (`UsuarioDao`, `UnidadDeTrabajo`, `Clock`…), nunca de JDBC. `Aplicacion` inyecta las implementaciones por constructor |

### DAO
Cada tabla tiene su interfaz (`dao/*.java`) y su implementación JDBC (`dao/jdbc/*DaoJdbc.java`) con
`PreparedStatement`. `UnidadDeTrabajo` / `GestorConexiones` agrupan varias operaciones en **una
transacción** (commit/rollback): por ejemplo, apostar descuenta el saldo y guarda la apuesta
juntos o no hace nada (probado en `TransaccionJdbcTest`).

### TDD
Se trabajó con el ciclo **Rojo → Verde → Refactor**: primero la prueba que falla (por ejemplo
"con saldo insuficiente no se guarda nada"), luego el mínimo código para pasarla y después la limpieza.
Resultado: **129 pruebas JUnit 5**, todas en verde.

| Nivel | Clases de prueba |
|---|---|
| Unitarias (modelo, seguridad, config) | `ModeloTest`, `ValidadorDniTest`, `HasheadorDniHmacTest`, `AlmacenSesionesMemoriaTest`, `LimitadorIntentosTest`, `ConfiguracionTest` |
| Unitarias de servicios (DAO en memoria como *test double*) | `AutenticacionServiceTest`, `BilleteraServiceTest`, `ApuestaServiceTest`, `PartidoServiceTest`, `EstrategiasYLiquidadorTest` |
| Contrato DAO (memoria y JDBC) | `ContratoDaoTest` → `DaoMemoriaTest`, `DaoJdbcTest`, `TransaccionJdbcTest` |
| Integración extremo a extremo (servidor real + HTTP) | `ApiIntegracionTest`, `LimiteIntentosApiTest` |

El reloj (`java.time.Clock`) se inyecta para probar expiración de sesiones y ventanas de intentos sin esperar.

## 4. Seguridad

| Amenaza | Medida |
|---|---|
| SQL Injection | Solo `PreparedStatement`; entradas validadas con lista blanca (`ValidadorDni`, enums estrictos). Pruebas con `' OR '1'='1` |
| DNI expuesto si roban la BD | El DNI **no se guarda**: solo `HMAC-SHA256(dni, pepper)` + versión enmascarada `12****78`. El pepper vive en una variable de entorno |
| Robo / adivinación de tokens | Token de 256 bits con `SecureRandom`; en memoria solo se guarda su SHA-256; expiran (2 h) y se invalidan al cerrar sesión |
| Fuerza bruta / enumeración de DNI | `LimitadorIntentos` (ventana deslizante por IP): 5 fallos de login / 15 min y 10 registros / hora |
| Manipulación de cuotas o montos | El cliente solo envía IDs y opción: **las cuotas y la ganancia las calcula el servidor**. Montos validados (1 – 5000, máx. 2 decimales, tope de ganancia) |
| Sobregiro por concurrencia | `UPDATE ... WHERE saldo >= ?` atómico + transacción (probado con 20 hilos) |
| Dinero con errores de redondeo | `BigDecimal` en todo el dinero y las cuotas |
| Escalada de privilegios | Autorización por rol en el `Enrutador` (403 si no es ADMIN) |
| Fuga de información en errores | Los errores devuelven un mensaje genérico; el detalle SQL/stack solo va al log |
| Cuerpos gigantes / DoS simple | Tope de 10 KB por petición (413) |
| XSS / clickjacking / sniffing | Cabeceras `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Cache-Control: no-store` |
| CORS abierto | Solo se permite el origen configurado |
| Secretos en el código | Credenciales y pepper por variables de entorno; en `prod` el arranque falla si faltan; `Configuracion.toString()` no los imprime |

## 5. Recursos de Java utilizados

| Recurso | Uso |
|---|---|
| `java.sql` (JDBC, `PreparedStatement`, transacciones, try-with-resources) | capa DAO |
| `java.security` / `javax.crypto` (`SecureRandom`, `MessageDigest`, `Mac`) | tokens y huella del DNI |
| `java.math.BigDecimal` | dinero y cuotas |
| `java.time` (`Instant`, `Clock`, `Duration`, `OffsetDateTime`) | fechas, expiración y pruebas sin esperas |
| `record`, `enum`, `switch` con expresiones, `List.copyOf` | modelo inmutable |
| Collections y Streams (`Map`, `ConcurrentHashMap`, `Collectors`) | servicios y almacenes |
| `Optional` | búsquedas sin `null` |
| `java.util.concurrent` (`ReentrantLock`, `ExecutorService`, `AtomicLong`) | concurrencia segura |
| `java.util.logging` | registro de errores sin datos sensibles |
| `com.sun.net.httpserver` y `java.net.http.HttpClient` | servidor embebido y pruebas HTTP, sin frameworks |
| `java.util.Properties`, `java.io`/`java.nio.charset` | configuración y lectura de `schema.sql` |
| JavaDoc | todas las clases y métodos públicos documentados |

## 6. Conectar el front (opcional)

El front actual guarda todo en `localStorage` (demo). Para usar esta API, cambia en `login.html`,
`registro.html` e `index.html` las funciones de `localStorage` por `fetch('http://localhost:8080/api/...')`
enviando el token en `Authorization`. Abre el front con un servidor local (por ejemplo Live Server en el
puerto 5500) o ajusta `APUESTATOTAL_CORS_ORIGIN`.

## 7. Límites conocidos (honestidad)

- Como pidió el proyecto, el **login es solo con DNI**: no hay segundo factor, así que quien conozca un
  DNI registrado puede entrar con él. Se mitiga con límites por IP, pero para dinero real haría falta
  otro factor (clave, OTP).
- Las sesiones viven en memoria: al reiniciar el servidor hay que volver a iniciar sesión.
- El esquema `schema.sql` es un subconjunto simplificado; el diseño completo con todos los mercados sigue en `../apuesta_total_schema.sql`.
