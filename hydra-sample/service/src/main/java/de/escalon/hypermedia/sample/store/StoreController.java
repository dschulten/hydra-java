package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.sample.beans.store.Offer;
import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.beans.store.Store;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

/**
 * Created by Dietrich on 17.02.2015.
 */
@RequestMapping("/store")
@Controller
public class StoreController {

    @Autowired
    ProductController productController;

    @RequestMapping
    public
    @ResponseBody
    Store getStoreWithOffers() {
        Store store = createStoreWithOffers();

        List<Offer> offers = store.getMakesOffer();
        for (Offer offer : offers) {
            Product itemOffered = offer.getItemOffered();
            itemOffered.add(linkTo(methodOn(OrderController.class).makeOrder(itemOffered))
                    .rel(new TypedResource("Order"), "orderedItem")
                    .build());
        }
        // TODO: support rev for links and make hydra collection aware of @reverse terms in context - both should work
        store.add(linkTo(methodOn(OrderController.class).getOrders(null)).reverseRel("seller", "orders")
                .build());
        return store;
    }

    public HttpEntity<CollectionModel<Offer>> getOffers() {
        CollectionModel<Offer> offers = new CollectionModel<Offer>(
                mockOffers());
        return new HttpEntity<CollectionModel<Offer>>(offers);
    }

    private List<Offer> mockOffers() {
        List<Offer> offers = new ArrayList<Offer>();
        offers.add(createOffer(productController.getProduct("9052001"), 2.80));
        offers.add(createOffer(productController.getProduct("9052002"), 1.40));
        offers.add(createOffer(productController.getProduct("9052003"), 1.10));
        offers.add(createOffer(productController.getProduct("9052004"), 1.50));
        offers.add(createOffer(productController.getProduct("9052005"), 2.20));
        return offers;
    }

    public Store createStoreWithOffers() {
        Store store = createStoreWithoutOffers();
        List<Offer> offers = mockOffers();
        for (Offer offer : offers) {
            store.addOffer(offer);
        }
        return store;
    }

    public Store createStoreWithoutOffers() {
        Store store = new Store();
        store.add(AffordanceBuilder.linkTo(methodOn(this
                .getClass()).getStoreWithOffers())
                .withSelfRel());
//                make it an error if manages blcok is incompliet
        return store;
    }

    private Offer createOffer(Product product, double val) {
        Offer offer = new Offer();
        offer.setItemOffered(product);
        BigDecimal price = BigDecimal.valueOf(val)
                .setScale(2);
        offer.setPrice(price);
        offer.setPriceCurrency(Currency.getInstance("EUR"));
        return offer;
    }


}
