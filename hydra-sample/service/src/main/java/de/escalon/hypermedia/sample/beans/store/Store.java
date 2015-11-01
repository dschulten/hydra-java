package de.escalon.hypermedia.sample.beans.store;

import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.sample.store.OrderController;
import de.escalon.hypermedia.sample.store.ProductController;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Expose("CafeOrCoffeeShop")
public class Store extends ResourceSupport {

    public String name = "Kaffeehaus Hagen";

    private List<Offer> offers = new ArrayList<Offer>();

    public List<Offer> getMakesOffer() {

        for (Offer offer : offers) {
            Product itemOffered = offer.getItemOffered();
            itemOffered.add(linkTo(methodOn(OrderController.class).makeOrder(itemOffered))
                    .rel(new TypedResource("Order"), "orderedItem")
                    .build());
        }
        return offers;
    }

    public void addOffer(Offer offer) {
        this.offers.add(offer);
    }

}
