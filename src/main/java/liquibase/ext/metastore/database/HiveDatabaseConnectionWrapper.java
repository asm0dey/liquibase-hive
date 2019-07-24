package liquibase.ext.metastore.database;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import org.apache.hive.jdbc.HiveConnection;

public class HiveDatabaseConnectionWrapper extends JdbcConnection {
    private JdbcConnection conn;

    public HiveDatabaseConnectionWrapper(JdbcConnection conn) {
        super(conn.getUnderlyingConnection());
        this.conn = conn;
    }

/*
    @Override
    public String getDatabaseProductName() throws DatabaseException {
        try {
            return conn.getUnderlyingConnection().getSchema();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
*/

    @Override
    public String getURL() {
        return ((HiveConnection) conn.getUnderlyingConnection()).getConnectedUrl();
    }

    @Override
    public String getConnectionUserName() {
        return "admin";
    }

    @Override
    public void attached(Database database) {
        database.addReservedWords(HiveDatabase.RESERVED);
    }
}
