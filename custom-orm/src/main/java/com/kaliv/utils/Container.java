package com.kaliv.utils;

import java.lang.reflect.Field;

public class Container {
    private final Field[] declaredFields;
    private final Field primaryKey;
    private final String tableName;

    public Container(Field[] declaredFields, Field primaryKey, String tableName) {
        this.declaredFields = declaredFields;
        this.primaryKey = primaryKey;
        this.tableName = tableName;
    }

    public Field[] getDeclaredFields() {
        return declaredFields;
    }

    public Field getPrimaryKey() {
        return primaryKey;
    }

    public String getTableName() {
        return tableName;
    }
}
