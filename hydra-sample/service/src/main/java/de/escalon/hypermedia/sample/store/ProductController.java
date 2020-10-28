package de.escalon.hypermedia.sample.store;

import de.escalon.hypermedia.sample.beans.store.Product;
import de.escalon.hypermedia.sample.model.store.ProductModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    int productCounter = 9052001;
    ProductAssembler assembler = new ProductAssembler();

    List<ProductModel> productModels = Arrays.asList(
            createProduct("Latte Macchiato"),
            createProduct("Caffè Macchiato"),
            createProduct("Caffè Espresso"),
            createProduct("Cup of El Salvador Finca El Carmen Bourbon"),
            createProduct("Cappuccino"),
            createProduct("extra shot of caffè"),
            createProduct("Brioche con crema"));


    @RequestMapping
    public
    @ResponseBody
    CollectionModel<Product> getProducts() {
        List<Product> resources = new ArrayList<Product>();
        for (ProductModel productModel : productModels) {
            Product product = assembler.toModel(productModel);
            resources.add(product);
        }
        return new CollectionModel<Product>(resources);
    }


    @RequestMapping("/{productID}")
    public
    @ResponseBody
    Product getProduct(@PathVariable String productID) {
        for (ProductModel productModel : productModels) {
            if (productID.equals(productModel.productId)) {
                return assembler.toModel(productModel);
            }
        }
        return null;
    }


    private ProductModel createProduct(String productName) {
        return new ProductModel(productName, String.valueOf(productCounter++));

    }
}
