package com.timedeal.api.common;

/**
 * API 경로 상수. URL 변경 시 한 곳만 수정.
 */
public final class ApiPaths {

    public static final String API = "/api";
    public static final String ITEMS = API + "/items";
    public static final String USERS = API + "/users";
    public static final String ORDERS = API + "/orders";
    public static final String AUTH = API + "/auth";
    public static final String ADMIN = API + "/admin";

    private ApiPaths() {}
}
