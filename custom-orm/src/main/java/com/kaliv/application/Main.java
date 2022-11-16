package com.kaliv.application;

import com.kaliv.entity.Philosopher;
import com.kaliv.mapper.ORM;
import com.kaliv.utils.PackageScanner;

import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class Main {
    public static void main(String[] args) throws Exception {
        //setting test data
        final Philosopher socrates = new Philosopher("Socrates", "Greek", "Athens");
        final Philosopher plato = new Philosopher("Plato", "Greek", "Athens");
        final Philosopher aristotle = new Philosopher("Aristotle", "Greek", "Athens");
        final Philosopher pythagoras = new Philosopher("Pythagoras", "Greek", "Crotone");

        ORM<Object> orm = ORM.getConnection();

        PackageScanner scanner = new PackageScanner();
        Set<Class<?>> classes = scanner.findClasses();
        orm.createTable(classes);

        //seed database
        orm.write(socrates);
        orm.write(plato);
        orm.write(aristotle);
        orm.write(pythagoras);

        //test read method
        Philosopher aristotle_from_db = (Philosopher) orm.read(Philosopher.class, 3);
        System.out.println(aristotle_from_db);
        System.out.println("=============");

        //test update method
        final Philosopher updated_aristotle=new Philosopher(
                aristotle.getName(),
                aristotle.getNationality(),
                "Stagira"
        );
        orm.update(updated_aristotle, 3);

        Philosopher updated_aristotle_from_db = (Philosopher) orm.read(Philosopher.class, 3);
        System.out.println(updated_aristotle_from_db);
        System.out.println("=============");

        //test readAll method
        List<Philosopher> result = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class);
        result.stream().forEach(e -> System.out.println(e));
        System.out.println("=============");

        //test delete method
        orm.delete(Philosopher.class, 1);
        List<Philosopher> updatedRsult = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class);
        updatedRsult.stream().forEach(e -> System.out.println(e));

//        orm.dropTable(Philosopher.class);
    }
}
