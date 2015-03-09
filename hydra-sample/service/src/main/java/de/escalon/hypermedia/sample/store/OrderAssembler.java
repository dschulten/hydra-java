package de.escalon.hypermedia.sample.store;


import de.escalon.hypermedia.sample.beans.Order;
import de.escalon.hypermedia.sample.beans.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Component
public class OrderAssembler extends ResourceAssemblerSupport<OrderModel, Order> {

    @Autowired
    private ProductAssembler itemAssembler;

    public OrderAssembler() {
        super(OrderController.class, Order.class);
    }

    @Override
    public Order toResource(OrderModel entity) {
        return createResourceWithId(entity.getId(), entity);
    }

    @Override
    protected Order instantiateResource(OrderModel entity) {
        Order order = super.instantiateResource(entity);
        List<ProductModel> products = entity.getProducts();
        for (ProductModel product : products) {
            Product orderedItem = itemAssembler.toResource(product);
            order.addItem(orderedItem);
        }
        return order;
    }
}
