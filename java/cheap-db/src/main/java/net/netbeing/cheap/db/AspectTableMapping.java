package net.netbeing.cheap.db;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps an AspectDef to a database table, with property-to-column mappings.
 * <p>
 * This class enables aspects to be stored in and loaded from custom database tables
 * rather than the standard aspect/property_value tables.
 */
public class AspectTableMapping
{
    private final String aspectDefName;
    private final String tableName;
    private final Map<String, String> propertyToColumnMap;

    /**
     * Creates a new AspectTableMapping.
     *
     * @param aspectDefName the name of the AspectDef
     * @param tableName the name of the database table
     * @param propertyToColumnMap map from property names to column names
     */
    public AspectTableMapping(
        @NotNull String aspectDefName,
        @NotNull String tableName,
        @NotNull Map<String, String> propertyToColumnMap)
    {
        this.aspectDefName = aspectDefName;
        this.tableName = tableName;
        this.propertyToColumnMap = new LinkedHashMap<>(propertyToColumnMap);
    }

    /**
     * Gets the AspectDef name.
     *
     * @return the AspectDef name
     */
    @NotNull
    public String aspectDefName()
    {
        return aspectDefName;
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    @NotNull
    public String tableName()
    {
        return tableName;
    }

    /**
     * Gets the property-to-column mapping.
     *
     * @return unmodifiable map from property names to column names
     */
    @NotNull
    public Map<String, String> propertyToColumnMap()
    {
        return Collections.unmodifiableMap(propertyToColumnMap);
    }
}
