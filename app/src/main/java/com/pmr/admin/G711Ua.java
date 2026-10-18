package com.pmr.admin;

public class G711Ua {
    private static final short[] aLawToLinear = new short[256];
    private static final byte[] linearToALaw = new byte[65536];

    static {
        for (int i = 0; i < 256; i++) {
            aLawToLinear[i] = aLaw2linear((byte) i);
        }
        for (int i = -32768; i <= 32767; i++) {
            linearToALaw[i + 32768] = linear2aLaw((short) i);
        }
    }

    public void encode(byte[] src, int offset, int length, byte[] dst) {
        for (int i = 0; i < length; i += 2) {
            short sample = (short) ((src[offset + i] & 0xFF) | (src[offset + i + 1] << 8));
            dst[i / 2] = linearToALaw[sample + 32768];
        }
    }

    public void decode(byte[] src, int offset, int length, byte[] dst) {
        for (int i = 0; i < length; i++) {
            short sample = aLawToLinear[src[offset + i] & 0xFF];
            dst[i * 2] = (byte) (sample & 0xFF);
            dst[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
    }

    private static short aLaw2linear(byte aLaw) {
        int aLawValue = aLaw ^ 0x55;
        int sign = (aLawValue & 0x80) != 0 ? -1 : 1;
        int exponent = (aLawValue & 0x70) >> 4;
        int mantissa = aLawValue & 0x0F;
        int magnitude = mantissa << 4;
        if (exponent != 0) {
            magnitude += 0x100;
            magnitude <<= (exponent - 1);
        }
        return (short) (sign * (magnitude + 8));
    }

    private static byte linear2aLaw(short sample) {
        int sign = (sample >> 8) & 0x80;
        if (sign != 0) sample = (short) -sample;
        if (sample > 32635) sample = 32635;
        sample += 0x84;

        int exponent = 7;
        for (int expMask = 0x4000; (sample & expMask) == 0; expMask >>= 1) {
            exponent--;
        }
        int mantissa = (sample >> (exponent + 3)) & 0x0F;
        return (byte) (sign | (exponent << 4) | mantissa);
    }
}