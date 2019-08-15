package liquibase.ext.metastore.lockservice;

import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.metastore.configuration.HiveMetastoreConfiguration;
import liquibase.ext.metastore.hive.database.HiveDatabase;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.StandardLockService;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.SelectFromDatabaseChangeLogLockStatement;
import liquibase.statement.core.UnlockDatabaseChangeLogStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.text.MessageFormat.format;

public class MetastoreLockService extends StandardLockService {

    private static final Logger LOG = LogFactory.getInstance().getLog();
    private ObjectQuotingStrategy quotingStrategy;
    private Boolean lockDb = LiquibaseConfiguration.getInstance().getConfiguration(HiveMetastoreConfiguration.class).getLock();

    @Override
    public boolean supports(Database database) {
        return database instanceof HiveDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean hasDatabaseChangeLogLockTable() throws DatabaseException {
        boolean hasChangeLogLockTable = false;
        try (Connection con = ((HiveDatabase) database).connect();
             Statement statement = con.createStatement()) {
            LOG.info("Looking for table '" + database.getDatabaseChangeLogLockTableName() + "'");
            try (ResultSet set = statement.executeQuery(format("SELECT id FROM {0}", database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName())))) {
                hasChangeLogLockTable = true;

            } catch (SQLException e) {
                LOG.info("Table '" + database.getDatabaseChangeLogLockTableName() + "' doesn't exists in hive metastore.");
                hasChangeLogLockTable = false;

            }
        } catch (InstantiationException | IllegalAccessException | SQLException e) {
            LOG.warning("can't perform query", e);
        }
        return hasChangeLogLockTable;
    }

    @Override
    public void releaseLock() throws LockException {
        if (lockDb) {
            ObjectQuotingStrategy incomingQuotingStrategy = null;
            if (this.quotingStrategy != null) {
                incomingQuotingStrategy = database.getObjectQuotingStrategy();
                database.setObjectQuotingStrategy(this.quotingStrategy);
            }

            Executor executor = ExecutorService.getInstance().getExecutor(database);
            try {
                if (this.hasDatabaseChangeLogLockTable()) {
                    executor.comment("Release hive metastore database lock");
                    database.rollback();
                    executor.execute(new UnlockDatabaseChangeLogStatement());
                    database.commit();
                }
            } catch (Exception e) {
                throw new LockException(e);
            } finally {
                try {
                    hasChangeLogLock = false;

                    database.setCanCacheLiquibaseTableInfo(false);

                    LOG.info("Change log lock has been successfully released");
                    database.rollback();
                } catch (DatabaseException e) {
                    LOG.warning("Rollback failed", e);
                }
                if (incomingQuotingStrategy != null) {
                    database.setObjectQuotingStrategy(incomingQuotingStrategy);
                }
            }
        }
    }

    @Override
    public boolean acquireLock() throws LockException {
        if (!lockDb || hasChangeLogLock) {
            return true;
        }

        quotingStrategy = database.getObjectQuotingStrategy();

        Executor executor = ExecutorService.getInstance().getExecutor(database);

        try {
            database.rollback();
            init();

            Boolean locked = ExecutorService.getInstance().getExecutor(database).queryForObject(new SelectFromDatabaseChangeLogLockStatement("LOCKED"), Boolean.class);

            if (locked != null && locked) {
                return false;
            } else {

                executor.comment("Lock hive metastore database");
                executor.execute(new LockDatabaseChangeLogStatement());
                database.commit();
                LOG.info("Change log lock has been successfully acquired");

                hasChangeLogLock = true;

                database.setCanCacheLiquibaseTableInfo(true);
                return true;
            }
        } catch (Exception e) {
            throw new LockException(e);
        } finally {
            try {
                database.rollback();
            } catch (DatabaseException e) {
                LOG.warning("Rollback failed", e);
            }
        }
    }

    @Override
    public void setDatabase(Database database) {
        if (lockDb) {
            super.setDatabase(database);
        }
    }

    @Override
    public void setChangeLogLockWaitTime(long changeLogLockWaitTime) {
        if (lockDb) {
            super.setChangeLogLockWaitTime(changeLogLockWaitTime);
        }
    }

    @Override
    public void setChangeLogLockRecheckTime(long changeLogLockRecheckTime) {
        if (lockDb) {
            super.setChangeLogLockWaitTime(changeLogLockRecheckTime);
        }
    }

    @Override
    public void init() throws DatabaseException {
        if (lockDb) {
            super.init();
        }
    }

    @Override
    public boolean hasChangeLogLock() {
        return !lockDb || super.hasChangeLogLock();
    }

    @Override
    public void waitForLock() throws LockException {
        if (lockDb) {
            super.waitForLock();
        }
    }

    @Override
    public DatabaseChangeLogLock[] listLocks() throws LockException {
        if (!lockDb) {
            return new DatabaseChangeLogLock[0];
        }
        return super.listLocks();
    }

    @Override
    public void forceReleaseLock() throws LockException, DatabaseException {
        if (lockDb) {
            super.forceReleaseLock();
        }
    }

    @Override
    public void reset() {
        if (lockDb) {
            super.reset();
        }

    }

    @Override
    public void destroy() throws DatabaseException {
        if (lockDb) {
            super.destroy();
        }
    }
}
