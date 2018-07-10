package com.aws.codestar.projecttemplates.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.List;
import java.util.Objects;

public class Customer {

    private String ffId;
    private String name;
    private String tier;
    private String avatar;

    private String confidence;
    private List<Offer> offers;

    public String getFfId() {
        return ffId;
    }

    public void setFfId(String ffId) {
        this.ffId = ffId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public void setOffers(List<Offer> offers) {
        this.offers = offers;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(ffId, customer.ffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ffId);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
