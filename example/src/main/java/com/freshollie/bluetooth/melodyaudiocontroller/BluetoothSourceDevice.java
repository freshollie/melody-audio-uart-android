package com.freshollie.bluetooth.melodyaudiocontroller;

/**
 * Created by freshollie on 08.12.17.
 *
 * I only support bluetooth source devices
 */

public class BluetoothSourceDevice {
    public class Link {
        private int profile;
        private int deviceId;

        public Link(int deviceId, int profile) {
            this.deviceId = deviceId;
            this.profile = profile;
        }

        public String getId() {
            return Integer.toHexString(deviceId) + Integer.toHexString(profile);
        }

        public int getProfile() {
            return profile;
        }
    }

    private final String address;
    private final String name;
    private Link[] links;

    public BluetoothSourceDevice(String name, String address, Link[] links) {
        this.address = address;
        this.name = name;
        this.links = links;
    }

    public void setLinks(Link[] links) {
        this.links = links;
    }

    public Link[] getLinks() {
        return links;
    }

    public Link getLink(int profile) {
        for (Link link: links) {
            if (link.getProfile() == profile) {
                return link;
            }
        }
        return null;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }
}
