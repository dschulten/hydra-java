package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.OrderModel;
import de.escalon.hypermedia.sample.model.store.ProductModel;
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
        return orderModel;
    }

    public OrderModel addOrderedItem(int orderId, ProductModel productModel) {
        OrderModel orderModel = orderModels.get(orderId);
        List<ProductModel> products = orderModel.getProducts();
        products.add(productModel);
        return orderModel;
    }

    public OrderModel orderAccessoryForOrderedItem(int orderId, int orderedItemId, ProductModel accessory) {
        OrderModel orderModel = orderModels.get(orderId);
        List<ProductModel> products = orderModel.getProducts();
        ProductModel productModel = products.get(orderedItemId);
        productModel.addAccessory(accessory);
        return orderModel;
    }

    public OrderModel getOrder(int id) {
        return orderModels.get(id);
    }

    public List<OrderModel> getOrders() {
        return orderModels;
    }
}
