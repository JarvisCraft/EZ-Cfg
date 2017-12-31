package ru.progrm_jarvis.minecraft.ezcfg;

import lombok.experimental.UtilityClass;
import lombok.experimental.var;
import lombok.val;

import java.lang.reflect.Field;
import java.util.Map;

@UtilityClass
public class EzCfgUtil {
    public boolean getFl() {
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
    }
}
