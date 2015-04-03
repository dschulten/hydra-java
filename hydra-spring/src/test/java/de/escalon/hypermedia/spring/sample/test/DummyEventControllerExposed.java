/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.sample.test;

import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.spring.Affordance;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;


/**
 * Sample controller demonstrating the use of AffordanceBuilder and hydra-core annotations such as @Expose on
 * request parameters.
 * Created by dschulten on 11.09.2014.
 */
@Controller
@RequestMapping("/events")
public class DummyEventControllerExposed extends DummyEventController {

    @RequestMapping(value = "/regex/{eventId:.+}", method = RequestMethod.GET)
    public
    @ResponseBody
    Resource<Event> getEventWithRegexPathVariableMapping(@PathVariable @Expose("ex:eventId") Integer eventId) {
        Resource<Event> resource = new Resource<Event>(getEvents().get(eventId));
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"eventName"})
    public
    @ResponseBody
    Resource<Event> getEvent(@RequestParam @Expose("http://schema.org/name") String eventName) {
        Resource<Event> ret = null;
        for (Event event : getEvents()) {
            if (event.getWorkPerformed()
                    .getContent().name.startsWith(eventName)) {
                Resource<Event> resource = new Resource<Event>(event);
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
