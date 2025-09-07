package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import net.netbeing.cheap.util.reflect.ReflectionWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Map;

public class RecordAspectDef extends ImmutableAspectDefImpl
{
    private final Class<? extends Record> recordClass;
    private final Map<String, GenericGetterSetter> methods;


    public RecordAspectDef(@NotNull Class<? extends Record> recordClass)
    {
        super(recordClass.getCanonicalName(), propDefsFrom(recordClass));
        this.recordClass = recordClass;
        this.methods = buildMethodMap();
    }

    protected @NotNull @Unmodifiable ImmutableMap<@NotNull String, @NotNull GenericGetterSetter> buildMethodMap()
    {
        Collection<? extends PropertyDef> propDefs = propertyDefs();
        ImmutableMap.Builder<@NotNull String, @NotNull GenericGetterSetter> builder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PropertyDef propDef : propDefs) {
            RecordPropertyDef recDef = (RecordPropertyDef) propDef;
            RecordComponent comp = recDef.field();
            Method method = comp.getAccessor();
            GenericGetterSetter getterHolder = ReflectionWrapper.createWrapper(method);
            builder.put(comp.getName(), getterHolder);
        }
        return builder.build();
    }

    protected GenericGetterSetter getter(@NotNull String propName)
    {
        return methods.get(propName);
    }

    public Class<? extends Record> getRecordClass()
    {
        return recordClass;
    }

    public static ImmutableMap<@NotNull String, @NotNull PropertyDef> propDefsFrom(@NotNull Class<? extends Record> klass)
    {
        RecordComponent[] fields = klass.getRecordComponents();
        ImmutableMap.Builder<@NotNull String, @NotNull PropertyDef> propDefs = ImmutableMap.builderWithExpectedSize(fields.length);

        for (var field : fields) {
            PropertyDef def = new RecordPropertyDef(field);
            propDefs.put(field.getName(), def);
        }

        return propDefs.build();
    }

}
