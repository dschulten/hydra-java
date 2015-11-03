package de.escalon.hypermedia.sample.store;


import de.escalon.hypermedia.sample.beans.store.Order;
import de.escalon.hypermedia.sample.beans.store.OrderedItem;
import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.OrderedItemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

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
        List<OrderedItemModel> orderedItems = entity.getOrderedItems();
        for (OrderedItemModel orderedItemModel : orderedItems) {
            Product product = itemAssembler.instantiateResource(orderedItemModel.orderedItem);
            Object parameters;
            product.add(linkTo(OrderedItemController.class, entity.getId())
                    .slash(orderedItemModel.orderedItemId)
                    .withSelfRel());
            order.addItem(product);
        }
        return order;
    }
}
