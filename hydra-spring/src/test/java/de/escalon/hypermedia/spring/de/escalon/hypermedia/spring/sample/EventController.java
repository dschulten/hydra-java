/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.sample;

import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.spring.Affordance;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Sample controller demonstrating the use of AffordanceBuilder and hydra-core annotations such as @Expose on
 * request parameters.
 * Created by dschulten on 11.09.2014.
 */
@Controller
@RequestMapping("/events")
public class EventController {

    final List<Event> events = Arrays.asList(new Event(1, "Walk off the Earth", "Gang of Rhythm Tour", "Wiesbaden", EventStatusType.EVENT_SCHEDULED),
            new Event(2, "Cornelia Bielefeldt", "Mein letzter Film", "Heilbronn", EventStatusType.EVENT_SCHEDULED));

    final List<EventResource> eventResources = Arrays.asList(new EventResource(1, "Walk off the Earth", "Gang of Rhythm Tour", "Wiesbaden"),
            new EventResource(2, "Cornelia Bielefeldt", "Mein letzter Film", "Heilbronn"));

    @RequestMapping
    public
    @ResponseBody
    Resources<Resource<Event>> getResourcesOfResourceOfEvent() {
        List<Resource<Event>> eventResourcesList = new ArrayList<Resource<Event>>();
        // each resource has links
        for (Event event : events) {
            Resource<Event> eventResource = new Resource<Event>(event);
            eventResource.add(linkTo(this.getClass()).slash(event.id)
                    .withSelfRel());
            eventResource.add(linkTo(methodOn(ReviewController.class)
                    .getReviews(event.id))
                    .withRel("review"));
            eventResource.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(this.getClass())
                    .updateEventWithRequestBody(eventResource.getContent().id, eventResource.getContent()))
                    .withSelfRel());
            eventResource.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(this.getClass())
                    .deleteEvent(eventResource.getContent().id))
                    .withSelfRel());
            eventResourcesList.add(eventResource);
        }

        // the resources have templated links to methods
        // specify method by reflection
        final Method getEventMethod = ReflectionUtils.findMethod(this.getClass(), "getEvent", String.class);
        final Affordance eventByNameAffordance = AffordanceBuilder.linkTo(getEventMethod, new Object[0])
                .withRel("eventByName");

        // specify method by sample invocation
        final Affordance eventByIdAffordance = AffordanceBuilder.linkTo(methodOn(this.getClass())
                .getEvent((Integer) null)) // passing null will result in a template variable
                .withRel("eventById");

        return new Resources<Resource<Event>>(eventResourcesList,
                eventByIdAffordance,
                eventByNameAffordance);
    }


    @RequestMapping("/list")
    public
    @ResponseBody
    List<Resource<Event>> getListOfResourceOfEvent() {
        List<Resource<Event>> eventResourcesList = new ArrayList<Resource<Event>>();
        for (Event event : events) {
            Resource<Event> eventResource = new Resource<Event>(event);
            eventResource.add(linkTo(this.getClass()).slash(event.id)
                    .withSelfRel());
            eventResource.add(linkTo(methodOn(ReviewController.class)
                    .getReviews(event.id))
                    .withRel("review"));
            eventResourcesList.add(eventResource);
        }
        return eventResourcesList;
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
    public
    @ResponseBody
    Resource<Event> getEvent(@PathVariable Integer eventId) {
        Resource<Event> resource = new Resource<Event>(events.get(eventId));
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"eventName"})
    public
    @ResponseBody
    Resource<Event> getEvent(@RequestParam @Expose("http://schema.org/name") String eventName) {
        Resource<Event> ret = null;
        for (Event event : events) {
            if (event.name.startsWith(eventName)) {
                Resource<Event> resource = new Resource<Event>(event);
                resource.add(linkTo(ReviewController.class).withRel("review"));
                ret = resource;
                break;
            }
        }
        return ret;
    }

    @RequestMapping("/resourcesupport/{eventId}")
    public
    @ResponseBody
    EventResource getResourceSupportEvent(@PathVariable int eventId) {
        EventResource resource = eventResources.get(eventId);
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateEventWithRequestBody(@PathVariable int eventId, @RequestBody Event event) {
        events.set(eventId - 1, event);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteEvent(@PathVariable int eventId) {
        events.remove(eventId - 1);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

}
