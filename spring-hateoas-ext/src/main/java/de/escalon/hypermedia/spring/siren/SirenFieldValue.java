package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Created by Dietrich on 24.04.2016.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SirenFieldValue {
    private Object value;
    private Boolean selected;

    // TODO: add title
    public SirenFieldValue(Object value, Boolean selected) {
        this.value = value;
        this.selected = selected != null && selected == true ? selected : null;
    }

    public Object getValue() {
        return value;
    }

    public Boolean isSelected() {
        return selected;
    }
}
