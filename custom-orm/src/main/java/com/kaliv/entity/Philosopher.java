package com.kaliv.entity;

import com.kaliv.annotation.Column;
import com.kaliv.annotation.Entity;
import com.kaliv.annotation.PrimaryKey;
import com.kaliv.annotation.Table;

@Entity
@Table(name = "philosophers")
public class Philosopher {
    @PrimaryKey
    private int id;

    @Column
    private String name;

    @Column
    private String nationality;

    @Column
    private String school;

    public Philosopher(String name, String nationality, String school) {
        this.name = name;
        this.nationality = nationality;
        this.school = school;
    }

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




