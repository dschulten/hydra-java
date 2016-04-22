package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Created by Dietrich on 18.04.2016.
 */
@JsonInclude(NON_EMPTY)
@JsonPropertyOrder({"name", "title","type", "value"})
public class SirenField extends AbstractSirenNode {
    private String name;
    private String type;
    private String value;

    public SirenField(String name, String type, String value, String title) {
        super(title);
        this.name = name;
        this.type = type;
        this.value = value;
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

}
