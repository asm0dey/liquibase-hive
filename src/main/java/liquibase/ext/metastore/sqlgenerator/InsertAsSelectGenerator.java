package liquibase.ext.metastore.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.ext.metastore.statement.InsertAsSelectStatement;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

import java.text.MessageFormat;
import java.util.stream.Collectors;

import static liquibase.util.SqlUtil.replacePredicatePlaceholders;

public class InsertAsSelectGenerator extends AbstractSqlGenerator<InsertAsSelectStatement> {

    @Override
    public boolean supports(InsertAsSelectStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(InsertAsSelectStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors errors = new ValidationErrors();
        errors.checkRequiredField("tableName", statement.getTableName());
        errors.checkRequiredField("dstTableName", statement.getDestTableName());
        return errors;
    }

    @Override
    public Sql[] generateSql(InsertAsSelectStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String catalogName = statement.getCatalogName();
        String schemaName = statement.getSchemaName();
        String tableName = statement.getTableName();
        String sql = MessageFormat.format("INSERT INTO {0} SELECT {1} FROM {2}",
                database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getDestTableName()),
                generateColumnNames(statement, database),
                database.escapeTableName(catalogName, schemaName, tableName));
        if (statement.getWhereCondition() != null) {
            sql += " WHERE " + replacePredicatePlaceholders(database, statement.getWhereCondition(), statement.getWhereColumnNames(), statement.getWhereParameters());
        }
        return new Sql[]{new UnparsedSql(sql, fetchAffectedTable(statement))};
    }

    private String generateColumnNames(InsertAsSelectStatement statement, Database database) {
        String catalogName = statement.getCatalogName();
        String schemaName = statement.getSchemaName();
        String tableName = statement.getTableName();
        return statement
                .getColumnNames()
                .stream()
                .map(it -> (it.startsWith("'") && it.endsWith("'")) ? it : database.escapeColumnName(catalogName, schemaName, tableName, it))
                .collect(Collectors.joining(", "));
    }

    private Relation fetchAffectedTable(InsertAsSelectStatement statement) {
        return new Table().setName(statement.getDestTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }
}
