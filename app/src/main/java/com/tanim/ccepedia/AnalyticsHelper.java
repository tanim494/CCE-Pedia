package com.tanim.ccepedia;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {
    private static FirebaseAnalytics mFirebaseAnalytics;

    public static void initialize(Context context) {
        if (mFirebaseAnalytics == null) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
    }

    // Screen view events
    public static void logScreenView(String screenName, String screenClass) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    // User actions
    public static void logUserAction(String action, String category, String label) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("action_category", category);
        bundle.putString("action_label", label);
        mFirebaseAnalytics.logEvent(action, bundle);
    }

    // Content views
    public static void logContentView(String contentType, String contentId, String contentName) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, contentId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, contentName);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    // Resource access
    public static void logResourceAccess(String resourceType, String resourceName, String semester) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("resource_type", resourceType);
        bundle.putString("resource_name", resourceName);
        bundle.putString("semester", semester);
        mFirebaseAnalytics.logEvent("resource_accessed", bundle);
    }

    // Notice/Announcement clicks
    public static void logNoticeClick(String noticeText) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("notice_content", noticeText.substring(0, Math.min(100, noticeText.length())));
        mFirebaseAnalytics.logEvent("notice_clicked", bundle);
    }

    // Community interactions
    public static void logCommunityAction(String action) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("community_action", action);
        mFirebaseAnalytics.logEvent("community_interaction", bundle);
    }

    // Share events
    public static void logShareEvent(String method) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, method);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
    }

    // Search events
    public static void logSearch(String searchTerm) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, searchTerm);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
    }

    // Error tracking
    public static void logError(String errorType, String errorMessage) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("error_type", errorType);
        bundle.putString("error_message", errorMessage.substring(0, Math.min(100, errorMessage.length())));
        mFirebaseAnalytics.logEvent("app_error", bundle);
    }

    // Login/Registration
    public static void logLogin(String method) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, method);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
    }

    public static void logSignUp(String method) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, method);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }

    // Set user properties
    public static void setUserProperty(String name, String value) {
        if (mFirebaseAnalytics == null) return;
        mFirebaseAnalytics.setUserProperty(name, value);
    }

    // Set user ID
    public static void setUserId(String userId) {
        if (mFirebaseAnalytics == null) return;
        mFirebaseAnalytics.setUserId(userId);
    }
}
