package de.escalon.hypermedia.sample.beans;

import de.escalon.hypermedia.sample.model.CreativeWork;
import de.escalon.hypermedia.sample.model.EventStatusType;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

/**
 * Sample Event Resource, represents an Event with links.
 * Created by dschulten on 11.09.2014.
 */
public class Event extends ResourceSupport {
    public final int id;
    public final String performer;
    public final String location;
    private EventStatusType eventStatus;
    public final Resource<CreativeWork> workPerformed;

    public Event(int id, String performer, CreativeWork workPerformed, String location, EventStatusType eventStatus) {
        this.id = id;
        this.performer = performer;
        this.workPerformed = new Resource<CreativeWork>(workPerformed);
        this.location = location;
        this.eventStatus = eventStatus;
    }

    /**
     * Presence of setter makes de.escalon.hypermedia.sample.event status writable by default.
     * @param eventStatus
     */
    public void setEventStatus(EventStatusType eventStatus) {
        this.eventStatus = eventStatus;
    }

    public EventStatusType getEventStatus() {
        return eventStatus;
    }

}
