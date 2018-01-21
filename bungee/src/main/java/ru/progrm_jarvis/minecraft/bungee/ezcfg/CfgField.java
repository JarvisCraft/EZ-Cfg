package ru.progrm_jarvis.minecraft.bungee.ezcfg;

import lombok.val;
import net.md_5.bungee.config.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CfgField {
    /**
     * The path by which the setting should be stored in file.
     * Is be used in {@link net.md_5.bungee.config.Configuration#get(String)}-like methods.
     *
     * @return the key by which to store the value
     */
    String value() default "";

    /**
     * Type of the stored value. By default it's automatically taken from the variable to store data in.
     *
     * @return the type of the value
     */
    Type type() default Type.AUTO;

    /**
     * The comment to be added before the field to explain it's meaning.
     *
     * @return the comment before the field
     */
    String[] comment() default {};

    @SuppressWarnings("unused")
            // Because enums can be taken automatically
    enum Type {
        AUTO(null),
        // Base types
        BOOLEAN(new ConfigDataBoolean(), boolean.class, Boolean.class),
        BYTE(new ConfigDataByte(), byte.class, Byte.class),
        SHORT(new ConfigDataShort(), short.class, Short.class),
        INT(new ConfigDataInt(), int.class, Integer.class),
        LONG(new ConfigDataLong(), long.class, Long.class),
        FLOAT(new ConfigDataFloat(), float.class, Float.class),
        DOUBLE(new ConfigDataDouble(), double.class, Double.class),
        CHAR(new ConfigDataChar(), char.class, Character.class),
        STRING(new ConfigDataString(), String.class),
        // Collections
        LIST(new ConfigDataList(), true),
        BOOLEAN_LIST(new ConfigDataListBoolean(), true, boolean.class, Boolean.class),
        BYTE_LIST(new ConfigDataListByte(), true, byte.class, Byte.class),
        SHORT_LIST(new ConfigDataListShort(), true, short.class, Short.class),
        INT_LIST(new ConfigDataListInt(), true, int.class, Integer.class),
        LONG_LIST(new ConfigDataListLong(), true, long.class, Long.class),
        FLOAT_LIST(new ConfigDataListFloat(), true, float.class, Float.class),
        DOUBLE_LIST(new ConfigDataListDouble(), true, double.class, Double.class),
        CHAR_LIST(new ConfigDataListChar(), true, char.class, Character.class),
        STRING_LIST(new ConfigDataListString(), true, String.class);

        /**
         * Method to get the value of the field
         */
        private final ConfigData dataType;

        public ConfigData getDataType() {
            return dataType;
        }

        private final Class<?>[] typeClasses;

        public Class<?>[] getTypeClasses() {
            return typeClasses;
        }

        private final boolean list;

        public boolean isList() {
            return list;
        }

        Type(final ConfigData dataType, Class... typeClasses) {
            this(dataType, false, typeClasses);
        }

        Type(final ConfigData dataType, boolean list, Class... typeClasses) {
            this.dataType = dataType;
            this.list = list;
            this.typeClasses = typeClasses;
        }

        @SuppressWarnings("Duplicates")
        public static Type getType(final Field field) {

            val isList = List.class.isAssignableFrom(field.getType());

            if (isList) {
                // If is list
                val listTypeClass = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];

                for (val type : values()) {
                    if (!type.isList()) continue;

                    for (val typeClass : type.typeClasses)
                        if (typeClass
                                .isAssignableFrom(listTypeClass)) return type;
                }
            } else {
                // If is not list
                for (val type : values()) {
                    if (type.isList()) continue;

                    for (val typeClass : type.typeClasses)
                        if (typeClass
                                .isAssignableFrom(field.getType())) return type;
                }
            }

            return null;
        }

        /**
         * Abstract Wrapper for all dataType required to work with various config data types.
         *
         * @param <T> data type
         */
        @SuppressWarnings("unused")
        public abstract static class ConfigData<T> {

            public void set(final Configuration configuration, final String path, final T value) {
                configuration.set(path, value);
            }

            public abstract T get(Configuration configuration, String path);

            public T get(final Configuration configuration, final String path, final T def) {
                val value = get(configuration, path);
                return value == null ? def : value;
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Base types
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataBoolean extends ConfigData<Boolean> {
            @Override
            public Boolean get(final Configuration configuration, final String path) {
                return configuration.getBoolean(path);
            }

            @Override
            public Boolean get(final Configuration configuration, final String path, final Boolean def) {
                val value = configuration.get(path, def);
                if (value != null) return value;
                else return def;
            }
        }

        private static class ConfigDataByte extends ConfigData<Byte> {
            @Override
            public Byte get(final Configuration configuration, final String path) {
                return (byte) configuration.getInt(path);
            }

            @Override
            public Byte get(final Configuration configuration, final String path, final Byte def) {
                val value = configuration.get(path);
                if (value instanceof Number) return ((Number) value).byteValue();
                else return def;
            }
        }

        private static class ConfigDataShort extends ConfigData<Short> {
            @Override
            public Short get(final Configuration configuration, final String path) {
                return (short) configuration.getInt(path);
            }

            @Override
            public Short get(final Configuration configuration, final String path, final Short def) {
                val value = configuration.get(path);
                if (value instanceof Number) return ((Number) value).shortValue();
                else return def;
            }
        }

        private static class ConfigDataInt extends ConfigData<Integer> {
            @Override
            public Integer get(final Configuration configuration, final String path) {
                return configuration.getInt(path);
            }

            @Override
            public Integer get(final Configuration configuration, final String path, final Integer def) {
                val value = configuration.get(path);
                if (value instanceof Number) return ((Number) value).intValue();
                else return def;
            }
        }

        private static class ConfigDataLong extends ConfigData<Long> {
            @Override
            public Long get(final Configuration configuration, final String path) {
                return configuration.getLong(path);
            }

            @Override
            public Long get(final Configuration configuration, final String path, final Long def) {
                val value = configuration.get(path, def);
                if (value != null) return value;
                else return def;
            }
        }

        private static class ConfigDataFloat extends ConfigData<Float> {
            @Override
            public Float get(final Configuration configuration, final String path) {
                return (float) configuration.getDouble(path);
            }

            @Override
            public Float get(final Configuration configuration, final String path, final Float def) {
                val value = configuration.get(path);
                if (value instanceof Number) return ((Number) value).floatValue();
                else return def;
            }
        }

        private static class ConfigDataDouble extends ConfigData<Double> {
            @Override
            public Double get(final Configuration configuration, final String path) {
                return configuration.getDouble(path);
            }

            @Override
            public Double get(final Configuration configuration, final String path, final Double def) {
                val value = configuration.get(path);
                if (value instanceof Number) return ((Number) value).doubleValue();
                else return def;
            }
        }

        private static class ConfigDataChar extends ConfigData<Character> {
            @Override
            public Character get(final Configuration configuration, final String path) {
                return configuration.getString(path).charAt(0);
            }

            @Override
            public Character get(final Configuration configuration, final String path, final Character def) {
                val value = configuration.getString(path);
                return value == null || value.isEmpty() ? def : value.charAt(0);
            }
        }

        private static class ConfigDataString extends ConfigData<String> {
            @Override
            public String get(final Configuration configuration, final String path) {
                return configuration.getString(path);
            }

            @Override
            public String get(final Configuration configuration, final String path, final String def) {
                return configuration.getString(path, def);
            }
        }

        private abstract static class AbstractConfigDataList<T> extends ConfigData<List<T>> {
        }

        ///////////////////////////////////////////////////////////////////////////
        // Lists
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataList extends AbstractConfigDataList<Object> {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(final Configuration configuration, final String path) {
                return (List<Object>) configuration.getList(path);
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(Configuration configuration, String path, List<Object> def) {
                return configuration.getList(path) == null ? def : (List<Object>) configuration.getList(path);
            }
        }

        private static class ConfigDataListBoolean extends AbstractConfigDataList<Boolean> {
            @Override
            public List<Boolean> get(final Configuration configuration, final String path) {
                return configuration.getBooleanList(path);
            }

            @Override
            public List<Boolean> get(Configuration configuration, String path, List<Boolean> def) {
                return configuration.getList(path) == null ? def : configuration.getBooleanList(path);
            }
        }

        private static class ConfigDataListByte extends AbstractConfigDataList<Byte> {
            @Override
            public List<Byte> get(final Configuration configuration, final String path) {
                return configuration.getByteList(path);
            }

            @Override
            public List<Byte> get(Configuration configuration, String path, List<Byte> def) {
                return configuration.getList(path) == null ? def : configuration.getByteList(path);
            }
        }

        private static class ConfigDataListShort extends AbstractConfigDataList<Short> {
            @Override
            public List<Short> get(final Configuration configuration, final String path) {
                return configuration.getShortList(path);
            }

            @Override
            public List<Short> get(Configuration configuration, String path, List<Short> def) {
                return configuration.getList(path) == null ? def : configuration.getShortList(path);
            }
        }

        private static class ConfigDataListInt extends AbstractConfigDataList<Integer> {
            @Override
            public List<Integer> get(final Configuration configuration, final String path) {
                return configuration.getIntList(path);
            }

            @Override
            public List<Integer> get(Configuration configuration, String path, List<Integer> def) {
                return configuration.getList(path) == null ? def : configuration.getIntList(path);
            }
        }

        private static class ConfigDataListLong extends AbstractConfigDataList<Long> {
            @Override
            public List<Long> get(final Configuration configuration, final String path) {
                return configuration.getLongList(path);
            }

            @Override
            public List<Long> get(Configuration configuration, String path, List<Long> def) {
                return configuration.getList(path) == null ? def : configuration.getLongList(path);
            }
        }

        private static class ConfigDataListFloat extends AbstractConfigDataList<Float> {
            @Override
            public List<Float> get(final Configuration configuration, final String path) {
                return configuration.getFloatList(path);
            }

            @Override
            public List<Float> get(Configuration configuration, String path, List<Float> def) {
                return configuration.getList(path) == null ? def : configuration.getFloatList(path);
            }
        }

        private static class ConfigDataListDouble extends AbstractConfigDataList<Double> {
            @Override
            public List<Double> get(final Configuration configuration, final String path) {
                return configuration.getDoubleList(path);
            }

            @Override
            public List<Double> get(Configuration configuration, String path, List<Double> def) {
                return configuration.getList(path) == null ? def : configuration.getDoubleList(path);
            }
        }

        private static class ConfigDataListChar extends AbstractConfigDataList<Character> {
            @Override
            public List<Character> get(final Configuration configuration, final String path) {
                return configuration.getCharList(path);
            }

            @Override
            public List<Character> get(Configuration configuration, String path, List<Character> def) {
                return configuration.getList(path) == null ? def : configuration.getCharList(path);
            }
        }

        private static class ConfigDataListString extends AbstractConfigDataList<String> {
            @Override
            public List<String> get(final Configuration configuration, final String path) {
                return configuration.getStringList(path);
            }

            @Override
            public List<String> get(Configuration configuration, String path, List<String> def) {
                return configuration.getList(path) == null ? def : configuration.getStringList(path);
            }
        }
    }
}
