package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.ResourceHandler;
import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.sample.beans.store.*;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.OrderStatus;
import de.escalon.hypermedia.sample.model.store.ProductModel;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

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

    @Autowired
    private ProductController productController;

    @Autowired
    private StoreController storeController;


    @ResourceHandler(Cardinality.COLLECTION)
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> makeOrder(@Input(include = "productID") @RequestBody Product product) {

        OrderModel orderModel = orderBackend.createOrder();
        Product resolvedProduct = productController.getProduct(product.productID);
        orderModel = orderBackend.addOrderedItem(orderModel.getId(),
                new ProductModel(resolvedProduct.name, resolvedProduct.productID));
        return redirectToUpdatedOrder(orderModel.getId());

    }

    @RequestMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable int orderId) {
        OrderModel orderModel = orderBackend.getOrder(orderId);
        Order order = orderAssembler.toResource(orderModel);

        // offer extras for each product
        List<? extends Product> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            Product product = items.get(i);
            Offer offer = new Offer();
            if (!product
                    .hasExtra("9052006")) {
                Offer shot = createAddOnOffer(product, "9052006", 0.2, orderId, i);
                offer.addOn(shot);
            }
            if (!product.hasExtra("9052007")) {
                Offer briocheCrema = createAddOnOffer(product, "9052007", 0.8, orderId, i);
                offer.addOn(briocheCrema);
            }
            if (!offer.getAddOns()
                    .isEmpty()) {
                product.addOffer(offer);
            }
        }

        Store store = storeController.createStoreWithoutOffers();
        store.add(linkTo(methodOn(this.getClass()).getOffersForOrder(orderId)).withRel("makesOffer"));
        order.setSeller(store);

        order.add(linkTo(methodOn(PaymentController.class).makePayment(orderId)).withRel("paymentUrl"));
        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }

    @RequestMapping(value = "/{orderId}/items/{orderedItemId}/accessories", method = RequestMethod.POST)
    public ResponseEntity<Void> orderAccessory(@PathVariable int orderId, @PathVariable int orderedItemId,
                                               @RequestBody @Input(include = "productID") Product product) {
        // TODO should write a productBackend to avoid this resolution nonsense:
        Product resolvedProduct = productController.getProduct(product.productID);
        orderBackend.orderAccessoryForOrderedItem(orderId, orderedItemId, new ProductModel(resolvedProduct.name,
                resolvedProduct.productID));
        return redirectToUpdatedOrder(orderId);
    }

    private ResponseEntity<Void> redirectToUpdatedOrder(int orderId) {
        AffordanceBuilder location = linkTo(methodOn(this.getClass()).getOrder(orderId));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location.toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.SEE_OTHER);
    }

    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.POST)
    public ResponseEntity<Void> orderAdditionalItem(
            @PathVariable int orderId, @RequestBody @Input(include = "productID") Product product) {

        Product resolvedProduct = productController.getProduct(product.productID);
        OrderModel orderModel = orderBackend.addOrderedItem(orderId,
                new ProductModel(resolvedProduct.name, resolvedProduct.productID));
        return redirectToUpdatedOrder(orderId);
    }

    private Offer createAddOnOffer(Product product, String addOnProductID, double price, int orderId, int
            orderedItemId) {
        Offer addOnOffer = new Offer();
        addOnOffer.setPriceCurrency(Currency.getInstance("EUR"));
        addOnOffer.setPrice(BigDecimal.valueOf(price));

        Product addOnProduct = productController.getProduct(addOnProductID);
        addOnOffer.setItemOffered(addOnProduct);

        String productSelfRel = product.getLink(Link.REL_SELF)
                .getHref();
        addOnProduct.add(linkTo(methodOn(OrderController.class).orderAccessory(orderId, orderedItemId, addOnProduct))
                .reverseRel("isAccessoryOrSparePartFor", new TypedResource("Product", productSelfRel))
                .build());

        return addOnOffer;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Resources<Order>> getOrders(@RequestParam(required = false) OrderStatus orderStatus) {
        Resources<Order> orderResources = new Resources<Order>(orderAssembler.toResources(orderBackend
                .getOrdersByStatus(orderStatus)));
        return new ResponseEntity<Resources<Order>>(orderResources, HttpStatus.OK);
    }


    @RequestMapping("/{orderId}/offers")
    public HttpEntity<Resources<Offer>> getOffersForOrder(@PathVariable int orderId) {
        HttpEntity<Resources<Offer>> offers = storeController.getOffers();
        for (Offer offer : offers.getBody()
                .getContent()) {
            Product itemOffered = offer.getItemOffered();

            // we can determine the subject URI of the order
            TypedResource subject = new TypedResource("Order",
                    linkTo(methodOn(this.getClass()).getOrder(orderId)).toString());

            itemOffered.add(linkTo(methodOn(OrderController.class)
                    .orderAdditionalItem(orderId, itemOffered))
                    .rel(subject, "orderedItem")
                    .build());
        }

        return offers;
    }
}
