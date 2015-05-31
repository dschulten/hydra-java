package de.escalon.hypermedia.sample.beans.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class Product extends ResourceSupport {
    public final String name;

    public final String productID;

    @JsonCreator
    public Product(@JsonProperty("name") String name, @JsonProperty("productID") String productID) {
        this.name = name;
        this.productID = productID;
    }

}
