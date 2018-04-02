package ru.progrm_jarvis.minecraft.bungee.ezcfg;

import lombok.experimental.var;
import lombok.val;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unused")
public interface YamlConfigData<T extends YamlConfigData<T, P>, P extends Plugin> {

    ConfigurationProvider configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

    P getPlugin();

    @SuppressWarnings({"unchecked", "Duplicates"})
    default T loadDataAndSave(final File file) throws IOException {
        val fieldsData = getFieldsData();

        val configuration = configurationProvider.load(file);

        var updated = false;
        for (Map.Entry<Field, CfgField.SerializationOptions> fieldData : fieldsData.entrySet()) {
            val accessible = fieldData.getKey().isAccessible();
            try {
                fieldData.getKey().setAccessible(true);

                var configValue = fieldData.getValue().getType().getDataType()
                        .get(configuration, fieldData.getValue().getPath(), null);

                if (configValue == null) try {
                    configValue = fieldData.getKey().get(this);
                    configuration.set(fieldData.getValue().getPath(), configValue);

                    updated = true;

                    continue;
                } catch (IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                }

                try {
                    // assign value to the field of this exact instance
                    fieldData.getKey().set(this, configValue);

                    if (fieldData.getValue().getComment().length > 0); // TODO: 02.04.2018 comments
                } catch (IllegalAccessException e) {
                    onExceptionSettingField(e);
                }
            } finally {
                fieldData.getKey().setAccessible(accessible);
            }
        }

        if (updated) configurationProvider.save(configuration, file);

        return (T) this;
    }

    @SuppressWarnings({"unchecked", "Duplicates"})
    default T saveData(final File file) throws IOException {
        val fieldsData = getFieldsData();

        val configuration = configurationProvider.load(file);

        var differs = false;
        for (Map.Entry<Field, CfgField.SerializationOptions> fieldData : fieldsData.entrySet()) {
            val accessible = fieldData.getKey().isAccessible();
            try {
                fieldData.getKey().setAccessible(true);


                final Object fieldValue;
                try {
                    fieldValue = fieldData.getKey().get(this);
                } catch (IllegalStateException | IllegalAccessException e) {
                    onExceptionGettingField(e);
                    continue;
                }

                val configValue = fieldData.getValue().getType().getDataType()
                        .get(configuration, fieldData.getValue().getPath());

                if (fieldValue != null && !fieldValue.equals(configValue)
                        || configValue != null && !configValue.equals(fieldValue)) {
                    configuration.set(fieldData.getValue().getPath(), fieldValue);

                    differs = true;
                }
            } finally {
                fieldData.getKey().setAccessible(accessible);
            }
        }

        if (differs) configurationProvider.save(configuration, file);

        return (T) this;
    }

    default Map<Field, CfgField.SerializationOptions> getFieldsData() {
        val fieldsData = new HashMap<Field, CfgField.SerializationOptions>();

        val fieldsDeclared = new ArrayList<Field>(Arrays.asList(this.getClass().getDeclaredFields()));
        for (val field : fieldsDeclared) if (field.isAnnotationPresent(CfgField.class)) {
            val data = field.getAnnotation(CfgField.class);

            fieldsData.put(field, CfgField.SerializationOptions.of(
                    data.type() == CfgField.Type.AUTO ? CfgField.Type.getType(field) : data.type(),
                    data.value().isEmpty() ? field.getName() : data.value(),
                    data.comment()
            ));
        }

        return fieldsData;
    }

    default T load(final File file) throws IOException {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return loadDataAndSave(file);
    }

    default T load(final String path) throws IOException {
        return load(new File(getPlugin().getDataFolder(), path));
    }

    default T load() throws IOException {
        return load("config.yml");
    }

    default T save(final File file) throws IOException {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return saveData(file);
    }

    default T save(final String path) throws IOException  {
        return save(new File(getPlugin().getDataFolder(), path));
    }

    default T save() throws IOException  {
        return save("config.yml");
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
}
