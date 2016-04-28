package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.Relation;

import java.util.*;

import static com.jayway.jsonassert.JsonAssert.with;
import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenUtilsTest {

    ObjectMapper objectMapper = new ObjectMapper();

    SirenUtils sirenUtils = new SirenUtils();

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
        sirenUtils.toSirenEntity(entity, new Customer());

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
        sirenUtils.toSirenEntity(entity, new Customer());

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
        sirenUtils.toSirenEntity(entity, customerResource);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities[0].rel", contains("address"));
        with(jsonNode.toString()).assertThat("$.entities[0].href",
                equalTo("http://api.example.com/customers/123/address"));
    }


    @Test
    public void testListOfResource() {
        List<Resource<Address>> addresses = new ArrayList<Resource<Address>>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Resource<Address>(new Address()));
        }
        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addresses);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities", hasSize(4));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(jsonNode.toString()).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));

    }

    @Test
    public void testResources() {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Address());
        }

        Resources<Address> addressResources = new Resources<Address>(addresses);
        addressResources.add(new Link("http://example.com/addresses", "self"));
        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addressResources);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities", hasSize(4));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(jsonNode.toString()).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
        with(jsonNode.toString()).assertThat("$.links", hasSize(1));

    }

    @Test
    public void testMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "Joe");
        map.put("address", new Address());

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, map);

        JsonNode jsonNode = objectMapper.valueToTree(entity);

        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Joe"));
        with(jsonNode.toString()).assertThat("$.properties.address.city.name", equalTo("Donnbronn"));

    }

    @Test
    public void testAttributeWithListOfBeans() {
        class Customer {
            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final List<Address> addresses = new ArrayList<Address>();

            Customer() {
                for (int i = 0; i < 4; i++) {
                    addresses.add(new Address());
                }
            }

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public List<Address> getAddresses() {
                return addresses;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        System.out.println(jsonNode.toString());

        with(jsonNode.toString()).assertThat("$.entities", hasSize(4));
        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Peter Joseph"));

        with(jsonNode.toString()).assertThat("$.entities[0].properties.city.postalCode",
                equalTo("74199"));
    }


    @Test
    public void testAttributeWithListOfSingleValueTypes() {
        class Customer {
            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final List<Integer> favoriteNumbers = Arrays.asList(1, 3, 5, 7);

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public List<Integer> getFavoriteNumbers() {
                return favoriteNumbers;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        JsonNode jsonNode = objectMapper.valueToTree(entity);

        with(jsonNode.toString()).assertThat("$.properties.favoriteNumbers", hasSize(4));
        with(jsonNode.toString()).assertThat("$.properties.favoriteNumbers", contains(1, 3, 5, 7));
        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Peter Joseph"));

    }

    enum Daytime {
        MORNING, NOON, AFTERNOON, EVENING, NIGHT
    }

    @Test
    public void testAttributeWithListOfEnums() {


        class Customer {
            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final List<Daytime> favoriteDaytime = Arrays.asList(Daytime.AFTERNOON, Daytime.NIGHT);

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public List<Daytime> getFavoriteNumbers() {
                return favoriteDaytime;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        JsonNode jsonNode = objectMapper.valueToTree(entity);

        with(jsonNode.toString()).assertThat("$.properties.favoriteNumbers", hasSize(2));
        with(jsonNode.toString()).assertThat("$.properties.favoriteNumbers",
                contains(Daytime.AFTERNOON.name(), Daytime.NIGHT.name()));
        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Peter Joseph"));

    }

    @Test
    public void testListOfBean() {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Address());
        }

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addresses);

        JsonNode jsonNode = objectMapper.valueToTree(entity);
        with(jsonNode.toString()).assertThat("$.entities", hasSize(4));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(jsonNode.toString()).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
    }


    @Test
    public void testMapContainingResource() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "Joe");
        Resource<Address> addressResource = new Resource<Address>(new Address());
        addressResource.add(new Link("http://example.com/addresses/1", "self"));
        map.put("address", addressResource);

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, map);

        JsonNode jsonNode = objectMapper.valueToTree(entity);

        System.out.println(jsonNode.toString());
        with(jsonNode.toString()).assertThat("$.properties.name", equalTo("Joe"));
        with(jsonNode.toString()).assertThat("$.entities[0].properties.street", equalTo("Grant Street"));
        with(jsonNode.toString()).assertThat("$.entities[0].links", hasSize(1));
    }


}