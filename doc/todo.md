# CHEAP To-Do
***

## TODAY
* Convert docs to MD
* Flesh out design doc
  * S11N/Persistence section
    * AspectDefs 2nd
    * All defs can have UUID and possibly URI (if URI there must be a UUID)
  * List of modules
    * core
      * includes FS and SQLite support
    * db
      * PG
      * MySQL/Maria
    * json
    * net


## GLOBAL
* Implement root catalog for DB
* Implement root catalog for filesystem
* cheap.net package
    * Proxy catalog for remote catalog
    * Catalog client and server implementations
    * Multiple protocols
        * JSON-over-HTTP
        * Protocol buffers
        * Cap'n Proto & Web

* Pansh project
    - Command line
    - 26 commands that give NYI response
    - Implement Copy

Code:
* Make Hierarchy impls threadsafe (or safe/nonsafe variants).
* Global Cheap config class
* Add Catalog lookup mechanism

Features:
* Spec kit?
* Design Postgres and Sqlite schemas for storing CHEAP
* PropertyMapper integration



## Research
Look into KiloCode
Look into OpenHands
Look into Arkalos - https://arkalos.com/
[ZZLook into Roocode https://roocode.com/ ]
[ZZLook into Cline https://cline.bot/ ]


## Archive

### 9/24


### Thru 9/23 unordered
Add AspectDef lookup to Factory.
Add AspectDef to Catalog properly when aspects are added.
Update serializers and their tests to put aspectDefs before hierarchies.
Retire both of the default hierarchies being hierarchies, make them part of Catalog. Neither hierarchy type is needed.
Add aspects to catalog not hierarchy


### TODO 8/15
* DBUtil

### TODO 7/1
X * FileUtil
X    - File record
X    - Directory record
X    - Create records for one directory
X    - Walker record-creation wrapping nio.Files walker.

### TODO 6/25
X * LocalEntity class which contains multiple AspectDef/Aspect pairs
X * Simplify Entity -localId, replace localRef with LocalEntity class

### TODO 6/14
X * Convert project to maven
X * Implement all 5 types of hierarchy generically

### TODO 6/13
X * Refactor method maps into aspect *defs*

### TODO 6/10/25
XX * Fork and expand RecordAspect into a writable PojoAspect
XX * Continue to expand Hierarchy & Catalog interfaces










