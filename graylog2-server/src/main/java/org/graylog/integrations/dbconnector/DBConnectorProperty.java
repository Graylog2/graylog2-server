package org.graylog.integrations.dbconnector;

public class DBConnectorProperty {
    public static final String COLUMN_QUERY = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
    public static final String TEST_QUERY = "select * from ";
    public static final String TIMESTAMP = "Timestamp";
    public static final String INCREMENT = "increment";
    public static final String DB_CONNECTOR_OFFSET = "DB_CONNECTOR";
    public static final String MONGODB = "MongoDB";
    public static final String MYSQL = "MySQL";
    public static final String MSSQL = "Microsoft SQL";
    public static final String DB2 = "DB2";
    public static final String POSTGRES = "PostgreSQL";
    public static final String ORACLE = "Oracle";
    public static final int LIMIT_RECORDS = 10;
    public static final int BATCH_SIZE = 500;
    public static final String COUNT_QUERY = "select COUNT(1) as recordCount from ";
    public static final String LIMIT_CLAUSE = " LIMIT ? OFFSET  ?";
    public static final String FROM = " FROM ";
    public static final String SELECT_QUERY = "select * from ";
    public static final String RECORD_COUNT = "recordCount";
    public static final String WHERE_CLAUSE = " where ";
    public static final String CLOSE_DELIMITER = ";";
    public static final String DATE_CONDITION = " > TIMESTAMP( ? )";
    public static final String OFFSET_CONDITION = " > ?";
    public static final String ORDER_BY = " ORDER BY ";
}
