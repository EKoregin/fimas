package ru.korevg.fimas.model;

import java.util.Set;

public class Policy {
    private long id;
    private String name;
    private String description;
    private Set<Address> srcAddresses;
    private Set<Address> dstAddresses;
    private Set<Service> services;
    private PolicyAction action;
    private PolicyStatus status;
}
