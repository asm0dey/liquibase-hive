package liquibase.ext.metastore.utils;

import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class CustomSqlGenerator {

    public static Sql[] generateSql(Database database, List<SqlStatement> statements) {
        return generateSqlInternal(database, statements);
    }

    public static Sql[] generateSql(Database database, SqlStatement... statements) {
        return generateSqlInternal(database, asList(statements));
    }

    private static Sql[] generateSqlInternal(Database database, Iterable<SqlStatement> statements) {
        List<Sql> sqls = new ArrayList<>();
        SqlGeneratorFactory generatorFactory = SqlGeneratorFactory.getInstance();
        for (SqlStatement statement : statements) {
            sqls.addAll(asList(generatorFactory.generateSql(statement, database)));
        }
        return sqls.toArray(new Sql[0]);
    }
}
