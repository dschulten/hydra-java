package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Offer;
import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.beans.store.Store;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Currency;

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
    Store getStore() {
        Store store = new Store();

        store.addOffer(createOffer(productController.getProduct("9052001"), 2.80));
        store.addOffer(createOffer(productController.getProduct("9052002"), 1.40));
        store.addOffer(createOffer(productController.getProduct("9052003"), 1.10));
        store.addOffer(createOffer(productController.getProduct("9052004"), 1.50));
        store.addOffer(createOffer(productController.getProduct("9052005"), 2.20));

        store.add(AffordanceBuilder.linkTo(methodOn(this
                .getClass()).getStore())
                .withSelfRel());
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
