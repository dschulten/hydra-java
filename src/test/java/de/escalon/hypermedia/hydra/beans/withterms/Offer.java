package de.escalon.hypermedia.hydra.beans.withterms;

import de.escalon.hypermedia.hydra.mapping.Expose;

import java.math.BigDecimal;

/**
 * Created by dschulten on 06.09.2014.
 */
@Expose("gr:Offering")
public class Offer {
    public BusinessFunction businessFunction = BusinessFunction.RENT;
    @Expose("gr:hasCurrencyValue")
    public BigDecimal price = BigDecimal.valueOf(1.99);
}

@Expose("gr:BusinessFunction")
enum BusinessFunction {
    @Expose("gr:LeaseOut")
    RENT,
    @Expose("gr:Sell")
    FOR_SALE,
    @Expose("gr:Buy")
    BUY
}