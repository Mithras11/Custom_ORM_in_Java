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

    public void createTable(final Set<Class<?>> classes) throws SQLException {
        for (Class<?> clazz : classes) {
            Container classData = readClassData(clazz);
            Field[] declaredFields = classData.getDeclaredFields();
            Field primaryKey = classData.getPrimaryKey();
            String tableName = classData.getTableName();
            String primaryKeyType = primaryKey.getType().getName();

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
                    String column = String.format(Constants.COL_VALUES,
                            columnName,
                            columnType,
                            isNullable);
                    joiner.add(column);
                }
            }

            String sqlQuery = String.format(Constants.CREATE_TABLE_QUERY,
                    tableName, primaryKeyType, joiner);
            PreparedStatement statement = connection.prepareStatement(sqlQuery);
            statement.executeUpdate();
        }
    }

    public void dropTable(final T t) throws SQLException {
        Class<?> clazz = (Class<?>) t;
        String tableName = clazz.getAnnotation(Table.class).name();
        String sqlQuery = String.format(Constants.DROP_TABLE_QUERY, tableName);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        statement.executeUpdate();
    }

    public void write(final T t) throws Exception {
        Class<?> clazz = t.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();

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
        String questionMarks = IntStream.range(0, size)
                .mapToObj(e -> "?")
                .collect(Collectors.joining(Constants.DELIMITER));

        String sqlQuery = String.format(Constants.INSERT_QUERY,
                clazz.getAnnotation(Table.class).name(),
                Objects.requireNonNull(primaryKey).getName(),
                joiner,
                questionMarks);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);

        int index = 1;
        int primaryKeyFromDb = getPrimaryKeyFromDb(clazz);
        String tableName = clazz.getAnnotation(Table.class).name();
        if (primaryKey.getType() == int.class) {
            primaryKeyFromDb++;
            statement.setInt(index++, primaryKeyFromDb);
            primaryKeysInDb.put(tableName, primaryKeyFromDb);
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

    public void update(final T t, final int id) throws Exception {
        Class<?> clazz = t.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();

        Field primaryKey = null;
        StringJoiner joiner = new StringJoiner(Constants.DELIMITER);
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                primaryKey = field;
                continue;
            }
            if (field.isAnnotationPresent(Column.class)) {
                String column = null;
                if (field.getType() == String.class) {
                    column = String.format(Constants.STRING_VALUE,
                            field.getAnnotation(Column.class).name(), field.get(t));
                } else if (field.getType() == int.class) {
                    column = String.format(Constants.NUMERIC_VALUE,
                            field.getAnnotation(Column.class).name(), (int) field.get(t));
                } else if (field.getType() == boolean.class) {
                    column = String.format(Constants.NUMERIC_VALUE,
                            field.getAnnotation(Column.class).name(), (boolean) field.get(t) ? 1 : 0);
                }
                joiner.add(column);
            }
        }

        String sqlQuery = String.format(Constants.UPDATE_QUERY,
                clazz.getAnnotation(Table.class).name(),
                joiner,
                Objects.requireNonNull(primaryKey).getName(),
                id);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        statement.executeUpdate();
    }

    public T read(final Class<?> clazz, final int id) throws Exception {
        Container classData = readClassData(clazz);
        Field[] declaredFields = classData.getDeclaredFields();
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sqlQuery = String.format(Constants.SELECT_QUERY,
                tableName, primaryKey.getName(), id);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();

        return readEntityFromDb(clazz, declaredFields, primaryKey, resultSet);
    }

    public List<T> readAll(final Class<?> clazz) throws Exception {
        Container classData = readClassData(clazz);
        Field[] declaredFields = classData.getDeclaredFields();
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sqlQuery = String.format(Constants.SELECT_ALL_QUERY, tableName);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();

        List<T> tList = new ArrayList<>();
        while (!resultSet.isAfterLast()) {
            T entity = readEntityFromDb(clazz, declaredFields, primaryKey, resultSet);
            tList.add(entity);
            resultSet.next();
        }
        return tList;
    }

    public void delete(final Class<?> clazz, final int id) throws Exception {
        Container classData = readClassData(clazz);
        Field primaryKey = classData.getPrimaryKey();
        String tableName = classData.getTableName();

        String sqlQuery = String.format(Constants.DELETE_QUERY,
                tableName, primaryKey.getName(), id);
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        statement.executeUpdate();
    }

    @SuppressWarnings("all")
    private T readEntityFromDb(final Class<?> tClass,
                               final Field[] declaredFields,
                               final Field primaryKey,
                               final ResultSet resultSet) throws Exception {
        T t = (T) tClass.getConstructor().newInstance();
        int id = resultSet.getInt(primaryKey.getName());
        primaryKey.setAccessible(true);
        primaryKey.set(t, id);

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                if (field.getType() == int.class) {
                    field.set(t, resultSet.getInt(field.getName()));
                } else if (field.getType() == String.class) {
                    field.set(t, resultSet.getString(field.getAnnotation(Column.class).name()));
                }
            }
        }
        return t;
    }

    private int getPrimaryKeyFromDb(final Class<?> clazz) throws SQLException {
        String tableName = clazz.getAnnotation(Table.class).name();
        if (primaryKeysInDb.containsKey(tableName)) {
            return primaryKeysInDb.get(tableName);
        }

        String sqlQuery = String.format(Constants.READ_LAST_PRIMARY_KEY_IN_DB, tableName);
        PreparedStatement getPrimaryKey = connection.prepareStatement(sqlQuery);
        ResultSet resultSet = getPrimaryKey.executeQuery();
        resultSet.next();
        return resultSet.getInt(1);
    }

    private Container readClassData(final Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();

        String tableName = clazz.getAnnotation(Table.class).name();
        return new Container(declaredFields, primaryKey, tableName);
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