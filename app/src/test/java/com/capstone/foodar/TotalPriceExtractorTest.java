package com.capstone.foodar;

import org.junit.Test;

import static org.junit.Assert.*;

public class TotalPriceExtractorTest {

    double acceptedError = 0.0;

    private double extractPrice(String sPrice) {
        String sExtractedPrice = sPrice.replaceAll("[^\\d.-]", "");
        return Double.parseDouble(sExtractedPrice);
    }

    @Test
    public void testNoDecimalPrice() {
        String input = "RM 11.00";
        double expectedOutput = 11.0;
        assertEquals(expectedOutput, extractPrice(input), acceptedError);
    }

    @Test
    public void testWithDecimalPrice() {
        String input = "RM 1005.50";
        double expectedOutput = 1005.5;
        assertEquals(expectedOutput, extractPrice(input), acceptedError);
    }
}
