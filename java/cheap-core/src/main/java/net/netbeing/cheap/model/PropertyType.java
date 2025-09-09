package net.netbeing.cheap.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * The enum Property type.
 */
public enum PropertyType
{
    /**
     * Integer property type.
     */
    Integer("INT", Long.class), // 64-bit signed
    /**
     * Float property type.
     */
    Float("FLT", Double.class), // 64 bits, commonly called "double precision"
    /**
     * Boolean property type.
     */
    Boolean("BLN", Boolean.class), // true, false or null
    /**
     * String property type.
     */
    String("STR", String.class), // Length limited to 8192, processed atomically
    /**
     * Text property type.
     */
    Text("TXT", String.class), // Unlimited length, processed atomically
    /**
     * Big integer property type.
     */
    BigInteger("BIG", String.class), // Unlimited Size
    /**
     * Date time property type.
     */
    DateTime("DAT", String.class), // ISO-8601 Date-Time string
    /**
     * Uri property type.
     */
    URI("URI", String.class), // RFC 3986.  Converting to/from a URI/URL object is left to the application.
    /**
     * Uuid property type.
     */
    UUID("UID", String.class), // RFC 4122. Converting to/from a UUID object is left to the application.
    /**
     * Clob property type.
     */
    CLOB("CLB", ReaderWriter.class), // CLOBs are represented by a Reader or Writer, depending on the operation
    /**
     * Blob property type.
     */
    BLOB("BLB", InputOutput.class), // CLOBs are represented by an InputStream or OutputStream, depending on the operation
    ;

    /**
     * The type Reader writer.
     */
    public record ReaderWriter(Reader reader, Writer writer) {}

    /**
     * The type Input output.
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
     * Type code string.
     *
     * @return the string
     */
    public String typeCode()
    {
        return typeCode;
    }

    /**
     * Gets java class.
     *
     * @return the java class
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
