package com.kaliv.application;

import com.kaliv.entity.Philosopher;
import com.kaliv.mapper.ORM;
import com.kaliv.utils.PackageScanner;

import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class Main {
    public static void main(String[] args) throws Exception {
        Philosopher socrates = new Philosopher("Socrates", "Greek", "Athens");
        Philosopher plato = new Philosopher("Plato", "Greek", "Athens");
        Philosopher aristotle = new Philosopher("Aristotle", "Greek", "Athens");
        Philosopher pythagoras = new Philosopher("Pythagoras", "Greek", "Crotone");

        ORM<Object> orm = ORM.getConnection();

        PackageScanner scanner = new PackageScanner();
        Set<Class<?>> classes = scanner.findClasses();
        orm.createTable(classes);

        orm.write(socrates);
        orm.write(plato);
        orm.write(aristotle);
        orm.write(pythagoras);

        Philosopher aristotle_from_db = (Philosopher) orm.read(Philosopher.class, 3);
        System.out.println(aristotle_from_db);
        System.out.println("=============");

        aristotle.setSchool("Stagira");
        orm.update(aristotle, 3);

        Philosopher updated_aristotle_from_db = (Philosopher) orm.read(Philosopher.class, 3);
        System.out.println(updated_aristotle_from_db);
        System.out.println("=============");

        List<Philosopher> result = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class);
        result.stream().forEach(e -> System.out.println(e));
        System.out.println("=============");

        orm.delete(Philosopher.class, 1);
        List<Philosopher> updatedRsult = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class);
        updatedRsult.stream().forEach(e -> System.out.println(e));

        orm.dropTable(Philosopher.class);
    }
}
