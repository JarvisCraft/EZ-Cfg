package ru.progrm_jarvis.minecraft.bungee.ezcfg;

import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings({"unused", "Duplicates"})
public interface YamlConfigData<T extends YamlConfigData<T, P>, P extends Plugin> {

    ConfigurationProvider configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

    P getPlugin();

    default boolean isSuperNotRequireCfgFieldAnnotation() {
        return true;
    }

    default List<Field> getFields(final Class<?> clazz) {
        val parentClass = (Class<?>) clazz.getSuperclass();

        val fields = new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields()));
        if (parentClass != null && parentClass != Object.class) fields.addAll(getFields(parentClass));

        return fields;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default T loadData(final File file, final boolean save) {
        val configuration = configurationProvider.load(file);

        var updated = false;
        for (val fieldData : getFieldsData().entrySet()) {
            val field = fieldData.getKey();
            val serializationOptions = fieldData.getValue();

            val accessible = field.isAccessible();
            try {
                field.setAccessible(true);

                val path = serializationOptions.getPath();
                var configValue = serializationOptions.getType().getDataType()
                        .get(configuration, field.getType(), path, null);

                if (configValue == null) try {
                    configValue = field.get(this);

                    // TODO: 09.06.2018 Empty sections support for BungeeCord
                    /*if (configValue == null) configuration.createSection();
                    else */configuration.set(path, configValue);

                    updated = true;

                    continue;
                } catch (final IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                }

                try {
                    // assign value to the field of this exact instance
                    try {
                        field.set(this, configValue);
                    } catch (final IllegalArgumentException e) {
                        field.set(this, null);
                    }

                    if (serializationOptions.getComment().length > 0); // TODO: 02.04.2018 comments
                } catch (final IllegalAccessException e) {
                    onExceptionSettingField(e);
                }
            } finally {
                field.setAccessible(accessible);
            }
        }

        if (save && updated) configurationProvider.save(configuration, file);

        return (T) this;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default T saveData(final File file) {
        val configuration = configurationProvider.load(file);

        var differs = false;
        for (val fieldData : getFieldsData().entrySet()) {
            val field = fieldData.getKey();
            val serializationOptions = fieldData.getValue();

            val accessible = field.isAccessible();
            try {
                field.setAccessible(true);

                final Object fieldValue;
                try {
                    fieldValue = field.get(this);
                } catch (final IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                    continue;
                }

                val configValue = serializationOptions.getType().getDataType()
                        .get(configuration, field.getType(), serializationOptions.getPath());

                if (fieldValue != null && !fieldValue.equals(configValue)
                        || configValue != null && !configValue.equals(fieldValue)) {
                    configuration.set(fieldData.getValue().getPath(), fieldValue);

                    differs = true;
                }
            } finally {
                field.setAccessible(accessible);
            }
        }

        if (differs) configurationProvider.save(configuration, file);

        return (T) this;
    }

    default Map<Field, CfgField.SerializationOptions> getFieldsData() {
        val fieldsData = new HashMap<Field, CfgField.SerializationOptions>();

        val thisClass = getClass();
        val superNotRequireCfgFieldAnnotation = isSuperNotRequireCfgFieldAnnotation();
        for (val field : getFields(thisClass)) {
            if (field.isAnnotationPresent(CfgField.class)) {
                val data = field.getAnnotation(CfgField.class);

                fieldsData.put(field, CfgField.SerializationOptions.of(
                        data.type() == CfgField.Type.AUTO ? CfgField.Type.getType(field) : data.type(),
                        data.value().isEmpty() ? field.getName() : data.value(),
                        data.comment()
                ));
            } else if (superNotRequireCfgFieldAnnotation && field.getDeclaringClass() != thisClass
                    && isModifiable(field.getModifiers())) fieldsData.put(field, CfgField.SerializationOptions
                    .of(CfgField.Type.getType(field), field.getName(), new String[0]));
        }

        return fieldsData;
    }

    @SneakyThrows
    default T load(final File file, final boolean save) {
        {
            val parent = file.getParentFile();
            if (!parent.isDirectory()) Files.createDirectory(parent.toPath());
        }
        if (!file.isFile()) Files.createFile(file.toPath());

        return loadData(file, save);
    }

    default T load(final String path, final boolean save) {
        return load(new File(getPlugin().getDataFolder(), path), save);
    }

    default T load(final boolean save) {
        return load("config.yml", save);
    }

    @SneakyThrows
    default T load(final File file) {
        {
            val parent = file.getParentFile();
            if (!parent.isDirectory()) Files.createDirectory(parent.toPath());
        }
        if (!file.isFile()) Files.createFile(file.toPath());

        return loadData(file, true);
    }

    default T load(final String path) {
        return load(new File(getPlugin().getDataFolder(), path));
    }

    default T load() {
        return load("config.yml");
    }

    @SneakyThrows
    default T save(final File file) {
        {
            val parent = file.getParentFile();
            if (!parent.isDirectory()) Files.createDirectory(parent.toPath());
        }
        if (!file.isFile()) Files.createFile(file.toPath());

        return saveData(file);
    }

    default T save(final String path)  {
        return save(new File(getPlugin().getDataFolder(), path));
    }

    default T save()  {
        return save("config.yml");
    }

    @SuppressWarnings("unchecked")
    default T copyFrom(final T otherConfigData) {
        val thisClass = getClass();
        val superNotRequireCfgFieldAnnotation = isSuperNotRequireCfgFieldAnnotation();
        for (val field : getFields(thisClass)) if (superNotRequireCfgFieldAnnotation && field.getDeclaringClass()
                != thisClass && isModifiable(field.getModifiers()) || field.isAnnotationPresent(CfgField.class)) {
            val accessible = field.isAccessible();
            try {
                field.setAccessible(true);

                field.set(this, field.get(otherConfigData));
            } catch (final IllegalAccessException e) {
                onExceptionCopyingField(e);
            } finally {
                field.setAccessible(accessible);
            }
        }

        return (T) this;
    }

    default void onExceptionGettingField(final Exception e) {
        getPlugin().getLogger().warning("Could not set default value to config file:");
        e.printStackTrace();
    }

    default void onExceptionSettingField(final Exception e) {
        getPlugin().getLogger().warning("Could not set value from config file:");
        e.printStackTrace();
    }

    default void onExceptionCopyingField(final IllegalAccessException e) {
        getPlugin().getLogger().warning("Could not copy value from one ConfigData object to another:");
        e.printStackTrace();
    }

    static boolean isModifiable(final int modifiers) {
        return (modifiers & Modifier.STATIC) == 0
                && (modifiers & Modifier.FINAL) == 0
                && (modifiers & Modifier.TRANSIENT) == 0;
    }
}
