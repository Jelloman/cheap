Cheap Design Notes
==================

Cheap is a data caching system and metadata model. Its design is focused on flexible and performant modeling and
usage of a wide variety of data sources and sinks, and also automated schema translation and mapping.
Cheap is NOT a database. All Cheap data is held in catalogs, and all Cheap Catalogs are caches or working copies
of external data (or other Catalogs).
The best analogy for understanding Cheap is **git**. Cheap is a git-like mechanism for structured data and objects. 

| Tier          | RDBMS equivalent | Filesystem equivalent                 |
|---------------|------------------|---------------------------------------|
| C - Catalog   | Database         | Volume                                |
| H - Hierarchy | Table or Index   | Directory structure, File manifest    |
| E - Entity    | Primary Key      | File, file element                    |
| A - Aspect    | Row              | File or element attributes or content |
| P - Property  | Column           | Single attribute or content atom      |

Purpose
-------
I'm building Cheap primarily to serve as the data layer for a featureful desktop/web application for
interacting with and managing disparate data sources. I expect it will be useful for many other purposes.


**WORK IN PROGRESS**
--------------------
Cheap is a work in progress. I'll add a roadmap soon, for now here are the broad strokes of a plan:
* cheap-net and cheapd modules (see [Modules](#modules) below)
* TypeScript port
* Python port
* MySQL/MariaDB integration
* protobuf, capnproto and flatbuffers support (read, write, schema translation) 
* Catalog-based task management and logging for current AND past read and write jobs to the upstream/source.


Modules
-------
| Module     | Description                                                                                               |
|------------|-----------------------------------------------------------------------------------------------------------|
| cheap-core | Core Cheap interfaces and basic implementations. Includes filesystem functionality. Minimal dependencies. |
| cheap-db   | Database connectors and catalog implementations.                                                          |
| cheap-json | JSON serialization and deserialization of Cheap elements.                                                 |
| cheap-net* | Networking library, including interop with wire protocols like protobuf, flatbuffers and Cap'n Proto/Web. |
| cheapd*    | Service to provide access to a set of catalogs through standard REST APIs or other protocols.             |

\* planned


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
* Mirror catalogs always have the same def as the upstream; clones and forks usually do, but can diverge.
* Each Catalog has an Aspectage, which is a directory of ALL AspectDefs in the catalog.
* A CatalogDef is purely informational, defining the types of data a catalog contains.
    * The CatalogDef includes a set of AspectDefs and HierarchyDefs.


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
    * The main reason for Local entity references is performance, thus avoiding UUID generation unless needed.

ASPECTS
-------
* An Aspect is a data record that is attached to a single Entity.
* Each Aspect is defined by a single AspectDef which defines the fields in the record.
* Aspects are always stored in an AspectMap in a Catalog, organized by AspectDef (much like RDBMS tables).
* Each Entity can have at most one Aspect of a given AspectDef.
  * If you need an entity to have multiple aspects of the same type, those aspects should really be entities 
    of their own, and the one-to-many relationship should be an entity-entity relationship, stored either in
    an entity tree hierarchy or a multivalued UUID or URI property.
* AspectDefs have a full name which must be globally unique and should use reverse domain name notation.
* AspectDefs may optionally have a UUID. If they do, they may also optionally have a URI and/or a version number.
  AspectDefs which are intended to be used for inter-catalog data exchange should have all three.

PROPERTIES
----------
* A Property is a "field" of data, which is either a simple value or an array of simple values ("multivalued").
* Every Property has a PropertyDef, which belongs to a single AspectDef.
  * PropertyDefs have a short name, which must be unique within the Aspect.
* Properties are never structured or nested objects; such things should be Aspects or Hierarchies.
* An individual Property object is always immutable.
  * Multivalued properties are not meant to be modified "in place".
* Each Property has:
  * a "Type", which is one of a limited set of types defined by Cheap; and
  * an optional "Native Type", which is a string describing another type for an external system.
    * Cheap allows for registration of type-native-type converters.
    * Native types allow for total compatibility with arbitrary type formats, e.g., any ISO date format.

### Property Types

| Type Name  | Type Code | Java Class    | Description                                                                 |
|------------|-----------|---------------|-----------------------------------------------------------------------------|
| Integer    | INT       | Long          | 64-bit signed integer values.                                               |
| Float      | FLT       | Double        | 64-bit floating-point values (double precision).                            |
| Boolean    | BLN       | Boolean       | Boolean values supporting true, false, or null states.                      |
| String     | STR       | String        | String values with length limited to 8192 characters, processed atomically. |
| Text       | TXT       | String*       | Text values with unlimited length, processed atomically.                    |
| BigInteger | BGI       | BigInteger    | Arbitrary precision integer values with unlimited size.                     |
| BigDecimal | BGF       | BigDecimal    | Arbitrary precision floating-point values with unlimited size.              |
| DateTime   | DAT       | ZonedDateTime | Date and time values, usually stored as ISO-8601 formatted strings.         |
| URI        | URI       | URI           | Uniform Resource Identifier values following RFC 3986 specification.        |
| UUID       | UID       | UUID          | Universally Unique Identifier values following RFC 4122 specification.      |
| CLOB       | CLB       | String*       | Character Large Object (CLOB) for streaming text data.                      |
| BLOB       | BLB       | byte[]*       | Binary Large Object (BLOB) for streaming binary data.                       |

\* Cheap will provide streaming mechanisms to read and write Text,CLOB and BLOB properties
(and multivalued properties of any type) in fixed-sized chunks.


Identity in Cheap
-----------------

### Global IDs
* Catalogs and AspectDefs always have a global UUID.
* AspectDefs also have a name that should be globally unique, preferably using reverse domain name notation.
* Entities have a UUID, and are, in fact, nothing but a UUID.
  * Local Entity objects can be used to lazily generate UUIDs for performance reasons.
* Catalogs usually have a URL.
  * All Cheap elements within such Catalogs are addressable via URLs.
* Hierarchies are owned by a single Catalog and do not have a global ID.
  * They must have a unique name within their catalog.
* PropertyDefs are owned by a single AspectDef and do not have a global ID.
  * They must have a unique name within their AspectDef.
* Properties are owned by an Aspect and do not have a global ID.
  * They must have a unique name within their Aspect, which matches the PropertyDef.

### Versions
* CatalogDefs, HierarchyDefs and AspectDefs have an implicit hash version.
  * Like a Git commit, based on the entire contents of the Def.
* Catalogs and Hierarchies have an explicit, monotonically increasing integer version number.
  * Incrementing this version number is typically restricted to "local access" and is not accessible via API.

| Element      | Global? | Owner     | Unique ID | Version Numbering |
|--------------|---------|-----------|-----------|-------------------|
| Catalog      | Yes     | -         | UUID      | Integer (manual)  |
| CatalogDef   | No      | -         | -         | Hash (implicit)   |
| HierarchyDef | No      | Catalog   | Name      | -                 |
| Hierarchy    | No      | Catalog   | Name      | Integer (manual)  |
| Entity       | Yes     | -         | UUID      | -                 |
| AspectDef    | Yes     | -         | UUID      | Hash (implicit)   |
| Aspect       | No      | Hierarchy | Entity ID | -                 |
| PropertyDef  | No      | AspectDef | Name      | -                 |
| Property     | No      | Aspect    | Name      | -                 |


Serialization and Persistence
-----------------------------
* Serialization (S11N) and persistence of Cheap Catalogs has some constraints:
  * PropertyDefs have no global ID and must be contained in an AspectDef.
  * Aspects are always stored in an AspectMapHierarchy.
    * Any other Aspect access methods (Entities and Hierarchies) are for convenience and not meant to be persisted.
  * AspectDefs must be serialized/persisted before any Aspects.
* These constraints do not apply to smaller atoms of S11N, i.e., JSON representing a single Aspect only.
* HierarchyDefs have no global ID and must be contained in a CatalogDef.
  * Both types of elements are purely informational and are stored as Aspects only.

Principles
----------
* Cheap does not dictate patterns of network communication or other external resource access.
* Cheap offers utility functions and callback mechanisms to allow applications to use it in conjunction with databases,
filesystems, etc. but it does not mandate their usage.
* Cheap is not concerned with access control, but tries to be flexible enough to accommodate fine-grained access control.


