package org.sfm.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.sfm.map.FieldMapper;
import org.sfm.map.column.FieldMapperColumnDefinition;
import org.sfm.map.mapper.PropertyMapping;
import org.sfm.reflect.Getter;
import org.sfm.reflect.impl.*;
import org.sfm.reflect.meta.PropertyMeta;

import java.sql.PreparedStatement;
import java.sql.Types;

import static org.mockito.Mockito.*;

public class PreparedStatementFieldMapperFactoryTest {

    private PreparedStatementFieldMapperFactory factory;

    private PreparedStatement ps;

    private int index;
    @Before
    public void setUp() {
        factory = PreparedStatementFieldMapperFactory.instance();
        ps = mock(PreparedStatement.class);
        index = 1;
    }

    @Test
    public void testMapBoolean() throws Exception {
        newFieldMapperAndMapToPS(new ConstantBooleanGetter<Object>(true), boolean.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Boolean>(false), Boolean.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Boolean>(), Boolean.class);

        verify(ps).setBoolean(1, true);
        verify(ps).setBoolean(2, false);
        verify(ps).setNull(3, Types.BOOLEAN);
    }

    @Test
    public void testMapByte() throws Exception {
        newFieldMapperAndMapToPS(new ConstantByteGetter<Object>((byte)2), byte.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Byte>((byte) 3), Byte.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Byte>(), Byte.class);

        verify(ps).setByte(1, (byte) 2);
        verify(ps).setByte(2, (byte) 3);
        verify(ps).setNull(3, Types.TINYINT);
    }

    @Test
    public void testMapChar() throws Exception {
        newFieldMapperAndMapToPS(new ConstantCharacterGetter<Object>((char)2), char.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Character>((char) 3), Character.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Character>(), Character.class);

        verify(ps).setInt(1, 2);
        verify(ps).setInt(2, 3);
        verify(ps).setNull(3, Types.INTEGER);
    }

    @Test
    public void testMapShort() throws Exception {
        newFieldMapperAndMapToPS(new ConstantShortGetter<Object>((short)2), short.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Short>((short) 3), Short.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Short>(), Short.class);

        verify(ps).setShort(1, (short) 2);
        verify(ps).setShort(2, (short) 3);
        verify(ps).setNull(3, Types.SMALLINT);
    }

    @Test
    public void testMapInt() throws Exception {
        newFieldMapperAndMapToPS(new ConstantIntGetter<Object>(2), int.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Integer>(3), Integer.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Integer>(), Integer.class);

        verify(ps).setInt(1, 2);
        verify(ps).setInt(2, 3);
        verify(ps).setNull(3, Types.INTEGER);
    }

    @Test
    public void testMapLong() throws Exception {
        newFieldMapperAndMapToPS(new ConstantLongGetter<Object>((long)2), long.class);
        newFieldMapperAndMapToPS(new ConstantGetter<Object, Long>((long) 3), Long.class);
        newFieldMapperAndMapToPS(new NullGetter<Object, Long>(), Long.class);

        verify(ps).setLong(1, (long) 2);
        verify(ps).setLong(2, (long) 3);
        verify(ps).setNull(3, Types.BIGINT);
    }

    @Test
    public void testMapFloat() {

    }

    @Test
    public void testMapDouble() {

    }

    @Test
    public void testMapDate() {

    }

    @Test
    public void testMapCalendar() {

    }

    @Test
    public void testMapString() {

    }


    @Test
    public void testMapUUID() {

    }

    @Test
    public void testMapURL() {

    }



    protected <T, P> void newFieldMapperAndMapToPS(Getter<T, P> getter, Class<P> clazz) throws Exception {
        FieldMapper<T, PreparedStatement> fieldMapper = factory.newFieldMapperToSource(newPropertyMapping(getter, clazz), null);
        fieldMapper.mapTo(null, ps, null);
    }

    @SuppressWarnings("unchecked")
    private <T, P> PropertyMapping<T, P, JdbcColumnKey, FieldMapperColumnDefinition<JdbcColumnKey>> newPropertyMapping(Getter<T, P> getter, Class<P> clazz) {
        PropertyMeta<T, P> propertyMeta = mock(PropertyMeta.class);
        when(propertyMeta.getGetter()).thenReturn(getter);
        when(propertyMeta.getPropertyType()).thenReturn(clazz);
        return
                new PropertyMapping<T, P, JdbcColumnKey, FieldMapperColumnDefinition<JdbcColumnKey>>(
                        propertyMeta,
                        new JdbcColumnKey("col", index++),
                        FieldMapperColumnDefinition.<JdbcColumnKey>identity());
    }






}