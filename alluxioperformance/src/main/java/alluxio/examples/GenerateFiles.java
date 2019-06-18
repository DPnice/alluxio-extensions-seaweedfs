package alluxio.examples;

public class GenerateFiles {

    /**
     * 生成指定大小的byte[]数组
     *
     * @return byte[]
     */
    public static byte[] newBytes(int n) {
        int b = n * 1024 * 1024;
        byte[] bytes = new byte[b];

        int index = 0;
        String str = "test";
        int size = 0;
        while (size < b) {
            byte[] testBytes = (str).getBytes();
            for (byte testByte : testBytes) {
                bytes[index] = testByte;
                index++;
            }
            size += testBytes.length;
        }
        return bytes;
    }

}