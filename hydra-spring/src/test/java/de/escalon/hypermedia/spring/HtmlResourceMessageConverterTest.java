package de.escalon.hypermedia.spring;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
@Ignore
public class HtmlResourceMessageConverterTest {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private static Map<String, String> namespaces = new HashMap<String, String>();

    static {
        namespaces.put("h", "http://www.w3.org/1999/xhtml");
    }

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();

    }

    @Test
    public void testCreatesHtmlFormForGet() throws Exception {
        this.mockMvc.perform(get("http://localhost/people/customerById").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("h:html/h:body/h:form/@action", namespaces).string("/people/customer"))
                .andExpect(xpath("//h:form/@name", namespaces).string("searchPerson"))
                .andExpect(xpath("//h:form/@method", namespaces).string("GET"));
    }

    @Test
    public void testCreatesHtmlFormForPost() throws Exception {
        this.mockMvc.perform(get("/people/customerByName").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("h:html/h:body/h:form/@action", namespaces).string("/people/customer"))
                .andExpect(xpath("//h:form/@name", namespaces).string("searchPerson"))
                .andExpect(xpath("//h:form/@method", namespaces).string("POST"));
    }

    @Test
    public void testCreatesHtmlFormForPut() throws Exception {
        this.mockMvc.perform(get("/people/customer/123/editor").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("h:html/h:body/h:form/@action", namespaces).string("/people/customer"))
                .andExpect(xpath("//h:form/@name", namespaces).string("updatePerson"))
                .andExpect(xpath("//h:form/@method", namespaces).string("PUT"));
    }

    @Test
    public void testCreatesInputFieldWithMinMaxNumber() throws Exception {

        this.mockMvc.perform(get("/people/customerById").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:input/@name", namespaces).string("personId"))
                .andExpect(xpath("//h:input/@type", namespaces).string("number"))
                .andExpect(xpath("//h:input/@min", namespaces).string("0"))
                .andExpect(xpath("//h:input/@max", namespaces).string("9999"))
                .andExpect(xpath("//h:input/@value", namespaces).string("1234"));
    }

    @Test
    public void testCreatesInputFieldWithDefaultText() throws Exception {

        this.mockMvc.perform(get("/people/customerByName").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:input/@name", namespaces).string("name"))
                .andExpect(xpath("//h:input/@type", namespaces).string("text"))
                .andExpect(xpath("//h:input/@value", namespaces).string("Bombur"));
    }

    /**
     * Tests if the form contains a personId input field with default value.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesHiddenInputField() throws Exception {

        this.mockMvc.perform(get("/people/customer/123/editor").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:input[@name='personId']", namespaces).exists())
                .andExpect(xpath("//h:input[@name='personId']/@type", namespaces).string("hidden"))
                .andExpect(xpath("//h:input[@name='personId']/@value", namespaces).string("123"))
                .andExpect(xpath("//h:input[@name='firstname']/@value", namespaces).string("Bilbo"));
    }

    /**
     * Tests if the form contains a select field.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesSelectFieldForEnum() throws Exception {

        this.mockMvc.perform(get("/people/customer/123/editor").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:select[@name='gender']", namespaces).exists())
                .andExpect(xpath("//h:select/h:option[1]/text()", namespaces).string("MALE"))
                .andExpect(xpath("//h:select/h:option[2]/text()", namespaces).string("FEMALE"))
                .andExpect(xpath("(//h:select/h:option)[@selected]/text()", namespaces).string("MALE"))
                .andExpect(xpath("//h:form/@name", namespaces).string("updatePerson"));
    }

    /**
     * Tests if the form contains a multiselect field with three preselected items, matching the person having id 123.
     *
     * @throws Exception
     */
//    @Test
//    public void testCreatesMultiSelectFieldForEnumArray() throws Exception {
//
//        this.mockMvc.perform(get("/people/customer/123/editor").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.TEXT_HTML))
//                .andExpect(xpath("//h:select[@name='sports' and @multiple]", namespaces).exists())
//                .andExpect(xpath("//h:select[@name='sports']/h:option", namespaces).nodeCount(Sport.values().length))
//                .andExpect(xpath("(//h:select[@name='sports']/h:option)[@selected]", namespaces).nodeCount(3));
//    }
//
//    /**
//     * Tests List<Enum> parameter.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void testCreatesMultiSelectFieldForEnumList() throws Exception {
//
//        this.mockMvc.perform(get("/people/customer/123/editor").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.TEXT_HTML))
//                .andExpect(xpath("//h:select[@name='gadgets' and @multiple]", namespaces).exists())
//                .andExpect(xpath("//h:select[@name='gadgets']/h:option", namespaces).nodeCount(Gadget.values().length))
//                .andExpect(xpath("(//h:select[@name='gadgets']/h:option)[@selected]", namespaces).nodeCount(0));
//    }

    /**
     * Tests List<String> parameter with a list of possible values.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesSelectFieldForListOfPossibleValues() throws Exception {

        this.mockMvc.perform(get("/people/customerByMood").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:select[@name='mood']", namespaces).exists())
                .andExpect(xpath("//h:select[@name='mood' and @multiple]", namespaces).doesNotExist())
                .andExpect(xpath("//h:select[@name='mood']/h:option", namespaces).nodeCount(5))
                .andExpect(xpath("(//h:select[@name='mood']/h:option)[@selected]", namespaces).string("angry"));

    }

    /**
     * Tests List<String> parameter with a list of possible values.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesMultiSelectFieldForListOfPossibleValuesFixed() throws Exception {

        this.mockMvc.perform(get("/people/customerByAttribute").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:select[@name='attr' and @multiple]", namespaces).exists())
                .andExpect(xpath("//h:select[@name='attr']/h:option", namespaces).nodeCount(3))
                .andExpect(xpath("(//h:select[@name='attr']/h:option)[@selected]", namespaces).string("hungry"));

    }

    /**
     * Tests List<String> parameter with a list of possible values.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesMultiSelectFieldForListOfPossibleValuesFromSpringBean() throws Exception {

        this.mockMvc.perform(get("/people/customer/123/details").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:select[@name='detail' and @multiple]", namespaces).exists())
                .andExpect(xpath("//h:select[@name='detail']/h:option", namespaces).nodeCount(3))
                .andExpect(xpath("(//h:select[@name='detail']/h:option)[1]", namespaces).string("beard"))
                .andExpect(xpath("(//h:select[@name='detail']/h:option)[2]", namespaces).string("afterShave"))
                .andExpect(xpath("(//h:select[@name='detail']/h:option)[3]", namespaces).string("noseHairTrimmer"));

    }

    /**
     * Tests List<String> parameter with a list of numbers.
     *
     * @throws Exception
     */
    @Test
    public void testCreatesMultipleInputWithDefaultForIntegerList() throws Exception {

        String defaultValue = "42";
        this.mockMvc.perform(get("/people/customer/123/numbers").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:input[@name='number']", namespaces).nodeCount(3))
                .andExpect(xpath("(//h:input[@name='number'])[1]/@value", namespaces).string(defaultValue));

    }

    /**
     * Tests List<String> parameter with a list of numbers.
     *
     * @throws Exception
     */
    @Test
    @Ignore("implement code on demand")
    public void testCreatesOneInputForIntegerListWithInputUpToAny() throws Exception {

        this.mockMvc.perform(get("/people/customer/123/numbers").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(xpath("//h:input[@name='number']", namespaces).nodeCount(1));
        // expect code-on-demand here

    }

}
