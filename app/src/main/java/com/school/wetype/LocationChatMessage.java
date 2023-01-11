package com.school.wetype;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationChatMessage extends ChatMessage {
    public LocationChatMessage(String messageUser) {
        super(messageUser);
    }

    public void setLocationText(Context context, Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Address address = addresses.get(0);
        String knownName = address.getThoroughfare() + ", " + address.getLocality();

        String messageText = "My current location is: \n" + knownName;

        this.setMessageText(messageText);
    }
}
