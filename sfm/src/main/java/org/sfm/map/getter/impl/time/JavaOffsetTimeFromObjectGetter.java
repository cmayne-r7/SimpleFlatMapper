package org.sfm.map.getter.impl.time;

import org.sfm.reflect.Getter;

import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.Date;


public class JavaOffsetTimeFromObjectGetter<S> implements Getter<S, OffsetTime> {
    private final Getter<S, ?> getter;
    private final ZoneId zone;

    public JavaOffsetTimeFromObjectGetter(Getter<S, ?> getter, ZoneId zoneId) {
        this.getter = getter;
        this.zone = zoneId;
    }

    @Override
    public OffsetTime get(S target) throws Exception {
        Object o = getter.get(target);

        if (o == null) {
            return null;
        }

        if (o instanceof Date) {
            final Instant instant = Instant.ofEpochMilli(((Date) o).getTime());
            return instant.atOffset(zone.getRules().getOffset(instant)).toOffsetTime();
        }

        if (o instanceof OffsetTime) {
            return (OffsetTime) o;
        }

        if (o instanceof LocalDateTime) {
            return ((LocalDateTime)o).atZone(zone).toOffsetDateTime().toOffsetTime();
        }

        if (o instanceof LocalTime) {
            return ((LocalTime)o).atOffset(zone.getRules().getStandardOffset(Instant.now()));
        }

        if (o instanceof TemporalAccessor) {
            return OffsetTime.from((TemporalAccessor)o);
        }

        throw new IllegalArgumentException("Cannot convert " + o + " to OffsetTime");
    }

    @Override
    public String toString() {
        return "JavaOffsetTimeFromObjectGetter{" +
                "getter=" + getter +
                '}';
    }
}
