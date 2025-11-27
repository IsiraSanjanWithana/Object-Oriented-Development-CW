package com.teammate.models;

// Abstract base class - satisfies "Inheritance" requirement
public abstract class Person {
    protected String id;
    protected String name;
    protected String email;

    public Person(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }

    // Abstract method to demonstrate polymorphism capability
    public abstract String getDetails();
}