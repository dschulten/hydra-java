package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.Product;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Component
public class ProductAssembler extends ResourceAssemblerSupport<ProductModel, Product> {

    public ProductAssembler() {
        super(ProductController.class, Product.class);
    }

    @Override
    public Product toResource(ProductModel entity) {
        return createResourceWithId(entity.productId, entity);
    }

    @Override
    protected Product instantiateResource(ProductModel entity) {
        Product orderedItem = new Product(entity.name);
        orderedItem.setProductID(entity.productId);
        return orderedItem;
    }
}
