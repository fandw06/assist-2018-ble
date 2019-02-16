package com.dawei.assist_2018;

import org.junit.Test;

import static org.junit.Assert.*;
/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testInverse() {
        byte b = (byte)0b10110001;
        byte i = DisplayActivity.inverseByte(b);
        System.out.println(i);
    }
}