package com.example.ecommerceapp.crispChatBox;

import android.content.Context;
import android.content.Intent;

import im.crisp.client.external.ChatActivity;
import im.crisp.client.external.Crisp;

public class ActivityCrisp {

    /**
     * Configures the Crisp chat SDK with the application context and website ID.
     * Should be called in the Application class onCreate method.
     *
     * @param context Application context
     */
    public static void configureCrisp(Context context) {
        // Replace with your WEBSITE_ID from https://app.crisp.chat/website/[YOUR_WEBSITE_ID]/
        Crisp.configure(context, "786ed8ad-0ec7-4e9b-a946-12dc19dde033");
    }

    /**
     * Launches the Crisp chat activity.
     *
     * @param context Activity context
     */
    public static void openCrispChat(Context context) {
        Intent crispIntent = new Intent(context, ChatActivity.class);
        context.startActivity(crispIntent);
    }
}
