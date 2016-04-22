package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Created by Dietrich on 17.04.2016.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@JsonPropertyOrder({"name", "title", "method", "href", "type", "fields"})
public class SirenAction extends AbstractSirenNode {

    private String name;
    private String method;
    private String href;
    private String type;
    private List<SirenField> fields;

    public SirenAction(String name, String title, String method,
                       String href, String type, List<SirenField> fields) {
        super(title);
        this.name = name;
        this.method = method;
        this.href = href;
        this.type = type;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public String getHref() {
        return href;
    }

    public String getType() {
        return type;
    }

    public List<SirenField> getFields() {
        return fields;
    }
}
