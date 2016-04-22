package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.hamcrest.Matchers;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;
import static org.junit.Assert.*;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenUtilsTest {

    ObjectMapper objectMapper = new ObjectMapper();

    RelProvider relProvider = new DefaultRelProvider();

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

        assertEquals("Peter Joseph", entity.getProperties()
                .get("name"));

        assertThat(entity.getEntities(), Matchers.notNullValue());
        assertThat(entity.getEntities()
                .get(0), Matchers.instanceOf(SirenEmbeddedRepresentation.class));
        SirenSubEntity t = new SirenEmbeddedLink(null, null, null);

        assertEquals("Grant Street", entity.getEntities()
                .get(0)
                .getProperties()
                .get("street"));
        assertThat(entity.getEntities()
                .get(0)
                .getRel(), Matchers.contains("address"));

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        System.out.println(jsonNode.toString());
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

        assertThat(entity.getEntities()
                .get(0)
                .getRel(), Matchers.contains("address"));
        assertEquals("http://api.example.com/customers/123/address", entity.getEntities()
                .get(0)
                .getHref());


        JsonNode jsonNode = objectMapper.valueToTree(entity);
        System.out.println(jsonNode.toString());
    }
}