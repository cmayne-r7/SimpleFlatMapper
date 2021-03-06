package org.sfm.csv;

import org.sfm.csv.impl.*;
import org.sfm.csv.mapper.CellSetter;
import org.sfm.csv.mapper.CsvMapperCellHandler;
import org.sfm.csv.mapper.CsvMapperCellHandlerFactory;
import org.sfm.csv.mapper.DelayedCellSetterFactory;
import org.sfm.map.*;
import org.sfm.map.column.ColumnProperty;
import org.sfm.map.mapper.*;
import org.sfm.reflect.*;
import org.sfm.reflect.meta.*;
import org.sfm.tuples.Tuple3;
import org.sfm.utils.ErrorHelper;
import org.sfm.utils.ForEachCallBack;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvMapperBuilder<T> {

	private final Type target;
	private final ReflectionService reflectionService;
	private final MapperConfig<CsvColumnKey, CsvColumnDefinition> mapperConfig;

    private final int minDelayedSetter;

	private final CellValueReaderFactory cellValueReaderFactory;
	private final PropertyMappingsBuilder<T, CsvColumnKey, CsvColumnDefinition> propertyMappingsBuilder;

	private String defaultDateFormat = "yyyy-MM-dd HH:mm:ss";

    public CsvMapperBuilder(final Type target) {
		this(target, ReflectionService.newInstance());
	}
	
	@SuppressWarnings("unchecked")
	public CsvMapperBuilder(final Type target, ReflectionService reflectionService) {
		this(target, (ClassMeta<T>)reflectionService.getClassMeta(target));
	}

	public CsvMapperBuilder(final Type target, final ClassMeta<T> classMeta) {
		this(target, classMeta, new IdentityCsvColumnDefinitionProvider());
	}

	public CsvMapperBuilder(final Type target, final ClassMeta<T> classMeta, ColumnDefinitionProvider<CsvColumnDefinition, CsvColumnKey> columnDefinitionProvider) {
		this(target, classMeta, 0,
                new CellValueReaderFactoryImpl(), MapperConfig.<CsvColumnKey, CsvColumnDefinition>config(columnDefinitionProvider));
	}

	public CsvMapperBuilder(final Type target,
							final ClassMeta<T> classMeta,
							int minDelayedSetter,
							CellValueReaderFactory cellValueReaderFactory,
							MapperConfig<CsvColumnKey, CsvColumnDefinition> mapperConfig
	) throws MapperBuildingException {
		this.target = target;
        this.minDelayedSetter = minDelayedSetter;
        this.reflectionService = classMeta.getReflectionService();
		this.propertyMappingsBuilder = new PropertyMappingsBuilder<T, CsvColumnKey, CsvColumnDefinition>(classMeta,
				mapperConfig.propertyNameMatcherFactory(), mapperConfig.mapperBuilderErrorHandler(),
				new PropertyWithSetter());
		this.cellValueReaderFactory = cellValueReaderFactory;
		this.mapperConfig = mapperConfig;
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey) {
		return addMapping(columnKey, propertyMappingsBuilder.size());
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey, int columnIndex) {
		return addMapping(new CsvColumnKey(columnKey, columnIndex), CsvColumnDefinition.identity());
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey, final CsvColumnDefinition columnDefinition) {
		return addMapping(columnKey, propertyMappingsBuilder.size(), columnDefinition);
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey, int columnIndex, CsvColumnDefinition columnDefinition) {
		return addMapping(new CsvColumnKey(columnKey, columnIndex), columnDefinition);
	}

	public final CsvMapperBuilder<T> addMapping(final CsvColumnKey key, final CsvColumnDefinition columnDefinition) {
		final CsvColumnDefinition composedDefinition = CsvColumnDefinition.compose(getColumnDefinition(key), columnDefinition);
		final CsvColumnKey mappedColumnKey = composedDefinition.rename(key);

		propertyMappingsBuilder.addProperty(mappedColumnKey, composedDefinition);

		return this;
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey, final ColumnProperty... properties) {
		return addMapping(columnKey, propertyMappingsBuilder.size(), properties);
	}

	public final CsvMapperBuilder<T> addMapping(final String columnKey, int columnIndex, final ColumnProperty... properties) {
		return addMapping(new CsvColumnKey(columnKey, columnIndex), properties);
	}

	public final CsvMapperBuilder<T> addMapping(final CsvColumnKey key, final ColumnProperty... properties) {
		return addMapping(key, CsvColumnDefinition.of(properties));
	}

	private <P> void addMapping(PropertyMeta<T, P> propertyMeta, final CsvColumnKey key, final CsvColumnDefinition columnDefinition) {
		propertyMappingsBuilder.addProperty(key, columnDefinition, propertyMeta);
	}
	
	private CsvColumnDefinition getColumnDefinition(CsvColumnKey key) {
		return CsvColumnDefinition.compose(CsvColumnDefinition.dateFormatDefinition(defaultDateFormat), mapperConfig.columnDefinitions().getColumnDefinition(key));
	}


	public void setDefaultDateFormat(String defaultDateFormat) {
		this.defaultDateFormat = defaultDateFormat;
	}

	public final CsvMapper<T> mapper() {
        ParsingContextFactoryBuilder parsingContextFactoryBuilder = new ParsingContextFactoryBuilder(propertyMappingsBuilder.maxIndex() + 1);

        Tuple3<Map<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>>, Integer, Boolean> constructorParams = buildConstructorParametersDelayedCellSetter();
        final Instantiator<CsvMapperCellHandler<T>, T> instantiator = getInstantiator(constructorParams.first());
        final CsvColumnKey[] keys = getKeys();

        // will build the context factory builder
        final CellSetter<T>[] setters = getSetters(parsingContextFactoryBuilder, constructorParams.second());
        final DelayedCellSetterFactory<T, ?>[] delayedCellSetterFactories = buildDelayedSetters(parsingContextFactoryBuilder, constructorParams.second(), constructorParams.third());

        // needs to happen last
        final CsvMapperCellHandlerFactory<T> csvMapperCellHandlerFactory = newCsvMapperCellHandlerFactory(parsingContextFactoryBuilder, instantiator, keys, delayedCellSetterFactories, setters);

        return new CsvMapperImpl<T>(csvMapperCellHandlerFactory,
                delayedCellSetterFactories,
                setters, getJoinKeys(), mapperConfig.rowHandlerErrorHandler());
	}

    private CsvMapperCellHandlerFactory<T> newCsvMapperCellHandlerFactory(ParsingContextFactoryBuilder parsingContextFactoryBuilder,
                                                                          Instantiator<CsvMapperCellHandler<T>, T> instantiator,
                                                                          CsvColumnKey[] keys,
                                                                          DelayedCellSetterFactory<T, ?>[] delayedCellSetterFactories,
                                                                          CellSetter<T>[] setters
    ) {

        final ParsingContextFactory parsingContextFactory = parsingContextFactoryBuilder.newFactory();
        if (isEligibleForAsmHandler()) {
            try {
                return reflectionService.getAsmFactory()
						.<T>createCsvMapperCellHandler(target, delayedCellSetterFactories, setters,
                        instantiator, keys, parsingContextFactory, mapperConfig.fieldMapperErrorHandler(),
								 mapperConfig.maxMethodSize());
            } catch (Exception e) {
                if (mapperConfig.failOnAsm()) {
                    return ErrorHelper.rethrow(e);
                } else {
                    return new CsvMapperCellHandlerFactory<T>(instantiator, keys, parsingContextFactory, mapperConfig.fieldMapperErrorHandler());
                }
            }
        } else {
            return new CsvMapperCellHandlerFactory<T>(instantiator, keys, parsingContextFactory, mapperConfig.fieldMapperErrorHandler());
        }
    }

    private boolean isEligibleForAsmHandler() {
        return reflectionService.hasAsmFactory()
                &&  this.propertyMappingsBuilder.size() < mapperConfig.asmMapperNbFieldsLimit();
    }


    private CsvColumnKey[] getKeys() {
		return propertyMappingsBuilder.getKeys().toArray(new CsvColumnKey[propertyMappingsBuilder.size()]);
	}

    private CsvColumnKey[] getJoinKeys() {
        final List<CsvColumnKey> keys = new ArrayList<CsvColumnKey>();

        propertyMappingsBuilder.forEachProperties(new ForEachCallBack<PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition>>() {
            @Override
            public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> pm) {
                if (pm.getColumnDefinition().isKey() && pm.getColumnDefinition().keyAppliesTo().test(pm.getPropertyMeta())) {
                    keys.add(pm.getColumnKey());
                }
            }
        });
        return keys.toArray(new CsvColumnKey[0]);
    }

	private  Instantiator<CsvMapperCellHandler<T>, T>  getInstantiator(Map<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>> params) throws MapperBuildingException {
		InstantiatorFactory instantiatorFactory = reflectionService.getInstantiatorFactory();

		try {
			return instantiatorFactory.getInstantiator(
					new TypeReference<CsvMapperCellHandler<T>>(){}.getType(),
					target,
					propertyMappingsBuilder,
					params,
					new GetterFactory<CsvMapperCellHandler<T>, CsvColumnKey>() {
						final CellSetterFactory cellSetterFactory = new CellSetterFactory(cellValueReaderFactory);

						@Override
						public <P> Getter<CsvMapperCellHandler<T>, P> newGetter(Type target, CsvColumnKey key, ColumnDefinition<?, ?> columnDefinition) {
							return cellSetterFactory.newDelayedGetter(key, target);
						}
					});
		} catch(Exception e) {
            return ErrorHelper.rethrow(e);
		}
	}

	private Tuple3<Map<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>>, Integer, Boolean> buildConstructorParametersDelayedCellSetter() {

        final BuildConstructorInjections buildConstructorInjections = new BuildConstructorInjections();
        propertyMappingsBuilder.forEachProperties(buildConstructorInjections);

		return new Tuple3<Map<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>>, Integer, Boolean>(buildConstructorInjections.constructorInjections,
                buildConstructorInjections.delayedSetterEnd, buildConstructorInjections.hasKeys);
	}


    @SuppressWarnings({ "unchecked" })
	private DelayedCellSetterFactory<T, ?>[] buildDelayedSetters(final ParsingContextFactoryBuilder parsingContextFactoryBuilder, int delayedSetterEnd, boolean hasKeys) {

		final Map<String, CsvMapperBuilder<?>> delegateMapperBuilders = new HashMap<String, CsvMapperBuilder<?>>();
        final Map<String, Integer> propertyToMapperIndex = new HashMap<String, Integer>();

		final DelayedCellSetterFactory<T, ?>[] delayedSetters = new DelayedCellSetterFactory[delayedSetterEnd];

        final int newMinDelayedSetter = minDelayedSetter != 0 ? minDelayedSetter : hasKeys ? delayedSetterEnd : 0;

		propertyMappingsBuilder.forEachProperties(new ForEachCallBack<PropertyMapping<T,?,CsvColumnKey, CsvColumnDefinition>>() {
			final CellSetterFactory cellSetterFactory = new CellSetterFactory(cellValueReaderFactory);

			@Override
			public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> propMapping) {
				if (propMapping != null) {
					PropertyMeta<T, ?> prop = propMapping.getPropertyMeta();
					CsvColumnKey key = propMapping.getColumnKey();
					if (prop != null) {
						if (prop.isSubProperty()) {
							addSubProperty(delegateMapperBuilders, prop, key, propMapping.getColumnDefinition());
						}else {
							delayedSetters[propMapping.getColumnKey().getIndex()] =  cellSetterFactory.getDelayedCellSetter(prop, key.getIndex(), propMapping.getColumnDefinition(), parsingContextFactoryBuilder);
						}
					}
				}
			}

			private <I, P> void addSubProperty(
					Map<String, CsvMapperBuilder<?>> delegateMapperBuilders,
					PropertyMeta<T, P> prop, CsvColumnKey key, CsvColumnDefinition columnDefinition) {
				SubPropertyMeta<T, I, P> subPropertyMeta = (SubPropertyMeta<T, I, P>)prop;

				final PropertyMeta<T, I> propOwner = subPropertyMeta.getOwnerProperty();
				CsvMapperBuilder<I> delegateMapperBuilder = (CsvMapperBuilder<I>) delegateMapperBuilders .get(propOwner.getName());

				if (delegateMapperBuilder == null) {
					delegateMapperBuilder = new CsvMapperBuilder<I>(propOwner.getPropertyType(), propOwner.getPropertyClassMeta(), newMinDelayedSetter, cellValueReaderFactory, mapperConfig);
					delegateMapperBuilders.put(propOwner.getName(), delegateMapperBuilder);
				}

                Integer currentIndex = propertyToMapperIndex.get(propOwner.getName());
                if (currentIndex == null || currentIndex < key.getIndex()) {
                    propertyToMapperIndex.put(propOwner.getName(), key.getIndex());
                }

				delegateMapperBuilder.addMapping(subPropertyMeta.getSubProperty(), key, columnDefinition);

			}
		}, 0, delayedSetterEnd);


		final Map<String, CsvMapper<?>> mappers = new HashMap<String, CsvMapper<?>>();


		propertyMappingsBuilder.forEachProperties(new ForEachCallBack<PropertyMapping<T,?,CsvColumnKey, CsvColumnDefinition>>() {
			@Override
			public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> propMapping) {
				if (propMapping == null) return;

				PropertyMeta<T, ?> prop = propMapping.getPropertyMeta();

				if (prop.isSubProperty()) {
					addSubPropertyDelayedSetter(delegateMapperBuilders,	delayedSetters, propMapping.getColumnKey().getIndex(), prop);
				}
			}

			private <I, P> void addSubPropertyDelayedSetter(
					Map<String, CsvMapperBuilder<?>> delegateMapperBuilders,
					DelayedCellSetterFactory<T, ?>[] delayedSetters,
					int setterIndex,
					PropertyMeta<T, P> prop) {
				PropertyMeta<T, I> subProp = ((SubPropertyMeta<T, I, P>) prop).getOwnerProperty();

				final String propName = subProp.getName();

				CsvMapper<I> mapper = (CsvMapper<I>) mappers.get(propName);

                if (mapper == null) {
                    CsvMapperBuilder<I> delegateMapperBuilder = (CsvMapperBuilder<I>) delegateMapperBuilders.get(propName);
                    mapper = delegateMapperBuilder.mapper();
                    mappers.put(propName, mapper);
                }

                int indexOfMapper = propertyToMapperIndex.get(propName);
                Setter<T, I> setter = null;

                if (!subProp.isConstructorProperty()) {
                    setter =  subProp.getSetter();
				}

                delayedSetters[setterIndex] = new DelegateMarkerDelayedCellSetterFactory<T, I>(mapper, setter, setterIndex, indexOfMapper);
            }
		}, 0, delayedSetterEnd);


		return delayedSetters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private CellSetter<T>[] getSetters(final ParsingContextFactoryBuilder parsingContextFactoryBuilder, final int delayedSetterEnd) {
		final Map<String, CsvMapperBuilder<?>> delegateMapperBuilders = new HashMap<String, CsvMapperBuilder<?>>();

        final Map<String, Integer> propertyToMapperIndex = new HashMap<String, Integer>();

        // calculate maxIndex
		int maxIndex = propertyMappingsBuilder.forEachProperties(new ForEachCallBack<PropertyMapping<T,?,CsvColumnKey, CsvColumnDefinition>>() {

			int maxIndex = delayedSetterEnd;
			@Override
			public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> propMapping) {
				if (propMapping != null) {
					maxIndex = Math.max(propMapping.getColumnKey().getIndex(), maxIndex);
					PropertyMeta<T, ?> prop = propMapping.getPropertyMeta();
					if (prop != null) {
						CsvColumnKey key = propMapping.getColumnKey();


						if (prop.isConstructorProperty()) {
							throw new IllegalStateException("Unexpected ConstructorPropertyMeta at " + key.getIndex());
						} else if (prop.isSubProperty()) {
							final PropertyMeta<?, ?> propOwner = ((SubPropertyMeta)prop).getOwnerProperty();
							CsvMapperBuilder<?> delegateMapperBuilder = delegateMapperBuilders .get(propOwner.getName());
							
							if (delegateMapperBuilder == null) {
								delegateMapperBuilder =
										new CsvMapperBuilder(propOwner.getPropertyType(), propOwner.getPropertyClassMeta(), minDelayedSetter, cellValueReaderFactory, mapperConfig);
								delegateMapperBuilders.put(propOwner.getName(), delegateMapperBuilder);
							}


                            Integer currentIndex = propertyToMapperIndex.get(propOwner.getName());
                            if (currentIndex == null || currentIndex < key.getIndex()) {
                                propertyToMapperIndex.put(propOwner.getName(), key.getIndex());
                            }

							delegateMapperBuilder.addMapping(((SubPropertyMeta) prop).getSubProperty(), key, propMapping.getColumnDefinition());
							
						}
					}
				}
			}
		}, delayedSetterEnd).maxIndex;


        // builder se setters
		final CellSetter<T>[] setters = new CellSetter[maxIndex + 1 - delayedSetterEnd];


		propertyMappingsBuilder.forEachProperties(new ForEachCallBack<PropertyMapping<T,?,CsvColumnKey, CsvColumnDefinition>>() {
			final Map<String, CsvMapperImpl<?>> mappers = new HashMap<String, CsvMapperImpl<?>>();
			final CellSetterFactory cellSetterFactory = new CellSetterFactory(cellValueReaderFactory);

			@Override
			public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> propMapping) {
				if (propMapping == null) {
					return;
				}
				
				PropertyMeta<T, ?> prop = propMapping.getPropertyMeta();

				if (prop == null || prop instanceof  DirectClassMeta.DirectPropertyMeta) {
					return;
				}

				if (prop instanceof SubPropertyMeta) {
                    DelegateMarkerSetter<T, ?> delegateMarkerSetter = getDelegateMarkerSetter((SubPropertyMeta) prop);

                    setters[propMapping.getColumnKey().getIndex()- delayedSetterEnd] = delegateMarkerSetter;
				} else {
					setters[propMapping.getColumnKey().getIndex()- delayedSetterEnd] = cellSetterFactory.getCellSetter(prop, propMapping.getColumnKey().getIndex(), propMapping.getColumnDefinition(), parsingContextFactoryBuilder);
				}
			}

            private <I, P> DelegateMarkerSetter<T, I> getDelegateMarkerSetter(SubPropertyMeta<T, I, P> prop) {
                final String propName = prop.getOwnerProperty().getName();

                CsvMapperImpl<I> mapper = (CsvMapperImpl<I>) mappers.get(propName);
                if (mapper == null) {
                    CsvMapperBuilder<I> delegateMapperBuilder = (CsvMapperBuilder<I>) delegateMapperBuilders.get(propName);
                    mapper = (CsvMapperImpl<I>) delegateMapperBuilder.mapper();
                    mappers.put(propName, mapper);
                }

                int indexOfMapper = propertyToMapperIndex.get(propName);

                return new DelegateMarkerSetter<T, I>(mapper, prop.getOwnerProperty().getSetter(), indexOfMapper);
            }
        }, delayedSetterEnd);
		
		
		return setters;
	}

	private class BuildConstructorInjections implements ForEachCallBack<PropertyMapping<T,?,CsvColumnKey, CsvColumnDefinition>> {
        final CellSetterFactory cellSetterFactory;
        private final Map<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>> constructorInjections;
        int delayedSetterEnd;
        boolean hasKeys;

        public BuildConstructorInjections() {
            this.constructorInjections = new HashMap<Parameter, Getter<? super CsvMapperCellHandler<T>, ?>>();
            cellSetterFactory = new CellSetterFactory(cellValueReaderFactory);
            delayedSetterEnd = minDelayedSetter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handle(PropertyMapping<T, ?, CsvColumnKey, CsvColumnDefinition> propMapping) {
            if (propMapping == null) return;

            PropertyMeta<T, ?> meta = propMapping.getPropertyMeta();

            hasKeys = hasKeys || propMapping.getColumnDefinition().isKey();

            if (meta == null) return;
            final CsvColumnKey key = propMapping.getColumnKey();
            if (meta.isConstructorProperty()) {
                delayedSetterEnd = Math.max(delayedSetterEnd, key.getIndex() + 1);
                Getter<CsvMapperCellHandler<T>, ?> delayedGetter = cellSetterFactory.newDelayedGetter(key, meta.getPropertyType());
                constructorInjections.put(((ConstructorPropertyMeta<T, ?>) meta).getParameter(), delayedGetter);
            } else if (meta instanceof DirectClassMeta.DirectPropertyMeta) {
                delayedSetterEnd = Math.max(delayedSetterEnd, key.getIndex() + 1);
            } else if (meta.isSubProperty()) {
                SubPropertyMeta<T, ?, ?> subMeta = (SubPropertyMeta<T, ?, ?>) meta;
                if (subMeta.getOwnerProperty().isConstructorProperty()) {
                    ConstructorPropertyMeta<?, ?> constPropMeta = (ConstructorPropertyMeta<?, ?>) subMeta.getOwnerProperty();
                    Getter<CsvMapperCellHandler<T>, ?> delayedGetter = cellSetterFactory.newDelayedGetter(key, constPropMeta.getPropertyType());
                    constructorInjections.put(constPropMeta.getParameter(), delayedGetter);
                    delayedSetterEnd = Math.max(delayedSetterEnd, key.getIndex() + 1);
                } else if (
						(propMapping.getColumnDefinition().isKey()
						&& propMapping.getColumnDefinition().keyAppliesTo().test(propMapping.getPropertyMeta()))
						|| subMeta.getSubProperty() instanceof DirectClassMeta.DirectPropertyMeta) {
                    delayedSetterEnd = Math.max(delayedSetterEnd, key.getIndex() + 1);
                }
            } else if (propMapping.getColumnDefinition().isKey() && propMapping.getColumnDefinition().keyAppliesTo().test(propMapping.getPropertyMeta())) {
                delayedSetterEnd = Math.max(delayedSetterEnd, key.getIndex() + 1);
            }
        }
    }
}