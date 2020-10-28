package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.Relation;

import java.util.*;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.Matchers.*;

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

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.customerId", equalTo("pj123"));
        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));
        with(json).assertThat("$.properties.address", Matchers.instanceOf(Map.class));
        with(json).assertThat("$.properties.address.street", equalTo("Grant Street"));
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
        private final CollectionModel<EmbeddedWrapper> embeddeds;

        public ProfileResource(String firstName, String lastName, CollectionModel<EmbeddedWrapper> embeddeds) {
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

        public CollectionModel<EmbeddedWrapper> getEmbeddeds() {
            return embeddeds;
        }
    }

//    @Test
//    public void testEmbeddedResource() {
//        EntityModel<Email> primary = new EntityModel<Email>(new Email("neo@matrix.net", "primary"));
//        EntityModel<Email> home = new EntityModel<Email>(new Email("t.anderson@matrix.net", "home"));
//
//        EmbeddedWrappers wrappers = new EmbeddedWrappers(true);
//
//        List<EmbeddedWrapper> embeddeds = Arrays.asList(wrappers.wrap(primary), wrappers.wrap(home));
//
//        CollectionModel<EmbeddedWrapper> embeddedEmails = new CollectionModel(embeddeds, new Link("self"));
//        // return ResponseEntity.ok(new EntityModel(new ProfileResource("Thomas", "Anderson", embeddedEmails), linkTo
// (ProfileController.class).withSelfRel()));
//    }

    @Test
    public void testNestedResourceToEmbeddedRepresentation() throws Exception {
        class Customer {

            private final String name = "Peter Joseph";
            private final EntityModel<Address> address = new EntityModel<Address>(new Address());

            public String getName() {
                return name;
            }

            public EntityModel<Address> getAddress() {
                address.add(new Link("http://example.com/customer/123/address/geolocation", "geolocation"));
                return address;
            }
        }

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, new Customer());

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));
        with(json).assertThat("$.entities[0].properties.street", equalTo("Grant Street"));
        with(json).assertThat("$.entities[0].rel", contains("address"));
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
        EntityModel<Customer> customerResource = new EntityModel<Customer>(new Customer());
        customerResource.add(new Link("http://api.example.com/customers/123/address", "address"));

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customerResource);

        String json = objectMapper.valueToTree(entity)
                .toString();
        with(json).assertThat("$.entities[0].rel", contains("address"));
        with(json).assertThat("$.entities[0].href",
                equalTo("http://api.example.com/customers/123/address"));
    }


    @Test
    public void testListOfResource() {
        List<EntityModel<Address>> addresses = new ArrayList<EntityModel<Address>>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new EntityModel<Address>(new Address()));
        }
        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addresses);

        String json = objectMapper.valueToTree(entity)
                .toString();
        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(json).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
    }

    @Test
    public void testResources() {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Address());
        }

        CollectionModel<Address> addressResources = new CollectionModel<Address>(addresses);
        addressResources.add(new Link("http://example.com/addresses", "self"));
        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addressResources);

        String json = objectMapper.valueToTree(entity)
                .toString();
        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(json).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
        with(json).assertThat("$.links", hasSize(1));
    }

    @Test
    public void testPagedResources() {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Address());
        }


        PagedModel<Address> addressResources = new PagedModel<Address>(addresses,
                new PageMetadata(2, 0, addresses.size()));
        addressResources.add(new Link("http://example.com/addresses", "self"));
        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addressResources);

        String json = objectMapper.valueToTree(entity)
                .toString();
        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(json).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
        with(json).assertThat("$.links", hasSize(1));
    }

    @Test
    public void testMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "Joe");
        map.put("address", new Address());

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, map);

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.name", equalTo("Joe"));
        with(json).assertThat("$.properties.address.city.name", equalTo("Donnbronn"));
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

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));

        with(json).assertThat("$.entities[0].properties.city.postalCode",
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

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.favoriteNumbers", hasSize(4));
        with(json).assertThat("$.properties.favoriteNumbers", contains(1, 3, 5, 7));
        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));
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

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.favoriteNumbers", hasSize(2));
        with(json).assertThat("$.properties.favoriteNumbers",
                contains(Daytime.AFTERNOON.name(), Daytime.NIGHT.name()));
        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));
    }

    @Test
    public void testListOfBean() {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 4; i++) {
            addresses.add(new Address());
        }

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, addresses);

        String json = objectMapper.valueToTree(entity)
                .toString();
        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.entities[0].properties.city.postalCode", equalTo("74199"));
        with(json).assertThat("$.entities[3].properties.city.name", equalTo("Donnbronn"));
    }


    @Test
    public void testMapContainingResource() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "Joe");
        EntityModel<Address> addressResource = new EntityModel<Address>(new Address());
        addressResource.add(new Link("http://example.com/addresses/1", "self"));
        map.put("address", addressResource);

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, map);

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.name", equalTo("Joe"));
        with(json).assertThat("$.entities[0].properties.street", equalTo("Grant Street"));
        with(json).assertThat("$.entities[0].links", hasSize(1));
    }

    @Test
    public void testAttributeWithResources() {
        class Customer {

            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private CollectionModel<Address> addresses;

            Customer() {
                List<Address> content = new ArrayList<Address>();
                for (int i = 0; i < 4; i++) {
                    content.add(new Address());
                }
                addresses = new CollectionModel<Address>(content);
            }

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public CollectionModel<Address> getAddresses() {
                return addresses;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.entities", hasSize(4));
        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));

        with(json).assertThat("$.entities[0].properties.city.postalCode",
                equalTo("74199"));
    }

    @Test
    public void testAttributeWithMap() {
        class Customer {

            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final Map<String, Object> address = new HashMap<String, Object>();

            Customer() {
                address.put("street", "Grant Street");
                Map<String, String> city = new HashMap<String, String>();
                address.put("city", city);
                city.put("name", "Donnbronn");
                city.put("postalCode", "74199");
            }

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public Map<String, Object> getAddress() {
                return address;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));

        with(json).assertThat("$.properties.address.city.postalCode",
                equalTo("74199"));
    }

    @Test
    public void testAttributeWithMapContainingBean() {
        class Customer {

            private final String customerId = "pj123";
            private final String name = "Peter Joseph";
            private final Map<String, Object> address = new HashMap<String, Object>();

            Customer() {
                address.put("street", "Grant Street");
                Map<String, String> city = new HashMap<String, String>();
                address.put("city", new City());
            }

            public String getCustomerId() {
                return customerId;
            }

            public String getName() {
                return name;
            }

            public Map<String, Object> getAddress() {
                return address;
            }
        }
        Customer customer = new Customer();

        SirenEntity entity = new SirenEntity();
        sirenUtils.toSirenEntity(entity, customer);

        String json = objectMapper.valueToTree(entity)
                .toString();

        with(json).assertThat("$.properties.name", equalTo("Peter Joseph"));

        with(json).assertThat("$.properties.address.city.postalCode",
                equalTo("74199"));
    }

    // TODO beans with setters, non-specific input parameter types
}