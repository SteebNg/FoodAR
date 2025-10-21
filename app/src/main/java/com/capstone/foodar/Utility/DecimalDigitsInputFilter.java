package com.capstone.foodar.Utility;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecimalDigitsInputFilter implements TextWatcher {
    private final int decimalDigits;
    private final TextInputEditText editText;
    private final Pattern pattern;

    public DecimalDigitsInputFilter(int decimalDigits, TextInputEditText editText) {
        this.decimalDigits = decimalDigits;
        this.editText = editText;
        this.pattern = Pattern.compile("^-?\\d*(\\.\\d{0," + decimalDigits + "})?$");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString();
        Matcher matcher = pattern.matcher(text);

        if (!matcher.matches()) {
            String cleanString = text.substring(0, text.length() - 1);

            editText.removeTextChangedListener(this);

            editText.setText(cleanString);

            editText.setSelection(cleanString.length());

            editText.addTextChangedListener(this);
        }
    }
}
