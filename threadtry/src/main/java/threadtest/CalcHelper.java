package threadtest;

import java.util.Random;

/**
 * Created by ZGY on 2016.10.30..
 */
public class CalcHelper {

    private static Integer generator = 0;

    public static RequestHolder getNextFromHazelcastETLqueue() {
        generator++;
        return new RequestHolder(generator.toString());
    }


    public static long cpuHeavyCalculation(int millis) {
        final long startTime = System.currentTimeMillis();
        long counter = 0;
        while (System.currentTimeMillis() - startTime < millis) {
            counter += new Random().nextInt(100);
        }
        return counter;
    }

}
