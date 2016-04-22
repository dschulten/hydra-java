package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Created by Dietrich on 18.04.2016.
 */
@JsonInclude(NON_EMPTY)
public class SirenField {
    private String name;
    private String type;
    private String value;
    private String title;

    public SirenField(String name, String type, String value, String title) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }
}
