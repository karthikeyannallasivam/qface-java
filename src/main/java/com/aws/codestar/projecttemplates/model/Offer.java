package com.aws.codestar.projecttemplates.model;

public class Offer {
    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String offerId;
    private String title;
    private String description;

    @Override
    public String toString() {
        return "Offer{" +
                "offerId='" + offerId + '\'' +
                "title='" + title + '\'' +
                "description='" + description + '\'' +
                '}';
    }
}
