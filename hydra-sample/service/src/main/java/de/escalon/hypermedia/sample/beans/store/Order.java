package de.escalon.hypermedia.sample.beans.store;

import de.escalon.hypermedia.hydra.mapping.Expose;
import org.springframework.hateoas.ResourceSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class Order extends ResourceSupport {
    private List<Product> items = new ArrayList<Product>();
    private Offer acceptedOffer;

    public void addItem(Product orderedItem) {
        this.items.add(orderedItem);
    }

    @Expose("orderedItem")
    public List<? extends Product> getItems() {
        return items;
    }

    public void setAcceptedOffer(Offer acceptedOffer) {
        this.acceptedOffer = acceptedOffer;
    }
}
