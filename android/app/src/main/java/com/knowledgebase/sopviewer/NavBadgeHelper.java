package com.knowledgebase.sopviewer;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Utility for showing / hiding the notification badge on the bottom navigation bar.
 */
public class NavBadgeHelper {

    private NavBadgeHelper() { }

    /**
     * Update the badge on the notifications tab.
     * Shows the count when > 0, removes the badge when 0.
     */
    public static void updateNotificationBadge(BottomNavigationView bottomNav, int count) {
        if (bottomNav == null) return;
        if (count > 0) {
            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.navigation_notifications);
            badge.setNumber(count);
            badge.setVisible(true);
        } else {
            bottomNav.removeBadge(R.id.navigation_notifications);
        }
    }
}
