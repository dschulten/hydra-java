package de.escalon.hypermedia.spring.uber;

/**
 * Created by Dietrich on 06.05.2016.
 */
public class UberField {
    private String name;
    private String value;

    public UberField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
