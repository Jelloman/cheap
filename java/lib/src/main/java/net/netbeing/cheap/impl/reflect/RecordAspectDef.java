package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import net.netbeing.cheap.util.reflect.LambdaWrapper;
import net.netbeing.cheap.util.reflect.LambdaWrapperHolder;
import net.netbeing.cheap.util.reflect.WrapperHolder;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Map;

public class RecordAspectDef extends ImmutableAspectDefImpl
{
    private final Class<? extends Record> recordClass;
    private final Map<String, LambdaWrapper> methods;


    public RecordAspectDef(@NotNull Class<? extends Record> recordClass)
    {
        super(recordClass.getCanonicalName(), propDefsFrom(recordClass));
        this.recordClass = recordClass;
        this.methods = buildMethodMap();
    }

    protected @NotNull @Unmodifiable ImmutableMap<String, LambdaWrapper> buildMethodMap()
    {
        LambdaWrapperHolder lambdaWrapperHolder = LambdaWrapperHolder.DEFAULT;
        Collection<RecordPropertyDef> propDefs = recordPropertyDefs();
        ImmutableMap.Builder<String, LambdaWrapper> builder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (RecordPropertyDef prop : propDefs) {
            RecordComponent comp = prop.field();
            Method method = comp.getAccessor();
            WrapperHolder<LambdaWrapper> getterHolder = lambdaWrapperHolder.createWrapper(method);
            builder.put(comp.getName(), getterHolder.getWrapper());
        }
        return builder.build();
    }

    protected LambdaWrapper getter(@NotNull String propName)
    {
        return methods.get(propName);
    }

    public Class<? extends Record> getRecordClass()
    {
        return recordClass;
    }

    public static ImmutableMap<String, PropertyDef> propDefsFrom(@NotNull Class<? extends Record> klass)
    {
        RecordComponent[] fields = klass.getRecordComponents();
        ImmutableMap.Builder<String, PropertyDef> propDefs = ImmutableMap.builderWithExpectedSize(fields.length);

        for (var field : fields) {
            PropertyDef def = new RecordPropertyDef(field);
            propDefs.put(field.getName(), def);
        }

        return propDefs.build();
    }

    public Collection<RecordPropertyDef> recordPropertyDefs()
    {
        //noinspection unchecked
        return (Collection<RecordPropertyDef>) propertyDefs();
    }


}
