package com.github.sandin.hundun.utils;

public class SimpleNameGenerator {
    private int index;

    public SimpleNameGenerator() {
        this.index = 0;
    }

    public String nextName() {
        StringBuilder name = new StringBuilder();
        int currentIndex = index;

        do {
            name.append((char) ('a' + (currentIndex % 26)));
            currentIndex /= 26;
        } while (currentIndex > 0);

        index++;
        return name.reverse().toString();
    }
}
