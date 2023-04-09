package net.yuukosu;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        if (args.length > 1) {
            String method = args[0];
            boolean compress = Pattern.matches("c|compress", method);
            String methodName = compress ? "圧縮" : "展開";
            File output = new File(compress ? "compress.yc" : "decompress.txt");
            File file = new File(args[1]);

            if (!file.exists()) {
                printStatus(2, "ファイルが存在しません。");
                return;
            }

            if (output.exists()) {
                if (output.delete()) {
                    printStatus(3, output.getName() + " deleted.");
                }
            }

            long start = System.currentTimeMillis();
            printStatus(0, methodName + "中...");
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 FileOutputStream fileOutputStream = new FileOutputStream(output)) {
                if (compress) {
                    int lastBuf = 0xFFFFFFFF;
                    int buf;
                    int count = 0;

                    while ((buf = fileInputStream.read()) != -1 || count != 0) {
                        if (lastBuf == 0xFFFFFFFF) {
                            lastBuf = buf;
                            continue;
                        }

                        count += 1;

                        if (buf != lastBuf) {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(5);
                            byteBuffer.put((byte) lastBuf);
                            byteBuffer.putInt(count);
                            fileOutputStream.write(byteBuffer.array());

                            lastBuf = buf;
                            count = 0;
                        }
                    }

                    fileOutputStream.flush();
                } else {
                    int length = 5;
                    for (int i = 0; i < fileInputStream.getChannel().size(); i += length) {
                        byte[] ch = new byte[1];
                        byte[] le = new byte[4];

                        int ignored1 = fileInputStream.read(ch);
                        int ignored2 = fileInputStream.read(le);

                        int count = Integer.parseInt(Integer.toHexString(ByteBuffer.wrap(le).getInt()), 16);

                        for (int j = 0; j < count; j++) {
                            fileOutputStream.write(ch);
                        }
                    }
                    fileOutputStream.flush();
                }
            } catch (IOException e) {
                printStatus(2, methodName + "に失敗しました。");
                e.printStackTrace();
                return;
            }

            printStatus(1, methodName + "に成功しました。");
            printStatus(0, methodName + "時間: " + String.format("%,d", (System.currentTimeMillis() - start)) + "ms");
        }
    }

    public static void printStatus(int status, String message) {
        String prefix = "[*]";

        switch (status) {
            case 1:
                prefix = "[+]";
                break;
            case 2:
                prefix = "[-]";
                break;
            case 3:
                prefix = "[?]";
                break;
            case 4:
                prefix = "[DEBUG]";
                break;
        }

        System.out.printf("%s %s\n", prefix, message);
    }
}
