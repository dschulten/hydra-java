package de.escalon.hypermedia.sample.event;

import de.escalon.hypermedia.sample.beans.event.Event;
import de.escalon.hypermedia.sample.beans.event.Rating;
import de.escalon.hypermedia.sample.beans.event.Review;
import de.escalon.hypermedia.sample.model.event.CreativeWork;
import de.escalon.hypermedia.sample.model.event.EventModel;
import de.escalon.hypermedia.sample.model.event.EventStatusType;
import de.escalon.hypermedia.spring.AffordanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample Event Controller. Created by dschulten on 28.12.2014.
 */
@Controller
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventBackend eventBackend;

    @Autowired
    private EventResourceAssembler assembler;

//    @RequestMapping(method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<CollectionModel<Event>> getEvents() {
//        List<Event> events = assembler.toResources(eventBackend.getEvents());
//        for (Event event : events) {
//            addAffordances(event);
//        }
//        CollectionModel<Event> eventResources = new CollectionModel<Event>(events);
//
//        eventResources.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(EventController.class).addEvent(null))
//                .withSelfRel());
//
//        return new ResponseEntity<CollectionModel<Event>>(eventResources, HttpStatus.OK);
//    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CollectionModel<Event>> findEvents(@RequestParam(required = false) String name) {
        CollectionModel<Event> events = assembler.toCollectionModel(eventBackend.getEvents());
        List<Event> matches = new ArrayList<>();
        for (Event event : events) {
            if (name == null || event.workPerformed.getContent().name.equals(name)) {
                addAffordances(event);
                matches.add(event);
            }
        }
        CollectionModel<Event> eventResources = CollectionModel.of(matches);

        eventResources.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(EventController.class)
                .addEvent(new Event(null, new CreativeWork(null), null, EventStatusType.EVENT_SCHEDULED)))
                .withSelfRel());

        eventResources.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(EventController.class)
                .findEvents(null))
                .withRel("hydra:search"));

        return new ResponseEntity<CollectionModel<Event>>(eventResources, HttpStatus.OK);
    }


    @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
    public ResponseEntity<Event> getEvent(@PathVariable Integer eventId) {
        Event event = assembler.toModel(eventBackend.getEvent(eventId));

        addAffordances(event);

        return new ResponseEntity<Event>(event, HttpStatus.OK);
    }

    private void addAffordances(Event event) {
        event.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(this.getClass())
                .getEvent(event.id))
                .and(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(EventController.class)
                        .updateEvent(event.id, event)))
                .and(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(EventController.class)
                        .deleteEvent(event.id)))
                .withSelfRel());
        event.workPerformed.add(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(ReviewController.class)
                .addReview(event.id, new Review(null, new Rating(3))))
                .withRel("review"));
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateEvent(@PathVariable int eventId, @RequestBody Event event) {
        eventBackend.updateEvent(eventId, event.getEventStatus());
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteEvent(@PathVariable int eventId) {
        eventBackend.deleteEvent(eventId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }


    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> addEvent(@RequestBody Event event) {
        EventModel eventModel = new EventModel(-1, event.performer, event.workPerformed.getContent(), event.location,
                event.getEventStatus());
        int eventId = eventBackend.addEvent(eventModel);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(this.getClass())
                .getEvent(eventId))
                .toUri());
        return new ResponseEntity<Void>(httpHeaders, HttpStatus.CREATED);
    }


}
