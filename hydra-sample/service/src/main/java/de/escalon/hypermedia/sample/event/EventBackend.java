package de.escalon.hypermedia.sample.event;

import de.escalon.hypermedia.sample.beans.event.Rating;
import de.escalon.hypermedia.sample.beans.event.Review;
import de.escalon.hypermedia.sample.model.event.CreativeWork;
import de.escalon.hypermedia.sample.model.event.EventModel;
import de.escalon.hypermedia.sample.model.event.EventStatusType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by dschulten on 28.12.2014.
 */
@Component
public class EventBackend {

    Map<Integer, EventModel> eventModels = new HashMap<Integer, EventModel>();

    Map<Integer, List<Review>> reviews = new HashMap<Integer, List<Review>>();

    private static int count = 0;

    public EventBackend() {

        count++;
        eventModels.put(count, new EventModel(count, "Walk off the Earth", new CreativeWork("Gang of Rhythm Tour"),
                "Wiesbaden", EventStatusType.EVENT_SCHEDULED));
        reviews.put(count, new ArrayList<Review>(Arrays.asList(new Review("Five peeps, one guitar", new Rating(5)))));


        count++;
        eventModels.put(count, new EventModel(count, "Cornelia Bielefeldt", new CreativeWork("Mein letzter Film"),
                "Heilbronn", EventStatusType.EVENT_SCHEDULED));
        reviews.put(count, new ArrayList<Review>(Arrays.asList(new Review("Great actress, special atmosphere", new
                Rating(5)))));
    }

    public Collection<EventModel> getEvents() {
        return eventModels.values();
    }

    public int addEvent(EventModel eventModel) {
        count++;
        eventModels.put(count, eventModel.withEventId(count));
        return count;
    }

    public void updateEvent(int eventId, EventStatusType eventStatus) {
        EventModel eventModel = eventModels.get(eventId);
        eventModels.put(eventId, eventModel.withEventStatus(eventStatus));
    }

    public EventModel getEvent(int eventId) {
        return eventModels.get(eventId);
    }

    public void deleteEvent(int eventId) {
        eventModels.remove(eventId);
        reviews.remove(eventId);
    }

    public Map<Integer, List<Review>> getReviews() {
        return reviews;
    }

    public void addReview(int eventId, String reviewBody, Rating reviewRating) {
        EventModel eventModel = eventModels.get(eventId);
        if (eventModel != null) {
            List<Review> reviewsForEvent = reviews.get(eventId);
            if (reviewsForEvent == null) {
                reviewsForEvent = new ArrayList<Review>();
                reviews.put(eventId, reviewsForEvent);
            }
            reviewsForEvent.add(new Review(reviewBody, reviewRating));
        } else {
            throw new NoSuchElementException("not found");
        }
    }
}
