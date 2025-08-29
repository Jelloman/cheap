package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;
import net.netbeing.cheap.util.reflect.LambdaWrapper;
import net.netbeing.cheap.util.reflect.LambdaWrapperHolder;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

public class ImmutablePojoAspectDef extends ImmutableAspectDefImpl
{
    private final Class<?> pojoClass;
    private final Map<String, LambdaWrapper> getters;

    public ImmutablePojoAspectDef(@NotNull Class<?> pojoClass)
    {
        super(pojoClass.getCanonicalName(), propDefsFrom(pojoClass));
        this.pojoClass = pojoClass;

        Collection<PojoPropertyDef> propDefs = pojoPropertyDefs();
        ImmutableMap.Builder<String, LambdaWrapper> getterBuilder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PojoPropertyDef prop : propDefs) {
            if (prop.getter() != null) {
                LambdaWrapper getterHolder = LambdaWrapperHolder.createWrapper(prop.getter());
                getterBuilder.put(prop.name(), getterHolder);
            }
        }
        this.getters =  getterBuilder.build();
    }

    public Class<?> getPojoClass()
    {
        return pojoClass;
    }

    protected LambdaWrapper getter(@NotNull String propName)
    {
        return getters.get(propName);
    }

    public static ImmutableMap<String, PropertyDef> propDefsFrom(@NotNull Class<?> pojoClass)
    {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(pojoClass, Object.class);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        ImmutableMap.Builder<String, PropertyDef> propDefs = ImmutableMap.builderWithExpectedSize(props.length);
        for (PropertyDescriptor prop : props)
        {
            PropertyDef def = PojoPropertyDef.fromPropertyDescriptor(prop, true);
            propDefs.put(prop.getName(), def);
        }
        return propDefs.build();
    }

    public Collection<PojoPropertyDef> pojoPropertyDefs()
    {
        //noinspection unchecked
        return (Collection<PojoPropertyDef>) propertyDefs();
    }


}
