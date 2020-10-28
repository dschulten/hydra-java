package de.escalon.hypermedia.sample.beans.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@JsonPropertyOrder({"itemOffered", "price", "priceCurrency", "addOns"})
@Relation("offer")
public class Offer extends RepresentationModel {
    private Product itemOffered;
    private BigDecimal price;
    private List<Offer> addOns = new ArrayList<Offer>();
    private Currency priceCurrency;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(Currency priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public Product getItemOffered() {
        return itemOffered;
    }

    public void setItemOffered(Product itemOffered) {
        this.itemOffered = itemOffered;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public List<Offer> getAddOns() {
        return addOns;
    }

    public void addOn(Offer addOn) {
        this.addOns.add(addOn);
    }
}
