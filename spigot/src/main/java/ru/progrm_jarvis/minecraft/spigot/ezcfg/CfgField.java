package ru.progrm_jarvis.minecraft.spigot.ezcfg;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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

    @Getter
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
        ENUM(new ConfigDataEnum(), Enum.class),
        MAP(new ConfigDataMap(), Map.class),
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
        COLOR(new ConfigDataColor(), Color.class),
        OBJECT(new ConfigDataObject());

        private final ConfigData dataType;

        private final Class<?>[] typeClasses;

        private final boolean list;

        Type(final ConfigData dataType, Class... typeClasses) {
            this(dataType, false, typeClasses);
        }

        Type(final ConfigData dataType, boolean list, Class... typeClasses) {
            this.dataType = dataType;
            this.list = list;
            this.typeClasses = typeClasses;
        }

        @SuppressWarnings("Duplicates")
        @NonNull public static Type getType(final Field field) {

            val isList = List.class.isAssignableFrom(field.getType());

            if (isList) {
                val fieldGenericType = field.getGenericType();
                if (fieldGenericType instanceof ParameterizedType) {
                    val typeArgument = ((ParameterizedType) fieldGenericType).getActualTypeArguments()[0];

                    for (val type : values()) {
                        if (!type.isList()) continue;

                        if (typeArgument instanceof Class) for (val typeClass : type.typeClasses) if (typeClass
                                .isAssignableFrom((Class<?>) typeArgument)) return type;

                        return LIST;
                    }
                } else return OBJECT;
            } else {
                // If is not list
                for (val type : values()) {
                    if (type.isList()) continue;

                    for (val typeClass : type.typeClasses) if (typeClass.isAssignableFrom(field.getType())) return type;
                }
            }

            return OBJECT;
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

            @SuppressWarnings("unchecked")
            public T get(final FileConfiguration configuration, final Class<T> type, final String path) {
                return (T) configuration.get(path);
            }

            public T get(final FileConfiguration configuration, final Class<T> type, final T def, final String path) {
                val value = get(configuration, type, path);
                return value == null ? def : value;
            }

            public abstract boolean isValid(FileConfiguration configuration, String path);
        }

        ///////////////////////////////////////////////////////////////////////////
        // Base types
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataBoolean extends ConfigData<Boolean> {
            @Override
            public Boolean get(final FileConfiguration configuration, final Class<Boolean> type, final String path) {
                return configuration.getBoolean(path);
            }

            @Override
            public Boolean get(final FileConfiguration configuration, final Class<Boolean> type, final Boolean def, final String path) {
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
            public Byte get(final FileConfiguration configuration, final Class<Byte> type, final String path) {
                return (byte) configuration.getInt(path);
            }

            @Override
            public Byte get(final FileConfiguration configuration, final Class<Byte> type, final Byte def, final String path) {
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
            public Short get(final FileConfiguration configuration, final Class<Short> type, final String path) {
                return (short) configuration.getInt(path);
            }

            @Override
            public Short get(final FileConfiguration configuration, final Class<Short> type, final Short def, final String path) {
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
            public Integer get(final FileConfiguration configuration, final Class<Integer> type, final String path) {
                return configuration.getInt(path);
            }

            @Override
            public Integer get(final FileConfiguration configuration, final Class<Integer> type, final Integer def, final String path) {
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
            public Long get(final FileConfiguration configuration, final Class<Long> type, final String path) {
                return configuration.getLong(path);
            }

            @Override
            public Long get(final FileConfiguration configuration, final Class<Long> type, final Long def, final String path) {
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
            public Float get(final FileConfiguration configuration, final Class<Float> type, final String path) {
                return (float) configuration.getDouble(path);
            }

            @Override
            public Float get(final FileConfiguration configuration, final Class<Float> type, final Float def, final String path) {
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
            public Double get(final FileConfiguration configuration, final Class<Double> type, final String path) {
                return configuration.getDouble(path);
            }

            @Override
            public Double get(final FileConfiguration configuration, final Class<Double> type, final Double def, final String path) {
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
            public Character get(final FileConfiguration configuration, final Class<Character> type, final String path) {
                return configuration.getString(path).charAt(0);
            }

            @Override
            public Character get(final FileConfiguration configuration, final Class<Character> type, final Character def, final String path) {
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
            public String get(final FileConfiguration configuration, final Class<String> type, final String path) {
                return configuration.getString(path);
            }

            @Override
            public String get(final FileConfiguration configuration, final Class<String> type, final String def, final String path) {
                return configuration.getString(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isString(path);
            }
        }

        private static class ConfigDataEnum<E extends Enum<E>> extends ConfigData<E> {
            @Override
            public E get(final FileConfiguration configuration, final Class<E> type, final String path) {
                val name = configuration.getString(path);
                if (name == null) return null;
                try {
                    return Enum.valueOf(type, name);
                } catch (final IllegalArgumentException e) {
                    return null;
                }
            }

            @Override
            public E get(final FileConfiguration configuration, final Class<E> type, final E def, final String path) {
                val value = get(configuration, type, path);
                return value == null ? def : value;
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isString(path);
            }
        }

        private static class ConfigDataMap extends ConfigData<Map<?, ?>> {
            @Override
            public Map<?, ?> get(final FileConfiguration configuration, final Class<Map<?, ?>> type, final String path) {
                return configuration.getConfigurationSection(path).getValues(false);
            }

            @Override
            public Map<?, ?> get(final FileConfiguration configuration, final Class<Map<?, ?>> type, final Map<?, ?> def, final String path) {
                val section = configuration.getConfigurationSection(path);
                return section == null ? def : section.getValues(false);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isConfigurationSection(path);
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Lists
        ///////////////////////////////////////////////////////////////////////////

        private abstract static class AbstractConfigDataList<T> extends ConfigData<List<T>> {
            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isList(path);
            }
        }

        private static class ConfigDataList extends AbstractConfigDataList<Object> {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(final FileConfiguration configuration, final Class<List<Object>> type, final String path) {
                return (List<Object>) configuration.getList(path);
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<Object> get(FileConfiguration configuration, final Class<List<Object>> type, List<Object> def, String path) {
                return configuration.getList(path) == null ? def : (List<Object>) configuration.getList(path);
            }
        }

        private static class ConfigDataListBoolean extends AbstractConfigDataList<Boolean> {
            @Override
            public List<Boolean> get(final FileConfiguration configuration, final Class<List<Boolean>> type, final String path) {
                return configuration.getBooleanList(path);
            }

            @Override
            public List<Boolean> get(FileConfiguration configuration, final Class<List<Boolean>> type, List<Boolean> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getBooleanList(path);
            }
        }

        private static class ConfigDataListByte extends AbstractConfigDataList<Byte> {
            @Override
            public List<Byte> get(final FileConfiguration configuration, final Class<List<Byte>> type, final String path) {
                return configuration.getByteList(path);
            }

            @Override
            public List<Byte> get(FileConfiguration configuration, final Class<List<Byte>> type, List<Byte> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getByteList(path);
            }
        }

        private static class ConfigDataListShort extends AbstractConfigDataList<Short> {
            @Override
            public List<Short> get(final FileConfiguration configuration, final Class<List<Short>> type, final String path) {
                return configuration.getShortList(path);
            }

            @Override
            public List<Short> get(FileConfiguration configuration, final Class<List<Short>> type, List<Short> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getShortList(path);
            }
        }

        private static class ConfigDataListInt extends AbstractConfigDataList<Integer> {
            @Override
            public List<Integer> get(final FileConfiguration configuration, final Class<List<Integer>> type, final String path) {
                return configuration.getIntegerList(path);
            }

            @Override
            public List<Integer> get(FileConfiguration configuration, final Class<List<Integer>> type, List<Integer> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getIntegerList(path);
            }
        }

        private static class ConfigDataListLong extends AbstractConfigDataList<Long> {
            @Override
            public List<Long> get(final FileConfiguration configuration, final Class<List<Long>> type, final String path) {
                return configuration.getLongList(path);
            }

            @Override
            public List<Long> get(FileConfiguration configuration, final Class<List<Long>> type, List<Long> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getLongList(path);
            }
        }

        private static class ConfigDataListFloat extends AbstractConfigDataList<Float> {
            @Override
            public List<Float> get(final FileConfiguration configuration, final Class<List<Float>> type, final String path) {
                return configuration.getFloatList(path);
            }

            @Override
            public List<Float> get(FileConfiguration configuration, final Class<List<Float>> type, List<Float> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getFloatList(path);
            }
        }

        private static class ConfigDataListDouble extends AbstractConfigDataList<Double> {
            @Override
            public List<Double> get(final FileConfiguration configuration, final Class<List<Double>> type, final String path) {
                return configuration.getDoubleList(path);
            }

            @Override
            public List<Double> get(FileConfiguration configuration, final Class<List<Double>> type, List<Double> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getDoubleList(path);
            }
        }

        private static class ConfigDataListChar extends AbstractConfigDataList<Character> {
            @Override
            public List<Character> get(final FileConfiguration configuration, final Class<List<Character>> type, final String path) {
                return configuration.getCharacterList(path);
            }

            @Override
            public List<Character> get(FileConfiguration configuration, final Class<List<Character>> type, List<Character> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getCharacterList(path);
            }
        }

        private static class ConfigDataListString extends AbstractConfigDataList<String> {
            @Override
            public List<String> get(final FileConfiguration configuration, final Class<List<String>> type, final String path) {
                return configuration.getStringList(path);
            }

            @Override
            public List<String> get(FileConfiguration configuration, final Class<List<String>> type, List<String> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getStringList(path);
            }
        }

        private static class ConfigDataListMap extends AbstractConfigDataList<Map<?, ?>> {
            @Override
            public List<Map<?, ?>> get(final FileConfiguration configuration, final Class<List<Map<?, ?>>> type, final String path) {
                return configuration.getMapList(path);
            }

            @Override
            public List<Map<?, ?>> get(FileConfiguration configuration, final Class<List<Map<?, ?>>> type, List<Map<?, ?>> def, String path) {
                return configuration.getList(path) == null ? def : configuration.getMapList(path);
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Special types
        ///////////////////////////////////////////////////////////////////////////

        private static class ConfigDataVector extends ConfigData<Vector> {
            @Override
            public Vector get(final FileConfiguration configuration, final Class<Vector> type, final String path) {
                return configuration.getVector(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isVector(path);
            }

            @Override
            public Vector get(final FileConfiguration configuration, final Class<Vector> type, final Vector def, final String path) {
                return configuration.getVector(path, def);
            }
        }

        private static class ConfigDataOfflinePlayer extends ConfigData<OfflinePlayer> {
            @Override
            public OfflinePlayer get(final FileConfiguration configuration, final Class<OfflinePlayer> type, final String path) {
                return configuration.getOfflinePlayer(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isOfflinePlayer(path);
            }

            @Override
            public OfflinePlayer get(final FileConfiguration configuration, final Class<OfflinePlayer> type, final OfflinePlayer def, final String path) {
                return configuration.getOfflinePlayer(path, def);
            }
        }

        private static class ConfigDataItemStack extends ConfigData<ItemStack> {
            @Override
            public ItemStack get(final FileConfiguration configuration, final Class<ItemStack> type, final String path) {
                return configuration.getItemStack(path);
            }

            @Override
            public ItemStack get(final FileConfiguration configuration, final Class<ItemStack> type, final ItemStack def, final String path) {
                return configuration.getItemStack(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isItemStack(path);
            }
        }

        private static class ConfigDataColor extends ConfigData<Color> {
            @Override
            public Color get(final FileConfiguration configuration, final Class<Color> type, final String path) {
                return configuration.getColor(path);
            }

            @Override
            public Color get(final FileConfiguration configuration, final Class<Color> type, final Color def, final String path) {
                return configuration.getColor(path, def);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isColor(path);
            }
        }

        private static class ConfigDataObject extends ConfigData {
            @Override
            public Object get(final FileConfiguration configuration, Class type, final String path) {
                return configuration.get(path);
            }

            @Override
            public boolean isValid(final FileConfiguration configuration, final String path) {
                return configuration.isSet(path);
            }
        }
    }

    @Value(staticConstructor = "of")
    class SerializationOptions {
        @NonNull private Type type;
        @NonNull private String path;
        @NonNull private String[] comment;
    }
}
