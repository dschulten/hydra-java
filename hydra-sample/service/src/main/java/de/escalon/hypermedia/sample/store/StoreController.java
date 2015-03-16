package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.Offer;
import de.escalon.hypermedia.sample.beans.Order;
import de.escalon.hypermedia.sample.beans.Product;
import de.escalon.hypermedia.sample.beans.Store;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;

/**
 * Created by Dietrich on 17.02.2015.
 */
@RequestMapping("/store")
@Controller
public class StoreController {

    @RequestMapping
    public
    @ResponseBody
    Store getStore() {
        Store store = new Store();
        return store;
    }
}
