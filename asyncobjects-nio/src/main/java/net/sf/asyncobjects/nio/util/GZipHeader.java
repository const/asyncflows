package net.sf.asyncobjects.nio.util; // NOPMD

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqForUnit;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The GZip Header data.The format is specified in
 * <a href="http://tools.ietf.org/html/rfc1952">RFC 1952: GZIP file format specification version 4.3</a>.
 *
 * @see GZipInput
 * @see GZipOutput
 */
public final class GZipHeader {
    /**
     * The deflate compression method for the GZip.
     */
    public static final byte DEFLATE_COMPRESSION = 0x08;
    /**
     * Compressor used maximum compression, slowest algorithm.
     */
    public static final byte MAXIMUM_COMPRESSION = 2;
    /**
     * Compressor used fastest algorithm.
     */
    public static final byte FASTEST_COMPRESSION = 4;
    /**
     * The unknown OS.
     */
    public static final byte UNKNOWN_OS = -1;
    /**
     * The length of the footer.
     */
    public static final int FOOTER_LENGTH = 8;
    /**
     * The magic number (2 bytes).
     */
    private static final char ID = 0x8b1f;
    /**
     * If FTEXT is set, the file is probably ASCII text.
     */
    private static final byte FTEXT = 1;
    /**
     * If FHCRC is set, a CRC16 for the gzip header is present,
     * immediately before the compressed data. The CRC16 consists
     * of the two least significant bytes of the CRC32 for all
     * bytes of the gzip header up to and not including the CRC16.
     * [The FHCRC bit was never set by versions of gzip up to
     * 1.2.4, even though it was documented with a different
     * meaning in gzip 1.2.4.]
     */
    private static final byte FHCRC = 2;
    /**
     * If FEXTRA is set, optional extra fields are present.
     */
    private static final byte FEXTRA = 4;
    /**
     * If FNAME is set, an original file name is present,
     * terminated by a zero byte.  The name must consist of ISO
     * 8859-1 (LATIN-1) characters; on operating systems using
     * EBCDIC or any other character set for file names, the name
     * must be translated to the ISO LATIN-1 character set.
     */
    private static final byte FNAME = 8;
    /**
     * If FCOMMENT is set, a zero-terminated file comment is
     * present.  This comment is not interpreted; it is only
     * intended for human consumption.  The comment must consist of
     * ISO 8859-1 (LATIN-1) characters.  Line breaks should be
     * denoted by a single line feed character (10 decimal).
     */
    private static final byte FCOMMENT = 16;
    /**
     * The mask for supported flags.
     */
    private static final byte SUPPORTED_FLAGS = 0x1F;
    /**
     * The minimal size of GZip header.
     */
    private static final int CORE_HEADER_SIZE = 9;
    /**
     * Extra fields.
     */
    private final List<ExtraField> extraFields = new LinkedList<ExtraField>();
    /**
     * The compression method.
     */
    private byte compressionMethod = DEFLATE_COMPRESSION;
    /**
     * The GZip flags.The field is actually ignored.during processing.
     */
    private byte flags;
    /**
     * The modification time.
     */
    private long modificationTime;
    /**
     * The extra flags.
     */
    private byte extraFlags = MAXIMUM_COMPRESSION;
    /**
     * The operating system.
     */
    private byte operatingSystem = UNKNOWN_OS;
    /**
     * The name.
     */
    private String name;
    /**
     * The comment.
     */
    private String comment;
    /**
     * If true, header CRC is generated during write or checked during read.
     */
    private boolean headerChecked;
    /**
     * If true, the file is ascii.
     */
    private boolean text;

    /**
     * Read GZip header from the context. Note that header might be already read. So the operation might return
     * with resolved promise.
     *
     * @param context the context
     * @return the GZip header value.
     */
    public static Promise<GZipHeader> read(final ByteParserContext context) {
        final GZipHeader header = new GZipHeader();
        final CRC32 headerCRC = new CRC32();
        return context.ensureAvailable(CORE_HEADER_SIZE).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return readCoreHeader(context, headerCRC, header);
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FEXTRA) == 0) {
                    return aVoid();
                }
                return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH).thenDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return readExtraFields(header, context, headerCRC);
                    }
                });
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FNAME) == 0) {
                    return aVoid();
                }
                return readStringZero(context, headerCRC).map(new AFunction<Void, String>() {
                    @Override
                    public Promise<Void> apply(final String value) throws Throwable {
                        header.setName(value);
                        return aVoid();
                    }
                });
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FCOMMENT) == 0) {
                    return aVoid();
                }
                return readStringZero(context, headerCRC).map(new AFunction<Void, String>() {
                    @Override
                    public Promise<Void> apply(final String value) throws Throwable {
                        header.setComment(value);
                        return aVoid();
                    }
                });
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if (!header.isHeaderChecked()) {
                    return aVoid();
                }
                return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH).thenDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        final int current = (char) headerCRC.getValue();
                        final int crc = getUInt16(context, headerCRC);
                        if (crc != current) {
                            throw new IOException(String.format(
                                    "Header CRC does not match: %04x != %04x ", current, crc));
                        }
                        return aVoid();
                    }
                });
            }
        }).thenValue(header);
    }

    /**
     * Read core header.
     *
     * @param context   the context
     * @param headerCRC the CRC
     * @param header    the header to fill
     * @return the promise that resolves when core header is read
     * @throws IOException in case of format problem.
     */
    private static Promise<Void> readCoreHeader(final ByteParserContext context, final CRC32 headerCRC,
                                                final GZipHeader header) throws IOException {
        final int magic = getUInt16(context, headerCRC);
        if (magic != ID) {
            throw new IOException(String.format("Not a GZip format: %04x", magic));
        }
        header.setCompressionMethod(get(context, headerCRC));
        if (header.getCompressionMethod() != DEFLATE_COMPRESSION) {
            throw new IOException("Unsupported compression method: " + header.getCompressionMethod());
        }
        header.setFlags(get(context, headerCRC));
        if ((~SUPPORTED_FLAGS & header.getFlags()) != 0) {
            throw new IOException(String.format("Unsupported flags are detected: %02x",
                    header.getFlags() & ByteIOUtil.BYTE_MASK));
        }
        header.setHeaderChecked((header.getFlags() & FHCRC) != 0);
        header.setText((header.getFlags() & FTEXT) != 0);
        header.setModificationTime(TimeUnit.SECONDS.toMillis(getUInt32(context, headerCRC)));
        header.setExtraFlags(get(context, headerCRC));
        if (header.getExtraFlags() != FASTEST_COMPRESSION && header.getExtraFlags() != MAXIMUM_COMPRESSION) {
            throw new IOException("Unknown extra flags value: " + Integer.toHexString(header.getExtraFlags()));
        }
        header.setOperatingSystem(get(context, headerCRC));
        return aVoid();
    }

    /**
     * Write GZip header.
     *
     * @param context the context
     * @param header  the header
     * @return the promise that resolves when header is added to the context
     */
    public static Promise<Void> writeHeader(final ByteGeneratorContext context, final GZipHeader header) {
        final CRC32 crc = new CRC32();
        return context.ensureAvailable(CORE_HEADER_SIZE).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return writeCoreHeader(header, context, crc);
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if (header.getExtraFields().isEmpty()) {
                    return aVoid();
                }
                return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH).thenDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        int size = 0;
                        for (final ExtraField extraField : header.getExtraFields()) {
                            size += extraField.size();
                        }
                        if (size > ByteIOUtil.UINT16_MASK) {
                            throw new IOException("Extra fields are too large: " + size);
                        }
                        putUInt16(context, crc, (char) size);
                        return aSeqForUnit(header.getExtraFields(), new AFunction<Boolean, ExtraField>() {
                            @Override
                            public Promise<Boolean> apply(final ExtraField field) throws Throwable {
                                return writeExtraField(field, context, crc).thenValue(true);
                            }
                        });
                    }
                });
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FNAME) == 0) {
                    return aVoid();
                }
                return writeStringZero(context, crc, header.getName());
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FCOMMENT) == 0) {
                    return aVoid();
                }
                return writeStringZero(context, crc, header.getComment());
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                if ((header.getFlags() & FHCRC) == 0) {
                    return aVoid();
                }
                return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH).thenDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        final char current = (char) crc.getValue();
                        putUInt16(context, crc, current);
                        return aVoid();
                    }
                });
            }
        });
    }

    /**
     * Write core header.
     *
     * @param context the context
     * @param crc     the CRC
     * @param header  the header to write
     * @return the promise that resolves when core header is written
     * @throws IOException in case of format problem.
     */
    private static Promise<Void> writeCoreHeader(final GZipHeader header, final ByteGeneratorContext context,
                                                 final CRC32 crc) throws IOException {
        header.setRealFlags();
        if (header.getCompressionMethod() != DEFLATE_COMPRESSION) {
            throw new IOException("Unsupported compression method: " + header.getCompressionMethod());
        }
        if ((~SUPPORTED_FLAGS & header.getFlags()) != 0) {
            throw new IOException(String.format("Unsupported flags are detected: %02x",
                    header.getFlags() & ByteIOUtil.BYTE_MASK));
        }
        if (header.getExtraFlags() != FASTEST_COMPRESSION && header.getExtraFlags() != MAXIMUM_COMPRESSION) {
            throw new IOException("Unknown extra flags value: " + Integer.toHexString(header.getExtraFlags()));
        }
        putUInt16(context, crc, ID);
        put(context, crc, header.getCompressionMethod());
        put(context, crc, header.getFlags());
        putInt32(context, crc, (int) TimeUnit.MILLISECONDS.toSeconds(header.getModificationTime()));
        put(context, crc, header.getExtraFlags());
        put(context, crc, header.getOperatingSystem());
        return aVoid();
    }

    /**
     * Write a single extra field.
     *
     * @param extraField the extra field to write
     * @param context    the context
     * @param headerCRC  the header CRC value
     * @return the promise that resolves when field is written
     */
    private static Promise<Void> writeExtraField(final ExtraField extraField, final ByteGeneratorContext context,
                                                 final CRC32 headerCRC) {
        return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH + ByteIOUtil.UINT16_LENGTH).thenDo(
                new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        if (extraField.getData().length() > ByteIOUtil.UINT16_MASK) {
                            throw new IOException("Extra field too big: " + extraField.size());
                        }
                        putUInt16(context, headerCRC, extraField.getId());
                        putUInt16(context, headerCRC, (char) extraField.getData().length());
                        return writeLatin1(context, headerCRC, extraField.getData(), true);
                    }
                });

    }

    /**
     * Write a zero terminated string.
     *
     * @param context   the context
     * @param headerCRC the header CRC value
     * @param text      the text to write
     * @return the promise that resolves when field is written
     */
    private static Promise<Void> writeStringZero(final ByteGeneratorContext context,
                                                 final CRC32 headerCRC, final String text) {
        return writeLatin1(context, headerCRC, text, false).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return context.ensureAvailable(1);
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                put(context, headerCRC, (byte) 0);
                return aVoid();
            }
        });

    }

    /**
     * Write a latin-1 string data.
     *
     * @param context   the context
     * @param headerCRC the header CRC value
     * @param text      the text to write
     * @param allowZero if true, zeros are allowed in the string
     * @return the promise that resolves when field is written
     */
    private static Promise<Void> writeLatin1(final ByteGeneratorContext context,
                                             final CRC32 headerCRC, final String text, final boolean allowZero) {
        return aSeqLoop(new ACallable<Boolean>() {
            private int pos;

            @Override
            public Promise<Boolean> call() throws Throwable {
                final ByteBuffer buffer = context.buffer();
                while (buffer.hasRemaining() && pos < text.length()) {
                    final char c = text.charAt(pos++);
                    if (c == 0 && !allowZero) {
                        throw new IOException("Zero is not allowed inside string here: " + text);
                    }
                    if (c > ByteIOUtil.BYTE_MASK) {
                        throw new IOException("Non-Latin-1 text is encountered: " + text);
                    }
                    put(context, headerCRC, (byte) c);
                }
                if (pos == text.length()) {
                    return aFalse();
                }
                return context.send();
            }
        });
    }

    /**
     * Read extra fields into the header.
     *
     * @param header    the header
     * @param context   the context
     * @param headerCRC the CRC to update
     * @return a void when reading finishes.
     */
    private static Promise<Void> readExtraFields(final GZipHeader header, final ByteParserContext context,
                                                 final CRC32 headerCRC) {
        final int totalSize = getUInt16(context, headerCRC);
        final int[] currentSize = new int[1];
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                if (currentSize[0] == totalSize) {
                    return aFalse();
                }
                return readExtraField(context, headerCRC, totalSize - currentSize[0]).map(
                        new AFunction<Boolean, ExtraField>() {
                            @Override
                            public Promise<Boolean> apply(final ExtraField value) throws Throwable {
                                currentSize[0] += value.size();
                                header.getExtraFields().add(value);
                                return aTrue();
                            }
                        });
            }
        });
    }

    /**
     * Get 16-bit unsigned value.
     *
     * @param context   the context
     * @param headerCRC the CRC to update
     * @return the 16-bit value
     */
    private static char getUInt16(final ByteParserContext context, final CRC32 headerCRC) {
        final byte b1 = get(context, headerCRC);
        final byte b2 = get(context, headerCRC);
        return ByteIOUtil.toChar(b2, b1);
    }

    /**
     * Get 32-bit unsigned value.
     *
     * @param context   the context
     * @param headerCRC the CRC to update
     * @return the 32-bit value as long
     */
    private static long getUInt32(final ByteParserContext context, final CRC32 headerCRC) {
        final byte b1 = get(context, headerCRC);
        final byte b2 = get(context, headerCRC);
        final byte b3 = get(context, headerCRC);
        final byte b4 = get(context, headerCRC);
        return ByteIOUtil.toUInt32(b4, b3, b2, b1);
    }

    /**
     * Get single byte from the buffer.
     *
     * @param context the context
     * @param crc     the CRC
     * @return the byte
     */
    private static byte get(final ByteParserContext context, final CRC32 crc) {
        final byte b = context.buffer().get();
        crc.update(b);
        return b;
    }

    /**
     * Put a single byte into the buffer.
     *
     * @param context the context
     * @param crc     the crc
     * @param b       the single byte
     */
    private static void put(final ByteGeneratorContext context, final CRC32 crc, final byte b) {
        crc.update(b);
        context.buffer().put(b);
    }

    /**
     * Put two bytes into the buffer.
     *
     * @param context the context
     * @param crc     the crc
     * @param b       the single byte
     */
    private static void putUInt16(final ByteGeneratorContext context, final CRC32 crc, final char b) {
        // CHECKSTYLE:OFF
        final byte b1 = (byte) b;
        final byte b2 = (byte) (b >> 8);
        // CHECKSTYLE:ON
        crc.update(b1);
        crc.update(b2);
        final ByteBuffer buffer = context.buffer();
        buffer.put(b1);
        buffer.put(b2);
    }

    /**
     * Put two bytes into the buffer.
     *
     * @param context the context
     * @param crc     the crc
     * @param b       the single byte
     */
    private static void putInt32(final ByteGeneratorContext context, final CRC32 crc, final int b) {
        // CHECKSTYLE:OFF
        final byte b1 = (byte) b;
        final byte b2 = (byte) (b >> 8);
        final byte b3 = (byte) (b >> 16);
        final byte b4 = (byte) (b >> 24);
        // CHECKSTYLE:ON
        crc.update(b1);
        crc.update(b2);
        crc.update(b3);
        crc.update(b4);
        final ByteBuffer buffer = context.buffer();
        buffer.put(b1);
        buffer.put(b2);
        buffer.put(b3);
        buffer.put(b4);
    }

    /**
     * Read zero terminating string.
     *
     * @param context the input context.
     * @param crc     the CRC to update
     * @return the promise for the string
     */
    private static Promise<String> readStringZero(final ByteParserContext context, final CRC32 crc) {
        final StringBuilder rc = new StringBuilder();
        return aSeq(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aSeqLoop(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        final ByteBuffer buffer = context.buffer();
                        while (true) {
                            if (!buffer.hasRemaining()) {
                                return context.readMore();
                            }
                            final byte b = buffer.get();
                            if (crc != null) {
                                crc.update(b);
                            }
                            if (b == 0) {
                                return aFalse();
                            } else {
                                rc.append(ByteIOUtil.toLatin1(b));
                            }
                        }
                    }
                });
            }
        }).thenDoLast(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Throwable {
                return aValue(rc.toString());
            }
        });
    }

    /**
     * Read string with the specified short size.
     *
     * @param context the input context
     * @param crc     the crc code to update
     * @param limit   the limit for the string
     * @return the string.
     */
    private static Promise<ExtraField> readExtraField(final ByteParserContext context, final CRC32 crc,
                                                      final int limit) {
        final StringBuilder rc = new StringBuilder();
        final char[] id = new char[1];

        return aSeq(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return context.ensureAvailable(ByteIOUtil.UINT16_LENGTH + ByteIOUtil.UINT16_LENGTH);
            }
        }).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                id[0] = getUInt16(context, crc);
                final int size = getUInt16(context, crc);
                if (size + ByteIOUtil.UINT16_LENGTH + ByteIOUtil.UINT16_LENGTH > limit) {
                    throw new IOException("Field size is too big: " + size);
                }
                return aSeqLoop(new ACallable<Boolean>() {
                    private int count;

                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        final ByteBuffer buffer = context.buffer();
                        while (count < size) {
                            if (!buffer.hasRemaining()) {
                                return context.readMore();
                            }
                            final byte b = buffer.get();
                            if (crc != null) {
                                crc.update(b);
                            }
                            count++;
                            rc.append(ByteIOUtil.toLatin1(b));
                        }
                        return aFalse();
                    }
                });
            }
        }).thenDoLast(new ACallable<ExtraField>() {
            @Override
            public Promise<ExtraField> call() throws Throwable {
                return aValue(new ExtraField(id[0], rc.toString()));
            }
        });

    }

    /**
     * @return the extra fields for the header
     */
    public List<ExtraField> getExtraFields() {
        return extraFields;
    }

    /**
     * @return the compression method
     */
    public byte getCompressionMethod() {
        return compressionMethod;
    }

    /**
     * Set compression method (only {@link #DEFLATE_COMPRESSION} is actually supported).
     *
     * @param compressionMethod the compression method.
     */
    public void setCompressionMethod(final byte compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    /**
     * @return gzip flags
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * Set GZip flags. Note that some flags are calculated depending on field values during write.
     * So here only non-derived flags are used.
     *
     * @param flags the flags
     */
    public void setFlags(final byte flags) {
        this.flags = flags;
    }

    /**
     * Set derived flags value basing on fields.
     */
    public void setRealFlags() {  // NOPMD
        flags = isText() ? FTEXT : 0;
        flags |= isHeaderChecked() ? FHCRC : 0;
        flags |= getExtraFields().isEmpty() ? 0 : FEXTRA;
        flags |= getName() == null ? 0 : FNAME;
        flags |= getComment() == null ? 0 : FCOMMENT;

    }

    /**
     * @return the modification time
     */
    public long getModificationTime() {
        return modificationTime;
    }

    /**
     * Set modification time. The time will will ignore seconds.
     *
     * @param modificationTime the modification time
     */
    public void setModificationTime(final long modificationTime) {
        this.modificationTime = modificationTime;
    }

    /**
     * @return gzip extra flags
     */
    public byte getExtraFlags() {
        return extraFlags;
    }

    /**
     * Set extra flags (for deflate: {@link #MAXIMUM_COMPRESSION} or {@link #FASTEST_COMPRESSION}).
     *
     * @param extraFlags the extra flags.
     */
    public void setExtraFlags(final byte extraFlags) {
        this.extraFlags = extraFlags;
    }

    /**
     * @return the operating system code
     */
    public byte getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * Set operating system.
     *
     * @param operatingSystem the operating system.
     */
    public void setOperatingSystem(final byte operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    /**
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Set file name (ISO-8859-1).
     *
     * @param name the name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the comment for the file
     */
    public String getComment() {
        return comment;
    }

    /**
     * Set comment (ISO-8859-1).
     *
     * @param comment the comment text
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * @return true if CRC was read or will be created for the header.
     */
    public boolean isHeaderChecked() {
        return headerChecked;
    }

    /**
     * @param headerChecked set header check status.
     */
    public void setHeaderChecked(final boolean headerChecked) {
        this.headerChecked = headerChecked;
    }

    /**
     * @return true if the file is supposed to be ASCII
     */
    public boolean isText() {
        return text;
    }

    /**
     * Set ASCII flag for the file.
     *
     * @param isAscii the flag value
     */
    public void setText(final boolean isAscii) {
        this.text = isAscii;
    }

    @Override
    public String toString() {
        return "GZipHeader{" + "extraFields=" + extraFields + ", compressionMethod=" + compressionMethod + ", flags="
                + flags + ", modificationTime=" + modificationTime + ", extraFlags=" + extraFlags
                + ", operatingSystem=" + operatingSystem + ", name='" + name + '\'' + ", comment='"
                + comment + '\'' + ", headerChecked=" + headerChecked + ", text=" + text + '}';
    }

    /**
     * The extra field structure.
     */
    public static final class ExtraField {
        /**
         * The field id.
         */
        private final char id;
        /**
         * The field data. Bytes are encoded as unicode code points.
         */
        private final String data;

        /**
         * The constructor.
         *
         * @param id   the extra field id.
         * @param data the extra field data.
         */
        public ExtraField(final char id, final String data) {
            this.id = id;
            this.data = data;
        }

        /**
         * @return the id of the field
         */
        public char getId() {
            return id;
        }

        /**
         * @return the total field size
         */
        private int size() {
            return ByteIOUtil.UINT16_LENGTH + ByteIOUtil.UINT16_LENGTH + data.length();
        }

        /**
         * @return the data of the field
         */
        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            return "ExtraField{" + "id=" + id + ", data='" + data + '\'' + '}';
        }
    }
}
