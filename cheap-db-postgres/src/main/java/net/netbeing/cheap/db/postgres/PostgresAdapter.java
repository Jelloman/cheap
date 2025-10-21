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

package net.netbeing.cheap.db.postgres;

import net.netbeing.cheap.db.CheapJdbcAdapter;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresAdapter extends CheapJdbcAdapter
{
    public PostgresAdapter(DataSource dataSource, CheapFactory factory)
    {
        super(dataSource, factory);
    }

    @Override
    public @NotNull Connection getConnection() throws SQLException
    {
        Connection conn = super.getConnection();
        try (Statement statement = conn.createStatement()) {
            statement.execute("SET TIME ZONE 'UTC'");
        }
        return conn;
    }
}
