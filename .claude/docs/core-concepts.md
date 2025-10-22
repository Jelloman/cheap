# Core Concepts

Cheap is analogous to **git** - it's a git-like mechanism for structured data and objects.
Cheap elements are organized into five tiers: Catalogs, Hierarchies, Entities, Aspects, and Properties (the CHEAP acronym).

## Catalogs

Cheap Catalogs are caches or working copies. Every catalog has either an upstream catalog or an external data source (never both).
Each catalog has exactly one "species" that determines its data source relationship and caching behavior:
- `SOURCE` - Read-only cache of an external data source
- `SINK` - Read-write working copy of an external data source
- `MIRROR` - Read-only cache of another catalog (always has same def as upstream)
- `CACHE` - Write-through cache of another catalog (writes may be buffered)
- `CLONE` - Write-back working copy of another catalog with manual reads/writes
- `FORK` - Transient copy of another catalog, severed from original (intended to become a SINK via "Save As")

Each catalog contains:
- An Aspectage - directory of all AspectDefs in the catalog
- Multiple named Hierarchies for organizing entities and aspects

## Hierarchies

Hierarchies organize entities and aspects within a catalog. Each hierarchy has a unique name within its catalog.
There are 5 hierarchy types:
- `ENTITY_LIST` (EL) - Ordered list containing entity IDs, possibly with duplicates
- `ENTITY_SET` (ES) - Possibly-ordered set containing unique entity IDs
- `ENTITY_DIR` (ED) - String-to-entity ID mapping (dictionary-like lookups)
- `ENTITY_TREE` (ET) - Tree structure with named nodes where leaves contain entity IDs
- `ASPECT_MAP` (AM) - Possibly-ordered map of entity IDs to aspects of a single type

AspectMap hierarchy names are always identical to the name of the AspectDef they contain.

## Entities

Entities are conceptual objects - they are nothing but IDs. All entity information is found in hierarchies and aspects.
Entities are analogous to primary keys in database terminology.

Entity IDs can be:
- **Global**: UUIDs used for cross-catalog references (most common)
- **Local**: Implementation-specific references for performance (can lazily generate UUIDs when needed)

Each entity can have multiple Aspects attached, but no more than one of each AspectDef type.

## Aspects

An Aspect is a data record attached to a single Entity. Each aspect:
- Is defined by a single AspectDef which specifies the record fields
- Is stored in an AspectMap hierarchy in a catalog, organized by AspectDef (similar to RDBMS tables)
- Can exist at most once per entity for a given AspectDef

AspectDefs:
- Have a full name (globally unique, using reverse domain name notation)
- Have a UUID, and optionally a URI and version number

## Properties

A Property is a "field" of data within an Aspect. Each property:
- Has a PropertyDef belonging to a single AspectDef
- Has a short name unique within the aspect
- Is either a simple value or an array of simple values (multivalued)
- Is always immutable (multivalued properties are not modified in place)
- Is never structured/nested - complex data should use Aspects or Hierarchies

## Identity and Versioning

**Global IDs:**
- Catalogs and AspectDefs: Always have UUID
- AspectDefs: Also have globally unique name (reverse domain notation)
- Entities: Almost always have UUID (local entities can lazily generate)
- Catalogs: Usually have URL (all elements within are URL-addressable)
- Hierarchies: No global ID, only unique name within catalog
- PropertyDefs: No global ID, only unique name within AspectDef
- Properties: No global ID, only unique name within Aspect

**Versioning:**
- CatalogDefs, HierarchyDefs, AspectDefs: Implicit hash version (like git commits)
- Catalogs and Hierarchies: Explicit integer version (manual increment)
