package com.capstone.foodar;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class BankNumTest {
    private static boolean isValidBankNumber(String bankAccountNumber)
    {

        // Regex to check valid BANK ACCOUNT NUMBER Code
        String regex = "^[0-9]{9,18}$";

        Pattern p = Pattern.compile(regex);

        if (bankAccountNumber == null) {
            return false;
        } else if (bankAccountNumber.length() != 16) {
            return false;
        }

        Matcher m = p.matcher(bankAccountNumber);

        return m.matches();
    }

    @Test
    public void testAValidCardNum() {
        String input = "5491862035625084";
        boolean expected = true;
        assertEquals(expected, isValidBankNumber(input));
    }

    @Test
    public void testWrongLengthCardNum() {
        String input = "17348576938";
        boolean expected = false;
        assertEquals(expected, isValidBankNumber(input));
    }

    @Test
    public void testContainAlphaCardNum() {
        String input = "549186203562508A";
        boolean expected = false;
        assertEquals(expected, isValidBankNumber(input));
    }
}
