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

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
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
    Resources<Resource<Event>> getEvents() {
        List<Resource<Event>> eventResources = new ArrayList<Resource<Event>>();
        for (Event event : events) {
            Resource<Event> eventResource = new Resource<Event>(event);
            eventResource.add(linkTo(this.getClass()).slash(event.id)
                    .withSelfRel());
            eventResource.add(linkTo(methodOn(ReviewController.class)
                    .getReviews(event.id))
                    .withRel("review"));
            eventResources.add(eventResource);
        }

        return new Resources(eventResources,
                new Link(linkTo(this.getClass()).toString() + "{/eventId}", "event"));
    }


    @RequestMapping("/list")
    public
    @ResponseBody
    List<Resource<Event>> getEventsList() {
        List<Resource<Event>> eventResources = new ArrayList<Resource<Event>>();
        for (Event event : events) {
            Resource<Event> eventResource = new Resource<Event>(event);
            eventResource.add(linkTo(this.getClass()).slash(event.id)
                    .withSelfRel());
            eventResource.add(linkTo(methodOn(ReviewController.class)
                    .getReviews(event.id))
                    .withRel("review"));
            eventResources.add(eventResource);
        }
        return eventResources;
    }

    @RequestMapping(value="/{eventId}", method=RequestMethod.GET)
    public
    @ResponseBody
    Resource<Event> getEvent(@PathVariable int eventId) {
        Resource<Event> resource = new Resource(events.get(eventId));
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    @RequestMapping("/resourcesupport/{eventId}")
    public
    @ResponseBody
    EventResource getResourceSupportEvent(@PathVariable int eventId) {
        EventResource resource = eventResources.get(eventId);
        resource.add(linkTo(ReviewController.class).withRel("review"));
        return resource;
    }

    @RequestMapping(value ="/{eventId}", method= RequestMethod.PUT)
    public ResponseEntity<Void> updateEvent(@PathVariable int eventId, @RequestBody Event event) {
        events.set(eventId, event);
        return new ResponseEntity<Void>(HttpStatus.OK);
        // TODO apply entity-headers and obey Content-*
    }

    @RequestMapping(value ="/{eventId}/status", method= RequestMethod.PUT)
    public ResponseEntity<Void> updateEventStatus(@PathVariable int eventId, @RequestParam EventStatusType eventStatus) {
        final Event event = events.get(eventId);
        event.setEventStatus(eventStatus);
        return new ResponseEntity<Void>(HttpStatus.OK);
        // TODO apply entity-headers and obey Content-*
    }
}
