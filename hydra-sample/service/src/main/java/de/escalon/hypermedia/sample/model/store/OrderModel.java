package de.escalon.hypermedia.sample.model.store;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class OrderModel {
    private List<OrderedItemModel> products = new ArrayList<OrderedItemModel>();
    private int id;
    private OrderStatus orderStatus = OrderStatus.ORDER_PROCESSING;

    public List<OrderedItemModel> getOrderedItems() {
        return products;
    }

    public void setOrderedItems(List<OrderedItemModel> products) {
        this.products = products;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }
}
