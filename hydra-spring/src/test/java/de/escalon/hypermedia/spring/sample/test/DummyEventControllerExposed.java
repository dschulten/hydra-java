/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
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

package de.escalon.hypermedia.spring.sample.test;

import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;


/**
 * Sample controller demonstrating the use of AffordanceBuilder and hydra-core annotations such as @Expose on request
 * parameters. Created by dschulten on 11.09.2014.
 */
@Controller
@RequestMapping("/events")
public class DummyEventControllerExposed extends DummyEventController {

    @RequestMapping(value = "/regex/{eventId:.+}", method = RequestMethod.GET)
    public
    @ResponseBody
    EntityModel<Event> getEventWithRegexPathVariableMapping(@PathVariable @Expose("ex:eventId") Integer eventId) {
        EntityModel<Event> resource = new EntityModel<Event>(getEvents().get(eventId));
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    public static class FooResource extends RepresentationModel {
        private Pageable pageable;

        public FooResource(Pageable pageable) {
            this.pageable = pageable;
        }

        public Pageable getPageable() {
            return pageable;
        }

    }

    public static class Bar {
        private int baz;

        public int getBaz() {
            return baz;
        }

        public void setBaz(int baz) {
            this.baz = baz;
        }
    }

    public static class Pageable {
        private int offset = 0;
        private int size = 0;
        private List<String> strings;

        public List<Bar> getBars() {
            return bars;
        }

        public void setBars(List<Bar> bars) {
            this.bars = bars;
        }

        public Bar getBar() {
            return bar;
        }

        public void setBar(Bar bar) {
            this.bar = bar;
        }

        private Bar bar;
        private List<Bar> bars;


        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void setStrings(List<String> strings) {
            this.strings = strings;
        }

        public List<String> getStrings() {
            return strings;
        }


    }

    @RequestMapping("/query")
    public HttpEntity<CollectionModel<FooResource>> findList(@Input(include = {"offset", "size"}) Pageable pageable,
                                                       @RequestParam(required = false) Integer foo1,
                                                       @RequestParam(required = false) Integer foo2) {

        FooResource fooResource = new FooResource(pageable);
        fooResource.add(AffordanceBuilder
                .linkTo(methodOn(this.getClass()).findList(null, null, null))
                .withRel("template"));

        return new HttpEntity<CollectionModel<FooResource>>(new CollectionModel<FooResource>(Collections.singleton(fooResource)));

    }

    @RequestMapping(method = RequestMethod.GET, params = {"evtName"})
    public
    @ResponseBody
    EntityModel<Event> findEventByName(@RequestParam("evtName") @Expose("http://schema.org/name") String eventName) {
        EntityModel<Event> ret = null;
        for (Event event : getEvents()) {
            if (event.getWorkPerformed()
                    .getContent().name.startsWith(eventName)) {
                EntityModel<Event> resource = new EntityModel<Event>(event);
                resource.add(linkTo(ReviewController.class).withRel("review"));
                ret = resource;
                break;
            }
        }
        return ret;
    }


    protected List<? extends EventResource> getEventResources() {
        return Arrays.asList(new EventResourceExposed(1, "Walk off the Earth", "Gang of Rhythm Tour", "Wiesbaden"),
                new EventResourceExposed(2, "Cornelia Bielefeldt", "Mein letzter Film", "Heilbronn"));
    }

}
