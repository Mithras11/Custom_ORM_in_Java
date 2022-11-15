package com.kaliv.utils;

public class Constants {
    public static final String PROPS = "app.properties";
    public static final String URL = "db.url";
    public static final String USER = "db.username";
    public static final String PASS = "db.password";
    public static final String PACKAGE = "package_name";

    public static final String DELIMITER = ", ";

    public static final String NOT_NULL = "not null";
    public static final String VARCHAR = "varchar(%d)";
    public static final String BIT = "bit";
    public static final String COL_VALUES = "%s %s %s";
    public static final String STRING_VALUE = "%s = '%s'";
    public static final String NUMERIC_VALUE = "%s = %d";

    public static final String READ_LAST_PRIMARY_KEY_IN_DB = "select max(id) from %s";
    public static final String CREATE_TABLE_QUERY = "create table if not exists %s (id %s not null, %s)";
    public static final String DROP_TABLE_QUERY = "drop table if exists %s";
    public static final String INSERT_QUERY = "insert into %s (%s, %s) values (%s)";
    public static final String UPDATE_QUERY = "update %s set %s where %s = %d";
    public static final String SELECT_QUERY = "select * from %s where %s = %d";
    public static final String SELECT_ALL_QUERY = "select * from %s";
    public static final String DELETE_QUERY = "delete from %s where %s = %d";
}
