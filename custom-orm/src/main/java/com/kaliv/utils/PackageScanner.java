package com.kaliv.utils;

import com.kaliv.annotation.Entity;
import org.reflections.Reflections;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

public class PackageScanner {

    public Set<Class<?>> findClasses() throws IOException {
        String pkg_name = getPkgName();
        Reflections reflections = new Reflections(pkg_name);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }

    private static String getPkgName() throws IOException {
        String rootPath = Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        String dbConfigPath = rootPath + Constants.PROPS;
        Properties props = new Properties();
        props.load(new FileInputStream(dbConfigPath));

        return props.getProperty(Constants.PACKAGE);
    }
}
