package org.sfm.reflect.meta;

import org.sfm.reflect.*;
import org.sfm.reflect.impl.NullSetter;

import java.lang.reflect.Type;

public class ConstructorPropertyMeta<T, P> extends PropertyMeta<T, P> {

    private final Class<T> owner;
    private final Setter<T, P> setter = NullSetter.setter();
    private final ScoredGetter<T, P> scoredGetter;
    private final Parameter parameter;

    public ConstructorPropertyMeta(String name,
                                   ReflectionService reflectService,
                                   Parameter parameter,
                                   Class<T> owner) {
        this(name, reflectService, parameter, owner, ScoredGetter.<T, P>nullGetter());
    }

    public ConstructorPropertyMeta(String name,
                                   ReflectionService reflectService,
                                   Parameter parameter,
                                   Class<T> owner, ScoredGetter<T, P> scoredGetter) {
		super(name, reflectService);
		this.parameter = parameter;
        this.owner = owner;
        this.scoredGetter = scoredGetter;
    }


	@Override
	public Setter<T, P> getSetter() {
        return setter;
	}

    @Override
    public Getter<T, P> getGetter() {
        return scoredGetter.getGetter();
    }

    public ConstructorPropertyMeta<T, P> getter(ScoredGetter<T, P> getter) {
        if (getter.isBetterThan(this.scoredGetter)) {
            return new ConstructorPropertyMeta<T, P>(getName(), reflectService, parameter, owner, getter);
        } else {
            return this;
        }
    }

    @Override
	public Type getPropertyType() {
		return parameter.getGenericType();
	}

	public Parameter getParameter() {
		return parameter;
	}
	
	public boolean isConstructorProperty() {
		return true;
	}

	@Override
	public String getPath() {
		return getName();
	}

    @Override
    public String toString() {
        return "ConstructorPropertyMeta{" +
                "owner=" + owner +
                ", constructorParameter=" + parameter +
                '}';
    }
}
