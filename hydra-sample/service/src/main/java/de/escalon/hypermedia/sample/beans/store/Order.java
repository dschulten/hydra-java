package de.escalon.hypermedia.sample.beans.store;

import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.sample.model.store.OrderStatus;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class Order extends RepresentationModel<Order> {
    private List<Product> items = new ArrayList<>();
    private Offer acceptedOffer;
    private Store seller;
    private OrderStatus orderStatus;

    public void setSeller(Store seller) {
        this.seller = seller;
    }

    public Store getSeller() {
        return seller;
    }

    public void addItem(Product orderedItem) {
        this.items.add(orderedItem);
    }

    @Expose("orderedItem")
    public List<? extends Product> getItems() {
        return items;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
}
