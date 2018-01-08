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

    void setConfiguration(Configuration configuration);

    Configuration getConfiguration();

    default T load(final File file) throws IOException {
        if (file.isDirectory()) throw new InputMismatchException("Given file is directory");

        if (!file.getParentFile().exists() && file.getParentFile().mkdirs()) onDirCreation();
        if (!file.exists() && file.createNewFile()) onFileCreation();

        return loadData(file);
    }

    default T load(final String path) throws IOException {
        return load(new File(getPlugin().getDataFolder(), path));
    }

    default T load() throws IOException {
        return load(new File(getPlugin().getDataFolder(), "config.yml"));
    }

    @SuppressWarnings({"unchecked", "Duplicates"})
    default T loadData(final File file) throws IOException {
        var configuration = configurationProvider.load(file);

        val fieldsDeclared = new ArrayList<Field>(Arrays.asList(this.getClass().getDeclaredFields()));
        val fields = new HashMap<Field, CfgField>();
        for (Field field : fieldsDeclared)
            if (field.isAnnotationPresent(CfgField.class)) fields
                    .put(field, field.getAnnotation(CfgField.class));

        var updated = false;
        for (Map.Entry<Field, CfgField> field : fields.entrySet()) {
            CfgField.Type type = null;

            if (field.getValue().type() == CfgField.Type.AUTO) type = CfgField.Type.getType(field.getKey());

            // return if unknown type of CfgField
            if (type == null) continue;

            val accessible = field.getKey().isAccessible();
            field.getKey().setAccessible(true);

            val path = field.getValue().value().isEmpty() ? field.getKey().getName() : field.getValue().value();

            var value = type.getDataType().get(configuration, path, null);

            // if no value is in config file
            if (value == null) try {
                value = field.getKey().get(this);
                configuration.set(path, value);

                updated = true;
            } catch (IllegalStateException | IllegalAccessException e) {
                onExceptionSettingTo(e);
            }

            try {
                // assign value to the field of this exact instance
                field.getKey().set(this, value);

                if (field.getValue().comment().length > 0) {
                    // TODO: 12.11.2017 comment
                }
            } catch (IllegalAccessException e) {
                onExceptionSettingFrom(e);
            }

            field.getKey().setAccessible(accessible);
        }

        if (updated) configurationProvider.save(configuration, file);

        setConfiguration(configuration);

        return (T) this;
    }

    default void onDirCreation() {
        getPlugin().getLogger().info("Config-file directory has been successfully created");
    }

    default void onFileCreation() {
        getPlugin().getLogger().info("Config-file has been successfully created");
    }

    default void onExceptionSettingTo(final Exception e) {
        getPlugin().getLogger().warning("Could not set default value to config file:");
        e.printStackTrace();
    }

    default void onExceptionSettingFrom(final Exception e) {
        getPlugin().getLogger().warning("Could not set value from config file:");
        e.printStackTrace();
    }
}
