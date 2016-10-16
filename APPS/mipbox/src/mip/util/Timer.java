package mip.util;

import java.util.concurrent.ConcurrentHashMap;
import static mip.util.DGBUtils.DBG;

public class Timer {

    private static final ConcurrentHashMap<Integer, Long> TIME_MAP = new ConcurrentHashMap<>(10);

    private static String format(double ms) {
        String ret;

        if (ms < 1000) {
            ret = String.format("%d ms ", (int) ms);
        } else if (ms < 60000) {
            ret = String.format("%d s ", (int) (ms / 1000.0)) + format(ms % 1000.0);
        } else if (ms < 3600000) {
            ret = String.format("%d min ", (int) (ms / 60000.0)) + format(ms % 60000.0);
        } else {
            ret = String.format("%d hr ", (int) (ms / 3600000.0)) + format(ms % 3600000.0);
        }

        return ret;
    }

    public Timer() {
        TIME_MAP.put(hashCode(), System.nanoTime());
    }

    public void printElapsedTime(String tag) {
        DBG.accept("[" + tag + "]\t" + format(getElapsedTime()) + "\n");
    }

    private double getElapsedTime() {
        return (System.nanoTime() - TIME_MAP.get(hashCode())) / 1000000.0;
    }

}
