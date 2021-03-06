// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class FakeEventStore implements EventStore {

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    public final List<Event> produced = new ArrayList<>();
    private UUID expectedStreamId;

    public void populateExistingEvents(UUID streamId, List<Event> events) {
        assertNewOrSame(streamId);
        eventStore.saveEvents(streamId, events, BEGINNING);
    }

    @Override
    public long saveEvents(UUID streamId, List<Event> newEvents, int expectedVersion) {
        assertNewOrSame(streamId);
        produced.addAll(newEvents);
        return eventStore.saveEvents(streamId, newEvents, expectedVersion);
    }

    private void assertNewOrSame(UUID streamId) {
        assertThat("streamId", streamId, is(notNullValue()));
        if (expectedStreamId == null) {
            expectedStreamId = streamId;
        }
        assertThat("streamId", streamId, is(expectedStreamId));
    }


    // generated delegate methods

    @Override
    public List<Event> getEventsForStream(UUID streamId, int sinceVersion) {
        return eventStore.getEventsForStream(streamId, sinceVersion);
    }

    @Override
    public List<Event> getAllEvents(long sincePosition) {
        return eventStore.getAllEvents(sincePosition);
    }

    @Override
    public int getCurrentVersion(UUID streamId) {
        return eventStore.getCurrentVersion(streamId);
    }

    @Override
    public long getCurrentPosition() {
        return eventStore.getCurrentPosition();
    }
}
