package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.Offer;
import de.escalon.hypermedia.sample.beans.Order;
import de.escalon.hypermedia.sample.beans.Store;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;

/**
 * Created by Dietrich on 17.02.2015.
 * <p/>
 * CafeOrCoffeeShop
 * makesOffer -> Offers
 * Offers have Products
 * Offers have potentialAction=OrderAction
 * Offers have acceptedPaymentMethods
 * <p/>
 * How to get from offer to order: there seems to be a payment in between,
 * since order retains information about a previous payment. Maybe a PayAction or AcceptAction with purpose offer?
 * <p/>
 * <p/>
 * How to accept an offer: AcceptAction, BuyAction, PayAction, ConsumeAction, OrderAction
 * How to pay for an offer: need acceptedPaymentMethod
 * Order has paymentUrl or potentialAction PayAction but seems to be after the fact??
 * <p/>
 * What is needed is: choose payment method, return paymentUrl
 */
@RequestMapping("/store")
@Controller
public class StoreController {

    @RequestMapping
    public
    @ResponseBody
    Store getStore() {
        Store store = new Store();
        List<Offer> offers = store.getMakesOffer();
        for (Offer offer : offers) {
            Order order = new Order();
            offer.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(OrderController.class)
                    .makeOrder(order))
                    .withRel("acceptedOffer"));
        }
        return store;
    }
}
