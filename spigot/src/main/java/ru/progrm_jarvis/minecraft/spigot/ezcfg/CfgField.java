package ru.progrm_jarvis.minecraft.spigot.ezcfg;

import lombok.val;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import static org.bukkit.util.NumberConversions.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CfgField {
    /**
     * The path by which the setting should be stored in file.
     * Is be used in {@link FileConfiguration#get(String)}-like methods.
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
        STRING_LIST(new ConfigDataListString(), true, String.class),
        MAP_LIST(new ConfigDataListMap(), true, Map.class),
        // Special types
        VECTOR(new ConfigDataVector(), Vector.class),
        OFFLINE_PLAYER(new ConfigDataOfflinePlayer(), OfflinePlayer.class),
        ITEM_STACK(new ConfigDataItemStack(), ItemStack.class),
        COLOR(new ConfigDataColor(), Color.class);

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
                val typeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                for (val type : values()) {
                    if (!type.isList()) continue;

                    if (typeArgument instanceof Class) for (val typeClass : type.typeClasses) if (typeClass
                            .isAssignableFrom((Class<?>) typeArgument)) return type;

                    return LIST;
                }
            } else {
                // If is not list
                for (val type : values()) {
                    if (type.isList()) continue;

                    for (val typeClass : type.typeClasses) if (typeClass.isAssignableFrom(field.getType())) return type;
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

            public void set(final FileConfiguration configuration, final String path, final T value) {
                configuration.set(path, value);
            }

            public boolean isSet(final FileConfiguration configuration, final String path) {
                return configuration.isSet(path);
            }

            public abstract T get(FileConfiguration configuration, String path);

            public T get(final FileConfiguration configuration, final String path, final T def) {
                val value = get(configuration, path);
                return value == null ? def : value;
            }

            public abstract boolean isValid(FileConfiguration configuration, String path);
        }

        ///////////////////////////////////////////////////////////////////////////
        // Base types
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataBoolean extends ConfigData<Boolean> {
            @Override
            public Boolean get(final FileConfiguration configuration, final String path) {
                return configuration.getBoolean(path);
            }

            @Override
            public Boolean get(final FileConfiguration configuration, final String path, final Boolean def) {
                val value = configuration.get(path, def);
                if (value instanceof Boolean) return (Boolean) value;
                else return def;
            }


            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isBoolean(path);
            }
        }

        private static class ConfigDataByte extends ConfigData<Byte> {
            @Override
            public Byte get(final FileConfiguration configuration, final String path) {
                return (byte) configuration.getInt(path);
            }

            @Override
            public Byte get(final FileConfiguration configuration, final String path, final Byte def) {
                val value = configuration.get(path);
                if (value instanceof Number) return (byte) toInt(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isInt(path);
            }
        }

        private static class ConfigDataShort extends ConfigData<Short> {
            @Override
            public Short get(final FileConfiguration configuration, final String path) {
                return (short) configuration.getInt(path);
            }

            @Override
            public Short get(final FileConfiguration configuration, final String path, final Short def) {
                val value = configuration.get(path);
                if (value instanceof Number) return (short) toInt(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isInt(path);
            }
        }

        private static class ConfigDataInt extends ConfigData<Integer> {
            @Override
            public Integer get(final FileConfiguration configuration, final String path) {
                return configuration.getInt(path);
            }

            @Override
            public Integer get(final FileConfiguration configuration, final String path, final Integer def) {
                val value = configuration.get(path);
                if (value instanceof Number) return toInt(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isInt(path);
            }
        }

        private static class ConfigDataLong extends ConfigData<Long> {
            @Override
            public Long get(final FileConfiguration configuration, final String path) {
                return configuration.getLong(path);
            }

            @Override
            public Long get(final FileConfiguration configuration, final String path, final Long def) {
                val value = configuration.get(path, def);
                if (value instanceof Number) return toLong(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isLong(path);
            }
        }

        private static class ConfigDataFloat extends ConfigData<Float> {
            @Override
            public Float get(final FileConfiguration configuration, final String path) {
                return (float) configuration.getDouble(path);
            }

            @Override
            public Float get(final FileConfiguration configuration, final String path, final Float def) {
                val value = configuration.get(path);
                if (value instanceof Number) return (float) toDouble(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isDouble(path);
            }
        }

        private static class ConfigDataDouble extends ConfigData<Double> {
            @Override
            public Double get(final FileConfiguration configuration, final String path) {
                return configuration.getDouble(path);
            }

            @Override
            public Double get(final FileConfiguration configuration, final String path, final Double def) {
                val value = configuration.get(path);
                if (value instanceof Number) return toDouble(value);
                else return def;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isDouble(path);
            }
        }

        private static class ConfigDataChar extends ConfigData<Character> {
            @Override
            public Character get(final FileConfiguration configuration, final String path) {
                return configuration.getString(path).charAt(0);
            }

            @Override
            public Character get(final FileConfiguration configuration, final String path, final Character def) {
                val value = configuration.getString(path);
                return value == null || value.isEmpty() ? def : value.charAt(0);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isInt(path);
            }
        }

        private static class ConfigDataString extends ConfigData<String> {
            @Override
            public String get(final FileConfiguration configuration, final String path) {
                return configuration.getString(path);
            }

            @Override
            public String get(final FileConfiguration configuration, final String path, final String def) {
                return configuration.getString(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isString(path);
            }
        }

        private abstract static class AbstractConfigDataList<T> extends ConfigData<List<T>> {
            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isList(path);
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Lists
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataList extends AbstractConfigDataList<Object> {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(final FileConfiguration configuration, final String path) {
                return (List<Object>) configuration.getList(path);
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(FileConfiguration configuration, String path, List<Object> def) {
                return configuration.getList(path) == null ? def : (List<Object>) configuration.getList(path);
            }
        }

        private static class ConfigDataListBoolean extends AbstractConfigDataList<Boolean> {
            @Override
            public List<Boolean> get(final FileConfiguration configuration, final String path) {
                return configuration.getBooleanList(path);
            }

            @Override
            public List<Boolean> get(FileConfiguration configuration, String path, List<Boolean> def) {
                return configuration.getList(path) == null ? def : configuration.getBooleanList(path);
            }
        }

        private static class ConfigDataListByte extends AbstractConfigDataList<Byte> {
            @Override
            public List<Byte> get(final FileConfiguration configuration, final String path) {
                return configuration.getByteList(path);
            }

            @Override
            public List<Byte> get(FileConfiguration configuration, String path, List<Byte> def) {
                return configuration.getList(path) == null ? def : configuration.getByteList(path);
            }
        }

        private static class ConfigDataListShort extends AbstractConfigDataList<Short> {
            @Override
            public List<Short> get(final FileConfiguration configuration, final String path) {
                return configuration.getShortList(path);
            }

            @Override
            public List<Short> get(FileConfiguration configuration, String path, List<Short> def) {
                return configuration.getList(path) == null ? def : configuration.getShortList(path);
            }
        }

        private static class ConfigDataListInt extends AbstractConfigDataList<Integer> {
            @Override
            public List<Integer> get(final FileConfiguration configuration, final String path) {
                return configuration.getIntegerList(path);
            }

            @Override
            public List<Integer> get(FileConfiguration configuration, String path, List<Integer> def) {
                return configuration.getList(path) == null ? def : configuration.getIntegerList(path);
            }
        }

        private static class ConfigDataListLong extends AbstractConfigDataList<Long> {
            @Override
            public List<Long> get(final FileConfiguration configuration, final String path) {
                return configuration.getLongList(path);
            }

            @Override
            public List<Long> get(FileConfiguration configuration, String path, List<Long> def) {
                return configuration.getList(path) == null ? def : configuration.getLongList(path);
            }
        }

        private static class ConfigDataListFloat extends AbstractConfigDataList<Float> {
            @Override
            public List<Float> get(final FileConfiguration configuration, final String path) {
                return configuration.getFloatList(path);
            }

            @Override
            public List<Float> get(FileConfiguration configuration, String path, List<Float> def) {
                return configuration.getList(path) == null ? def : configuration.getFloatList(path);
            }
        }

        private static class ConfigDataListDouble extends AbstractConfigDataList<Double> {
            @Override
            public List<Double> get(final FileConfiguration configuration, final String path) {
                return configuration.getDoubleList(path);
            }

            @Override
            public List<Double> get(FileConfiguration configuration, String path, List<Double> def) {
                return configuration.getList(path) == null ? def : configuration.getDoubleList(path);
            }
        }

        private static class ConfigDataListChar extends AbstractConfigDataList<Character> {
            @Override
            public List<Character> get(final FileConfiguration configuration, final String path) {
                return configuration.getCharacterList(path);
            }

            @Override
            public List<Character> get(FileConfiguration configuration, String path, List<Character> def) {
                return configuration.getList(path) == null ? def : configuration.getCharacterList(path);
            }
        }

        private static class ConfigDataListString extends AbstractConfigDataList<String> {
            @Override
            public List<String> get(final FileConfiguration configuration, final String path) {
                return configuration.getStringList(path);
            }

            @Override
            public List<String> get(FileConfiguration configuration, String path, List<String> def) {
                return configuration.getList(path) == null ? def : configuration.getStringList(path);
            }
        }

        private static class ConfigDataListMap extends AbstractConfigDataList<Map<?, ?>> {
            @Override
            public List<Map<?, ?>> get(final FileConfiguration configuration, final String path) {
                return configuration.getMapList(path);
            }

            @Override
            public List<Map<?, ?>> get(FileConfiguration configuration, String path, List<Map<?, ?>> def) {
                return configuration.getList(path) == null ? def : configuration.getMapList(path);
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Special types
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataVector extends ConfigData<Vector> {
            @Override
            public Vector get(final FileConfiguration configuration, final String path) {
                return configuration.getVector(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isVector(path);
            }

            @Override
            public Vector get(final FileConfiguration configuration, final String path, final Vector def) {
                return configuration.getVector(path, def);
            }
        }

        private static class ConfigDataOfflinePlayer extends ConfigData<OfflinePlayer> {
            @Override
            public OfflinePlayer get(final FileConfiguration configuration, final String path) {
                return configuration.getOfflinePlayer(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isOfflinePlayer(path);
            }

            @Override
            public OfflinePlayer get(final FileConfiguration configuration, final String path, final OfflinePlayer def) {
                return configuration.getOfflinePlayer(path, def);
            }
        }

        private static class ConfigDataItemStack extends ConfigData<ItemStack> {
            @Override
            public ItemStack get(final FileConfiguration configuration, final String path) {
                return configuration.getItemStack(path);
            }

            @Override
            public ItemStack get(final FileConfiguration configuration, final String path, final ItemStack def) {
                return configuration.getItemStack(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isItemStack(path);
            }
        }

        private static class ConfigDataColor extends ConfigData<Color> {
            @Override
            public Color get(final FileConfiguration configuration, final String path) {
                return configuration.getColor(path);
            }

            @Override
            public Color get(final FileConfiguration configuration, final String path, final Color def) {
                return configuration.getColor(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isColor(path);
            }
        }

        private static class ConfigDataOther extends ConfigData {
            @Override
            public Object get(final FileConfiguration configuration, final String path) {
                return configuration.get(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isSet(path);
            }
        }
    }
}
