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

package com.ultimatesoftil.itorrent.adapters;

import android.content.Context;


import com.ultimatesoftil.itorrent.R;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentFilesFragment;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentInfoFragment;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentPeersFragment;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentPiecesFragment;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentStateFragment;
import com.ultimatesoftil.itorrent.fragments.DetailTorrentTrackersFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class TorrentStatusPagerAdapter extends ViewPagerAdapter
{
    public static final int NUM_FRAGMENTS = 6;
    public static final int INFO_FRAG_POS = 0;
    public static final int STATE_FRAG_POS = 1;
    public static final int FILES_FRAG_POS = 2;
    public static final int TRACKERS_FRAG_POS = 3;
    public static final int PEERS_FRAG_POS = 4;
    public static final int PIECES_FRAG_POS = 5;

    public TorrentStatusPagerAdapter(String torrentId, FragmentManager fm, Context context)
    {
        super(torrentId, fm);

        fragmentTitleList.add(context.getString(R.string.torrent_info));
        fragmentTitleList.add(context.getString(R.string.torrent_state));
        fragmentTitleList.add(context.getString(R.string.torrent_files));
        fragmentTitleList.add(context.getString(R.string.torrent_trackers));
        fragmentTitleList.add(context.getString(R.string.torrent_peers));
        fragmentTitleList.add(context.getString(R.string.torrent_pieces));
    }

    @Override
    public Fragment getItem(int position)
    {
        switch (position) {
            case INFO_FRAG_POS:
                return DetailTorrentInfoFragment.newInstance();
            case STATE_FRAG_POS:
                return DetailTorrentStateFragment.newInstance();
            case FILES_FRAG_POS:
                return DetailTorrentFilesFragment.newInstance();
            case TRACKERS_FRAG_POS:
                return DetailTorrentTrackersFragment.newInstance();
            case PEERS_FRAG_POS:
                return DetailTorrentPeersFragment.newInstance();
            case PIECES_FRAG_POS:
                return DetailTorrentPiecesFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public int getCount()
    {
        return NUM_FRAGMENTS;
    }
}
