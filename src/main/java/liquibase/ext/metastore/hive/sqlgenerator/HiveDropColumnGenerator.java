package liquibase.ext.metastore.hive.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.logging.LogService;
import liquibase.logging.Logger;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.DropColumnStatement;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class HiveDropColumnGenerator extends AbstractSqlGenerator<DropColumnStatement> {
    private static final Logger LOG = LogService.getLog(HiveDropColumnGenerator.class);

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(DropColumnStatement dropColumnStatement, Database database) {
        return database instanceof HiveDatabase && super.supports(dropColumnStatement, database);
    }

    @Override
    public ValidationErrors validate(DropColumnStatement dropColumnStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        if (dropColumnStatement.isMultiple()) {
            ValidationErrors validationErrors = new ValidationErrors();
            DropColumnStatement firstColumn = dropColumnStatement.getColumns().get(0);

            for (DropColumnStatement drop : dropColumnStatement.getColumns()) {
                validationErrors.addAll(validateSingleColumn(drop));
                if (drop.getTableName() != null && !drop.getTableName().equals(firstColumn.getTableName())) {
                    validationErrors.addError("All columns must be targeted at the same table");
                }
                if (drop.isMultiple()) {
                    validationErrors.addError("Nested multiple drop column statements are not supported");
                }
            }
            return validationErrors;
        } else {
            return validateSingleColumn(dropColumnStatement);
        }
    }

    private ValidationErrors validateSingleColumn(DropColumnStatement dropColumnStatement) {
        ValidationErrors validationErrors = new ValidationErrors();
        validationErrors.checkRequiredField("tableName", dropColumnStatement.getTableName());
        validationErrors.checkRequiredField("columnName", dropColumnStatement.getColumnName());
        return validationErrors;
    }

    @Override
    public Sql[] generateSql(DropColumnStatement dropColumnStatement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        Map<String, String> columnsPreserved = columnsMap((HiveDatabase) database, dropColumnStatement);
        return generateMultipleColumnSql(dropColumnStatement, database, columnsPreserved);
    }

    private Map<String, String> columnsMap(HiveDatabase database, DropColumnStatement dropColumnStatement) {
        String tableName = database.escapeObjectName(dropColumnStatement.getTableName(), Table.class);
        String query = MessageFormat.format("DESCRIBE {0}", tableName);
        Map<String, String> mapOfColNameDataTypes = null;
        try {
            mapOfColNameDataTypes = new HashMap<>();
            try (Connection con = database.connect();
                 Statement statement = con.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    String colName = resultSet.getString("col_name");
                    String dataType = resultSet.getString("data_type");
                    mapOfColNameDataTypes.put(colName.toUpperCase(), dataType);
                }
            }
        } catch (Exception e) {
            LOG.warning("can't perform query", e);
        }
        return mapOfColNameDataTypes;
    }

    private Sql[] generateMultipleColumnSql(DropColumnStatement dropColumnStatement, Database database, Map<String, String> columnsPreserved) {
        if (columnsPreserved == null) {
            throw new UnexpectedLiquibaseException("no columns to preserve");
        }
        List<Sql> result = new ArrayList<>();
        Map<String, String> columnsPreservedCopy = new HashMap<>(columnsPreserved);
        StringBuilder alterTable;
        List<DropColumnStatement> columns = null;

        if (dropColumnStatement.isMultiple()) {
            columns = dropColumnStatement.getColumns();
            for (DropColumnStatement statement : columns) {
                columnsPreservedCopy.remove(statement.getColumnName());
            }
            String tableName = database.escapeTableName(columns.get(0).getCatalogName(), columns.get(0).getSchemaName(), columns.get(0).getTableName());
            alterTable = new StringBuilder(MessageFormat.format("ALTER TABLE {0} REPLACE COLUMNS ", tableName));
        } else {
            columnsPreservedCopy.remove(dropColumnStatement.getColumnName());
            String tableName = database.escapeTableName(dropColumnStatement.getCatalogName(), dropColumnStatement.getSchemaName(), dropColumnStatement.getTableName());
            alterTable = new StringBuilder(MessageFormat.format("ALTER TABLE {0} REPLACE COLUMNS ", tableName));
        }

        String columnsDef = columnsPreservedCopy
                .entrySet()
                .stream()
                .map(entry -> database.escapeObjectName(entry.getKey(), Column.class) + " " + entry.getValue())
                .collect(joining(",", "(", ")"));

        alterTable.append(columnsDef);

        if (dropColumnStatement.isMultiple()) {
            result.add(new UnparsedSql(alterTable.toString(), getAffectedColumns(columns)));
        } else {
            result.add(new UnparsedSql(alterTable.toString(), getAffectedColumn(dropColumnStatement)));
        }
        return result.toArray(new Sql[0]);
    }

    private Column[] getAffectedColumns(List<DropColumnStatement> columns) {
        List<Column> affected = new ArrayList<>();
        for (DropColumnStatement column : columns) affected.add(getAffectedColumn(column));
        return affected.toArray(new Column[0]);
    }

    private Column getAffectedColumn(DropColumnStatement statement) {
        return new Column().setName(statement.getColumnName()).setRelation(new Table().setName(statement.getTableName()).setSchema(statement.getCatalogName(), statement.getSchemaName()));
    }
}
