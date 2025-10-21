/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.db;

import net.netbeing.cheap.util.CheapFactory;
import net.netbeing.cheap.util.PropertyValueAdapter;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

public abstract class CheapJdbcAdapter
{
    /**
     * Data source providing database connections.
     */
    protected final DataSource dataSource;

    /**
     * Factory for creating Cheap model objects (entities, aspects, hierarchies, etc.).
     * Maintains entity registry to ensure entity identity is preserved across loads.
     */
    protected CheapFactory factory;

    /** PropertyValueAdapter for adapting values read from the Database **/
    protected PropertyValueAdapter valueAdapter = new PropertyValueAdapter();


    protected CheapJdbcAdapter(@NotNull DataSource dataSource, @NotNull CheapFactory factory)
    {
        this.dataSource = dataSource;
        this.factory = factory;
    }

    public CheapFactory getFactory()
    {
        return factory;
    }

    public void setFactory(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    /**
     * Return the current PropertyValueAdapter in this CatalogAdapter.
     * @return the current PropertyValueAdapter
     */
    public PropertyValueAdapter getValueAdapter()
    {
        return valueAdapter;
    }

    /**
     * Set a new PropertyValueAdapter in this CatalogAdapter
     * @param valueAdapter a new PropertyValueAdapter
     */
    public void setValueAdapter(@NotNull PropertyValueAdapter valueAdapter)
    {
        this.valueAdapter = valueAdapter;
    }

    public void setTimeZone(@NotNull TimeZone zone)
    {
        this.valueAdapter.setTimeZone(zone);
        this.factory.setTimeZone(zone);
    }

    /**
     * Create and return a connection to the database.
     *
     * @return a new connection (or one from a pool).
     */
    public @NotNull Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }
}
