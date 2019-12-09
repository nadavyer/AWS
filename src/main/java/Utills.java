import java.io.IOException;
import java.io.PrintWriter;

import static java.lang.Thread.sleep;

public class Utills {

    public static String uncapitalizeChars(String s) {
        return s.replace('A','a').
                replace('B','b').replace('C','c').replace('D','d').replace('E','e')
                .replace('F','f').replace('G','g').replace('H','h').replace('I','i')
                .replace('J','j').replace('K','k').replace('L','l').replace('M','m')
                .replace('N','n').replace('O','o').replace('P','p').replace('Q','q')
                .replace('R','r').replace('S','s').replace('T','t').replace('U','u')
                .replace('V','v').replace('W','w').replace('X','x').replace('Y','y')
                .replace('Z','z');
    }

    public static void sleepMs(int ms) {
        try {
            sleep(ms);
        }
        catch (InterruptedException ignored) {}
    }

    public static void stringToText(String filename, String outputText) throws IOException {
        try {
            PrintWriter pw = new PrintWriter(filename + ".txt");
            pw.println(outputText);
            pw.close();
        } catch (IOException e) {
            throw new IOException("stringToText fail");
        }
    }
}
