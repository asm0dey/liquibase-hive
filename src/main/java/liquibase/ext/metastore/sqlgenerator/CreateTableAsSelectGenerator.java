package liquibase.ext.metastore.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.ext.metastore.statement.CreateTableAsSelectStatement;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

import java.text.MessageFormat;
import java.util.stream.Collectors;

import static liquibase.util.SqlUtil.replacePredicatePlaceholders;

public class CreateTableAsSelectGenerator extends AbstractSqlGenerator<CreateTableAsSelectStatement> {

    @Override
    public boolean supports(CreateTableAsSelectStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(CreateTableAsSelectStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors errors = new ValidationErrors();
        errors.checkRequiredField("tableName", statement.getTableName());
        return errors;
    }

    @Override
    public Sql[] generateSql(CreateTableAsSelectStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String tablename = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getDestTableName());
        String columnNames = generateColumnNames(statement, database);
        String fromTableName = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName());
        String sql = MessageFormat.format("CREATE TABLE {0} AS SELECT {1} FROM {2}", tablename, columnNames, fromTableName);
        if (statement.getWhereCondition() != null) {
            sql += " WHERE " + replacePredicatePlaceholders(
                    database,
                    statement.getWhereCondition(),
                    statement.getWhereColumnNames(),
                    statement.getWhereParameters()
            );
        }
        return new Sql[]{new UnparsedSql(sql, fetchAffectedTable(statement))};
    }

    private String generateColumnNames(CreateTableAsSelectStatement statement, Database database) {
        return statement
                .getColumnNames()
                .stream()
                .map(it -> database.escapeColumnName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName(), it))
                .collect(Collectors.joining(", "));
    }

    private Relation fetchAffectedTable(CreateTableAsSelectStatement statement) {
        return new Table().setName(statement.getDestTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }
}
