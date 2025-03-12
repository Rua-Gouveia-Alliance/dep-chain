package group13.depchain.util;

import java.util.Arrays;

public class Message {

    static public String appendEnd(String msg, String... elements) {
        for (String e : elements)
            msg += "\n" + e;

        return msg;
    }

    static public String appendStart(String msg, String... elements) {
        String finalMsg = "";
        for (String e : elements)
            finalMsg += e + "\n";

        finalMsg += msg;
        return finalMsg;
    }

    static public String appendEnd(String msg, int... elements) {
        for (int e : elements)
            msg += "\n" + e;

        return msg;
    }

    static public String appendStart(String msg, int... elements) {
        String finalMsg = "";
        for (int e : elements)
            finalMsg += e + "\n";

        finalMsg += msg;
        return finalMsg;
    }

    static public String[] extractEnd(String[] split, int count) {
        if (count > split.length)
            return null;

        String[] result = new String[count + 1];
        if (count == split.length) {
            result[0] = null;
        } else {
            result[0] = String.join("\n", Arrays.copyOfRange(split, 0, split.length - count));
        }

        for (int i = 1; i < count + 1; ++i)
            result[i] = split[split.length - i];

        return result;
    }
}
