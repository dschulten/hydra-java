package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.OrderedItemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;

/**
 * Created by Dietrich on 02.11.2015.
 */
@RequestMapping("/orders/{orderId}/items")
@Controller
public class OrderedItemController {

    @Autowired
    private OrderBackend orderBackend;

    @Autowired
    private ProductAssembler productAssembler;

    @RequestMapping("/{orderedItemId}")
    public ResponseEntity<Product> getOrderedItem(@PathVariable int orderId, @PathVariable int orderedItemId) {
        OrderModel order = orderBackend.getOrder(orderId);
        List<OrderedItemModel> orderedItems = order.getOrderedItems();
        OrderedItemModel found = null;
        for (OrderedItemModel orderedItem : orderedItems) {
            if (orderedItem.orderedItemId == orderedItemId) {
                found = orderedItem;
                break;
            }
        }
        Product product = null;
        if (found != null) {
            product = productAssembler.instantiateModel(found.orderedItem);
            product.add(linkTo(methodOn(this.getClass())
                    .getOrderedItem(orderId, orderedItemId))
                    .withSelfRel());
        }
        return new ResponseEntity<Product>(product, HttpStatus.OK);
    }
}
