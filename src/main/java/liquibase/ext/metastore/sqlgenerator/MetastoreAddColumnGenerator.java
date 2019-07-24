package liquibase.ext.metastore.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.DatabaseDataType;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.sqlgenerator.core.AddColumnGenerator;
import liquibase.statement.core.AddColumnStatement;

import static java.text.MessageFormat.format;

public class MetastoreAddColumnGenerator extends AddColumnGenerator {

    @Override
    public boolean supports(AddColumnStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    protected String generateSingleColumnSQL(AddColumnStatement statement, Database database) {
        DatabaseDataType databaseColumnType = DataTypeFactory.getInstance().fromDescription(statement.getColumnType(), database).toDatabaseDataType(database);
        return format(" ADD COLUMNS ({0} {1})",
                database.escapeColumnName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), statement.getColumnName()),
                databaseColumnType);
    }
}
