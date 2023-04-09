package net.yuukosu;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        if (args.length > 1) {
            MethodType methodType = MethodType.matches(args[0]);
            String input = args[1];
            String output = methodType == MethodType.COMPRESS ? input + ".yc" : input + ".txt";

            if (args.length > 2) {
                output = args[2];
            }

            File inputFile = new File(input);
            File outputFile = new File(output);

            if (!inputFile.exists()) {
                printStatus(2, "ファイルが存在しません。");
                return;
            }

            if (outputFile.exists()) {
                printStatus(2, "ファイルが存在します。");
                return;
            }

            long start = System.currentTimeMillis();
            printStatus(0, methodType.getName() + "中...");
            try (FileInputStream fileInputStream = new FileInputStream(inputFile);
                 FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                if (methodType == MethodType.COMPRESS) {
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
                }

                if (methodType == MethodType.DECOMPRESS) {
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
                printStatus(2, methodType.getName() + "に失敗しました。");
                e.printStackTrace();
                return;
            }

            printStatus(1, methodType.getName() + "に成功しました。");
            printStatus(0, methodType.getName() + "時間: " + String.format("%,d", (System.currentTimeMillis() - start)) + "ms");
            return;
        }

        printStatus(0, "----- YuukosuCompressor -----");
        printStatus(0, "使い方:");
        printStatus(0, "圧縮: java -jar YuukosuCompressor.jar c <圧縮するファイル.txt> [<出力パス>]");
        printStatus(0, "展開: java -jar YuukosuCompressor.jar d <展開するファイル.yc> [<出力パス>]");
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

    private enum MethodType {
        COMPRESS("圧縮", "^compress$|^c$"),
        DECOMPRESS("展開", "^decompress$|^d$");

        private final String name;
        private final String pattern;

        MethodType(String name, String pattern) {
            this.name = name;
            this.pattern = pattern;
        }

        public static MethodType matches(String input) {
            return Arrays.stream(MethodType.values()).filter(methodType -> Pattern.matches(methodType.getPattern(), input)).findFirst().orElse(null);
        }

        public String getName() {
            return this.name;
        }

        public String getPattern() {
            return this.pattern;
        }
    }
}
