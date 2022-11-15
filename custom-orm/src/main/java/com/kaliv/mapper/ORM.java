package com.kaliv.mapper;

import com.kaliv.annotation.Column;
import com.kaliv.annotation.Id;
import com.kaliv.annotation.Table;
import com.kaliv.utils.Constants;
import com.kaliv.utils.Container;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ORM<T> {
    private final Connection connection;
    private final Map<String, Integer> primaryKeysInDb = new HashMap<>();

    public static <T> ORM<T> getConnection() throws Exception {
        return new ORM<>();
    }

    private ORM() throws Exception {
        MysqlDataSource dataSource = getDataSource();
        this.connection = dataSource.getConnection();
    }

    public void createTable(Set<Class<?>> classes) throws SQLException {
        for (Class<?> cls : classes) {
            Container classData = readClassData(cls);
            Field[] declaredFields = classData.getDeclaredFields();
            Field primaryKey = classData.getPrimaryKey();
            String tableName = classData.getTableName();
            String pKeyType = primaryKey.getType().getName();

            StringJoiner joiner = new StringJoiner(Constants.DELIMITER);
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Column.class)) {
                    String columnName = field.getAnnotation(Column.class).name();
                    String isNullable = field.getAnnotation(Column.class).nullable() ? "" : Constants.NOT_NULL;

                    //possible to add other types
                    String columnType = null;
                    if (field.getType() == String.class) {
                        columnType = String.format(Constants.VARCHAR, field.getAnnotation(Column.class).length());
                    } else if (field.getType() == int.class) {
                        columnType = int.class.getName();
                    } else if (field.getType() == boolean.class) {
                        columnType = Constants.BIT;
                    }
                    String col = String.format(Constants.COL_VALUES,
                            columnName,
                            columnType,
                            isNullable);
                    joiner.add(col);
                }
            }

            String sql = String.format(Constants.CREATE_TABLE_QUERY,
                    tableName, pKeyType, joiner);

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        }
    }

    public void dropTable(T t) throws SQLException {
        Class<?> cls = (Class<?>) t;
        String tableName = cls.getAnnotation(Table.class).name();
        String sql = String.format(Constants.DROP_TABLE_QUERY, tableName);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.executeUpdate();
    }

    public void write(T t) throws Exception {
        Class<?> cls = t.getClass();
        Field[] declaredFields = cls.getDeclaredFields();

        Field primaryKey = null;
        ArrayList<Field> columns = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(Constants.DELIMITER);
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
                .collect(Collectors.joining(Constants.DELIMITER));

        String sql = String.format(Constants.INSERT_QUERY,
                cls.getAnnotation(Table.class).name(),
                Objects.requireNonNull(primaryKey).getName(),
                joiner,
                qMarks);

        PreparedStatement statement = connection.prepareStatement(sql);

        int index = 1;
        int pkInDb = getPrimaryKeyFromDb(cls);
        String tableName = cls.getAnnotation(Table.class).name();
        if (primaryKey.getType() == int.class) {
            pkInDb++;
            statement.setInt(index++, pkInDb);
            primaryKeysInDb.put(tableName, pkInDb);
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

    public void update(T t, int id) throws Exception {
        Class<?> cls = t.getClass();
        Field[] declaredFields = cls.getDeclaredFields();

        Field primaryKey = null;
        StringJoiner joiner = new StringJoiner(Constants.DELIMITER);
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = field;
                continue;
            }
            if (field.isAnnotationPresent(Column.class)) {
                String col = null;
                if (field.getType() == String.class) {
                    col = String.format(Constants.STRING_VALUE,
                            field.getAnnotation(Column.class).name(), field.get(t));
                } else if (field.getType() == int.class) {
                    col = String.format(Constants.NUMERIC_VALUE,
                            field.getAnnotation(Column.class).name(), (int) field.get(t));
                } else if (field.getType() == boolean.class) {
                    col = String.format(Constants.NUMERIC_VALUE,
                            field.getAnnotation(Column.class).name(), (boolean) field.get(t) ? 1 : 0);
                }
                joiner.add(col);
            }
        }

        String sql = String.format(Constants.UPDATE_QUERY,
                cls.getAnnotation(Table.class).name(),
                joiner,
                Objects.requireNonNull(primaryKey).getName(),
                id);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.executeUpdate();
    }

    public T read(Class<?> cls, int id) throws Exception {
        Container classData = readClassData(cls);
        Field[] declaredFields = classData.getDeclaredFields();
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sql = String.format(Constants.SELECT_QUERY,
                tableName, primaryKey.getName(), id);

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        rs.next();

        return readEntityFromDb(cls, declaredFields, primaryKey, rs);
    }

    public List<T> readAll(Class<?> cls) throws Exception {
        Container classData = readClassData(cls);
        Field[] declaredFields = classData.getDeclaredFields();
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sql = String.format(Constants.SELECT_ALL_QUERY, tableName);
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        rs.next();

        List<T> tList = new ArrayList<>();
        while (!rs.isAfterLast()) {
            T entity = readEntityFromDb(cls, declaredFields, primaryKey, rs);
            tList.add(entity);
            rs.next();
        }
        return tList;
    }

    public void delete(Class<?> cls, int l) throws Exception {
        Container classData = readClassData(cls);
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sql = String.format(Constants.DELETE_QUERY,
                tableName, primaryKey.getName(), l);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.executeUpdate();
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

    private int getPrimaryKeyFromDb(Class<?> cls) throws SQLException {
        String tableName = cls.getAnnotation(Table.class).name();
        if (primaryKeysInDb.containsKey(tableName)) {
            return primaryKeysInDb.get(tableName);
        }

        String sql = String.format(Constants.READ_LAST_PRIMARY_KEY_IN_DB, tableName);
        PreparedStatement getPrimaryKey = connection.prepareStatement(sql);
        ResultSet rs = getPrimaryKey.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private Container readClassData(Class<?> cls) {
        Field[] declaredFields = cls.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();

        String tableName = cls.getAnnotation(Table.class).name();
        return new Container(cls, declaredFields, primaryKey, tableName);
    }

    private static MysqlDataSource getDataSource() throws IOException {
        String rootPath = Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        String dbConfigPath = rootPath + Constants.PROPS;
        Properties dbProps = new Properties();
        dbProps.load(new FileInputStream(dbConfigPath));

        String url = dbProps.getProperty(Constants.URL);
        String username = dbProps.getProperty(Constants.USER);
        String password = dbProps.getProperty(Constants.PASS);

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setUrl(url);
        return dataSource;
    }
}