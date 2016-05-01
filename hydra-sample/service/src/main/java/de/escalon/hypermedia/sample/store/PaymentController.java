package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Payment;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Controller
public class PaymentController {

    @RequestMapping("/{id}/payment")
    public ResponseEntity<Void> makePayment(@PathVariable int id) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(linkTo(AffordanceBuilder.methodOn(this.getClass())
                .getPayment()).toUri());
        return new ResponseEntity<Void>(httpHeaders, HttpStatus.CREATED);
    }

    public Payment getPayment() {
        return null;
    }


}
