package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.action.ResourceHandler;
import de.escalon.hypermedia.sample.beans.store.Order;
import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

/**
 * Created by Dietrich on 17.02.2015.
 */
@ExposesResourceFor(Order.class)
@RequestMapping("/orders")
@Controller
public class OrderController {

    @Autowired
    private OrderBackend orderBackend;

    @Autowired
    private OrderAssembler orderAssembler;


    @ResourceHandler(Cardinality.COLLECTION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> makeOrder(@RequestBody Product product) {
        OrderModel orderModel = orderBackend.createOrder();
        orderModel = orderBackend.addOrderedItem(orderModel.getId(),
                new ProductModel(product.name, product.productID));
        AffordanceBuilder location = linkTo(methodOn(this.getClass()).getOrder(orderModel.getId()));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location.toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }


    @RequestMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable int id) {
        OrderModel orderModel = orderBackend.getOrder(id);
        Order order = orderAssembler.toResource(orderModel);
        order.add(linkTo(methodOn(PaymentController.class).makePayment(id)).withRel("paymentUrl"));
        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Resources<Order>> getOrders() {
        Resources<Order> orderResources = new Resources<Order>(orderAssembler.toResources(orderBackend.getOrders()));
        return new ResponseEntity<Resources<Order>>(orderResources, HttpStatus.OK);
    }
}
