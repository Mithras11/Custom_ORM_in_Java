package com.kaliv;

import com.kaliv.annotation.Column;
import com.kaliv.annotation.PrimaryKey;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ORM<T> {
    private final Connection connection;

    private AtomicLong id = new AtomicLong(0L);

    public static <T> ORM<T> getConnection() throws Exception {
        return new ORM<>();
    }

    private ORM() throws Exception {
//        this.connection = DriverManager.getConnection(
//                "jdbc:mysql://localhost:3306/custom_orm?useSSL=false&serverTimezone=UTC",
//                "root", "Goedel.Escher.Bach");

        String rootPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
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

    public void createTable(Set<T> classes) {

    }

    public void write(T t) throws Exception {
        Class<?> cls = t.getClass();
        Field[] declaredFields = cls.getDeclaredFields();

        Field primaryKey = null;
        ArrayList<Field> columns = new ArrayList<>();
        StringJoiner joiner = new StringJoiner(", ");

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                primaryKey = field;

            } else if (field.isAnnotationPresent(Column.class)) {
                columns.add(field);
                joiner.add(field.getName());
            }
        }

        int size = columns.size() + 1;
        String qMarks = IntStream.range(0, size)
                .mapToObj(e -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("insert into %s (%s, %s) values (%s)",
                cls.getSimpleName(), Objects.requireNonNull(primaryKey).getName(), joiner, qMarks);

        PreparedStatement statement = connection.prepareStatement(sql);

        int index = 1;
        if (primaryKey.getType() == long.class) {
            statement.setLong(index++, id.incrementAndGet());
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

    public T read(String className, long l) throws Exception {
        Class<?> tClass = Class.forName(String.format("com.kaliv.entity.%s", className));
        Field[] declaredFields = tClass.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
                .findFirst()
                .orElseThrow();

        String sql = String.format("select * from %s where %s = %d",
                tClass.getSimpleName(), primaryKey.getName(), l);

        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        rs.next();

        return readEntityFromDb(tClass, declaredFields, primaryKey, rs);
    }

    public List<T> readAll(String className) throws Exception {
        Class<?> tClass = Class.forName(String.format("com.kaliv.entity.%s", className));
        Field[] declaredFields = tClass.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
                .findFirst()
                .orElseThrow();

        String sql = String.format("select * from %s", tClass.getSimpleName());

        PreparedStatement stmt = connection.prepareStatement(sql);

        ResultSet rs = stmt.executeQuery();
        rs.next();

        List<T> tList = new ArrayList<>();
        while (!rs.isAfterLast()) {
            T t = readEntityFromDb(tClass, declaredFields, primaryKey, rs);
            tList.add(t);
            rs.next();
        }
        return tList;
    }

    public void delete(String className, long l) throws Exception {
        Class<?> tClass = Class.forName(String.format("com.kaliv.entity.%s", className));
        Field[] declaredFields = tClass.getDeclaredFields();
        Field primaryKey = Arrays.stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
                .findFirst()
                .orElseThrow();

        String sql = String.format("delete from %s where %s = %d",
                tClass.getSimpleName(), primaryKey.getName(), l);

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.executeUpdate();
    }

    @SuppressWarnings("all")
    private T readEntityFromDb(Class<?> tClass,
                               Field[] declaredFields,
                               Field primaryKey,
                               ResultSet rs) throws Exception {
        T t = (T) tClass.getConstructor().newInstance();
        long transactionId = rs.getInt(primaryKey.getName());
        primaryKey.setAccessible(true);
        primaryKey.set(t, transactionId);

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);

                if (field.getType() == int.class) {
                    field.set(t, rs.getInt(field.getName()));
                } else if (field.getType() == String.class) {
                    field.set(t, rs.getString(field.getName()));
                }
            }
        }
        return t;
    }
}