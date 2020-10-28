/*
 * Copyright (c) 2015. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.uber;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.escalon.hypermedia.spring.SpringActionDescriptor;
import de.escalon.hypermedia.spring.SpringActionInputParameter;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static org.junit.Assert.*;

public class UberUtilsTest {

    private static final String URL_HOME = "http://www.example.com";
    private static final Link LINK_HOME = new Link(URL_HOME, "home");
    private static final String FOO_VALUE = "foo";
    private static final String BAR_VALUE = "bar";
    private static final String NESTED_FOO_VALUE = "nestedFooValue";
    private static final String NESTED_BAR_VALUE = "nestedBarValue";


    @Test
    public void linkGetToUberNode() throws Exception {
        UberNode linkNode = UberUtils.toUberLink("/foo", new SpringActionDescriptor("get", RequestMethod.GET.name()),
                IanaLinkRelations.SELF.value());
        assertEquals(Arrays.asList(IanaLinkRelations.SELF.value()), linkNode.getRel());
        assertEquals("/foo", linkNode.getUrl());
        assertNull(linkNode.getModel());
        assertNull(linkNode.getAction());
    }

    public static class FooRequestBody {
        String bar;
        int foo;

        public void setBar(String bar) {
            this.bar = bar;
        }

        public String getBar() {
            return bar;
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(int foo) {
            this.foo = foo;
        }
    }

    public void requestBody(FooRequestBody requestBody) {

    }

    @Test
    public void linkPostToUberNode() throws Exception {
        // TODO create a Link with variables separate from URITemplate for POST
        SpringActionDescriptor actionDescriptor = new SpringActionDescriptor("post", RequestMethod
                .POST.name());
        actionDescriptor.setRequestBody(
                new SpringActionInputParameter(new MethodParameter(this.getClass()
                        .getMethod("requestBody", FooRequestBody.class), 0), null));
        UberNode linkNode = UberUtils.toUberLink("/foo", actionDescriptor, IanaLinkRelations.SELF.value());
        assertEquals(Arrays.asList(IanaLinkRelations.SELF.value()), linkNode.getRel());
        assertEquals("/foo", linkNode.getUrl());
        assertThat(linkNode.getModel(),
                Matchers.containsString("foo={foo}"));
        assertThat(linkNode.getModel(),
                Matchers.containsString("bar={bar}"));
        assertEquals(UberAction.APPEND, linkNode.getAction());
    }

    class NestedBean {

        String foo = FOO_VALUE;
        Bean bean = new Bean(NESTED_FOO_VALUE, NESTED_BAR_VALUE);

        public NestedBean() {

        }

        public NestedBean(String foo, Bean bar) {
            this.foo = foo;
            this.bean = bar;
        }

        public String getFoo() {
            return foo;
        }

        public Bean getBean() {
            return bean;
        }
    }

    class Bean {

        String foo = FOO_VALUE;
        String bar = BAR_VALUE;

        public Bean() {

        }

        public Bean(String foo, String bar) {
            super();
            this.foo = foo;
            this.bar = bar;
        }

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }
    }


    class BeanResource extends RepresentationModel {


        public String getFoo() {
            return FOO_VALUE;
        }

        public String getBar() {
            return BAR_VALUE;
        }
    }

    @Test
    public void resourceToUberNode() throws Exception {
        EntityModel<Bean> beanResource = new EntityModel<Bean>(new Bean());
        beanResource.add(LINK_HOME);

        UberNode node = new UberNode();
        UberUtils.toUberData(node, beanResource);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(BAR_VALUE, node.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
        assertEquals(URL_HOME, node.getFirstByRel("home")
                .getUrl());
    }

    @Test
    public void beansToUberNode() throws Exception {
        List<Bean> beans = Arrays.asList(new Bean(), new Bean("fooValue2", "barValue2"));

        UberNode node = new UberNode();
        UberUtils.toUberData(node, beans);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(2, node.getData()
                .size());
        Iterator<UberNode> dataNodes = node.iterator();
        UberNode first = dataNodes.next();
        assertEquals(BAR_VALUE, first.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, first.getFirstByName("foo")
                .getValue());
        UberNode second = dataNodes.next();
        assertEquals("barValue2", second.getFirstByName("bar")
                .getValue());
        assertEquals("fooValue2", second.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void resourcesToUberNode() throws Exception {
        List<Bean> beans = Arrays.asList(new Bean(), new Bean("fooValue2", "barValue2"));
        CollectionModel<Bean> beanResources = new CollectionModel<Bean>(beans);
        beanResources.add(LINK_HOME);

        UberNode node = new UberNode();
        UberUtils.toUberData(node, beanResources);

        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(3, node.getData()
                .size());
        Iterator<UberNode> dataNodes = node.iterator();
        UberNode first = dataNodes.next();
        assertEquals(BAR_VALUE, first.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, first.getFirstByName("foo")
                .getValue());
        UberNode second = dataNodes.next();
        assertEquals("barValue2", second.getFirstByName("bar")
                .getValue());
        assertEquals("fooValue2", second.getFirstByName("foo")
                .getValue());
        assertEquals(URL_HOME, node.getFirstByRel("home")
                .getUrl());
    }


    @Test
    public void identifiableToUberNode() throws Exception {
        BeanResource bean = new BeanResource();
        String canonicalUrl = "http://www.example.com/bean/1";
        bean.add(new Link(canonicalUrl, IanaLinkRelations.SELF.value()));
        UberNode node = new UberNode();
        UberUtils.toUberData(node, bean);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        UberNode selfRel = node.getFirstByRel(IanaLinkRelations.SELF.value());
        assertEquals(canonicalUrl, selfRel.getUrl());
    }

    @Test
    public void resourceSupportToUberNode() throws Exception {
        BeanResource bean = new BeanResource();
        UberNode node = new UberNode();
        UberUtils.toUberData(node, bean);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(BAR_VALUE, node.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void nestedBeanToUberNode() throws Exception {
        NestedBean bean = new NestedBean();
        UberNode node = new UberNode();
        UberUtils.toUberData(node, bean);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
        UberNode nestedBean = node.getFirstByName("bean");
        assertNotNull("nested bean missing", nestedBean);
        assertEquals(NESTED_BAR_VALUE, nestedBean.getFirstByName("bar")
                .getValue());
        assertEquals(NESTED_FOO_VALUE, nestedBean.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void beanToUberNode() throws Exception {
        Bean bean = new Bean();
        UberNode node = new UberNode();
        UberUtils.toUberData(node, bean);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(BAR_VALUE, node.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void beanWithNullValueToUberNode() throws Exception {
        Bean bean = new Bean(FOO_VALUE, null);
        UberNode node = new UberNode();
        UberUtils.toUberData(node, bean);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(UberNode.NULL_VALUE, node.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void mapOfStringsToUberNode() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", FOO_VALUE);
        map.put("bar", BAR_VALUE);
        UberNode node = new UberNode();
        UberUtils.toUberData(node, map);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(2, node.getData()
                .size());
        assertEquals(BAR_VALUE, node.getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("foo")
                .getValue());
    }

    @Test
    public void mapOfBeansToUberNode() throws Exception {
        Map<String, Bean> map = new HashMap<String, Bean>();
        map.put("baz", new Bean());
        UberNode node = new UberNode();
        UberUtils.toUberData(node, map);
        System.out.println(new ObjectMapper().writeValueAsString(node));
        assertEquals(BAR_VALUE, node.getFirstByName("baz")
                .getFirstByName("bar")
                .getValue());
        assertEquals(FOO_VALUE, node.getFirstByName("baz")
                .getFirstByName("foo")
                .getValue());
    }
}
