package com.capstone.foodar.Model;

import android.net.Uri;

import com.google.firebase.Timestamp;

public class Review {
    public Uri profileImage;
    public String comment, userId;
    public double rating;
    public Timestamp timestamp;
}
