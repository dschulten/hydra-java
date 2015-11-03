package de.escalon.hypermedia.sample.model.store;

/**
 * Created by Dietrich on 02.11.2015.
 */
public class OrderedItemModel {
    public final ProductModel orderedItem;
    public final int orderedItemId;

    public OrderedItemModel(ProductModel orderedItem, int id) {
        this.orderedItem = orderedItem;
        this.orderedItemId = id;
    }
}
