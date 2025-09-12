package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.embedded.DatabaseConnectionPreparer;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.ds.PGSimpleDataSource;
import io.zonky.test.db.postgres.junit5.*;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class PostgresCatalogTest
{
    //@RegisterExtension
    //public SingleInstancePostgresExtension simpleDB = EmbeddedPostgresExtension.singleInstance();

    @RegisterExtension
    public static PreparedDbExtension flywayDB = EmbeddedPostgresExtension.preparedDatabase(FlywayPreparer.forClasspathLocation("db/pg"));

    //@RegisterExtension
    //public PreparedDbExtension db = EmbeddedPostgresExtension.preparedDatabase(preparer)
    //    .customize(builder -> builder.setConnectConfig("connectTimeout", "20"));

    private DataSource dataSource;
    private PostgresCatalog catalog;
    
    @BeforeEach
    void setUp() {
        dataSource = flywayDB.getTestDatabase();
        catalog = new PostgresCatalog(dataSource);
    }

    @Test
    void constructor_LoadsTestTable()
    {
        // Should be a single test table and the Flyway schema table
        assertEquals(2, catalog.getTables().size());

        AspectDef aspectDef = catalog.getTableDef("test_table");
        assertEquals(9, aspectDef.propertyDefs().size());
    }

}