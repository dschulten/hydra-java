package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.ProductModel;
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
        Product product = new Product(entity.name, entity.productId);
        for (ProductModel accessory : entity.accessories) {
            product.addAccessory(new Product(accessory.name, accessory.productId));
        }
        return product;
    }
}
