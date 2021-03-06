// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;

public class Repository<T extends AggregateRoot> {

    private final Class<T> aggregateType;
    private final EventStore eventStore;

    public Repository(EventStore eventStore) {
        this.aggregateType = getAggregateRootType();
        this.eventStore = eventStore;
    }

    @SuppressWarnings("unchecked")
    private Class<T> getAggregateRootType() {
        ParameterizedType myType = (ParameterizedType) getClass().getGenericSuperclass();
        Class<?> typeArg = (Class<?>) myType.getActualTypeArguments()[0];
        if (AggregateRoot.class.isAssignableFrom(typeArg)) {
            return (Class<T>) typeArg;
        } else {
            throw new IllegalArgumentException("Not aggregate root: " + typeArg);
        }
    }

    public T create(UUID id) {
        try {
            T aggregate = aggregateType.newInstance();
            aggregate.setId(id);
            return aggregate;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public T createOrGet(UUID id) {
        T aggregate = create(id);
        List<Event> events = eventStore.getEventsForStream(id);
        aggregate.loadFromHistory(events);
        return aggregate;
    }

    public T getById(UUID id) {
        T aggregate = createOrGet(id);
        if (aggregate.getVersion() == EventStore.BEGINNING) {
            throw new EventStreamNotFoundException(id);
        }
        return aggregate;
    }

    public void save(T aggregate, int expectedVersion) {
        List<Event> events = aggregate.getUncommittedChanges();
        eventStore.saveEvents(aggregate.getId(), events, expectedVersion);
        aggregate.markChangesAsCommitted();
    }
}
