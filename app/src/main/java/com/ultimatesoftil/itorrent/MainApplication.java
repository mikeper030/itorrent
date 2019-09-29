/*
 * Copyright (C) 2016-2018 Yaroslav Pronin <proninyaroslav@mail.ru>
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

import android.app.Application;
import android.content.Context;

import com.ultimatesoftil.itorrent.core.utils.Utils;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;

//@ReportsCrashes(mailTo = "proninyaroslav@mail.ru",
//                mode = ReportingInteractionMode.DIALOG,
//                reportDialogClass = ErrorReportActivity.class)

public class MainApplication extends Application
{
    @SuppressWarnings("unused")
    private static final String TAG = MainApplication.class.getSimpleName();
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
       MainApplication.context = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        Utils.migrateTray2SharedPreferences(this);
        ACRA.init(this);
        EventBus.builder().logNoSubscriberMessages(false).installDefaultEventBus();
    }
    public static Context getAppContext() {
        return MainApplication.context;
    }
}