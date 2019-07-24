package liquibase.ext.metastore.hive.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.ValidationErrors;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.ext.metastore.hive.statement.HiveInsertStatement;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

import java.text.MessageFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class HiveInsertGenerator extends AbstractSqlGenerator<HiveInsertStatement> {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(HiveInsertStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public ValidationErrors validate(HiveInsertStatement insertStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", insertStatement.getTableName());
        validationErrors.checkRequiredField("columns", insertStatement.getColumnValues());

        return validationErrors;
    }

    @Override
    public Sql[] generateSql(HiveInsertStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {


        String tableName = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName());
        String sql = MessageFormat.format("INSERT INTO {0} VALUES {1}", tableName, generateValues(statement, database));

        return new Sql[]{new UnparsedSql(sql, getAffectedTable(statement))};
    }

    private String generateValues(HiveInsertStatement statement, Database database) {
        return statement.getColumnValues()
                .stream()
                .map(it -> plainSQLByNewValue(database, it))
                .collect(Collectors.joining(", ", "(", ")"));

    }

    private String plainSQLByNewValue(Database database, Object newValue) {
        if (newValue == null || newValue.toString().equalsIgnoreCase("NULL")) {
            return "NULL";
        } else if (newValue instanceof String && !looksLikeFunctionCall(((String) newValue), database)) {
            return DataTypeFactory.getInstance().fromObject(newValue, database).objectToSql(newValue, database);
        } else if (newValue instanceof Date) {
            return database.getDateLiteral(((Date) newValue));
        } else if (newValue instanceof Boolean) {
            if (((Boolean) newValue)) {
                return DataTypeFactory.getInstance().getTrueBooleanValue(database);
            } else {
                return DataTypeFactory.getInstance().getFalseBooleanValue(database);
            }
        } else if (newValue instanceof DatabaseFunction) {
            return database.generateDatabaseFunctionValue((DatabaseFunction) newValue);
        } else {
            return newValue.toString();
        }
    }

    private Relation getAffectedTable(HiveInsertStatement statement) {
        return new Table().setName(statement.getTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }
}
