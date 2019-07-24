package liquibase.ext.metastore.datatype;

import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.datatype.core.DateType;
import liquibase.ext.metastore.hive.database.HiveDatabase;

@DataTypeInfo(name = "datetime", aliases = {"java.sql.Types.DATETIME"},
        minParameters = 0, maxParameters = 1, priority = LiquibaseDataType.PRIORITY_DATABASE)
public class MetastoreDateTimeType extends DateType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof HiveDatabase) {
            return new DatabaseDataType("TIMESTAMP", getParameters());
        }

        return super.toDatabaseDataType(database);
    }
}
