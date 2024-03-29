/*
 * Copyright (C) 2018 Yaroslav Pronin <proninyaroslav@mail.ru>
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

package com.ultimatesoftil.itorrent.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.ultimatesoftil.itorrent.settings.SettingsManager;

import com.ultimatesoftil.itorrent.R;
import com.ultimatesoftil.itorrent.core.FeedChannel;
import com.ultimatesoftil.itorrent.core.FeedItem;
import com.ultimatesoftil.itorrent.core.FeedParser;
import com.ultimatesoftil.itorrent.core.storage.FeedStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FeedFetcherService extends JobIntentService
{
    @SuppressWarnings("unused")
    private static final String TAG = FeedFetcherService.class.getSimpleName();

    public static final String ACTION_FETCH_CHANNEL = "FeedFetcherService.ACTION_FETCH_CHANNEL";
    public static final String ACTION_FETCH_CHANNEL_LIST = "FeedFetcherService.ACTION_FETCH_CHANNEL_LIST";
    public static final String ACTION_FETCH_ALL_CHANNELS = "FeedFetcherService.ACTION_FETCH_ALL_CHANNELS";
    public static final String ACTION_CHANNEL_RESULT = "FeedFetcherService.ACTION_CHANNEL_RESULT";
    public static final String TAG_CHANNEL_URL_ARG = "channel_url_arg";
    public static final String TAG_CHANNEL_URL_LIST_ARG = "channel_url_list_arg";
    public static final String TAG_CHANNEL_URL_RESULT = "channel_url_result";
    public static final String TAG_NO_AUTO_DOWNLOAD = "no_download";

    private FeedStorage storage;

    public static void enqueueWork(Context context, Intent i)
    {
        enqueueWork(context, FeedFetcherService.class, TAG.hashCode(), i);
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent)
    {
        if (intent == null || intent.getAction() == null)
            return;

        deleteOldItems();
        boolean noAutoDownload = intent.getBooleanExtra(TAG_NO_AUTO_DOWNLOAD, false);
        switch (intent.getAction()) {
            case ACTION_FETCH_CHANNEL:
                fetchChannel(intent.getStringExtra(TAG_CHANNEL_URL_ARG), noAutoDownload);
                break;
            case ACTION_FETCH_CHANNEL_LIST:
                fetchChannelsByUrl(intent.getStringArrayListExtra(TAG_CHANNEL_URL_LIST_ARG), noAutoDownload);
                break;
            case ACTION_FETCH_ALL_CHANNELS:
                fetchChannels(storage.getAllChannels(), noAutoDownload);
                break;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.i(TAG, "Start " + FeedFetcherService.class.getSimpleName());

        storage = new FeedStorage(getApplicationContext());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Log.i(TAG, "Stop " + FeedFetcherService.class.getSimpleName());
    }

    private void fetchChannelsByUrl(List<String> urls, boolean noAutoDownload)
    {
        if (urls == null)
            return;
        for (String url : urls) {
            if (url == null)
                continue;
            fetchChannel(url, noAutoDownload);
        }
    }

    private void fetchChannels(List<FeedChannel> channels, boolean noAutoDownload)
    {
        if (channels == null)
            return;
        for (FeedChannel channel : channels) {
            if (channel == null)
                continue;
            fetchChannel(channel.getUrl(), noAutoDownload);
        }
    }

    private void fetchChannel(String url, boolean noAutoDownload)
    {
        if (url == null || !storage.channelExists(url))
            return;

        FeedParser parser;
        try {
            parser = new FeedParser(getApplicationContext(), url);
        } catch (Exception e) {
            FeedChannel channel = storage.getChannelByUrl(url);
            if (channel != null) {
                channel.setFetchError(e.getMessage());
                storage.updateChannel(channel);
            }
            Intent i = new Intent(ACTION_CHANNEL_RESULT);
            i.putExtra(TAG_CHANNEL_URL_RESULT, url);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);

            return;
        }

        List<FeedItem> items = parser.getItems();
        items = storage.addItems(items); /* Added items */

        FeedChannel channel = storage.getChannelByUrl(url);
        if (channel != null) {
            channel.setFetchError(null);
            if (channel.getName() == null || TextUtils.isEmpty(channel.getName()))
                channel.setName(parser.getTitle());
            channel.setLastUpdate(System.currentTimeMillis());
            storage.updateChannel(channel);
        }

        Intent i = new Intent(ACTION_CHANNEL_RESULT);
        i.putExtra(TAG_CHANNEL_URL_RESULT, url);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        if (!noAutoDownload && channel != null && channel.isAutoDownload())
            sendUrls(channel, items);
    }

    private void deleteOldItems()
    {
        SharedPreferences pref = SettingsManager.getPreferences(getApplicationContext());
        long keepTime = pref.getLong(getString(R.string.pref_key_feed_keep_items_time),
                                     SettingsManager.Default.feedItemKeepTime);
        long keepDateBorderTime = (keepTime > 0 ? System.currentTimeMillis() - keepTime : 0);
        storage.deleteItemsOlderThan(keepDateBorderTime);
    }

    private void sendUrls(FeedChannel channel, List<FeedItem> items)
    {
        ArrayList<String> urlsForDownload = new ArrayList<>();
        for (FeedItem item : items) {
            if (item.isRead())
                continue;
            String url = item.getDownloadUrl();
            if (url == null || TextUtils.isEmpty(url))
                return;
            if (isMatch(item, channel.getFilter(), channel.isRegexFilter())) {
                urlsForDownload.add(url);
                storage.markAsRead(item);
            }
        }

        if (!urlsForDownload.isEmpty()) {
            Intent i = new Intent(getApplicationContext(), FeedDownloaderService.class);
            i.setAction(FeedDownloaderService.ACTION_DOWNLOAD_TORRENT_LIST);
            i.putExtra(FeedDownloaderService.TAG_URL_LIST_ARG, urlsForDownload);
            FeedDownloaderService.enqueueWork(this, i);
        }
    }

    private boolean isMatch(FeedItem item, String filter, boolean isRegex)
    {
        if (filter == null || TextUtils.isEmpty(filter) || item.getTitle() == null)
            return true;

        if (isRegex) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(filter);
            } catch (PatternSyntaxException e) {
                //TODO: maybe there is an option better?
                Log.e(TAG, "Invalid pattern: " + filter);
                return true;
            }

            return pattern.matcher(item.getTitle()).matches();
        } else {
            String[] words = filter.split(FeedStorage.FILTER_SEPARATOR);
            for (String word : words)
                if (item.getTitle().toLowerCase().contains(word.toLowerCase().trim()))
                    return true;
        }

        return false;
    }
}
