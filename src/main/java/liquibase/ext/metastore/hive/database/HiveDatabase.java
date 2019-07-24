package liquibase.ext.metastore.hive.database;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.ext.metastore.database.HiveDatabaseConnectionWrapper;
import liquibase.logging.LogService;
import liquibase.logging.Logger;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class HiveDatabase extends AbstractJdbcDatabase {

    public static final List<String> RESERVED = Arrays.asList("ALL", "ALTER", "AND", "ARRAY", "AS", "AUTHORIZATION", "BETWEEN",
            "BIGINT", "BINARY", "BOOLEAN", "BOTH", "BY", "CASE", "CAST", "CHAR",
            "COLUMN", "CONF", "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE",
            "CURRENT_TIMESTAMP", "CURSOR", "DATABASE", "DATE", "DECIMAL", "DELETE",
            "DESCRIBE", "DISTINCT", "DOUBLE", "DROP", "ELSE", "END", "EXCHANGE", "EXISTS",
            "EXTENDED", "EXTERNAL", "FALSE", "FETCH", "FLOAT", "FOLLOWING", "FOR", "FROM",
            "FULL", "FUNCTION", "GRANT", "GROUP", "GROUPING", "HAVING", "IF", "IMPORT", "IN",
            "INNER", "INSERT", "INT", "INTERSECT", "INTERVAL", "INTO", "IS", "JOIN", "LATERAL",
            "LEFT", "LESS", "LIKE", "LOCAL", "MACRO", "MAP", "MORE", "NONE", "NOT", "NULL", "OF",
            "ON", "OR", "ORDER", "OUT", "OUTER", "OVER", "PARTIALSCAN", "PARTITION", "PERCENT",
            "PRECEDING", "PRESERVE", "PROCEDURE", "RANGE", "READS", "REDUCE", "REVOKE", "RIGHT",
            "ROLLUP", "ROW", "ROWS", "SELECT", "SET", "SMALLINT", "TABLE", "TABLESAMPLE", "THEN",
            "TIMESTAMP", "TO", "TRANSFORM", "TRIGGER", "TRUE", "TRUNCATE", "UNBOUNDED", "UNION",
            "UNIQUEJOIN", "UPDATE", "USER", "USING", "UTC_TMESTAMP", "VALUES", "VARCHAR", "WHEN",
            "WHERE", "WINDOW", "WITH", "COMMIT", "ONLY", "REGEXP", "RLIKE", "ROLLBACK", "START",
            "CACHE", "CONSTRAINT", "FOREIGN", "PRIMARY", "REFERENCES", "DAYOFWEEK", "EXTRACT",
            "FLOOR", "INTEGER", "PRECISION", "VIEWS");
    private static final Logger LOG = LogService.getLog(HiveDatabase.class);
    private final String databaseProductName;
    private final String prefix;
    private final String databaseDriver;

    public HiveDatabase() {
        this.databaseProductName = "Apache Hive";
        this.prefix = "jdbc:hive2";
        this.databaseDriver = "org.apache.hive.jdbc.HiveDriver";

    }

    @Override
    public Integer getDefaultPort() {
        return 10000;
    }

    public void setReservedWords() {
        addReservedWords(RESERVED);
    }

    @Override
    public String getCurrentDateTimeFunction() {
        return "CURRENT_TIMESTAMP()";
    }

    @Override
    protected String getConnectionSchemaName() {
        String[] tokens = super.getConnection().getURL().split("/");
        String dbName = tokens[tokens.length - 1].split(";")[0];
        String schema = getSchemaDatabaseSpecific("SHOW SCHEMAS LIKE '" + dbName + "'");
        return schema == null ? "default" : schema;
    }

    @Override
    protected String getQuotingStartCharacter() {
        return "`";
    }

    @Override
    protected String getQuotingEndCharacter() {
        return "`";
    }

    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection databaseConnection) throws DatabaseException {
        return databaseProductName.equalsIgnoreCase(databaseConnection.getDatabaseProductName());
    }

    @Override
    public String getDefaultDriver(String url) {
        if (url.startsWith(prefix)) {
            return databaseDriver;
        }
        return null;
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return databaseProductName;
    }

    @Override
    public String getShortName() {
        return databaseProductName.toLowerCase();
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean requiresPassword() {
        return false;
    }

    @Override
    public boolean requiresUsername() {
        return true;
    }

    @Override
    public boolean isAutoCommit() throws DatabaseException {
        return true;
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }

    @Override
    public boolean supportsAutoIncrement() {
        return false;
    }

    @Override
    public boolean supportsRestrictForeignKeys() {
        return false;
    }

    @Override
    public boolean supportsDropTableCascadeConstraints() {
        return false;
    }

    @Override
    public boolean supportsDDLInTransaction() {
        return false;
    }

    @Override
    public boolean supportsPrimaryKeyNames() {
        return false;
    }

    @Override
    public void setConnection(DatabaseConnection conn) {
        setReservedWords();
        super.setConnection(new HiveDatabaseConnectionWrapper((JdbcConnection) conn));
    }

    @Override
    public void setAutoCommit(boolean b) throws DatabaseException {
    }

    public Statement getStatement() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        String url = super.getConnection().getURL();
        Driver driver = (Driver) Class.forName(getDefaultDriver(url)).newInstance();
        Connection con = driver.connect(url, System.getProperties());
        return con.createStatement();
    }

    protected String getSchemaDatabaseSpecific(String query) {
        Statement statement = null;
        try {
            statement = getStatement();
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            String schema = resultSet.getString(1);
            LOG.info("Schema name is '" + schema + "'");
            return schema;
        } catch (Exception e) {
            LOG.info("Can't get default schema:", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.warning("Can't close cursor", e);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }
}
