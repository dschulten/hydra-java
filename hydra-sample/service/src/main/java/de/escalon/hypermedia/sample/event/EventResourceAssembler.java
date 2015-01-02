package de.escalon.hypermedia.sample.event;

import de.escalon.hypermedia.sample.beans.Event;
import de.escalon.hypermedia.sample.model.EventModel;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

/**
 * Created by dschulten on 28.12.2014.
 */
@Component
public class EventResourceAssembler extends ResourceAssemblerSupport<EventModel, Event> {

    public EventResourceAssembler() {
        super(EventController.class, Event.class);
    }

    @Override
    public Event toResource(EventModel eventModel) {
        return new Event(eventModel.id, eventModel.performer, eventModel.workPerformed, eventModel.location, eventModel.eventStatus);
    }
}
