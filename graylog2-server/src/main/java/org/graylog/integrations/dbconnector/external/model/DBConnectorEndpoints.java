package org.graylog.integrations.dbconnector.external.model;

public enum DBConnectorEndpoints {
    ORACLE("Oracle", "Oracle"),
    MYSQL("MySQL", "MySQL"),
    MICROSOFT_SQL("Microsoft SQL", "Microsoft SQL"),
    POSTGRESQL("PostgreSQL", "PostgreSQL"),
    DB2("DB2", "DB2"),
    MONGODB("MongoDB", "MongoDB");

    private String displayName;
    private String url;

    private DBConnectorEndpoints(String displayName, String url) {
        this.displayName = displayName;
        this.url = url;
    }

    public static DBConnectorEndpoints getEnum(String value) {
        for (DBConnectorEndpoints v : values()) {
            if (v.name().equalsIgnoreCase(value)
                    || v.displayName().equalsIgnoreCase(value)
                    || v.url().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unable to find enum value for " + value);
    }

    public String displayName() {
        return displayName;
    }

    public String url() {
        return url;
    }

}
