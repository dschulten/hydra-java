package de.escalon.hypermedia.sample.event;

import de.escalon.hypermedia.sample.beans.event.Event;
import de.escalon.hypermedia.sample.model.event.EventModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

/**
 * Created by dschulten on 28.12.2014.
 */
@Component
public class EventResourceAssembler extends RepresentationModelAssemblerSupport<EventModel, Event> {

    public EventResourceAssembler() {
        super(EventController.class, Event.class);
    }

    @Override
    public Event toModel(EventModel eventModel) {
        return new Event(eventModel.id, eventModel.performer, eventModel.workPerformed, eventModel.location,
                eventModel.eventStatus);
    }
}
