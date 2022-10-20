package com.kaliv.mapper;

import com.kaliv.annotation.Column;
import com.kaliv.annotation.Id;
import com.kaliv.annotation.Table;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ORM<T> {
    private final Connection connection;

    private AtomicInteger id = new AtomicInteger(0);

    public static <T> ORM<T> getConnection() throws Exception {
        return new ORM<>();
    }

    private ORM() throws Exception {
        String rootPath = Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        String dbConfigPath = rootPath + "db.properties";
        Properties dbProps = new Properties();
        dbProps.load(new FileInputStream(dbConfigPath));

        String url = dbProps.getProperty("url");
        String username = dbProps.getProperty("username");
        String password = dbProps.getProperty("password");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setUrl(url);

        this.connection = dataSource.getConnection();
    }

    public void createTable(Set<Class<?>> classes) throws SQLException {
        for (Class<?> cls : classes) {
            String tableName = cls.getAnnotation(Table.class).name();
            Field[] declaredFields = cls.getDeclaredFields();
            Field primaryKey = Arrays.stream(declaredFields)
                    .filter(f -> f.isAnnotationPresent(Id.class))
                    .findFirst()
                    .orElseThrow();
            String pKeyType = primaryKey.getType().getName();

            StringJoiner joiner = new StringJoiner(", ");

            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Column.class)) {
                    String columnName = field.getAnnotation(Column.class).name();
                    String isNullable = field.getAnnotation(Column.class).nullable() ? "" : "not null";

                    //TODO: add boolean and other types
                    String columnType = null;
                    if (field.getType() == String.class) {
                        columnType = String.format("varchar(%d)", field.getAnnotation(Column.class).length());
                    } else if (field.getType() == int.class) {
                        columnType = int.class.getName();
                    }
                    String col = String.format("%s %s %s",
                            columnName,
                            columnType,
                            isNullable);
                    joiner.add(col);
                }
            }

            String sql = String.format("create table if not exists %s (id %s not null, %s)",
                    tableName, pKeyType, joiner);

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        }
    }

    public void dropTable(T t) throws SQLException {
        Class<?> cls = (Class<?>) t;
        String tableName = cls.getAnnotation(Table.class).name();
        String sql = String.format("drop table if exists %s", tableName);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.executeUpdate();
    }

    public void write(T t) throws Exception {
        Class<?> cls = t.getClass();
        Field[] declaredFields = cls.getDeclaredFields();

        Field primaryKey = null;
        ArrayList<Field> columns = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(", ");

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = field;

            } else if (field.isAnnotationPresent(Column.class)) {
                columns.add(field);
                joiner.add(field.getAnnotation(Column.class).name());
            }
        }

        int size = columns.size() + 1;
        String qMarks = IntStream.range(0, size)
                .mapToObj(e -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("insert into %s (%s, %s) values (%s)",
                cls.getAnnotation(Table.class).name(),
                Objects.requireNonNull(primaryKey).getName(),
                joiner,
                qMarks);

        PreparedStatement statement = connection.prepareStatement(sql);

        int index = 1;
        if (primaryKey.getType() == int.class) {
            statement.setInt(index++, id.incrementAndGet());
        }

        for (Field field : columns) {
            field.setAccessible(true);
            if (field.getType() == int.class) {
                statement.setInt(index++, (int) field.get(t));
            } else if (field.getType() == String.class) {
                statement.setString(index++, (String) field.get(t));
            }
        }
        statement.executeUpdate();
    }

    public T read(T t, int id) throws Exception {
        Class<?> cls = (Class<?>) t;
        Field[] declaredFields = cls.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();

        String tableName = cls.getAnnotation(Table.class).name();
        String sql = String.format("select * from %s where %s = %d",
                tableName, primaryKey.getName(), id);

        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        rs.next();

        return readEntityFromDb(cls, declaredFields, primaryKey, rs);
    }

    public List<T> readAll(T t) throws Exception {
        Class<?> cls = (Class<?>) t;
        Field[] declaredFields = cls.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();

        String tableName = cls.getAnnotation(Table.class).name();
        String sql = String.format("select * from %s", tableName);

        PreparedStatement stmt = connection.prepareStatement(sql);

        ResultSet rs = stmt.executeQuery();
        rs.next();

        List<T> tList = new ArrayList<>();
        while (!rs.isAfterLast()) {
            T entity = readEntityFromDb(cls, declaredFields, primaryKey, rs);
            tList.add(entity);
            rs.next();
        }
        return tList;
    }

    public void delete(T t, long l) throws Exception {
        Class<?> cls = (Class<?>) t;
        Field[] declaredFields = cls.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();

        String tableName = cls.getAnnotation(Table.class).name();
        String sql = String.format("delete from %s where %s = %d",
                tableName, primaryKey.getName(), l);

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.executeUpdate();
    }

    @SuppressWarnings("all")
    private T readEntityFromDb(Class<?> tClass,
                               Field[] declaredFields,
                               Field primaryKey,
                               ResultSet rs) throws Exception {
        T t = (T) tClass.getConstructor().newInstance();
        int id = rs.getInt(primaryKey.getName());
        primaryKey.setAccessible(true);
        primaryKey.set(t, id);

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);

                if (field.getType() == int.class) {
                    field.set(t, rs.getInt(field.getName()));
                } else if (field.getType() == String.class) {
                    field.set(t, rs.getString(field.getAnnotation(Column.class).name()));
                }
            }
        }
        return t;
    }
}