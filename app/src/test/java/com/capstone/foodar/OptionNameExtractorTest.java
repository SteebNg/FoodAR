package com.capstone.foodar;

import org.junit.Test;

import static org.junit.Assert.*;

public class OptionNameExtractorTest {

    // The method we are testing
    private String extractOptionName(String optionName) {
        String[] parts = optionName.split("\\s*[+-]\\s*RM");
        return parts[0].trim();
    }

    @Test
    public void testBasicPositivePrice() {
        String input = "Pizza +RM 5.00";
        String expected = "Pizza";
        assertEquals(expected, extractOptionName(input));
    }

    @Test
    public void testBasicNegativePrice() {
        String input = "Extra Cheese -RM 2.50";
        String expected = "Extra Cheese";
        assertEquals(expected, extractOptionName(input));
    }

    @Test
    public void testNoWhitespace() {
        String input = "Soda+RM3.00";
        String expected = "Soda";
        assertEquals(expected, extractOptionName(input));
    }

    @Test
    public void testMultipleWordsInName() {
        String input = "Garlic Bread with Sauce -RM 1.00";
        String expected = "Garlic Bread with Sauce";
        assertEquals(expected, extractOptionName(input));
    }

    @Test
    public void testNameWithTrailingWhitespace() {
        String input = "Burger    +RM 10.50";
        String expected = "Burger";
        assertEquals(expected, extractOptionName(input));
    }

    @Test
    public void testOptionWithoutPrice() {
        String input = "Plain Water";
        String expected = "Plain Water";
        assertEquals(expected, extractOptionName(input));
    }
}
