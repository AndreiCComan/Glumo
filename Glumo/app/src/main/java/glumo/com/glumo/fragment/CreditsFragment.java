package glumo.com.glumo.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;

/**
 * This class handles the display of credits for the app
 */
public class CreditsFragment extends Fragment {

    // view
    private View view;
    private LinearLayout creditsSubtitleContainer;

    /**
     * This method just sets the options menu
     * @param savedInstanceState previously saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * This method just handles the hiding of the options menu
     * @param menu options menu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem actionRefreshMenuItem = menu.findItem(R.id.action_refresh);
        actionRefreshMenuItem.setVisible(false);
        MenuItem actionShakePhoneMenuItem = menu.findItem(R.id.action_shake_phone);
        actionShakePhoneMenuItem.setVisible(false);
    }

    /**
     * This method handles the view elements for the activity
     * @param inflater layout inflater
     * @param container viewgroup container
     * @param savedInstanceState previously saved instance
     * @return processed view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // view
        view =  inflater.inflate(R.layout.fragment_credits, container, false);

        // credits subtitles
        creditsSubtitleContainer = (LinearLayout) view.findViewById(R.id.credits_subtitle_container);

        // general title
        Appearance.setTitle(getString(R.string.drawer_credits));
        Appearance.removeActionBarShadow();

        // set color
        int color = GlumoApplication.getPreferences().getInt(getString(R.string.theme_color), 0);
        creditsSubtitleContainer.setBackgroundColor(color);
        Appearance.setActionBarColor(color);
        Appearance.setStatusBarColor(color);

        return view;
    }
}