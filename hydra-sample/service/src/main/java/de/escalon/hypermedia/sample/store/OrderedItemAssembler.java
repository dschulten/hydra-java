package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.OrderedItem;
import de.escalon.hypermedia.sample.model.store.OrderedItemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

/**
 * Assembles product as ordererdItem.
 *
 * Created by Dietrich on 02.11.2015.
 */
@Component
public class OrderedItemAssembler extends ResourceAssemblerSupport<OrderedItemModel, OrderedItem> {


    @Autowired
    ProductAssembler productAssembler;

    public OrderedItemAssembler() {
        super(OrderedItemController.class, OrderedItem.class);
    }

    @Override
    public OrderedItem toResource(OrderedItemModel entity) {
        return createResourceWithId(entity.orderedItemId, entity);
    }

    @Override
    protected OrderedItem instantiateResource(OrderedItemModel entity) {
        OrderedItem orderedItem = super.instantiateResource(entity);
        orderedItem.setOrderedItem(productAssembler.toResource(entity.orderedItem));
        orderedItem.setOrderItemNumber(Integer.toString(entity.orderedItemId));
        return orderedItem;
    }
}
