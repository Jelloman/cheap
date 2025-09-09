package net.netbeing.cheap.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Defines the supported data types for properties in the CHEAP model.
 * Each property type specifies storage characteristics, validation rules,
 * and the corresponding Java class used for value representation.
 * 
 * <p>Property types range from basic primitives to complex streaming types,
 * providing comprehensive data storage capabilities while maintaining
 * type safety and efficient serialization.</p>
 */
public enum PropertyType
{
    /**
     * 64-bit signed integer values. Uses Java Long class for representation
     * to ensure full range support and consistency across platforms.
     */
    Integer("INT", Long.class),
    
    /**
     * 64-bit floating-point values (double precision). Provides standard
     * IEEE 754 double-precision floating-point arithmetic.
     */
    Float("FLT", Double.class),
    
    /**
     * Boolean values supporting true, false, or null states. The null state
     * allows for three-valued logic in data storage and queries.
     */
    Boolean("BLN", Boolean.class),
    
    /**
     * String values with length limited to 8192 characters, processed atomically.
     * Suitable for short text fields, identifiers, and labels where size
     * limits ensure efficient storage and retrieval.
     */
    String("STR", String.class),
    
    /**
     * Text values with unlimited length, processed atomically. Suitable for
     * large text content, documents, and descriptions where size flexibility
     * is more important than storage efficiency.
     */
    Text("TXT", String.class),
    
    /**
     * Arbitrary precision integer values with unlimited size. Stored as
     * strings to avoid platform-specific size limitations and ensure
     * exact precision for mathematical operations.
     */
    BigInteger("BIG", String.class),
    
    /**
     * Date and time values stored as ISO-8601 formatted strings. This ensures
     * timezone information is preserved and provides human-readable storage
     * with standardized parsing support.
     */
    DateTime("DAT", String.class),
    
    /**
     * Uniform Resource Identifier values following RFC 3986 specification.
     * Stored as strings with application-level conversion to/from URI/URL objects
     * to maintain flexibility in handling various URI schemes.
     */
    URI("URI", String.class),
    
    /**
     * Universally Unique Identifier values following RFC 4122 specification.
     * Stored as strings with application-level conversion to/from UUID objects
     * to ensure consistent representation across different systems.
     */
    UUID("UID", String.class),
    
    /**
     * Character Large Object (CLOB) for streaming text data. Represented by
     * Reader for input operations and Writer for output operations, enabling
     * efficient processing of large text content without memory constraints.
     */
    CLOB("CLB", ReaderWriter.class),
    
    /**
     * Binary Large Object (BLOB) for streaming binary data. Represented by
     * InputStream for input operations and OutputStream for output operations,
     * enabling efficient processing of large binary content without memory constraints.
     */
    BLOB("BLB", InputOutput.class)
    ;

    /**
     * Container for CLOB stream operations, providing both input (Reader) and
     * output (Writer) stream access for character-based large object data.
     * 
     * @param reader stream for reading character data from the CLOB
     * @param writer stream for writing character data to the CLOB
     */
    public record ReaderWriter(Reader reader, Writer writer) {}

    /**
     * Container for BLOB stream operations, providing both input (InputStream) and
     * output (OutputStream) stream access for binary large object data.
     * 
     * @param input stream for reading binary data from the BLOB
     * @param output stream for writing binary data to the BLOB
     */
    public record InputOutput(InputStream input, OutputStream output) {}

    private final String typeCode;
    private final Class<?> javaClass;

    PropertyType(String typeCode, Class<?> javaClass)
    {
        this.typeCode = typeCode;
        this.javaClass = javaClass;
    }

    /**
     * Returns the short string code that identifies this property type.
     * These codes are used for serialization, storage, and compact representation.
     *
     * @return the three-character type code for this property type, never null
     */
    public String typeCode()
    {
        return typeCode;
    }

    /**
     * Returns the Java class used to represent values of this property type.
     * This class is used for type checking and serialization operations.
     *
     * @return the Java class that represents this property type's values, never null
     */
    public Class<?> getJavaClass()
    {
        return javaClass;
    }

    public String toString()
    {
        return typeCode;
    }
}
