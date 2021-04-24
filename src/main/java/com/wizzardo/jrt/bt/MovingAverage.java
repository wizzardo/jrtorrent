package com.wizzardo.jrt.bt;


import java.util.ArrayList;
import java.util.List;

public class MovingAverage {
    final int size;
    final List<Long> values;
    int position;

    public MovingAverage(int size) {
        this.size = size;
        values = new ArrayList<>(size);
    }

    public void add(long value) {
        if (values.size() < size)
            values.add(value);
        else
            values.set(position++, value);

        if (position >= size)
            position = 0;
    }

    public long get() {
        long sum = 0;
        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum  / values.size();
    }

    @Override
    public String toString() {
        return "MovingAverage{" +
                " values=" + values +
                " avg=" + get() +
                '}';
    }
}
