package org.deephacks.westty.internal.cluster;

import com.hazelcast.core.Member;
import org.deephacks.westty.cluster.EntryEvent;
import org.deephacks.westty.server.Server;

import java.net.InetSocketAddress;

public class WesttyEntryEvent<K, V> implements EntryEvent<K, V> {
    private com.hazelcast.core.EntryEvent<K, V> event;

    public WesttyEntryEvent(com.hazelcast.core.EntryEvent<K, V> event) {
        this.event = event;
    }

    @Override
    public K getKey() {
        return event.getKey();
    }

    @Override
    public V getOldValue() {
        return event.getOldValue();
    }

    @Override
    public V getValue() {
        return event.getValue();
    }

    @Override
    public Server getMember() {
        Member member = event.getMember();
        InetSocketAddress add = member.getInetSocketAddress();
        return new Server(add.getAddress().getHostAddress(), add.getPort());
    }

    @Override
    public EntryEventType getEventType() {
        if (event.getEventType() == com.hazelcast.core.EntryEventType.ADDED) {
            return EntryEventType.ADDED;
        } else if (event.getEventType() == com.hazelcast.core.EntryEventType.REMOVED) {
            return EntryEventType.REMOVED;
        } else if (event.getEventType() == com.hazelcast.core.EntryEventType.UPDATED) {
            return EntryEventType.UPDATED;
        } else {
            return EntryEventType.EVICTED;
        }
    }

    @Override
    public String getName() {
        return event.getName();
    }

}
