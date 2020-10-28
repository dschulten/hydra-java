package de.escalon.hypermedia.sample.beans.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.action.Select;
import de.escalon.hypermedia.sample.model.event.CreativeWork;
import de.escalon.hypermedia.sample.model.event.EventStatusType;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * Sample Event EntityModel, represents an Event with links. Created by dschulten on 11.09.2014.
 */
public class Event extends RepresentationModel<Event> {
    public final int id;
    public final String performer;
    public final String location;
    private EventStatusType eventStatus;
    public final EntityModel<CreativeWork> workPerformed;

    @JsonCreator
    public Event(@JsonProperty("performer") String performer,
                 @JsonProperty("workPerformed") CreativeWork workPerformed,
                 @JsonProperty("location") String location,
                 @JsonProperty("eventStatus") @Select() EventStatusType eventStatus) {
        this.id = 0;
        this.performer = performer;
        this.location = location;
        this.workPerformed = new EntityModel<CreativeWork>(workPerformed);
        this.eventStatus = eventStatus;
    }

    public Event(int id, String performer, CreativeWork workPerformed, String location, EventStatusType eventStatus) {
        this.id = id;
        this.performer = performer;
        this.workPerformed = new EntityModel<CreativeWork>(workPerformed);
        this.location = location;
        this.eventStatus = eventStatus;
    }

    /**
     * Presence of setter makes de.escalon.hypermedia.sample.event status writable by default.
     *
     * @param eventStatus
     */
    public void setEventStatus(EventStatusType eventStatus) {
        this.eventStatus = eventStatus;
    }

    public EventStatusType getEventStatus() {
        return eventStatus;
    }


}
