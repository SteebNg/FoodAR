package com.capstone.foodar.Model;

import android.net.Uri;

import java.sql.Timestamp;

public class Review {
    public Uri profileImage;
    public String comment, userId, userName;
    public double rating;
    public Timestamp timestamp;
}
