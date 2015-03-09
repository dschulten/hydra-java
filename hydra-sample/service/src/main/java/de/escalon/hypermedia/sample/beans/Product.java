package de.escalon.hypermedia.sample.beans;

import org.springframework.hateoas.ResourceSupport;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class Product extends ResourceSupport {
    public final String name;
    private String productId;

    public Product(String name) {
        this.name = name;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
