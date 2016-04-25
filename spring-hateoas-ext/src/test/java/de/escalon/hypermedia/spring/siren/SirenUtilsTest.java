package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonassert.JsonAssert;
import com.jayway.jsonassert.JsonAsserter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.hateoas.core.Relation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static com.jayway.jsonassert.JsonAssert.with;
import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenUtilsTest {

    ObjectMapper objectMapper = new ObjectMapper();

    RelProvider relProvider = new DefaultRelProvider();

    @Before
    public void setUp() {

    }

    @Relation("city")
    class City {
        String postalCode = "74199";
        String name = "Donnbronn";

        public String getPostalCode() {
            return postalCode;
        }

        public String getName() {
            return name;
        }
    }

    @Relation("address")
    class Address {
        String street = "Grant Street";
        City city = new City();

        public String getStreet() {
            return street;
        }

        public City getCity() {
            return city;
        }
    }

    @Test
    public void testNestedBeansToSirenEntityProperties() throws Exception {

        class Customer {
            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final Address address = new Address();

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public Address getAddress() {
                return address;
            }
        }

        SirenEntity entity = new SirenEntity();
        SirenUtils.toSirenEntity(entity, new Customer(), relProvider);

        assertEquals("pj123", entity.properties.get("customerId"));
        assertEquals("Peter Joseph", entity.properties.get("name"));
        assertThat(entity.properties.get("address"), Matchers.instanceOf(Map.class));
        assertEquals("Grant Street", ((Map<String, Map<String, Object>>) entity.properties.get("address")).get
                ("street"));

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        System.out.println(jsonNode.toString());
    }

    @Relation(value = "email", collectionRelation = "emails")
    public class Email {
        private final String email;
        private final String type;

        public Email(String email, String type) {
            this.email = email;
            this.type = type;
        }

        public String getEmail() {
            return email;
        }

        public String getType() {
            return type;
        }
    }

    @Relation(value = "profile")
    public class ProfileResource {
        private final String firstName;
        private final String lastName;
        @JsonUnwrapped
        private final Resources<EmbeddedWrapper> embeddeds;

        public ProfileResource(String firstName, String lastName, Resources<EmbeddedWrapper> embeddeds) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.embeddeds = embeddeds;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public Resources<EmbeddedWrapper> getEmbeddeds() {
            return embeddeds;
        }
    }

//    @Test
//    public void testEmbeddedResource() {
//        Resource<Email> primary = new Resource<Email>(new Email("neo@matrix.net", "primary"));
//        Resource<Email> home = new Resource<Email>(new Email("t.anderson@matrix.net", "home"));
//
//        EmbeddedWrappers wrappers = new EmbeddedWrappers(true);
//
//        List<EmbeddedWrapper> embeddeds = Arrays.asList(wrappers.wrap(primary), wrappers.wrap(home));
//
//        Resources<EmbeddedWrapper> embeddedEmails = new Resources(embeddeds, new Link("self"));
//        // return ResponseEntity.ok(new Resource(new ProfileResource("Thomas", "Anderson", embeddedEmails), linkTo
// (ProfileController.class).withSelfRel()));
//    }

    @Test
    public void testNestedResourceToEmbeddedRepresentation() throws Exception {
        class Customer {
            private final String name = "Peter Joseph";
            private final Resource<Address> address = new Resource<Address>(new Address());

            public String getName() {
                return name;
            }

            public Resource<Address> getAddress() {
                address.add(new Link("http://example.com/customer/123/address/geolocation", "geolocation"));
                return address;
            }
        }

        SirenEntity entity = new SirenEntity();
        SirenUtils.toSirenEntity(entity, new Customer(), relProvider);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Peter Joseph"));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.street", equalTo("Grant Street"));
        with(jsonNode.toString()).assertThat("$.entities[0].rel", contains("address"));

    }

    @Test
    public void testEmbeddedLink() {
        class Customer {
            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final Address address = new Address();

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }
        }
        Resource<Customer> customerResource = new Resource<Customer>(new Customer());
        customerResource.add(new Link("http://api.example.com/customers/123/address", "address"));

        SirenEntity entity = new SirenEntity();
        SirenUtils.toSirenEntity(entity, customerResource, relProvider);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities[0].rel", contains("address"));
        with(jsonNode.toString()).assertThat("$.entities[0].href",
                equalTo("http://api.example.com/customers/123/address"));


        System.out.println(jsonNode.toString());
    }

    public void testListOfBean() {

    }

    @Test
    public void testListOfResource() {
        List<Resource<Address>> addresses = new ArrayList<Resource<Address>>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Resource<Address>(new Address()));
        }
        SirenEntity entity = new SirenEntity();
        SirenUtils.toSirenEntity(entity, addresses, relProvider);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities", hasSize(4));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(jsonNode.toString()).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));

    }

    public void testResources() {

    }

    public void testMapContainingResource() {

    }

    public void testRadio() {

    }

    public void testCheckBox() {

    }
}