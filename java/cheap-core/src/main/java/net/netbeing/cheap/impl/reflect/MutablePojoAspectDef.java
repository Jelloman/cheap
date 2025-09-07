package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.MutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import net.netbeing.cheap.util.reflect.ReflectionWrapper;
import org.jetbrains.annotations.NotNull;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

public class MutablePojoAspectDef extends MutableAspectDefImpl
{
    private final Class<?> pojoClass;
    private final Map<String, GenericGetterSetter> getters;
    private final Map<String, GenericGetterSetter> setters;

    public MutablePojoAspectDef(@NotNull Class<?> pojoClass)
    {
        super(pojoClass.getCanonicalName(), propDefsFrom(pojoClass));
        this.pojoClass = pojoClass;

        Collection<? extends PropertyDef> propDefs = propertyDefs();
        ImmutableMap.Builder<String, GenericGetterSetter> getterBuilder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        ImmutableMap.Builder<String, GenericGetterSetter> setterBuilder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PropertyDef prop : propDefs) {
            PojoPropertyDef pojoDef = (PojoPropertyDef) prop;
            if (pojoDef.getter() != null) {
                GenericGetterSetter getterHolder = ReflectionWrapper.createWrapper(pojoDef.getter());
                getterBuilder.put(prop.name(), getterHolder);
            }
            if (pojoDef.setter() != null) {
                GenericGetterSetter setterHolder = ReflectionWrapper.createWrapper(pojoDef.setter());
                setterBuilder.put(prop.name(), setterHolder);
            }
        }
        this.getters =  getterBuilder.build();
        this.setters =  setterBuilder.build();
    }

    public Class<?> getPojoClass()
    {
        return pojoClass;
    }

    protected GenericGetterSetter getter(@NotNull String propName)
    {
        return getters.get(propName);
    }

    protected GenericGetterSetter setter(@NotNull String propName)
    {
        return setters.get(propName);
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
            PropertyDef def = PojoPropertyDef.fromPropertyDescriptor(prop, false);
            propDefs.put(prop.getName(), def);
        }
        return propDefs.build();
    }

}
