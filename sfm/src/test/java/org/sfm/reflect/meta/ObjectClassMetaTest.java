package org.sfm.reflect.meta;

import org.junit.Test;
import org.sfm.beans.DbFinalObject;
import org.sfm.beans.DbObject;
import org.sfm.reflect.ReflectionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjectClassMetaTest {


    @Test
    public void testGenerateHeaders() {
        String[] names = {"id", "name", "email", "creation_time", "type_ordinal", "type_name"};
        assertArrayEquals(
                names,
                ReflectionService.newInstance().getClassMeta(DbFinalObject.class).generateHeaders());
        assertArrayEquals(
                names,
                ReflectionService.newInstance().getClassMeta(DbObject.class).generateHeaders());
    }

    @Test
    public void testGetterOnly() {
        ClassMeta<Object> classMeta = ReflectionService.newInstance().getClassMeta(GetterOnly.class);

        assertNotNull(classMeta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("string")));

    }

    @Test
    public void testFieldWithImcompatibleGetterType() throws Exception {
        IncompatibleGetter target = new IncompatibleGetter();
        target.value = "aa";

        ClassMeta<IncompatibleGetter> meta = ReflectionService.newInstance().getClassMeta(IncompatibleGetter.class);

        PropertyMeta<IncompatibleGetter, Object> pm = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value"));

        assertEquals("aa", pm.getGetter().get(target));
    }


    @Test
    public void testGetterBetterThanName() throws Exception {
        GetterBetterThanName target = new GetterBetterThanName();

        ClassMeta<GetterBetterThanName> meta = ReflectionService.newInstance().getClassMeta(GetterBetterThanName.class);

        PropertyMeta<GetterBetterThanName, Object> pm = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value"));

        assertEquals("getValue", pm.getGetter().get(target));
    }
    @Test
    public void testGetterSetterWithoutPrefix() throws Exception {
        UnprefixedBean target = new UnprefixedBean();

        ClassMeta<UnprefixedBean> meta =
                ReflectionService.newInstance()
                        .getClassMeta(UnprefixedBean.class);

        PropertyMeta<UnprefixedBean, Object> pm = meta
                .newPropertyFinder()
                .findProperty(DefaultPropertyNameMatcher.of("value"));

        assertNotNull(pm);
        pm.getSetter().set(target, "aa");
        assertEquals("aa", pm.getGetter().get(target));
    }

    @Test
    public void testFieldWithCompatibleGetterType() throws Exception {
        CompatibleGetter target = new CompatibleGetter();

        ClassMeta<CompatibleGetter> meta = ReflectionService.newInstance().getClassMeta(CompatibleGetter.class);

        PropertyMeta<CompatibleGetter, Object> pm = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value"));

        assertEquals(Arrays.asList("aa"), pm.getGetter().get(target));

        PropertyMeta<CompatibleGetter, Object> pm2 = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value2"));

        assertEquals(2, pm2.getGetter().get(target));

    }

    @Test
    public void testFieldWithCompatibleSetterType() throws Exception {
        CompatibleGetter target = new CompatibleGetter();

        ClassMeta<CompatibleGetter> meta = ReflectionService.newInstance().getClassMeta(CompatibleGetter.class);

        PropertyMeta<CompatibleGetter, Object> pm = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value"));


        pm.getSetter().set(target, null);

        assertEquals(Arrays.asList("bb"), target.value);

        PropertyMeta<CompatibleGetter, Object> pm2 = meta.newPropertyFinder().findProperty(DefaultPropertyNameMatcher.of("value2"));

        pm2.getSetter().set(target, 2);
        assertEquals(3, target.value2);

    }

    public static class GetterBetterThanName {
        public String getValue() {
            return "getValue";
        }
        public String value() {
            return "value";
        }
    }

    public static class IncompatibleGetter {
        private String value;

        public int getValue() {
            return 1;
        }
    }

    public static class CompatibleGetter {
        private List<String> value;
        private Number value2;


        public List getValue() {
            return Arrays.asList("aa");
        }

        public void setValue(ArrayList list) {
            this.value = Arrays.asList("bb");
        }

        public Integer getValue2() {
            return 2;
        }

        public void setValue2(int o) {
            value2 = 3;
        }
    }

    public static class GetterOnly {
        public String getString() {
            return "value";
        }

        public int intValue() {
            return 3;
        }
    }
    public static class MyClass{
        private String id;

        private DbObject o;

        public MyClass(String id) {
            this.id = id;
        }
        public MyClass() {

        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public DbObject getO() {
            return o;
        }

        public void setO(DbObject o) {
            this.o = o;
        }

    }
    @Test
    public void testGenerateHeadersWithConstructorAndSetterProperty() {
        String[] names = {"id", "o_id", "o_name", "o_email", "o_creation_time", "o_type_ordinal", "o_type_name"};
        assertArrayEquals(
                names,
                ReflectionService.newInstance().getClassMeta(MyClass.class).generateHeaders());
    }


    private class UnprefixedBean {
        private String alt;
        private int elt;
        public void value(String value) {
            this.alt = value;
        }
        public String value() {
            return alt;
        }
    }
}
