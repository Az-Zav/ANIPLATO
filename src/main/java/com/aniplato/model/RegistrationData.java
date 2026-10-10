package com.aniplato.model;

public class RegistrationData {
    private static RegistrationData instance;

    private String role; // "BUSINESS_DONOR" or "FOOD_GROUP"
    private String orgName;
    private String orgType;
    private String city;
    private String contactPerson;
    private String contactNumber;
    private String email;

    private RegistrationData() {}

    public static RegistrationData getInstance() {
        if (instance == null) {
            instance = new RegistrationData();
        }
        return instance;
    }

    // Getters and Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public String getOrgType() { return orgType; }
    public void setOrgType(String orgType) { this.orgType = orgType; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void clear() {
        this.role = null;
        this.orgName = null;
        this.orgType = null;
        this.city = null;
        this.contactPerson = null;
        this.contactNumber = null;
        this.email = null;
    }
}