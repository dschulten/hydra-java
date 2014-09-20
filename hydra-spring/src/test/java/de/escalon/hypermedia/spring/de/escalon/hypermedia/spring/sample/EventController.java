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

import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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

    final List<Event> events = Arrays.asList(new Event(1, "Walk off the Earth", "Gang of Rhythm Tour", "Wiesbaden"),
            new Event(2, "Cornelia Bielefeldt", "Mein letzter Film", "Heilbronn"));

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
                    .withRel("reviews"));
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
                    .withRel("reviews"));
            eventResources.add(eventResource);
        }
        return eventResources;
    }

    @RequestMapping("/{index}")
    public
    @ResponseBody
    Resource<Event> getEvent(@PathVariable int index) {
        Resource<Event> resource = new Resource(events.get(index));
        resource.add(linkTo(ReviewController.class).withRel("reviews"));
        return resource;
    }

    @RequestMapping("/resourcesupport/{index}")
    public
    @ResponseBody
    EventResource getResourceSupportEvent(@PathVariable int index) {
        EventResource resource = eventResources.get(index);
        resource.add(linkTo(ReviewController.class).withRel("reviews"));
        return resource;
    }

}
