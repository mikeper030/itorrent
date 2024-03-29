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

package com.ultimatesoftil.itorrent.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ultimatesoftil.itorrent.R;
import com.ultimatesoftil.itorrent.core.TorrentMetaInfo;
import com.ultimatesoftil.itorrent.core.stateparcel.AdvanceStateParcel;
import com.ultimatesoftil.itorrent.core.stateparcel.BasicStateParcel;
import com.ultimatesoftil.itorrent.core.utils.DateFormatUtils;
import com.ultimatesoftil.itorrent.core.utils.Utils;

import java.util.Locale;

/*
 * The fragment for displaying torrent state. Part of DetailTorrentFragment.
 */

public class DetailTorrentStateFragment extends Fragment
{
    @SuppressWarnings("unused")
    private static final String TAG = DetailTorrentStateFragment.class.getSimpleName();

    private static final String HEAVY_STATE_TAG = TAG + "_" + HeavyInstanceStorage.class.getSimpleName();
    private static final String TAG_BASIC_STATE = "basic_state";
    private static final String TAG_ADVANCE_STATE = "advance_state";
    private static final String TAG_INFO = "info";

    private AppCompatActivity activity;
    private BasicStateParcel basicState;
    private AdvanceStateParcel advanceState;
    private TorrentMetaInfo info;
    TextView downloadUploadSpeed, downloadCounter, textViewETA,
            textViewSeeds, textViewLeechers, textViewPieces,
            textViewUploaded, shareRatio, availability,
            textViewActiveTime, textViewSeedingTime;

    public static DetailTorrentStateFragment newInstance()
    {
        DetailTorrentStateFragment fragment = new DetailTorrentStateFragment();

        Bundle b = new Bundle();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (context instanceof AppCompatActivity)
            activity = (AppCompatActivity)context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_detail_torrent_state, container, false);

        downloadUploadSpeed = v.findViewById(R.id.torrent_state_speed);
        downloadCounter = v.findViewById(R.id.torrent_state_downloading);
        textViewETA = v.findViewById(R.id.torrent_state_ETA);
        textViewSeeds = v.findViewById(R.id.torrent_state_seeds);
        textViewLeechers = v.findViewById(R.id.torrent_state_leechers);
        textViewPieces = v.findViewById(R.id.torrent_state_pieces);
        textViewUploaded = v.findViewById(R.id.torrent_state_uploaded);
        shareRatio = v.findViewById(R.id.torrent_state_share_ratio);
        availability = v.findViewById(R.id.torrent_state_availability);
        textViewSeedingTime = v.findViewById(R.id.torrent_state_seeding_time);
        textViewActiveTime = v.findViewById(R.id.torrent_state_active_time);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (activity == null)
            activity = (AppCompatActivity) getActivity();

        HeavyInstanceStorage storage = HeavyInstanceStorage.getInstance(getFragmentManager());
        if (storage != null) {
            Bundle heavyInstance = storage.popData(HEAVY_STATE_TAG);
            if (heavyInstance != null) {
                advanceState = heavyInstance.getParcelable(TAG_ADVANCE_STATE);
                info = heavyInstance.getParcelable(TAG_INFO);
            }
        }
        if (savedInstanceState != null)
            basicState = savedInstanceState.getParcelable(TAG_BASIC_STATE);

        reloadInfoView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putParcelable(TAG_BASIC_STATE, basicState);

        Bundle b = new Bundle();
        b.putParcelable(TAG_ADVANCE_STATE, advanceState);
        b.putParcelable(TAG_INFO, info);
        HeavyInstanceStorage storage = HeavyInstanceStorage.getInstance(getFragmentManager());
        if (storage != null)
            storage.pushData(HEAVY_STATE_TAG, b);
    }

    public void setBasicState(BasicStateParcel basicState)
    {
        this.basicState = basicState;
        reloadInfoView();
    }

    public void setAdvanceState(AdvanceStateParcel advanceState)
    {
        this.advanceState = advanceState;
        reloadInfoView();
    }

    public void setStates(BasicStateParcel basicState, AdvanceStateParcel advanceState)
    {
        this.basicState = basicState;
        this.advanceState = advanceState;
        reloadInfoView();
    }

    public void setInfo(TorrentMetaInfo info)
    {
        this.info = info;
    }

    public void reloadInfoView()
    {
        if (activity == null || basicState == null || advanceState == null)
            return;

        String speedTemplate = activity.getString(R.string.download_upload_speed_template);
        String downloadSpeed = Formatter.formatFileSize(activity, basicState.downloadSpeed);
        String uploadSpeed = Formatter.formatFileSize(activity, basicState.uploadSpeed);
        downloadUploadSpeed.setText(String.format(speedTemplate, downloadSpeed, uploadSpeed));

        String counterTemplate = activity.getString(R.string.download_counter_template);
        String totalBytes = Formatter.formatFileSize(activity, basicState.totalBytes);
        String receivedBytes;
        if (basicState.progress == 100)
            receivedBytes = totalBytes;
        else
            receivedBytes = Formatter.formatFileSize(activity, basicState.receivedBytes);
        downloadCounter.setText(String.format(counterTemplate, receivedBytes, totalBytes, basicState.progress));

        String ETA;
        if (basicState.ETA == -1 || basicState.ETA == 0)
            ETA = Utils.INFINITY_SYMBOL;
        else
            ETA = DateFormatUtils.formatElapsedTime(activity.getApplicationContext(), basicState.ETA);
        textViewETA.setText(ETA);

        String seedsTemplate = activity.getString(R.string.torrent_peers_template);
        textViewSeeds.setText(String.format(seedsTemplate, advanceState.seeds, advanceState.totalSeeds));

        String peersTemplate = activity.getString(R.string.torrent_peers_template);
        int leechers = Math.abs(basicState.peers - advanceState.seeds);
        int totalLeechers = basicState.totalPeers - advanceState.totalSeeds;
        textViewLeechers.setText(
                String.format(peersTemplate, leechers, totalLeechers));

        String uploaded = Formatter.formatFileSize(activity, basicState.uploadedBytes);
        textViewUploaded.setText(uploaded);

        shareRatio.setText(String.format(Locale.getDefault(), "%,.3f", advanceState.shareRatio));
        availability.setText(String.format(Locale.getDefault(), "%,.3f", advanceState.availability));

        if (info != null) {
            String piecesTemplate = activity.getString(R.string.torrent_pieces_template);
            String pieceLength = Formatter.formatFileSize(activity, info.pieceLength);
            textViewPieces.setText(String.format(piecesTemplate, advanceState.downloadedPieces,
                                                 info.numPieces, pieceLength));
        }
        textViewActiveTime.setText(DateFormatUtils.formatElapsedTime(
                activity.getApplicationContext(), advanceState.activeTime));
        textViewSeedingTime.setText(DateFormatUtils.formatElapsedTime(
                activity.getApplicationContext(), advanceState.seedingTime));
    }
}