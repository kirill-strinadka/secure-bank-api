package com.kstrinadka.securebankapi.config;

public final class CacheNames {

    public static final String USER_SEARCH = "userSearch";
    public static final String CURRENT_USER_EMAILS = "currentUserEmails";
    public static final String CURRENT_USER_PHONES = "currentUserPhones";
    public static final String USER_EXISTS = "userExists";
    public static final String EMAIL_EXISTS = "emailExists";
    public static final String PHONE_EXISTS = "phoneExists";

    public static final String[] ALL = {
            USER_SEARCH,
            CURRENT_USER_EMAILS,
            CURRENT_USER_PHONES,
            USER_EXISTS,
            EMAIL_EXISTS,
            PHONE_EXISTS
    };

    private CacheNames() {
    }
}
