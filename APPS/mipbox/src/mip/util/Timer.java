package mip.util;

import java.util.concurrent.ConcurrentHashMap;

public class Timer {

    private static final ConcurrentHashMap<Integer, Long> startTimeTable = new ConcurrentHashMap<>();

    public Timer() {
        startTimeTable.put(hashCode(), System.nanoTime());
    }

    public void printElapsedTime(String tag) {
        System.out.println("[" + tag + "]\t" + format(getElapsedTime())); // TODO log4j
    }

    private double getElapsedTime() {
        return (System.nanoTime() - startTimeTable.get(hashCode())) / 1000000.0;
    }

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
}
