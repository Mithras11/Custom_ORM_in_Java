package com.kaliv.utils;

import com.kaliv.annotation.Entity;
import com.kaliv.annotation.Table;
import org.reflections.Reflections;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PackageScanner {
    public Set<Class<?>> findClasses(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Entity.class);

//        List<String> tableNames = classes.stream()
//                .map(cls -> cls.getAnnotation(Table.class).name())
//                .collect(Collectors.toList());

        return classes;
    }
}
