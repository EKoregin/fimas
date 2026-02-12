package ru.korevg.fimas.model;

import java.util.Set;

public class Firewall {
    private long id;
    private String name;
    private String description;
    private Model model;
    private Set<Policy> policies;
}
