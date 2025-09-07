package net.netbeing.cheap.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public enum PropertyType
{
    Integer("INT", Long.class), // 64-bit signed
    Float("FLT", Double.class), // 64 bits, commonly called "double precision"
    Boolean("BLN", Boolean.class), // true, false or null
    String("STR", String.class), // Length limited to 8192, processed atomically
    Text("TXT", String.class), // Unlimited length, processed atomically
    BigInteger("BIG", String.class), // Unlimited Size
    DateTime("DAT", String.class), // ISO-8601 Date-Time string
    URI("URI", String.class), // RFC 3986.  Converting to/from a URI/URL object is left to the application.
    UUID("UID", String.class), // RFC 4122. Converting to/from a UUID object is left to the application.
    CLOB("CLB", ReaderWriter.class), // CLOBs are represented by a Reader or Writer, depending on the operation
    BLOB("BLB", InputOutput.class), // CLOBs are represented by an InputStream or OutputStream, depending on the operation
    ;

    public record ReaderWriter(Reader reader, Writer writer) {}
    public record InputOutput(InputStream input, OutputStream output) {}

    private final String typeCode;
    private final Class<?> javaClass;

    PropertyType(String typeCode, Class<?> javaClass)
    {
        this.typeCode = typeCode;
        this.javaClass = javaClass;
    }

    public String typeCode()
    {
        return typeCode;
    }

    public Class<?> getJavaClass()
    {
        return javaClass;
    }

    public String toString()
    {
        return typeCode;
    }
}
