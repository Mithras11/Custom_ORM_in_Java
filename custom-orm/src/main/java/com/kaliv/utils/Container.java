package com.kaliv.utils;

import java.lang.reflect.Field;

public class Container {
    private final Class<?> cls;
    private final Field[] declaredFields;
    private final Field primaryKey;
    private final String tableName;

    public Container(Class<?> cls, Field[] declaredFields, Field primaryKey, String tableName) {
        this.cls = cls;
        this.declaredFields = declaredFields;
        this.primaryKey = primaryKey;
        this.tableName = tableName;
    }

    public Class<?> getCls() {
        return cls;
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
