package ru.korevg.fimas.model;

import java.net.InetAddress;
import java.util.Set;

public interface Address {

    public String getName();
    public String getDescription();
    public Set<InetAddress> getValue();
    public boolean isDynamic();

}
