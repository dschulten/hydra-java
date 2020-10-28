package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.ProductModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Component
public class ProductAssembler extends RepresentationModelAssemblerSupport<ProductModel, Product> {

    public ProductAssembler() {
        super(ProductController.class, Product.class);
    }

    @Override
    public Product toModel(ProductModel entity) {
        return createModelWithId(entity.productId, entity);
    }

    @Override
    protected Product instantiateModel(ProductModel entity) {
        Product product = new Product(entity.name, entity.productId);
        for (ProductModel accessory : entity.accessories) {
            product.addAccessory(new Product(accessory.name, accessory.productId));
        }
        return product;
    }
}
