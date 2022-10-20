package com.kaliv.utils;

import com.kaliv.annotation.Entity;
import org.reflections.Reflections;

import java.util.Set;

public class PackageScanner {

    private final String PACKAGE_NAME = "com.kaliv.entity";

    public Set<Class<?>> findClasses() {
        Reflections reflections = new Reflections(PACKAGE_NAME);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }
}
