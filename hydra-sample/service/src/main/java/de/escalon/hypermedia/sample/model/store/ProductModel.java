package de.escalon.hypermedia.sample.model.store;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
public class ProductModel {
    public final String name;
    public final String productId;
    public final List<ProductModel> accessories = new ArrayList<ProductModel>();

    public ProductModel(String name, String productId) {
        this.name = name;
        this.productId = productId;
    }

    public void addAccessory(ProductModel accessory) {
        accessories.add(accessory);
    }

    public void removeAccessory(final int accessoryId) {
        accessories.remove(accessoryId);
    }
}
