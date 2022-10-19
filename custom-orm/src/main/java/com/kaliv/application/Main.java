package com.kaliv.application;

import com.kaliv.ORM;
import com.kaliv.entity.Philosopher;
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

//        PackageScanner scanner = new PackageScanner();
////        Set<Class<?>> classes = scanner.findAllClassesUsingGoogleGuice(
////                "com.kaliv.entity");
//
        orm.write(socrates);
        orm.write(plato);
        orm.write(aristotle);
        orm.write(pythagoras);

        Philosopher socrates_from_db =
                (Philosopher) orm.read(Philosopher.class.getSimpleName(), 1L);
        System.out.println(socrates_from_db);
        System.out.println("=============");

        List<Philosopher> result = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class.getSimpleName());
        result.stream().forEach(e -> System.out.println(e));
        System.out.println("=============");

        orm.delete(Philosopher.class.getSimpleName(), 1L);
        List<Philosopher> updatedRsult = (List<Philosopher>) (List<?>) orm.readAll(Philosopher.class.getSimpleName());
        updatedRsult.stream().forEach(e -> System.out.println(e));
    }
}
