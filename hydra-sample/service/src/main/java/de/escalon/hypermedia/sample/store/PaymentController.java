package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Order;
import de.escalon.hypermedia.sample.beans.store.Payment;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.OrderStatus;
import de.escalon.hypermedia.spring.AffordanceBuilder;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private OrderBackend orderBackend;

    @Autowired
    private OrderAssembler orderAssembler;

    @RequestMapping("/{id}/payment")
    public ResponseEntity<Void> makePayment(@PathVariable int orderId) {
        orderBackend.payOrder(orderId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(linkTo(AffordanceBuilder.methodOn(this.getClass()).getPayment()).toUri());
        return new ResponseEntity<Void>(httpHeaders, HttpStatus.CREATED);
    }

    public Payment getPayment() {
        return null;
    }

}
