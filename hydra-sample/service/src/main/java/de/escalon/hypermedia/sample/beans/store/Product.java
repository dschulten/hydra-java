package de.escalon.hypermedia.sample.beans.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.hydra.mapping.Term;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Term(define = "accessories", as = "isAccessoryOrSparePartFor", reverse = true)
@Relation("product")
public class Product extends RepresentationModel<Product> {
    public final String name;
    public final String productID;
    private Offer offer;
    public List<Product> accessories = new ArrayList<>();

    public Offer getOffers() {
        return offer;
    }

    @JsonCreator
    public Product(@JsonProperty("name") String name, @JsonProperty("productID") String productID) {
        this.name = name;
        this.productID = productID;
    }

    public void addOffer(Offer offer) {
        this.offer = offer;
    }

    public boolean hasExtra(String accessoryId) {
        for (Product accessory : accessories) {
            if (accessory.productID.equals(accessoryId)) {
                return true;
            }
        }
        return false;
    }

    public Product getExtra(String accessoryId) {
        for (Product accessory : accessories) {
            if (accessory.productID.equals(accessoryId)) {
                return accessory;
            }
        }
        return null;
    }

    public int getExtraId(String accessoryId) {
        int index = 0;
        for (Product accessory : accessories) {
            if (accessory.productID.equals(accessoryId)) {
                return index;
            }
        }
        return -1;
    }

    public void addAccessory(Product product) {
        this.accessories.add(product);
    }
}
