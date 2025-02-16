package com.example.bread.model;

import java.io.Serializable;

public class Participant implements Serializable {
    private Profile profile;

    public Participant(Profile profile) {
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
