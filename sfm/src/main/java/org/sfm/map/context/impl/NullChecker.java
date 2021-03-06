package org.sfm.map.context.impl;

import org.sfm.map.context.KeySourceGetter;
import org.sfm.utils.ErrorHelper;
import org.sfm.utils.Predicate;

import java.util.List;

public class NullChecker<S, K> implements Predicate<S> {

    private final List<K> keys;
    private final KeySourceGetter<K, S> keySourceGetter;

    public NullChecker(List<K> keys, KeySourceGetter<K, S> keySourceGetter) {
        this.keys = keys;
        this.keySourceGetter = keySourceGetter;
    }

    @Override
    public boolean test(S s) {
        try {
            if (keys.isEmpty()) return false;
            for (int i = 0; i < keys.size(); i++) {
                if (keySourceGetter.getValue(keys.get(i), s) != null) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            ErrorHelper.rethrow(e);
            throw new IllegalStateException();
        }
    }
}
