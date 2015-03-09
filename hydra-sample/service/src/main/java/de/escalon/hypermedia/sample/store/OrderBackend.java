package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.Order;
import org.springframework.context.annotation.Bean;
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

    public int createOrder() {
        OrderModel orderModel = new OrderModel();
        orderModel.setId(counter);
        orderModels.add(orderModel);
        return counter++;
    }

    public OrderModel getOrder(int id) {
        return orderModels.get(id);
    }

}
