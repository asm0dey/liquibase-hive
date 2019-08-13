package liquibase.ext.metastore.sqlgenerator;

import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.ext.metastore.configuration.HiveMetastoreConfiguration;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.ext.metastore.utils.CustomSqlGenerator;
import liquibase.ext.metastore.utils.UserSessionSettings;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateDatabaseChangeLogLockTableGenerator;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.CreateTableStatement;

public class MetastoreCreateDatabaseChangeLogLockTableGenerator extends CreateDatabaseChangeLogLockTableGenerator {
    private Boolean syncDb = LiquibaseConfiguration.getInstance().getConfiguration(HiveMetastoreConfiguration.class).getSyncDDL();

    @Override
    public boolean supports(CreateDatabaseChangeLogLockTableStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public Sql[] generateSql(CreateDatabaseChangeLogLockTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        CreateTableStatement createTableStatement = new CreateTableStatement(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName())
                .addColumn("ID", DataTypeFactory.getInstance().fromDescription("INT", database))
                .addColumn("LOCKED", DataTypeFactory.getInstance().fromDescription("BOOLEAN", database))
                .addColumn("LOCKGRANTED", DataTypeFactory.getInstance().fromDescription("TIMESTAMP", database))
                .addColumn("LOCKEDBY", DataTypeFactory.getInstance().fromDescription("STRING", database));

        return syncDb ?
                CustomSqlGenerator.generateSql(database, UserSessionSettings.syncDdlStart(), createTableStatement, UserSessionSettings.syncDdlStop())
                : CustomSqlGenerator.generateSql(database, createTableStatement);

    }
}
