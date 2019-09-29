/*
 * Copyright (C) 2016, 2017 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ultimatesoftil.itorrent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.claudiodegio.msv.MaterialSearchView;
import com.claudiodegio.msv.OnSearchViewListener;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ultimatesoftil.itorrent.adapters.SlidingTabAdapter;
import com.ultimatesoftil.itorrent.core.utils.Utils;
import com.ultimatesoftil.itorrent.dialogs.BaseAlertDialog;
import com.ultimatesoftil.itorrent.dialogs.filemanager.FileManagerConfig;
import com.ultimatesoftil.itorrent.dialogs.filemanager.FileManagerDialog;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentFragment;
import com.ultimatesoftil.itorrent.fragments.FragmentCallback;
import com.ultimatesoftil.itorrent.fragments.MainFragment;
import com.ultimatesoftil.itorrent.receivers.NotificationReceiver;
import com.ultimatesoftil.itorrent.services.TorrentTaskService;
import com.ultimatesoftil.itorrent.settings.SettingsActivity;
import com.ultimatesoftil.itorrent.settings.SettingsManager;

import com.ultimatesoftil.itorrent.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements
        FragmentCallback,
        DetailTorrentFragment.Callback,
        OnSearchViewListener,
        BaseAlertDialog.OnClickListener,
        BaseAlertDialog.OnDialogShowListener{
    @SuppressWarnings("unused")
    private static final String TAG = MainActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private static final String TAG_PERM_DIALOG_IS_SHOW = "perm_dialog_is_show";
    public static final String ACTION_ADD_TORRENT_SHORTCUT = "org.proninyaroslav.libretorrent.ADD_TORRENT_SHORTCUT";
    private static final String TAG_ADD_LINK_DIALOG = "add_link_dialog";

    private static final String TAG_DELETE_TORRENT_DIALOG = "delete_torrent_dialog";

    private static final String TAG_ERROR_OPEN_TORRENT_FILE_DIALOG = "error_open_torrent_file_dialog";
    private static final String TAG_SAVE_ERROR_DIALOG = "save_error_dialog";

    public static final String TAG_ABOUT_DIALOG = "about_dialog";
    public static final String TAG_TORRENT_SORTING = "torrent_sorting";
    private  ActionBarDrawerToggle mDrawerToggle;
    private boolean permDialogIsShow = false;
    MainFragment mainFragment;
    private DrawerLayout mDrawerLayout;
    private TabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private ArrayList<String> titles;
    private ArrayList<Fragment> fragments;
    private static final int ADD_TORRENT_REQUEST = 1;
    private static final int TORRENT_FILE_CHOOSE_REQUEST = 2;
    private static final int CREATE_TORRENT_REQUEST = 3;
    private MaterialSearchView materialSearchView;
    private FloatingActionButton openFileButton;
    private FloatingActionButton addLinkButton;

   private FloatingActionMenu addTorrentButton;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setTheme(Utils.getAppTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);

        if (getIntent().getAction() != null &&
            getIntent().getAction().equals(NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP))
        {
            finish();

            return;
        }

        if (savedInstanceState != null)
            permDialogIsShow = savedInstanceState.getBoolean(TAG_PERM_DIALOG_IS_SHOW);

        if (!Utils.checkStoragePermission(getApplicationContext()) && !permDialogIsShow) {
            permDialogIsShow = true;
            startActivity(new Intent(this, RequestPermissions.class));
        }

        startService(new Intent(this, TorrentTaskService.class));

        setContentView(R.layout.activity_main);
        Utils.showColoredStatusBar_KitKat(this);

        FragmentManager fm = getSupportFragmentManager();
      //  mainFragment = (MainFragment)fm.findFragmentById(R.id.main_fragmentContainer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        //--------------------------------------------------------------------------
        // create the material navdrawer toggle and bind it to the navigation_drawer
        //--------------------------------------------------------------------------
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if(getSupportActionBar()!=null)
        getSupportActionBar().setTitle("");
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(5); // tabcachesize (=tabcount for better performance)
       materialSearchView = (MaterialSearchView) findViewById(R.id.sv);
        materialSearchView.setOnSearchViewListener(this); // this class implements OnSearchViewListener

        //--------------------------------------------------------------------------
        // create sliding tabs and bind them to the viewpager
        //--------------------------------------------------------------------------
        mSlidingTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        bindNavDrawerEvents();
        addContent();
        initFabs();
    }

    private void initFabs() {
        addTorrentButton = findViewById(R.id.add_torrent_button);
        addTorrentButton.setClosedOnTouchOutside(true);

        openFileButton = findViewById(R.id.open_file_button);
        openFileButton.setOnClickListener((View view) -> {
            addTorrentButton.close(true);

            torrentFileChooserDialog();
        });

        addLinkButton = findViewById(R.id.add_link_button);
        addLinkButton.setOnClickListener((View view) -> {
            addTorrentButton.close(true);
            addLinkDialog();
        });
    }
    private void addLinkDialog()
    {
        FragmentManager fm = getSupportFragmentManager();
        if (fm != null && fm.findFragmentByTag(TAG_ADD_LINK_DIALOG) == null) {
            BaseAlertDialog addLinkDialog = BaseAlertDialog.newInstance(
                    getString(R.string.dialog_add_link_title),
                    null,
                    R.layout.dialog_text_input,
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    null,
                    this);

            addLinkDialog.show(fm, TAG_ADD_LINK_DIALOG);
        }
    }
    private void torrentFileChooserDialog()
    {
        Intent i = new Intent(MainActivity.this, FileManagerDialog.class);
        List<String> fileType = new ArrayList<>();
        fileType.add("torrent");
        FileManagerConfig config = new FileManagerConfig(null,
                getString(R.string.torrent_file_chooser_title),
                fileType,
                FileManagerConfig.FILE_CHOOSER_MODE);
        i.putExtra(FileManagerDialog.TAG_CONFIG, config);
        startActivityForResult(i, TORRENT_FILE_CHOOSE_REQUEST);
    }
    public void addContent(){
        titles=new ArrayList<>();

        fragments= new ArrayList<>();
        MainFragment.OnRecyclerScrolled onRecyclerScrolled = new MainFragment.OnRecyclerScrolled() {
            @Override
            public void showFab() {
                addTorrentButton.showMenuButton(true);
            }

            @Override
            public void hideFab() {
                addTorrentButton.hideMenuButton(true);

            }
        };
        titles.add(getString(R.string.all));
        MainFragment all= MainFragment.newInstance(getString(R.string.spinner_all_torrents));
        all.setScrollListener(onRecyclerScrolled);
        fragments.add(all);

        titles.add(getString(R.string.queue));
        MainFragment queue=MainFragment.newInstance(getString(R.string.spinner_downloading_torrents));
        queue.setScrollListener(onRecyclerScrolled);
        fragments.add(queue);

        titles.add(getString(R.string.spinner_downloaded_torrents));
        MainFragment downloaded= MainFragment.newInstance(getString(R.string.spinner_downloaded_torrents));
        downloaded.setScrollListener(onRecyclerScrolled);

        fragments.add(downloaded);


        SlidingTabAdapter sla= new SlidingTabAdapter(getSupportFragmentManager(),fragments,titles);
        mViewPager.setAdapter(sla);
        mSlidingTabLayout.setupWithViewPager(mViewPager);

        // mSlidingTabLayout.populateTabStrip();
        // Inflate your Layouts here

    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        //--------------------------------------------------------------------------
        // make sure the drawer toggle is in the right state, nothing to do here
        //--------------------------------------------------------------------------
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean(TAG_PERM_DIALOG_IS_SHOW, permDialogIsShow);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        SharedPreferences pref = SettingsManager.getPreferences(this);
        if (isFinishing() && !pref.getBoolean(getString(R.string.pref_key_keep_alive),
                                              SettingsManager.Default.keepAlive)) {
            Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
            i.setAction(TorrentTaskService.ACTION_SHUTDOWN);
            startService(i);
        }
    }

    public void bindNavDrawerEvents() {
        // Click event for one Navigation element
        LinearLayout navButton = (LinearLayout) findViewById(R.id.txtNavButton);
        navButton.setOnClickListener(v -> {

            // close drawer if you want

            if (mDrawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
                mDrawerLayout.closeDrawers();
            }
            // display a nice toast message


            // update loaded Views if you want
            //mViewPager.getAdapter().notifyDataSetChanged();
        });
        LinearLayout settingsNav=(LinearLayout)findViewById(R.id.nav_settings);
        settingsNav.setOnClickListener(v -> {
            if (mDrawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
                mDrawerLayout.closeDrawers();
            }
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }
    /*
     * Changed in detail fragment.
     */

    @Override
    public void onBackPressed() {
        if(fragments.size()>0&&fragments.get(0)!=null){
            MainFragment fragment= (MainFragment) fragments.get(0);
            if(fragment.isInSearch()){
                fragment.resetFilter();
                fragment.setInSearch(false);
                return;
            }
        }
        if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT| Gravity.LEFT)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        if(materialSearchView.isShown()){
            materialSearchView.closeSearch();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onTorrentInfoChanged()
    {
        if (mainFragment == null)
            return;

        DetailTorrentFragment fragment = mainFragment.getCurrentDetailFragment();
        if (fragment != null)
            fragment.onTorrentInfoChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        MenuItem item = menu.findItem(R.id.action_search);

        materialSearchView.setMenuItem(item);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //--------------------------------------------------------------------------
        // triggers if the user selects a menu item
        //--------------------------------------------------------------------------
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        switch (id){
            case R.id.action_exit:
                Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
                i.setAction(TorrentTaskService.ACTION_SHUTDOWN);
                startService(i);
                finish();
                break;
        }



        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTorrentInfoChangesUndone()
    {
        if (mainFragment == null)
            return;

        DetailTorrentFragment fragment = mainFragment.getCurrentDetailFragment();
        if (fragment != null)
            fragment.onTorrentInfoChangesUndone();
    }

    @Override
    public void onTorrentFilesChanged()
    {
        if (mainFragment == null)
            return;

        DetailTorrentFragment fragment = mainFragment.getCurrentDetailFragment();
        if (fragment != null)
            fragment.onTorrentFilesChanged();
    }

    @Override
    public void onTrackersChanged(ArrayList<String> trackers, boolean replace)
    {
        if (mainFragment == null)
            return;

        DetailTorrentFragment fragment = mainFragment.getCurrentDetailFragment();
        if (fragment != null)
            fragment.onTrackersChanged(trackers, replace);
    }

    @Override
    public void openFile(String relativePath)
    {
        if (mainFragment == null)
            return;

        DetailTorrentFragment fragment = mainFragment.getCurrentDetailFragment();
        if (fragment != null)
            fragment.openFile(relativePath);
    }

    @Override
    public void fragmentFinished(Intent intent, ResultCode code)
    {
        switch (code) {
            case OK:
                Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
                i.setAction(TorrentTaskService.ACTION_SHUTDOWN);
                startService(i);
                finish();
                break;
            case CANCEL:
            case BACK:
                if (mainFragment != null)
                    mainFragment.resetCurOpenTorrent();
                break;
        }
    }

    @Override
    public void onSearchViewShown() {

    }

    @Override
    public void onSearchViewClosed() {

    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if(mViewPager.getCurrentItem()!=0)mViewPager.setCurrentItem(0);
        try {
            MainFragment fragment=(MainFragment) fragments.get(0);

            fragment.triggerSearch(s);

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onQueryTextChange(String s) {


    }
    @Override
    public void onShow(final AlertDialog dialog)
    {
        if (dialog != null) {
            FragmentManager fm =getSupportFragmentManager();
            if (fm == null)
                return;

            if (fm.findFragmentByTag(TAG_ADD_LINK_DIALOG) != null)
                initAddDialog(dialog);
            else if (fm.findFragmentByTag(TAG_ABOUT_DIALOG) != null)
                initAboutDialog(dialog);
            //else if (fm.findFragmentByTag(TAG_TORRENT_SORTING) != null)
               // initTorrentSortingDialog(dialog);
        }
    }
    private void initAboutDialog(final AlertDialog dialog)
    {
        TextView version = dialog.findViewById(R.id.about_version);
        if (version != null) {
            String versionName = Utils.getAppVersionName(MainActivity.this);
            if (versionName != null)
                version.setText(versionName);
        }
    }


    private void initAddDialog(final AlertDialog dialog) {
        final TextInputEditText field = dialog.findViewById(R.id.text_input_dialog);
        final TextInputLayout fieldLayout = dialog.findViewById(R.id.layout_text_input_dialog);

        /* Dismiss error label if user has changed the text */
        if (field != null && fieldLayout != null) {
            field.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                    /* Nothing */
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                    fieldLayout.setErrorEnabled(false);
                    fieldLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable s)
                {
                    /* Nothing */
                }
            });
        }

        /*
         * It is necessary in order to the dialog is not closed by
         * pressing positive button if the text checker gave a false result
         */
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        positiveButton.setOnClickListener((View v) -> {
            if (field != null && field.getText() != null && fieldLayout != null) {
                String link = field.getText().toString();
                if (checkEditTextField(link, fieldLayout)) {
                    String url;
                    if (link.toLowerCase().startsWith(Utils.MAGNET_PREFIX))
                        url = link;
                    else if (Utils.isHash(link))
                        url = Utils.normalizeMagnetHash(link);
                    else
                        url = Utils.normalizeURL(link);

                    if (url != null)
                        addTorrentDialog(Uri.parse(url));

                    dialog.dismiss();
                }
            }
        });

        /* Inserting a link from the clipboard */
        String clipboard = Utils.getClipboard(getApplicationContext());
        String url = null;
        if (clipboard != null) {
            String c = clipboard.toLowerCase();
            if (c.startsWith(Utils.MAGNET_PREFIX) ||
                    c.startsWith(Utils.HTTP_PREFIX) ||
                    c.startsWith(Utils.HTTPS_PREFIX) ||
                    Utils.isHash(clipboard)) {
                url = clipboard;
            }

            if (field != null && url != null)
                field.setText(url);
        }
    }
    private void addTorrentDialog(Uri uri)
    {
        if (uri == null)
            return;

        Intent i = new Intent(MainActivity.this, AddTorrentActivity.class);
        i.putExtra(AddTorrentActivity.TAG_URI, uri);
        startActivityForResult(i, ADD_TORRENT_REQUEST);
    }

    private boolean checkEditTextField(String s, TextInputLayout layout)
    {
        if (s == null || layout == null)
            return false;

        if (TextUtils.isEmpty(s)) {
            layout.setErrorEnabled(true);
            layout.setError(getString(R.string.error_empty_link));
            layout.requestFocus();

            return false;
        }

        layout.setErrorEnabled(false);
        layout.setError(null);

        return true;
    }
    @Override
    public void onPositiveClicked(@Nullable View v)
    {

    }

    @Override
    public void onNegativeClicked(@Nullable View v)
    {
        FragmentManager fm = getSupportFragmentManager();
        if (fm == null)
            return;

        if (fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) != null) {
          //  selectedTorrents.clear();
        } else if (fm.findFragmentByTag(TAG_ABOUT_DIALOG) != null) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getString(R.string.about_changelog_link)));
            startActivity(i);
        }
    }

    @Override
    public void onNeutralClicked(@Nullable View v)
    {
        /* Nothing */
    }


}
