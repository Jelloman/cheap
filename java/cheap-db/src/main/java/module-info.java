module net.netbeing.cheap.db {
    exports net.netbeing.cheap.db;

    requires transitive net.netbeing.cheap.core;
    requires org.sqlite.jdbc;
    requires org.postgresql.jdbc;
    requires static org.jetbrains.annotations;
}
