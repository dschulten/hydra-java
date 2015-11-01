package de.escalon.hypermedia.sample.model.event;

/**
 * Sample Event model bean.
 * Created by dschulten on 11.09.2014.
 */
public class EventModel {
    public final int id;
    public final String performer;
    public final String location;
    public EventStatusType eventStatus;
    public final CreativeWork workPerformed;

    public EventModel(int id, String performer, CreativeWork workPerformed, String location, EventStatusType eventStatus) {
        this.id = id;
        this.performer = performer;
        this.workPerformed = workPerformed;
        this.location = location;
        this.eventStatus = eventStatus;
    }

    public EventModel withEventStatus(EventStatusType eventStatus) {
        return new EventModel(this.id, this.performer, this.workPerformed, this.location, eventStatus);
    }

    public EventModel withEventId(int id) {
        return new EventModel(id, this.performer, this.workPerformed, this.location, this.eventStatus);
    }
}
