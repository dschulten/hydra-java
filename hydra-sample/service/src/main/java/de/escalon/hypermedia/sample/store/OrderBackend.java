package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Order;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Component
public class OrderBackend {
    public static int counter;
    public List<OrderModel> orderModels = new ArrayList<OrderModel>();

    public OrderModel createOrder() {
        OrderModel orderModel = new OrderModel();
        orderModel.setId(counter++);
        orderModels.add(orderModel);
        counter++;
        return orderModel;
    }

    public OrderModel addOrderedItem(int id, ProductModel productModel) {
        OrderModel orderModel = orderModels.get(id);
        List<ProductModel> products = orderModel.getProducts();
        products.add(productModel);
        return orderModel;
    }

    public OrderModel getOrder(int id) {
        return orderModels.get(id);
    }

    public List<OrderModel> getOrders() {
        return orderModels;
    }
}
