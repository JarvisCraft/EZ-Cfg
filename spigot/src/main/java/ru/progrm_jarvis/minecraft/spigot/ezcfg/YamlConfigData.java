package ru.progrm_jarvis.minecraft.spigot.ezcfg;

import lombok.experimental.var;
import lombok.val;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings({"unused", "Duplicates"})
public interface YamlConfigData<T extends YamlConfigData<T, P>, P extends Plugin> {

    P getPlugin();

    default boolean isSuperNotRequireCfgFieldAnnotation() {
        return true;
    }

    default List<Field> getFields(final Class<?> clazz) {

        val parentClass = (Class<?>) clazz.getSuperclass();

        val fields = new ArrayList<Field>();
        for (val field : clazz.getDeclaredFields()) if ((field.getModifiers() & Modifier.STATIC) == 0
                && (field.getModifiers() & Modifier.TRANSIENT) == 0
                && (field.getModifiers() & Modifier.FINAL) == 0) fields.add(field);
        if (parentClass != null && parentClass != Object.class) fields.addAll(getFields(parentClass));

        return fields;
    }

    @SuppressWarnings("unchecked")
    default T loadData(final File file, final boolean save) throws IOException, InvalidConfigurationException {
        val configuration = new YamlConfiguration() {{
            load(file);
        }};

        var updated = false;
        for (val fieldData : getFieldsData().entrySet()) {
            val accessible = fieldData.getKey().isAccessible();
            try {
                fieldData.getKey().setAccessible(true);

                var configValue = fieldData.getValue().getType().getDataType()
                        .get(configuration, fieldData.getKey().getType(), null, fieldData.getValue().getPath());

                if (configValue == null) try {
                    configValue = fieldData.getKey().get(this);
                    if (configValue == null) configuration.set(fieldData.getValue().getPath(), fieldData.getValue()
                            .getType().getDataType().getDefault());
                    else configuration.set(fieldData.getValue().getPath(), configValue);

                    updated = true;
                    
                    continue;
                } catch (final IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                }

                try {
                    // assign value to the field of this exact instance
                    try {
                        fieldData.getKey().set(this, configValue);
                    } catch (final IllegalArgumentException e) {
                        fieldData.getKey().set(this, null);
                    }

                    if (fieldData.getValue().getComment().length > 0); // TODO: 02.04.2018 comments
                } catch (final IllegalAccessException e) {
                    onExceptionSettingField(e);
                }
            } finally {
                fieldData.getKey().setAccessible(accessible);
            }
        }

        if (save && updated) configuration.save(file);

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    default T saveData(final File file) throws IOException, InvalidConfigurationException {
        val configuration = new YamlConfiguration() {{
            load(file);
        }};

        var differs = false;
        for (val fieldData : getFieldsData().entrySet()) {
            val accessible = fieldData.getKey().isAccessible();
            try {
                fieldData.getKey().setAccessible(true);

                final Object fieldValue;
                try {
                    fieldValue = fieldData.getKey().get(this);
                } catch (final IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                    continue;
                }

                val configValue = fieldData.getValue().getType().getDataType()
                        .get(configuration, fieldData.getKey().getType(), fieldData.getValue().getPath());

                if (fieldValue != null && !fieldValue.equals(configValue)
                        || configValue != null && !configValue.equals(fieldValue)) {
                    configuration.set(fieldData.getValue().getPath(), fieldValue);

                    differs = true;
                }
            } finally {
                fieldData.getKey().setAccessible(accessible);
            }
        }

        if (differs) configuration.save(file);

        return (T) this;
    }

    default Map<Field, CfgField.SerializationOptions> getFieldsData() {
        val fieldsData = new HashMap<Field, CfgField.SerializationOptions>();

        val thisClass = getClass();
        val superNotRequireCfgFieldAnnotation = isSuperNotRequireCfgFieldAnnotation();
        for (val field : getFields(thisClass)) if (field.isAnnotationPresent(CfgField.class)) {
            val data = field.getAnnotation(CfgField.class);

            fieldsData.put(field, CfgField.SerializationOptions.of(
                    data.type() == CfgField.Type.AUTO ? CfgField.Type.getType(field) : data.type(),
                    data.value().isEmpty() ? field.getName() : data.value(),
                    data.comment()
            ));
        } else if (superNotRequireCfgFieldAnnotation && field.getDeclaringClass() != thisClass) fieldsData
                .put(field, CfgField.SerializationOptions
                        .of(CfgField.Type.getType(field), field.getName(), new String[0]));

        return fieldsData;
    }

    default T load(final File file, final boolean save) throws IOException, InvalidConfigurationException  {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return loadData(file, save);
    }

    default T load(final String path, final boolean save) throws IOException, InvalidConfigurationException  {
        return load(new File(getPlugin().getDataFolder(), path), save);
    }

    default T load(final boolean save) throws IOException, InvalidConfigurationException  {
        return load("config.yml", save);
    }

    default T load(final File file) throws IOException, InvalidConfigurationException {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return loadData(file, true);
    }

    default T load(final String path) throws IOException, InvalidConfigurationException {
        return load(new File(getPlugin().getDataFolder(), path));
    }

    default T load() throws IOException, InvalidConfigurationException {
        return load("config.yml");
    }

    default T save(final File file) throws IOException, InvalidConfigurationException {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return saveData(file);
    }

    default T save(final String path) throws IOException, InvalidConfigurationException  {
        return save(new File(getPlugin().getDataFolder(), path));
    }

    default T save() throws IOException, InvalidConfigurationException  {
        return save("config.yml");
    }

    @SuppressWarnings("unchecked")
    default T copyFrom(final T otherConfigData) {
        val thisClass = getClass();
        val superNotRequireCfgFieldAnnotation = isSuperNotRequireCfgFieldAnnotation();
        for (val field : getFields(thisClass)) if (superNotRequireCfgFieldAnnotation && field.getDeclaringClass()
                != thisClass || field.isAnnotationPresent(CfgField.class)) {
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

    default void onDirCreation() {
        getPlugin().getLogger().info("Config-file directory has been successfully created");
    }

    default void onFileCreation() {
        getPlugin().getLogger().info("Config-file has been successfully created");
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
}
