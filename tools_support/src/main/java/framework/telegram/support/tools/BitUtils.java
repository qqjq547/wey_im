package framework.telegram.support.tools;

public class BitUtils {
    public BitUtils() {
    }

    public static byte getBitValue(byte source, int pos) {
        return (byte) (source >> pos & 1);
    }

    public static byte setBitValue(byte source, int pos, byte value) {
        byte mask = (byte) (1 << pos);
        if (value > 0) {
            source |= mask;
        } else {
            source = (byte) (source & ~mask);
        }

        return source;
    }

    public static int setBitValue(int source,int index, int pos, boolean value) {
        byte b;
        if (value){
            b=1;
        }else {
            b=0;
        }
        byte[] data = Helper.int2Bytes(source);
        data[index] = BitUtils.setBitValue(data[index], pos, b);
        return Helper.bytes2Int(data);
    }

    public static byte reverseBitValue(byte source, int pos) {
        byte mask = (byte) (1 << pos);
        return (byte) (source ^ mask);
    }

    public static boolean checkBitValue(byte source, int pos) {
        source = (byte) (source >>> pos);
        return (source & 1) == 1;
    }

    public static void main(String[] args) {
        byte source = 11;

        byte i;
        for (i = 7; i >= 0; --i) {
            System.out.printf("%d ", getBitValue(source, i));
        }

        System.out.println("\n" + setBitValue(source, 6, (byte) 1));
        System.out.println(reverseBitValue(source, 6));
        System.out.println(checkBitValue(source, 6));

        for (i = 0; i < 8; ++i) {
            if (checkBitValue(source, i)) {
                System.out.printf("%d ", i);
            }
        }

    }
}
