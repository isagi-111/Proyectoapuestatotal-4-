package com.apuestatotal.dao;

import com.apuestatotal.dao.jdbc.ApuestaDaoJdbc;
import com.apuestatotal.dao.jdbc.BilleteraDaoJdbc;
import com.apuestatotal.dao.jdbc.FabricaConexiones;
import com.apuestatotal.dao.jdbc.GestorConexiones;
import com.apuestatotal.dao.jdbc.InicializadorEsquema;
import com.apuestatotal.dao.jdbc.PartidoDaoJdbc;
import com.apuestatotal.dao.jdbc.UsuarioDaoJdbc;
import java.util.UUID;

/** Ejecuta el MISMO contrato contra JDBC (H2 en modo PostgreSQL, base nueva por test). */
class DaoJdbcTest extends ContratoDaoTest {

    @Override
    void preparar() {
        FabricaConexiones fabrica = new FabricaConexiones(
                "jdbc:h2:mem:dao_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        InicializadorEsquema.ejecutar(fabrica);
        GestorConexiones gestor = new GestorConexiones(fabrica);
        usuarios = new UsuarioDaoJdbc(gestor);
        billeteras = new BilleteraDaoJdbc(gestor);
        partidos = new PartidoDaoJdbc(gestor);
        apuestas = new ApuestaDaoJdbc(gestor);
    }
}
