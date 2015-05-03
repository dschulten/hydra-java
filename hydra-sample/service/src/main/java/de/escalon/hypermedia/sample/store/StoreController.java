package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Store;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

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
        store.add(AffordanceBuilder.linkTo(methodOn(this.getClass()).getStore()).withSelfRel());
        return store;
    }
}
