package com.kaliv.entity;

import com.kaliv.annotation.Column;
import com.kaliv.annotation.Entity;
import com.kaliv.annotation.Id;
import com.kaliv.annotation.Table;

@Entity
@Table(name = "philosophers")
public class Philosopher {
    @Id
    private int id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "nationality", length = 20, nullable = true)
    private String nationality;

    @Column(name = "school_of_thought", length = 30, nullable = false)
    private String school;

    public Philosopher(String name, String nationality, String school) {
        this.name = name;
        this.nationality = nationality;
        this.school = school;
    }

    //needed for creating new instance through reflection
    public Philosopher() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    @Override
    public String toString() {
        return "Philosopher{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", nationality='" + nationality + '\'' +
                ", school=" + school +
                '}';
    }
}