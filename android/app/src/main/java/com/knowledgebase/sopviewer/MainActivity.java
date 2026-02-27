package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    /** Set by onActivityResult (CreateDocument result) so HomeFragment can refresh. */
    public boolean pendingHomeRefresh = false;

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        // Start SSE badge polling
        SseManager.getInstance().start(currentUser);

        // Wire up NavController + BottomNavigationView
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;
        navController = navHostFragment.getNavController();
        bottomNav = findViewById(R.id.bottom_navigation);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // FAB — visible only on the Home fragment
        FloatingActionButton fab = findViewById(R.id.fab);
        navController.addOnDestinationChangedListener((ctrl, dest, args) ->
                fab.setVisibility(dest.getId() == R.id.navigation_home ? View.VISIBLE : View.GONE));

        fab.setOnClickListener(v ->
                startActivityForResult(new Intent(this, CreateDocumentActivity.class), 1001));

        // Live badge update from SSE
        SseManager.getInstance().addListener(count ->
                NavBadgeHelper.updateNotificationBadge(bottomNav, count));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null)
            NavBadgeHelper.updateNotificationBadge(bottomNav, SseManager.getInstance().getUnreadCount());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            pendingHomeRefresh = true;
        }
    }
}
