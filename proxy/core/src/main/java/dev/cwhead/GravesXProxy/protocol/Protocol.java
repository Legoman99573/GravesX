package dev.cwhead.GravesXProxy.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Wire protocol shared between the GravesX backend plugin and the proxy plugin.
 * Keep this file in sync with dev.cwhead.GravesX.proxy.ProxyProtocol in the GravesX plugin.
 */
public final class Protocol {
    public static final String CHANNEL = "gravesx:proxy";
    public static final int VERSION = 1;

    /**
     * Zero UUID meaning "the newest grave".
     */
    public static final UUID LATEST = new UUID(0L, 0L);
    public static final byte HELLO = 0x01;
    public static final byte GRAVES_UPDATE = 0x02;
    public static final byte TELEPORT_REQUEST = 0x03;
    public static final byte NETWORK_REQUEST = 0x04;
    public static final byte QUERY = 0x11;
    public static final byte TELEPORT_EXECUTE = 0x12;
    public static final byte NETWORK_RESPONSE = 0x13;
    public static final byte ACK = 0x14;

    private Protocol() {}

    public record GraveSummary(UUID id, String world, double x, double y, double z, long created) {
        public void write(DataOutputStream out) throws IOException {
            writeUuid(out, id);
            out.writeUTF(world);
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeLong(created);
        }

        public static GraveSummary read(DataInputStream in) throws IOException {
            return new GraveSummary(readUuid(in), in.readUTF(), in.readDouble(), in.readDouble(), in.readDouble(), in.readLong());
        }
    }

    public static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }
}
