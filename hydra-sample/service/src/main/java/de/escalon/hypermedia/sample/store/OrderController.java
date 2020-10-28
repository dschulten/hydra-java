package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.ResourceHandler;
import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.sample.beans.store.Offer;
import de.escalon.hypermedia.sample.beans.store.Order;
import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.beans.store.Store;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.OrderStatus;
import de.escalon.hypermedia.sample.model.store.ProductModel;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
    public ResponseEntity<Void> makeOrder(@Input(readOnly = {"productID"}) @RequestBody Product product) {
        OrderModel orderModel = orderBackend.createOrder();
        Product resolvedProduct = productController.getProduct(product.productID);
        orderModel = orderBackend.addOrderedItem(orderModel.getId(),
                new ProductModel(resolvedProduct.name, resolvedProduct.productID));
        return redirectToUpdatedOrder(orderModel.getId());
    }

    @RequestMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable int orderId) {
        OrderModel orderModel = orderBackend.getOrder(orderId);
        Order order = orderAssembler.toModel(orderModel);
        //TODO need to add additional action for delete to self rel

        // offer extras for each product
        List<? extends Product> items = order.getItems();
        for (int orderItemId = 0; orderItemId < items.size(); orderItemId++) {
            Product product = items.get(orderItemId);
            Offer offer = new Offer();

            if (!product.hasExtra("9052006")) {
                Offer shot = createAddOnOffer(product, "9052006", 0.2, orderId, orderItemId);
                offer.addOn(shot);
            }
            else {
                final String productId = "9052006";
                deleteOrderAccessory(orderId, orderItemId, product, productId);
            }

            if (!product.hasExtra("9052007")) {
                Offer briocheCrema = createAddOnOffer(product, "9052007", 0.8, orderId, orderItemId);
                offer.addOn(briocheCrema);
            }
            else {
                final String productId = "9052007";
                deleteOrderAccessory(orderId, orderItemId, product, productId);
            }

            if (!offer.getAddOns().isEmpty()) {
                offer.add(linkTo(methodOn(this.getClass()).deleteOrderedItem(orderId, orderItemId)).withRel("removeOffer"));
                product.addOffer(offer);
            }
        }

        Store store = storeController.createStoreWithoutOffers();
        store.add(linkTo(methodOn(this.getClass()).getOffersForOrder(orderId)).withRel("makesOffer"));
        order.setSeller(store);

        order.add(linkTo(methodOn(PaymentController.class).makePayment(orderId)).withRel("paymentUrl"));
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    private void deleteOrderAccessory(final int orderId, final int orderItemId, final Product product, final String productId) {
        Product accessory = productController.getProduct(productId);
        String accessorySelfRel = accessory.getLink(IanaLinkRelations.SELF).get().getHref();
        final int extraId = product.getExtraId(productId);
        product.getExtra(productId).add(linkTo(methodOn(OrderController.class).removeAccessory(orderId, orderItemId,
            extraId))
            .reverseRel("isAccessoryOrSparePartFor", "extras", new TypedResource("Product", accessorySelfRel))
            .build());
    }

    @RequestMapping(value = "/{orderId}/items/{orderedItemId}/accessories", method = RequestMethod.POST)
    public ResponseEntity<Void> orderAccessory(@PathVariable int orderId, @PathVariable int orderedItemId,
                                               @RequestBody @Input(readOnly = "productID") Product product) {
        // TODO should write a productBackend to avoid this resolution nonsense:
        Product resolvedProduct = productController.getProduct(product.productID);
        orderBackend.orderAccessoryForOrderedItem(orderId, orderedItemId,
            new ProductModel(resolvedProduct.name, resolvedProduct.productID));
        return redirectToUpdatedOrder(orderId);
    }

    @RequestMapping(value = "/{orderId}/items/{orderedItemId}/accessories/{accessoryId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> removeAccessory(@PathVariable int orderId, @PathVariable int orderedItemId, @PathVariable int accessoryId) {
        orderBackend.removeAccessoryForOrderedItem(orderId, orderedItemId, accessoryId);
        return redirectToUpdatedOrder(orderId);
    }

    private ResponseEntity<Void> redirectToUpdatedOrder(int orderId) {
        AffordanceBuilder location = linkTo(methodOn(this.getClass()).getOrder(orderId));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location.toUri());
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @RequestMapping(value = "/{orderId}/items", method = RequestMethod.POST)
    public ResponseEntity<Void> orderAdditionalItem(
            @PathVariable int orderId, @RequestBody @Input(readOnly = "productID") Product product) {
        Product resolvedProduct = productController.getProduct(product.productID);
        orderBackend.addOrderedItem(orderId,
                new ProductModel(resolvedProduct.name, resolvedProduct.productID));
        return redirectToUpdatedOrder(orderId);
    }

    @RequestMapping(value = "/{orderId}/items/{orderedItemId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteOrderedItem(@PathVariable int orderId, @PathVariable int orderedItemId) {
        orderBackend.deleteOrderedItem(orderId, orderedItemId);
        return redirectToUpdatedOrder(orderId);
    }

    private Offer createOffer(final double price) {
        Offer addOnOffer = new Offer();
        addOnOffer.setPriceCurrency(Currency.getInstance("EUR"));
        addOnOffer.setPrice(BigDecimal.valueOf(price));
        return addOnOffer;
    }

    private Offer createAddOnOffer(Product product, String addOnProductID, double price, int orderId, int orderedItemId) {
        Offer addOnOffer = createOffer(price);

        Product addOnProduct = productController.getProduct(addOnProductID);
        addOnOffer.setItemOffered(addOnProduct);

        String productSelfRel = product.getLink(IanaLinkRelations.SELF).get().getHref();
        addOnProduct.add(linkTo(methodOn(OrderController.class).orderAccessory(orderId, orderedItemId, addOnProduct))
                .reverseRel("isAccessoryOrSparePartFor", "extras", new TypedResource("Product", productSelfRel))
                .build());

        return addOnOffer;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<Order>> getOrders(@RequestParam(required = false) OrderStatus orderStatus) {
        List<OrderModel> orders;
        if (orderStatus == null) {
            orders = orderBackend.getOrders();
        } else {
            orders = orderBackend.getOrdersByStatus(orderStatus);
        }
        CollectionModel<Order> orderResources = orderAssembler.toCollectionModel(orders);
        return new ResponseEntity<>(orderResources, HttpStatus.OK);
    }


    @RequestMapping("/{orderId}/offers")
    public HttpEntity<CollectionModel<Offer>> getOffersForOrder(@PathVariable int orderId) {
        HttpEntity<CollectionModel<Offer>> offers = storeController.getOffers();
        for (Offer offer : offers.getBody().getContent()) {
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
