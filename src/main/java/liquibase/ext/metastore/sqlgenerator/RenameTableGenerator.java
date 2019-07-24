package liquibase.ext.metastore.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.RenameTableStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

import java.text.MessageFormat;

import static java.text.MessageFormat.format;

public class RenameTableGenerator extends AbstractSqlGenerator<RenameTableStatement> {

    @Override
    public boolean supports(RenameTableStatement statement, Database database) {
        return database instanceof HiveDatabase && super.supports(statement, database);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(RenameTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors errors = new ValidationErrors();
        errors.checkRequiredField("newTableName", statement.getNewTableName());
        errors.checkRequiredField("oldTableName", statement.getOldTableName());
        return errors;
    }

    @Override
    public Sql[] generateSql(RenameTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String sql = format("ALTER TABLE {0} RENAME TO {1}",
                database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getOldTableName()),
                database.escapeObjectName(statement.getNewTableName(), Table.class));
        return new Sql[]{
                new UnparsedSql(sql,
                        fetchAffectedOldTable(statement),
                        fetchAffectedNewTable(statement)
                )
        };
    }

    private Relation fetchAffectedNewTable(RenameTableStatement statement) {
        return new Table().setName(statement.getNewTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }

    private Relation fetchAffectedOldTable(RenameTableStatement statement) {
        return new Table().setName(statement.getOldTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName());
    }
}
