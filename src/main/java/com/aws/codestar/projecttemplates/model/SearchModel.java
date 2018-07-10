package com.aws.codestar.projecttemplates.model;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class SearchModel {

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    private String photo;

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    private String collectionId;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
