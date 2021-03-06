// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.FastTests;
import fi.luontola.cqrshotel.util.Struct;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@Category(FastTests.class)
public class ProjectionTest {

    private static final DummyEvent one = new DummyEvent("one");
    private static final DummyEvent two = new DummyEvent("two");
    private static final DummyEvent three = new DummyEvent("three");

    private final EventStore eventStore = new InMemoryEventStore();
    private final SpyProjection projection = new SpyProjection(eventStore);

    @Test
    public void does_nothing_if_no_events() {
        projection.update();

        assertThat(projection.receivedEvents, is(empty()));
    }

    @Test
    public void updates_events_for_all_aggregates() {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
        projection.update();

        assertThat(projection.receivedEvents, is(asList(one, two)));
    }

    @Test
    public void updates_only_new_events_since_last_update() {
        eventStore.saveEvents(UUID.randomUUID(), singletonList(one), EventStore.BEGINNING);
        eventStore.saveEvents(UUID.randomUUID(), singletonList(two), EventStore.BEGINNING);
        projection.update();
        projection.receivedEvents.clear();

        eventStore.saveEvents(UUID.randomUUID(), singletonList(three), EventStore.BEGINNING);
        projection.update();

        assertThat("new events", projection.receivedEvents, is(singletonList(three)));
    }

    private static class SpyProjection extends Projection {

        public final List<DummyEvent> receivedEvents = new ArrayList<>();

        public SpyProjection(EventStore eventStore) {
            super(eventStore);
        }

        @EventListener
        private void apply(DummyEvent event) {
            receivedEvents.add(event);
        }
    }

    private static class DummyEvent extends Struct implements Event {
        public final String message;

        private DummyEvent(String message) {
            this.message = message;
        }
    }
}