Cheap Design Notes
==================

Cheap is a data caching system and metadata model. Its design is focused on flexible and performant modeling and
usage of a wide variety of data sources and sinks.
Cheap is NOT a database. All Cheap data is held in catalogs, and all Cheap Catalogs are caches.

| Tier          | RDBMS equivalent | Filesystem equivalent                 |
|---------------|------------------|---------------------------------------|
| C - Catalog   | Database         | Volume                                |
| H - Hierarchy | Table or Index   | Directory structure, File manifest    |
| E - Entity    | Primary Key      | File, file element                    |
| A - Aspect    | Row              | File or element attributes or content |
| P - Property  | Column           | Single attribute or content atom      |



Cheap does not dictate patterns of network communication or other external resource access.
Cheap offers utility functions and callback mechanisms to allow applications to use it in conjunction with databases,
filesystems, etc. but it does not mandate their usage.
Cheap is not concerned with access control, but tries to be flexible enough to accommodate fine-grained access control.





CATALOGS
--------
* All Cheap Catalogs are considered working copies or caches.
* Every Catalog has either an upstream Catalog or an external data source (never both).
* Catalogs are also task managers and logs that manage and report on current AND past read and write jobs to the upstream/source.
* Each Catalog has exactly one "Species". There are six species of catalog:
    * A "source" - a read-only cache of an external data source;
    * A "sink" - a read-write working copy of an external data source;
    * A "mirror" - a read-only cache of another catalog;
    * A "cache" - a (possibly buffered) write-though cache of another catalog;
    * A "clone" - a write-back working copy of another catalog with manual writes; or
    * A "fork" - a transient copy of another catalog, intended to diverge.
* A fork is intended to be used as a short-lived transition from a mirror, cache or clone into a sink, AKA "Save As...".
* Each Catalog has a Catalog Def that defines the structure and types of data it contains.
  * The CatalogDef includes a set of AspectDefs and HierarchyDefs.
  * Each Catalog has a "strict" flag. Strict catalogs can only contain Aspects and Hierarchies defined by their CatalogDef.
* Mirror catalogs always have the same def as the upstream; clones and forks usually do, but can diverge.
* Each Catalog has an Aspectage, which is a directory of ALL AspectDefs in the catalog.


HIERARCHIES
-----------
* Hierarchies come in 5 types:

| Type        | Code | Description                                                                              |
|-------------|------|------------------------------------------------------------------------------------------|
| ENTITY_LIST | EL   | an ordered list containing entity IDs, possibly duplicates                               |
| ENTITY_SET  | ES   | a non-ordered set containing entity IDs                                                  |
| ENTITY_DIR  | ED   | a map (KV pairs) mapping strings to entity IDs                                           |
| ENTITY_TREE | ET   | a map (KV pairs) mapping strings to entity IDs OR other entity tree nodes                |
| ASPECT_MAP  | AM   | an ordered map (KV pairs) mapping local or global entity IDs to aspects of a single type |


* Hierarchies in a catalog are assigned unique names within the catalog.
  * AspectMap hierarchy names are always identical to the name of the AspectDef.
* The Global ID of hierarchies is not a UUID, it's a URI composed of the catalog and hierarchy name within that catalog.

ENTITIES
--------
* Entities are nothing but IDs. All other information about entities is found in hierarchies and aspects.
* Entity IDs can be local or global.
  * Global Entity IDs are UUIDs.
  * Local entity IDs are implementation/language-specific references.
    * Local entities usually can lazily generate a global UUID.

ASPECTS
-------
* An Aspect is a data record that is attached to a single Entity.
* Each Aspect is defined by a single AspectDef which defines the fields in the record.
* Each Entity can have either zero or one Aspect of a given AspectDef.
* Aspects are always stored in an AspectMap in a Catalog, organized by AspectDef (much like RDBMS tables).
* AspectDefs have a full name which must be globally unique, and should follow a naming convention like Java classes.
* Currently the design is for AspectDefs to NOT have a UUID, but that may need to be revisited.
    - Aspect versioning needs to be considered more thoroughly.

PROPERTIES
----------
* A Property is a "field" of data, which is either a simple value or an array of simple values ("multivalued").
* Every Property has a PropertyDef, which belongs to a single AspectDef.
  * PropertyDefs have a short name, which must be unique within the Aspect.
* Properties are never structured or nested objects; such things should be Aspects or Hierarchies.
* An individual Property object is always immutable.
  * Multivalued properties are not meant to be modified "in place".
* 

| Type Name   | Type Code | Java Class     | Description                                                                 |
|-------------|-----------|----------------|-----------------------------------------------------------------------------|
| Integer     | INT       | Long           | 64-bit signed integer values.                                               |
| Float       | FLT       | Double         | 64-bit floating-point values (double precision).                            |
| Boolean     | BLN       | Boolean        | Boolean values supporting true, false, or null states.                      |
| String      | STR       | String         | String values with length limited to 8192 characters, processed atomically. |
| Text        | TXT       | String         | Text values with unlimited length, processed atomically.                    |
| BigInteger  | BGI       | BigInteger     | Arbitrary precision integer values with unlimited size.                     |
| BigDecimal  | BGF       | BigDecimal     | Arbitrary precision floating-point values with unlimited size.              |
| DateTime    | DAT       | ZonedDateTime  | Date and time values stored as ISO-8601 formatted strings.                  |
| URI         | URI       | URI            | Uniform Resource Identifier values following RFC 3986 specification.        |
| UUID        | UID       | UUID           | Universally Unique Identifier values following RFC 4122 specification.      |
| CLOB        | CLB       | String         | Character Large Object (CLOB) for streaming text data.                      |
| BLOB        | BLB       | byte[]         | Binary Large Object (BLOB) for streaming binary data.                       |


Serialization and Persistence
-----------------------------
* Any serialization (S11N) and persistence of CHEAP elements requires AspectDefs (and their PropertyDefs)
  to be serialized first.
* 


