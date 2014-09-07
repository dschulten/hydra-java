/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.github.jsonldjava.core.JsonLdError;
import de.escalon.hypermedia.hydra.JsonLdTestUtils;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Vocab;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;


public class JacksonHydraSerializerTest {

    private ObjectMapper mapper;

    StringWriter w = new StringWriter();


    @Before
    public void setUp() {
        mapper = new ObjectMapper();

        mapper.registerModule(new SimpleModule() {

            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    public JsonSerializer<?> modifySerializer(
                            SerializationConfig config,
                            BeanDescription beanDesc,
                            JsonSerializer<?> serializer) {

                        if (serializer instanceof BeanSerializerBase) {
                            return new JacksonHydraSerializer(
                                    (BeanSerializerBase) serializer);
                        } else {
                            return serializer;
                        }
                    }
                });
            }
        });

    }

    @Test
    public void testDefaultVocabIsRendered() throws Exception {

        class Person {
            private String name = "Dietrich Schulten";

            public String getName() {
                return name;
            }
        }

        mapper.writeValue(w, new Person());
        System.out.println(w);
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"" +
                "}" +
                ",\"@type\":\"Person\"," +
                "\"name\":\"Dietrich Schulten\"}"
                , w.toString());
    }

    @Vocab("http://xmlns.com/foaf/0.1/")
    class Person {
        private String name = "Dietrich Schulten";

        public String getName() {
            return name;
        }
    }

    @Test
    public void testFoafVocabIsRendered() throws Exception {

        mapper.writeValue(w, new Person());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/\"" +
                "}" +
                ",\"@type\":\"Person\"," +
                "\"name\":\"Dietrich Schulten\"}"
                , w.toString());
    }

    @Test
    public void testAppliesPackageDefinedVocab() throws IOException {
        mapper.writeValue(w, new de.escalon.hypermedia.hydra.beans.withvocab.Person("1964-08-08", "Schulten"));
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/;\"," +
                "\"surname\":\"http://schema.org/familyName\"" +
                "}," +
                "\"@type\":\"Person\"," +
                "\"birthDate\":\"1964-08-08\"," +
                "\"surname\":\"Schulten\"}", w.toString());
    }

    @Test
    public void testAppliesPackageDefinedTerms() throws IOException, JsonLdError {
        mapper.writeValue(w, new de.escalon.hypermedia.hydra.beans.withterms.Offer());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"," +
                "\"gr\":\"http://purl.org/goodrelations/v1#\"," +
                "\"dc\":\"http://purl.org/dc/elements/1.1/\"," +
                "\"price\":\"gr:hasCurrencyValue\"}," +
                "\"@type\":\"gr:Offering\"," +
                "\"businessFunction\":\"RENT\"," +
                "\"price\":1.99" +
                "}", w.toString());
        final String newline = System.lineSeparator();
        assertEquals("{" + newline +
                "  \"@type\" : \"http://purl.org/goodrelations/v1#Offering\"," + newline +
                "  \"http://purl.org/goodrelations/v1#hasCurrencyValue\" : 1.99," + newline +
                "  \"http://schema.org/businessFunction\" : \"RENT\"" + newline +
                "}", JsonLdTestUtils.applyContext(w.toString()));
    }

    @Test
    public void testNestedContextWithDifferentVocab() throws Exception {

        @Vocab("http://purl.org/dc/elements/1.1/")
        @Expose("BibliographicResource")
        class Document {
            public String title = "Moby Dick";
            public Person creator = new Person();
        }

        mapper.writeValue(w, new Document());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://purl.org/dc/elements/1.1/\"" +
                "}" +
                ",\"@type\":\"BibliographicResource\"" +
                ",\"title\":\"Moby Dick\"" +
                ",\"creator\":{" +
                "\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/\"}" +
                ",\"@type\":\"Person\"" +
                ",\"name\":\"Dietrich Schulten\"}}"
                , w.toString());
    }

    @Test
    public void testDefaultVocabWithCustomTerm() throws Exception {

        class Person {
            public String birthDate;
            public String firstName;

            // override field name by schema.org property
            @Expose("familyName")
            public String lastName;

            // override getter by schema.org property
            @Expose("givenName")
            public String getFirstName() {
                return firstName;
            }

            public Person(String birthDate, String firstName, String lastName) {
                this.birthDate = birthDate;
                this.lastName = lastName;
                this.firstName = firstName;
            }
        }


        mapper.writeValue(w, new Person("1964-08-08", "Dietrich", "Schulten"));
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"," +
                "\"lastName\":\"familyName\"," +
                "\"firstName\":\"givenName\"" +
                "}," +
                "\"@type\":\"Person\"," +
                "\"birthDate\":\"1964-08-08\"," +
                "\"firstName\":\"Dietrich\"," +
                "\"lastName\":\"Schulten\"}"
                , w.toString());

    }

    //    class Movie {
    //        public String name = "Pirates oft the Caribbean: On Strander Tides (2011)";
    //        public String description = "Jack Sparrow and Barbossa embark on a quest to\n" +
    //                " find the elusive fountain of youth, only to discover that Blackbeard and\n" +
    //                " his daughter are after it too.";
    //
    //        public String model = "http://www.imdb.com/title/tt0325980/";
    //
    //        public Offer offers = new Offer();
    //    }

    class Offer {
        public String businessFunction = "http://purl.org/goodrelations/v1#LeaseOut";
        public UnitPriceSpecification priceSpecification = new UnitPriceSpecification();
        public String availableDeliveryMethod = "http://purl.org/goodrelations/v1#DirectDownload";
        public QuantitativeValue eligibleDuration = new QuantitativeValue();
    }

    enum BusinessFunction {
        @Expose("http://purl.org/goodrelations/v1#LeaseOut")
        RENT,
        @Expose("http://purl.org/goodrelations/v1#Sell")
        FOR_SALE,
        @Expose("http://purl.org/goodrelations/v1#Buy")
        BUY
    }

    class UnitPriceSpecification {
        public BigDecimal price = BigDecimal.valueOf(3.99);
        public String priceCurrency = "USD";
        public String datetime = "2012-12-31T23:59:59Z";
    }

    class QuantitativeValue {
        public String value = "30";
        public String unitCode = "DAY";
    }

    @Test
    public void testSchemaOrgClassWithGoodrelationsExtensions() throws IOException {
        mapper.writeValue(w, new Offer());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"}," +
                "\"@type\":\"Offer\"," +
                "\"businessFunction\":\"http://purl.org/goodrelations/v1#LeaseOut\"," +
                "\"priceSpecification\":{" +
                "\"@context\":{\"@vocab\":\"http://schema.org/\"}," +
                "\"@type\":\"UnitPriceSpecification\"," +
                "\"price\":3.99," +
                "\"priceCurrency\":\"USD\"," +
                "\"datetime\":\"2012-12-31T23:59:59Z\"}," +
                "\"availableDeliveryMethod\":\"http://purl.org/goodrelations/v1#DirectDownload\"," +
                "\"eligibleDuration\":" +
                "{\"@context\":{\"@vocab\":\"http://schema.org/\"}," +
                "\"@type\":\"QuantitativeValue\"," +
                "\"value\":\"30\"," +
                "\"unitCode\":\"DAY\"}}", w.toString());
    }

    @Test
    @Ignore
    public void testDoesNotRepeatContextIfUnnecessary() throws IOException {
        // TODO better way than using threadlocal to keep a stack of current @contexts?
        mapper.writeValue(w, new Offer());
        assertEquals("{\"@context\":" +
                "{\"@vocab\":\"http://schema.org/\"}," +
                "\"@type\":\"Offer\"," +
                "\"businessFunction\":\"http://purl.org/goodrelations/v1#LeaseOut\"," +
                "\"priceSpecification\":{" +
                "\"@type\":\"UnitPriceSpecification\"," +
                "\"price\":3.99," +
                "\"priceCurrency\":\"USD\"," +
                "\"datetime\":\"2012-12-31T23:59:59Z\"}," +
                "\"availableDeliveryMethod\":\"http://purl.org/goodrelations/v1#DirectDownload\"," +
                "\"eligibleDuration\":{" +
                "\"@type\":\"QuantitativeValue\"," +
                "\"value\":\"30\"," +
                "\"unitCode\":\"DAY\"}}", w.toString());
    }

}