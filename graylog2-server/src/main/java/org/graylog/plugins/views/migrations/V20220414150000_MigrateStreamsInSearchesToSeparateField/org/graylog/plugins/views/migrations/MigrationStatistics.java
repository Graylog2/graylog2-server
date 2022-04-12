package org.graylog.plugins.views.migrations.V20220414150000_MigrateStreamsInSearchesToSeparateField.org.graylog.plugins.views.migrations;

public class MigrationStatistics {

    private long numMigrated = 0L;
    private long numUnchanged = 0L;
    private long numWrongFilter = 0L;

    public boolean canBeMigrated() {
        return !hasFilterError() && numMigrated > 0;
    }

    public boolean canBeUnchanged() {
        return !hasFilterError() && numMigrated == 0;
    }

    public boolean hasFilterError() {
        return numWrongFilter > 0;
    }

    public void newUnchanged() {
        numUnchanged++;
    }

    public void newMigrated() {
        numMigrated++;
    }

    public void newWrongFilter() {
        numWrongFilter++;
    }

    public long getNumMigrated() {
        return numMigrated;
    }

    public long getNumUnchanged() {
        return numUnchanged;
    }

    public long getNumWrongFilter() {
        return numWrongFilter;
    }
}
