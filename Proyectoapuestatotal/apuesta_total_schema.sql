-- =====================================================================
-- APUESTA TOTAL - Base de datos de apuestas deportivas
-- Motor: PostgreSQL 14+
-- Curso Integrador I (UTP) - Stack: Spring Boot + Angular + PostgreSQL
-- =====================================================================
-- Diseño pensado para actualización EN TIEMPO REAL de cuotas/mercados:
--   - Toda tabla de cuotas tiene "actualizado_en" + trigger automático.
--   - "cuota_historial" guarda cada cambio de cuota (para graficar
--     movimiento de cuotas y para que el backend emita eventos por
--     WebSocket/STOMP cada vez que se inserta una fila nueva).
--   - Los mercados están modelados de forma GENÉRICA (catálogo de
--     tipos de mercado + opciones), así puedes agregar mercados nuevos
--     sin tener que crear tablas nuevas cada vez.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. EXTENSIONES Y TIPOS
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE estado_partido AS ENUM (
    'PROGRAMADO',   -- aún no empieza
    'EN_VIVO',      -- se está jugando
    'ENTRETIEMPO',
    'FINALIZADO',
    'SUSPENDIDO',
    'CANCELADO',
    'POSPUESTO'
);

CREATE TYPE estado_mercado AS ENUM (
    'ABIERTO',      -- se puede apostar
    'SUSPENDIDO',   -- pausado temporalmente (ej. gol reciente)
    'CERRADO',      -- ya no se puede apostar
    'LIQUIDADO'     -- ya se pagó a los ganadores
);

CREATE TYPE resultado_opcion AS ENUM (
    'PENDIENTE',
    'GANADA',
    'PERDIDA',
    'ANULADA',      -- push / devolución de dinero
    'MEDIO_GANADA', -- usado en hándicaps asiáticos
    'MEDIO_PERDIDA'
);

CREATE TYPE estado_apuesta AS ENUM (
    'PENDIENTE',
    'GANADA',
    'PERDIDA',
    'ANULADA',
    'CASHOUT'       -- cobrada anticipadamente
);

CREATE TYPE tipo_documento_identidad AS ENUM (
    'DNI',
    'PASAPORTE',
    'CARNE_EXTRANJERIA'
);

CREATE TYPE tipo_apuesta AS ENUM (
    'SIMPLE',       -- una sola selección
    'COMBINADA',    -- varias selecciones (parlay)
    'SISTEMA'
);

CREATE TYPE tipo_transaccion AS ENUM (
    'DEPOSITO',
    'RETIRO',
    'APUESTA',
    'PREMIO',
    'CASHOUT',
    'BONO',
    'AJUSTE'
);

-- ---------------------------------------------------------------------
-- 1. USUARIOS Y BILLETERA
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_documento      tipo_documento_identidad NOT NULL,
    numero_documento    VARCHAR(20) NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    correo              VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash     VARCHAR(255) NOT NULL,
    telefono            VARCHAR(20),
    fecha_nacimiento    DATE NOT NULL,
    genero              VARCHAR(30),
    direccion           VARCHAR(200),
    departamento        VARCHAR(80),
    provincia           VARCHAR(80),
    distrito            VARCHAR(80),
    pais                VARCHAR(60) DEFAULT 'Perú',
    verificado          BOOLEAN NOT NULL DEFAULT FALSE,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    rol                 VARCHAR(20) NOT NULL DEFAULT 'CLIENTE', -- CLIENTE, ADMIN, TRADER
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE billetera (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id      UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    saldo_real      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo_real >= 0),
    saldo_bono      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo_bono >= 0),
    moneda          VARCHAR(3) NOT NULL DEFAULT 'PEN',
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, moneda)
);

CREATE TABLE transaccion (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    billetera_id    UUID NOT NULL REFERENCES billetera(id) ON DELETE CASCADE,
    tipo            tipo_transaccion NOT NULL,
    monto           NUMERIC(12,2) NOT NULL,
    saldo_resultante NUMERIC(12,2) NOT NULL,
    referencia_id   UUID, -- id de la apuesta u operación relacionada
    descripcion     VARCHAR(255),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 2. CATÁLOGO DEPORTIVO
-- ---------------------------------------------------------------------
CREATE TABLE deporte (
    id      SMALLSERIAL PRIMARY KEY,
    nombre  VARCHAR(60) NOT NULL UNIQUE,   -- Fútbol, Básquet, Tenis, Vóley...
    icono   VARCHAR(255),
    orden   SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE pais (
    id      SMALLSERIAL PRIMARY KEY,
    nombre  VARCHAR(80) NOT NULL UNIQUE,
    bandera VARCHAR(255)
);

CREATE TABLE liga (
    id          SERIAL PRIMARY KEY,
    deporte_id  SMALLINT NOT NULL REFERENCES deporte(id),
    pais_id     SMALLINT REFERENCES pais(id),
    nombre      VARCHAR(120) NOT NULL,     -- Liga 1, Premier League, Champions...
    temporada   VARCHAR(20),               -- 2026, 2026-2027
    logo        VARCHAR(255),
    activa      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE equipo (
    id          SERIAL PRIMARY KEY,
    deporte_id  SMALLINT NOT NULL REFERENCES deporte(id),
    nombre      VARCHAR(120) NOT NULL,
    abreviatura VARCHAR(10),
    escudo      VARCHAR(255),
    pais_id     SMALLINT REFERENCES pais(id)
);

-- ---------------------------------------------------------------------
-- 3. PARTIDOS / EVENTOS
-- ---------------------------------------------------------------------
CREATE TABLE partido (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    liga_id         INTEGER NOT NULL REFERENCES liga(id),
    equipo_local_id     INTEGER NOT NULL REFERENCES equipo(id),
    equipo_visitante_id INTEGER NOT NULL REFERENCES equipo(id),
    fecha_hora_inicio  TIMESTAMPTZ NOT NULL,
    estado          estado_partido NOT NULL DEFAULT 'PROGRAMADO',
    minuto_actual   SMALLINT,              -- minuto del partido si está EN_VIVO
    marcador_local  SMALLINT NOT NULL DEFAULT 0,
    marcador_visita SMALLINT NOT NULL DEFAULT 0,
    destacado       BOOLEAN NOT NULL DEFAULT FALSE,   -- para la portada (Fecha 01, etc.)
    etiqueta        VARCHAR(60),           -- "Fecha 01", "Cuartos de final"...
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (equipo_local_id <> equipo_visitante_id)
);

CREATE INDEX idx_partido_estado_fecha ON partido (estado, fecha_hora_inicio);
CREATE INDEX idx_partido_liga ON partido (liga_id);

-- Eventos dentro del partido (para el timeline en vivo: goles, tarjetas...)
CREATE TABLE partido_evento (
    id          BIGSERIAL PRIMARY KEY,
    partido_id  UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
    minuto      SMALLINT NOT NULL,
    tipo        VARCHAR(30) NOT NULL,   -- GOL, TARJETA_AMARILLA, TARJETA_ROJA, CAMBIO, VAR
    equipo_id   INTEGER REFERENCES equipo(id),
    detalle     VARCHAR(150),
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 4. MERCADOS DE APUESTA (catálogo genérico -> cubre TODOS los mercados)
-- ---------------------------------------------------------------------
-- Catálogo de tipos de mercado disponibles en la casa de apuestas.
-- Aquí registras cada mercado que quieras ofrecer, sin tocar el esquema.
CREATE TABLE tipo_mercado (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(40) NOT NULL UNIQUE,   -- '1X2', 'DOBLE_OPORTUNIDAD', 'OVER_UNDER'...
    nombre          VARCHAR(100) NOT NULL,         -- 'Resultado final', 'Total de goles'...
    descripcion     VARCHAR(255),
    deporte_id      SMALLINT REFERENCES deporte(id),
    tiene_linea     BOOLEAN NOT NULL DEFAULT FALSE, -- ej: Over/Under 2.5, Hándicap -1.5
    periodo         VARCHAR(30) NOT NULL DEFAULT 'PARTIDO_COMPLETO', -- PRIMER_TIEMPO, SEGUNDO_TIEMPO...
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

-- Catálogo inicial con los mercados más usados en apuestas de fútbol
INSERT INTO tipo_mercado (codigo, nombre, descripcion, tiene_linea, periodo) VALUES
 ('1X2',                    'Resultado final',              'Local, empate o visitante', FALSE, 'PARTIDO_COMPLETO'),
 ('DOBLE_OPORTUNIDAD',      'Doble oportunidad',            'Combina dos resultados posibles', FALSE, 'PARTIDO_COMPLETO'),
 ('OVER_UNDER',             'Total de goles',                'Más o menos de X goles en el partido', TRUE, 'PARTIDO_COMPLETO'),
 ('AMBOS_ANOTAN',           'Ambos equipos anotan',          'Sí o no marcan los dos equipos', FALSE, 'PARTIDO_COMPLETO'),
 ('HANDICAP_EUROPEO',       'Hándicap europeo',              'Resultado con ventaja/desventaja fija', TRUE, 'PARTIDO_COMPLETO'),
 ('HANDICAP_ASIATICO',      'Hándicap asiático',             'Ventaja/desventaja con líneas fraccionadas', TRUE, 'PARTIDO_COMPLETO'),
 ('MARCADOR_CORRECTO',      'Marcador correcto',             'Resultado exacto del partido', FALSE, 'PARTIDO_COMPLETO'),
 ('PRIMER_EN_ANOTAR',       'Primer equipo en anotar',       'Quién anota primero', FALSE, 'PARTIDO_COMPLETO'),
 ('TOTAL_GOLES_LOCAL',      'Total de goles del local',      'Over/Under de goles del equipo local', TRUE, 'PARTIDO_COMPLETO'),
 ('TOTAL_GOLES_VISITA',     'Total de goles del visitante',  'Over/Under de goles del equipo visitante', TRUE, 'PARTIDO_COMPLETO'),
 ('RESULTADO_1ER_TIEMPO',   'Resultado 1er tiempo',          '1X2 solo del primer tiempo', FALSE, 'PRIMER_TIEMPO'),
 ('OVER_UNDER_1ER_TIEMPO',  'Total de goles 1er tiempo',     'Over/Under solo del primer tiempo', TRUE, 'PRIMER_TIEMPO'),
 ('PAR_IMPAR',              'Goles par/impar',               'Total de goles del partido par o impar', FALSE, 'PARTIDO_COMPLETO'),
 ('DOBLE_RESULTADO',        'Resultado y ambos anotan',      'Combina 1X2 con ambos anotan', FALSE, 'PARTIDO_COMPLETO');

-- Instancia de un mercado para un partido concreto (lo que se ve/actualiza en vivo)
CREATE TABLE mercado (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    partido_id      UUID NOT NULL REFERENCES partido(id) ON DELETE CASCADE,
    tipo_mercado_id INTEGER NOT NULL REFERENCES tipo_mercado(id),
    linea           NUMERIC(5,2),           -- valor de la línea si aplica (2.5, -1.5, etc.)
    estado          estado_mercado NOT NULL DEFAULT 'ABIERTO',
    en_vivo         BOOLEAN NOT NULL DEFAULT FALSE, -- si se puede apostar mientras el partido corre
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mercado_partido ON mercado (partido_id, estado);

-- Opciones/selecciones dentro de un mercado, con su cuota (lo que cambia en tiempo real)
CREATE TABLE mercado_opcion (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mercado_id      UUID NOT NULL REFERENCES mercado(id) ON DELETE CASCADE,
    codigo          VARCHAR(30) NOT NULL,   -- 'LOCAL', 'EMPATE', 'VISITA', 'OVER', 'UNDER', 'SI', 'NO', '2-1'...
    etiqueta        VARCHAR(60) NOT NULL,   -- texto mostrado al usuario, ej. "Alianza Lima"
    cuota           NUMERIC(6,2) NOT NULL CHECK (cuota >= 1.01),
    resultado       resultado_opcion NOT NULL DEFAULT 'PENDIENTE',
    orden           SMALLINT NOT NULL DEFAULT 0,
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (mercado_id, codigo)
);

CREATE INDEX idx_mercado_opcion_mercado ON mercado_opcion (mercado_id);

-- Historial de cuotas: cada cambio queda registrado (sirve para el gráfico
-- de "movimiento de cuota" y para que el backend detecte el cambio y
-- lo empuje por WebSocket a los clientes conectados a ese partido).
CREATE TABLE cuota_historial (
    id              BIGSERIAL PRIMARY KEY,
    mercado_opcion_id UUID NOT NULL REFERENCES mercado_opcion(id) ON DELETE CASCADE,
    cuota_anterior  NUMERIC(6,2),
    cuota_nueva     NUMERIC(6,2) NOT NULL,
    registrado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cuota_historial_opcion ON cuota_historial (mercado_opcion_id, registrado_en DESC);

-- ---------------------------------------------------------------------
-- 5. APUESTAS DEL USUARIO
-- ---------------------------------------------------------------------
CREATE TABLE apuesta (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id      UUID NOT NULL REFERENCES usuario(id),
    tipo            tipo_apuesta NOT NULL DEFAULT 'SIMPLE',
    monto           NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    cuota_total     NUMERIC(8,2) NOT NULL,      -- producto de las cuotas (combinadas)
    ganancia_potencial NUMERIC(12,2) NOT NULL,
    estado          estado_apuesta NOT NULL DEFAULT 'PENDIENTE',
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    liquidado_en    TIMESTAMPTZ
);

-- Detalle: cada selección que compone la apuesta (1 fila si es simple, N si es combinada)
CREATE TABLE apuesta_seleccion (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    apuesta_id          UUID NOT NULL REFERENCES apuesta(id) ON DELETE CASCADE,
    mercado_opcion_id   UUID NOT NULL REFERENCES mercado_opcion(id),
    cuota_congelada     NUMERIC(6,2) NOT NULL,  -- cuota al momento de apostar
    resultado           resultado_opcion NOT NULL DEFAULT 'PENDIENTE'
);

CREATE INDEX idx_apuesta_usuario ON apuesta (usuario_id, estado);
CREATE INDEX idx_apuesta_seleccion_apuesta ON apuesta_seleccion (apuesta_id);

-- ---------------------------------------------------------------------
-- 6. TRIGGERS: actualizado_en automático (soporte para tiempo real)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_partido_actualizado
    BEFORE UPDATE ON partido
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_mercado_actualizado
    BEFORE UPDATE ON mercado
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_mercado_opcion_actualizado
    BEFORE UPDATE ON mercado_opcion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

-- Cada vez que cambia la cuota de una opción, se guarda automáticamente
-- en el historial (así el backend solo necesita hacer UPDATE sobre
-- mercado_opcion y esta tabla queda poblada sola, lista para el
-- listener de Spring Boot que emite el evento por WebSocket).
CREATE OR REPLACE FUNCTION fn_registrar_historial_cuota()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.cuota IS DISTINCT FROM OLD.cuota THEN
        INSERT INTO cuota_historial (mercado_opcion_id, cuota_anterior, cuota_nueva)
        VALUES (NEW.id, OLD.cuota, NEW.cuota);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cuota_historial
    BEFORE UPDATE ON mercado_opcion
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial_cuota();

-- ---------------------------------------------------------------------
-- 7. VISTA DE UTILIDAD: partidos con sus mercados/cuotas al día
--    (ideal para el endpoint REST inicial y para "refrescar" el estado
--    al reconectar el WebSocket)
-- ---------------------------------------------------------------------
CREATE VIEW vw_partido_mercados AS
SELECT
    p.id                AS partido_id,
    l.nombre            AS liga,
    el.nombre           AS equipo_local,
    ev.nombre           AS equipo_visitante,
    p.fecha_hora_inicio,
    p.estado            AS estado_partido,
    p.marcador_local,
    p.marcador_visita,
    tm.codigo           AS tipo_mercado,
    tm.nombre           AS nombre_mercado,
    m.id                AS mercado_id,
    m.linea,
    m.estado            AS estado_mercado,
    mo.codigo           AS opcion_codigo,
    mo.etiqueta         AS opcion_etiqueta,
    mo.cuota
FROM partido p
JOIN liga l              ON l.id = p.liga_id
JOIN equipo el            ON el.id = p.equipo_local_id
JOIN equipo ev             ON ev.id = p.equipo_visitante_id
JOIN mercado m             ON m.partido_id = p.id
JOIN tipo_mercado tm       ON tm.id = m.tipo_mercado_id
JOIN mercado_opcion mo     ON mo.mercado_id = m.id
ORDER BY p.fecha_hora_inicio, tm.id, mo.orden;

-- ---------------------------------------------------------------------
-- 8. DATOS DE EJEMPLO (para probar rápido, según tu imagen "Fecha 01")
-- ---------------------------------------------------------------------
INSERT INTO deporte (nombre, orden) VALUES ('Fútbol', 1);

INSERT INTO pais (nombre) VALUES
 ('Turquía'), ('Francia'), ('Italia'), ('Bélgica'),
 ('Hungría'), ('Ucrania'), ('Suecia'), ('Rumania'),
 ('Polonia'), ('Bosnia y Herzegovina'), ('Marruecos'), ('Gabón');

INSERT INTO liga (deporte_id, nombre, temporada) VALUES
 (1, 'Clasificatorias Mundial', '2026');

INSERT INTO equipo (deporte_id, nombre, pais_id) VALUES
 (1, 'Turquía',   (SELECT id FROM pais WHERE nombre = 'Turquía')),
 (1, 'Francia',   (SELECT id FROM pais WHERE nombre = 'Francia')),
 (1, 'Italia',    (SELECT id FROM pais WHERE nombre = 'Italia')),
 (1, 'Bélgica',   (SELECT id FROM pais WHERE nombre = 'Bélgica'));

-- Partido de ejemplo: Turquía vs Francia
WITH nuevo_partido AS (
    INSERT INTO partido (liga_id, equipo_local_id, equipo_visitante_id, fecha_hora_inicio, etiqueta)
    SELECT l.id,
           (SELECT id FROM equipo WHERE nombre = 'Turquía'),
           (SELECT id FROM equipo WHERE nombre = 'Francia'),
           now() + interval '2 days',
           'Fecha 01'
    FROM liga l WHERE l.nombre = 'Clasificatorias Mundial'
    RETURNING id
),
mercado_1x2 AS (
    INSERT INTO mercado (partido_id, tipo_mercado_id)
    SELECT np.id, tm.id FROM nuevo_partido np, tipo_mercado tm WHERE tm.codigo = '1X2'
    RETURNING id
)
INSERT INTO mercado_opcion (mercado_id, codigo, etiqueta, cuota, orden)
SELECT id, 'LOCAL',  'Turquía', 2.45, 1 FROM mercado_1x2
UNION ALL
SELECT id, 'EMPATE', 'Empate',  3.10, 2 FROM mercado_1x2
UNION ALL
SELECT id, 'VISITA', 'Francia', 2.70, 3 FROM mercado_1x2;

-- =====================================================================
-- FIN DEL SCRIPT
-- Sugerencia de uso desde Spring Boot:
--  - Repositorios JPA sobre partido / mercado / mercado_opcion.
--  - Un @Scheduled o listener de proveedor de cuotas hace UPDATE sobre
--    mercado_opcion.cuota -> el trigger llena cuota_historial solo.
--  - Un @TransactionalEventListener publica el cambio a un topic STOMP
--    (ej. /topic/partido/{partidoId}) para que Angular reciba la
--    cuota nueva en tiempo real sin hacer polling.
-- =====================================================================
