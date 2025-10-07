/**
 * The cheap-json module provides JSON schema definitions and utilities
 * for the Cheap data model.
 *
 * <p>This module contains:
 * <ul>
 *   <li>JSON schemas for all Cheap model components</li>
 *   <li>Utilities for JSON serialization and validation</li>
 * </ul>
 */
module net.netbeing.cheap.json
{
    requires transitive net.netbeing.cheap.core;
    requires com.google.common;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires static org.jetbrains.annotations;

    exports net.netbeing.cheap.json;
}