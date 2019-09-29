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

package com.ultimatesoftil.itorrent.adapters;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.core.graphics.ColorUtils;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ramijemli.percentagechartview.PercentageChartView;
import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider;

import com.ultimatesoftil.itorrent.MainApplication;
import com.ultimatesoftil.itorrent.R;
import com.ultimatesoftil.itorrent.core.TorrentStateCode;
import com.ultimatesoftil.itorrent.core.sorting.TorrentSortingComparator;
import com.ultimatesoftil.itorrent.core.stateparcel.BasicStateParcel;
import com.ultimatesoftil.itorrent.core.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TorrentListAdapter extends SelectableAdapter<RecyclerView.ViewHolder>
{
    @SuppressWarnings("unused")
    private static final String TAG = TorrentListAdapter.class.getSimpleName();

    private Context context;
    private DownloadViewHolder.ClickListener downloadclickListener;
    private SearchViewHolder.ClickListener searchClickListener;

    /* Filtered items */
    private List<TorrentListItem> currentItems;
    private Map<String, TorrentListItem> allItems;
    private AtomicReference<TorrentListItem> curOpenTorrent = new AtomicReference<>();
    private DisplayFilter displayFilter = new DisplayFilter();
    private SearchFilter searchFilter = new SearchFilter();
    private TorrentSortingComparator sorting;
    ArrayList<TorrentListItem>temp;

    public void clearDividers() {
        for(Iterator<Map.Entry<String, TorrentListItem>> it = allItems.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, TorrentListItem> entry = it.next();
            if(entry.getValue() instanceof TorrentItemDivider) {
                it.remove();
            }
        }
    }

    public synchronized void addItemAt(int i, TorrentListItem item) {

        currentItems.add(i,item);

        notifyDataSetChanged();

    }

    public void search2(String query) {
        ArrayList<TorrentListItem> newList=new ArrayList<>();
        for (TorrentListItem userInfo : currentItems){
            String username=userInfo.getName().toLowerCase();

            if (username.contains(query)){
                newList.add(userInfo);
            }
        }
        setFilter(newList);
    }

    public enum VIEW_TYPE{ITEM_DOWNLOAD,ITEM_SEARCH,ITEM_SECTION_DIVIDER}
    public void setFilter(List<TorrentListItem> newList){
        temp=new ArrayList<>(newList);
        ArrayList<TorrentListItem>data=new ArrayList<>();
        currentItems.clear();
        currentItems.addAll(newList);
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        return currentItems.get(position) instanceof TorrentItemDivider?2:currentItems.get(position) instanceof SearchListItem?1:0;
    }

    public TorrentListAdapter(Map<String, TorrentListItem> items, Context context,
                              DownloadViewHolder.ClickListener clickListener, SearchViewHolder.ClickListener clickListener2,
                              TorrentSortingComparator sorting)
    {
        this.context = context;
        this.downloadclickListener = clickListener;
        this.searchClickListener=clickListener2;
        this.sorting = sorting;
        allItems = items;
        currentItems = new ArrayList<>(allItems.values());
        Collections.sort(currentItems, sorting);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v=null;
        VIEW_TYPE whichView = VIEW_TYPE.values()[viewType];
        switch (whichView){
            case ITEM_DOWNLOAD:
                 v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_torrent_list, parent, false);
                 return  new DownloadViewHolder(v, downloadclickListener, currentItems);

            case ITEM_SEARCH:
                 v = LayoutInflater.from(parent.getContext()).inflate(R.layout.torrent_search_items_list, parent, false);
                return new SearchViewHolder(v, searchClickListener, currentItems);
            case ITEM_SECTION_DIVIDER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_divider, parent, false);
                return new DividerViewHolder(v);

        }

      return  new DownloadViewHolder(v, downloadclickListener, currentItems);
    }

    @SuppressWarnings("ResourceType")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position)
    {

        VIEW_TYPE whichView = VIEW_TYPE.values()[baseHolder.getItemViewType()];
        switch (whichView){
            case ITEM_DOWNLOAD:{
                DownloadViewHolder holder=(DownloadViewHolder)baseHolder;
                TorrentListItem item = currentItems.get(holder.getAdapterPosition());

                Utils.setBackground(holder.indicatorCurOpenTorrent,
                        ContextCompat.getDrawable(context, android.R.color.transparent));

                TypedArray a = context.obtainStyledAttributes(new TypedValue().data, new int[] {
                        R.attr.defaultSelectRect,
                        R.attr.defaultRectRipple
                });

                if (isSelected(holder.getAdapterPosition()))
                    Utils.setBackground(holder.itemTorrentList, a.getDrawable(0));
                else
                    Utils.setBackground(holder.itemTorrentList, a.getDrawable(1));
                a.recycle();

                holder.name.setText(item.name);
                if (item.stateCode == TorrentStateCode.DOWNLOADING_METADATA) {
                    //  holder.progress.setIndeterminate(true);
                } else {
                    // holder.progress.setIndeterminate(false);
                    try {
                        holder.progress.setProgress(item.progress,true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }

                String stateString = "";
                switch (item.stateCode) {
                    case DOWNLOADING:
                        stateString = context.getString(R.string.torrent_status_downloading);
                        break;
                    case SEEDING:
                        stateString = context.getString(R.string.torrent_status_seeding);
                        break;
                    case PAUSED:
                        stateString = context.getString(R.string.torrent_status_paused);
                        break;
                    case STOPPED:
                        stateString = context.getString(R.string.torrent_status_stopped);
                        break;
                    case FINISHED:
                        stateString = context.getString(R.string.torrent_status_finished);
                        break;
                    case CHECKING:
                        stateString = context.getString(R.string.torrent_status_checking);
                        break;
                    case DOWNLOADING_METADATA:
                        stateString = context.getString(R.string.torrent_status_downloading_metadata);
                }
                holder.item.setText(stateString);

                String counterTemplate = context.getString(R.string.download_counter_ETA_template);
                String totalBytes = Formatter.formatFileSize(context, item.totalBytes);
                String receivedBytes;
                String ETA;
                if (item.ETA == -1)
                    ETA = "\u2022 " + Utils.INFINITY_SYMBOL;
                else if (item.ETA == 0)
                    ETA = "";
                else
                    ETA = "\u2022 " + DateUtils.formatElapsedTime(item.ETA);

                if (item.progress == 100)
                    receivedBytes = totalBytes;
                else
                    receivedBytes = Formatter.formatFileSize(context, item.receivedBytes);

                holder.downloadCounter.setText(String.format(counterTemplate, receivedBytes, totalBytes
                        , ETA));

                String speedTemplate = context.getString(R.string.download_upload_speed_template);
                String downloadSpeed = Formatter.formatFileSize(context, item.downloadSpeed);
                String uploadSpeed = Formatter.formatFileSize(context, item.uploadSpeed);
                holder.downloadUploadSpeed.setText(String.format(speedTemplate, downloadSpeed, uploadSpeed));

                String peersTemplate = context.getString(R.string.peers_template);
                holder.peers.setText(String.format(peersTemplate, item.peers, item.totalPeers));
                holder.setPauseButtonState(item.stateCode == TorrentStateCode.PAUSED);

                if (item.error != null) {
                    holder.error.setVisibility(View.VISIBLE);
                    String errorTemplate = context.getString(R.string.error_template);
                    holder.error.setText(String.format(errorTemplate, item.error));
                } else {
                    holder.error.setVisibility(View.GONE);
                }

                TorrentListItem curTorrent = curOpenTorrent.get();
                if (curTorrent != null) {
                    if (getItemPosition(curTorrent) == holder.getAdapterPosition() && Utils.isTwoPane(context)) {
                        if (!isSelected(holder.getAdapterPosition())) {
                            a = context.obtainStyledAttributes(new TypedValue().data, new int[]{ R.attr.curOpenTorrentIndicator });
                            Utils.setBackground(holder.itemTorrentList, a.getDrawable(0));
                            a.recycle();
                        }

                        Utils.setBackground(holder.indicatorCurOpenTorrent,
                                ContextCompat.getDrawable(context, R.color.accent));
                    }
                }
                break;
            }
            case ITEM_SEARCH:{
                SearchListItem item=(SearchListItem) currentItems.get(baseHolder.getAdapterPosition());
                SearchViewHolder holder=(SearchViewHolder)baseHolder;
                holder.size.setText(item.getSize());
                holder.title.setText(item.getTitle());
                holder.date.setText(context.getString(R.string.torrent_added_date)+": "+item.getDate());
                holder.peers.setText("peers "+String.valueOf(item.getPeers()));
                holder.seeds.setText("seeds "+item.getSeeds());

                break;
            }
            case ITEM_SECTION_DIVIDER:{
               TorrentItemDivider item=(TorrentItemDivider) currentItems.get(baseHolder.getAdapterPosition());
               DividerViewHolder holder=(DividerViewHolder) baseHolder;
               holder.title.setText(item.getTitle());
               if(item.getTitle()==context.getString(R.string.downloads_available)){
                   holder.promo.setImageDrawable(context.getDrawable(R.drawable.globe));
               }
            }
        }

    }

    public synchronized void addItem(TorrentListItem item)
    {
        if (item == null)
            return;

        TorrentListItem filtered = displayFilter.filter(item);
        if (filtered != null) {
            currentItems.add(filtered);
            Collections.sort(currentItems, sorting);
            notifyItemInserted(currentItems.indexOf(filtered));
        }
        allItems.put(item.torrentId, item);
    }

    public synchronized void addItem2(TorrentListItem item)
    {
        if (item == null)
            return;



            currentItems.add(item);

            notifyDataSetChanged();


    }

    public synchronized void addItems(Collection<TorrentListItem> items)
    {
        if (items == null)
            return;

        List<TorrentListItem> filtered = displayFilter.filter(items);
        currentItems.addAll(filtered);
        Collections.sort(currentItems, sorting);
        notifyItemRangeInserted(0, filtered.size());
        for (TorrentListItem item : items)
            allItems.put(item.torrentId, item);
    }

    /*
     * Mark the torrent as currently open.
     */

    public void markAsOpen(TorrentListItem item)
    {
        curOpenTorrent.set(item);
        notifyDataSetChanged();
    }

    public synchronized void updateItem(BasicStateParcel state,boolean sort)
    {
        if (state == null)
            return;

        TorrentListItem item = allItems.get(state.torrentId);
        if (item == null)
            item = new TorrentListItem();
        item.copyFrom(state);

        if (!currentItems.contains(item)) {
            TorrentListItem filtered = displayFilter.filter(item);
            if (filtered != null) {
                currentItems.add(item);
                Collections.sort(currentItems, sorting);
                notifyItemInserted(currentItems.indexOf(filtered));
            }
        } else {
            int position = currentItems.indexOf(item);
            if (position >= 0) {
                currentItems.remove(position);
                TorrentListItem filtered = displayFilter.filter(item);
                if (filtered != null) {
                    currentItems.add(position, item);

                    if(!sort)Collections.sort(currentItems, sorting);

                    int newPosition = currentItems.indexOf(item);
                    if (newPosition == position)
                        notifyItemChanged(position);
                    else
                        notifyDataSetChanged();
                } else {
                    notifyItemRemoved(position);
                }
            }
        }
        allItems.put(item.torrentId, item);
    }
    public synchronized int getFilteredItemsCount(){
        return currentItems.size();
    }
    public void setDisplayFilter(DisplayFilter displayFilter)
    {
        if (displayFilter == null)
            return;

        this.displayFilter = displayFilter;
        currentItems.clear();
        currentItems.addAll(displayFilter.filter(allItems.values()));
        Collections.sort(currentItems, sorting);

        notifyDataSetChanged();
    }

    public synchronized void search(String searchPattern)
    {

        if (searchPattern == null)
            return;

        searchFilter.filter(searchPattern);
    }

    public void clearAll()
    {
        allItems.clear();

        int size = currentItems.size();
        if (size > 0) {
            currentItems.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    public void deleteItem(String id)
    {
        if (id == null)
            return;

        currentItems.remove(getItem(id));
        allItems.remove(id);

        notifyDataSetChanged();
    }

    public void setSorting(TorrentSortingComparator sorting)
    {
        if (sorting == null)
            return;

        this.sorting = sorting;
        Collections.sort(currentItems, sorting);

        notifyItemRangeChanged(0, currentItems.size());
    }

    public TorrentListItem getItem(int position)
    {
        if (position < 0 || position >= currentItems.size())
            return null;

        return currentItems.get(position);
    }

    public TorrentListItem getItem(String id)
    {
        if (id == null || !allItems.containsKey(id))
            return null;

        TorrentListItem item = allItems.get(id);

        return (currentItems.contains(item) ? item : null);
    }

    public int getItemPosition(TorrentListItem item)
    {
        if (item == null)
            return -1;

        return currentItems.indexOf(item);
    }

    public int getItemPosition(String id)
    {
        if (id == null || !allItems.containsKey(id))
            return -1;

        return currentItems.indexOf(allItems.get(id));
    }

    public boolean isEmpty()
    {
        return currentItems.isEmpty();
    }

    @Override
    public int getItemCount()
    {
        return currentItems.size();
    }

    public synchronized void clearSearchItems() {

        for(Iterator<Map.Entry<String, TorrentListItem>> it = allItems.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, TorrentListItem> entry = it.next();
            if(entry.getValue() instanceof SearchListItem) {
                it.remove();
            }
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener
    {
        private int colorOne= Color.parseColor("#F44336");;
        private int colorTwo= Color.parseColor("#FFEA00");;
        private int  colorThree = Color.parseColor("#03A9F4");
        private int colorFour = Color.parseColor("#00E676");
        private AdaptiveColorProvider colorProvider = new AdaptiveColorProvider() {
            @Override
            public int provideProgressColor(float progress) {
                if (progress <= 25)
                    return colorOne;
                else if (progress <= 50)
                    return colorTwo;
                else if (progress <= 75)
                    return colorThree;
                else
                    return colorFour;
            }

            @Override
            public int provideBackgroundColor(float progress) {
                return ColorUtils.blendARGB(provideProgressColor(progress),
                        Color.BLACK,
                        .8f);
            }
//for text color match
//            @Override
//            public int provideTextColor(float progress) {
//
//                return provideProgressColor(progress);
//
//            }

            @Override
            public int provideBackgroundBarColor(float progress) {
                return ColorUtils.blendARGB(provideProgressColor(progress),
                        Color.BLACK,
                        .5f);
            }
        };
        private Context context;
        private ClickListener listener;
        private List<TorrentListItem> items;
        LinearLayout itemTorrentList;
        TextView name;
        ImageButton pauseButton;
        PercentageChartView progress;
        TextView item;
        TextView downloadCounter;
        TextView downloadUploadSpeed;
        TextView peers;
        TextView error;
        View indicatorCurOpenTorrent;
        private AnimatedVectorDrawableCompat playToPauseAnim;
        private AnimatedVectorDrawableCompat pauseToPlayAnim;
        private AnimatedVectorDrawableCompat currAnim;

        public DownloadViewHolder(View itemView, final ClickListener listener, final List<TorrentListItem> items)
        {
            super(itemView);

            this.context = itemView.getContext();
            this.listener = listener;
            this.items = items;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            playToPauseAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.play_to_pause);
            pauseToPlayAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.pause_to_play);
            itemTorrentList = itemView.findViewById(R.id.item_torrent_list);
            name = itemView.findViewById(R.id.torrent_name);
            pauseButton = itemView.findViewById(R.id.pause_torrent);
            pauseButton.setOnClickListener((View v) -> {
                int position = getAdapterPosition();
                if (listener != null && position >= 0) {
                    TorrentListItem item = items.get(position);
                    listener.onPauseButtonClicked(position, item);
                }
            });
            progress = itemView.findViewById(R.id.torrent_progress);
            progress.setAdaptiveColorProvider(colorProvider);

            //Utils.colorizeProgressBar(context, progress);
            item = itemView.findViewById(R.id.torrent_status);
            downloadCounter = itemView.findViewById(R.id.torrent_download_counter);
            downloadUploadSpeed = itemView.findViewById(R.id.torrent_download_upload_speed);
            peers = itemView.findViewById(R.id.torrent_peers);
            error = itemView.findViewById(R.id.torrent_error);
            indicatorCurOpenTorrent = itemView.findViewById(R.id.indicator_cur_open_torrent);
        }

        @Override
        public void onClick(View v)
        {
            int position = getAdapterPosition();

            if (listener != null && position >= 0) {
                TorrentListItem item = items.get(position);
                listener.onItemClicked(position, item);
            }
        }

        @Override
        public boolean onLongClick(View v)
        {
            int position = getAdapterPosition();

            if (listener != null && position >= 0) {
                TorrentListItem item = items.get(getAdapterPosition());
                listener.onItemLongClicked(position, item);

                return true;
            }

            return false;
        }

        void setPauseButtonState(boolean isPause)
        {
            AnimatedVectorDrawableCompat prevAnim = currAnim;
            currAnim = (isPause ? pauseToPlayAnim : playToPauseAnim);
            pauseButton.setImageDrawable(currAnim);
            if (currAnim != prevAnim)
                currAnim.start();
        }

        public interface ClickListener
        {
            void onItemClicked(int position, TorrentListItem item);

            boolean onItemLongClicked(int position, TorrentListItem item);

            void onPauseButtonClicked(int position, TorrentListItem item);
        }
    }

    public static class  SearchViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener
    {

        private Context context;
        private ClickListener listener;
        private List<TorrentListItem> items;
        LinearLayout itemTorrentList;
        ImageButton downloadButton;
        TextView seeds,peers,title,size,date;


        public SearchViewHolder(View itemView, final ClickListener listener, final List<TorrentListItem> items)
        {
            super(itemView);

            this.context = itemView.getContext();
            this.listener = listener;
            this.items = items;
            itemView.setOnClickListener(this);


            downloadButton=itemView.findViewById(R.id.item_download_click);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SearchListItem item = (SearchListItem) items.get(getAdapterPosition());
                    listener.onDownloadButtonClicked(getAdapterPosition(), item);
                }
            });
            seeds=itemView.findViewById(R.id.seeds);
            peers=itemView.findViewById(R.id.peers);
            title=itemView.findViewById(R.id.item_title);
            date=itemView.findViewById(R.id.item_date);
            size=itemView.findViewById(R.id.item_size);
        }

        @Override
        public void onClick(View v)
        {
            int position = getAdapterPosition();
            Toast.makeText(MainApplication.getAppContext(),String.valueOf(v.getId()),Toast.LENGTH_SHORT).show();
            if (listener != null && position >= 0) {
               SearchListItem item = (SearchListItem) items.get(position);
               listener.onItemClicked(position,item);
            }
        }





        public interface ClickListener
        {
            void onItemClicked(int position,SearchListItem item);



            void onDownloadButtonClicked(int position, SearchListItem item);
        }
    }
    public static class DividerViewHolder extends RecyclerView.ViewHolder

    {



        TextView title;
        ImageView promo;

        public DividerViewHolder(View itemView)
        {
            super(itemView);

            promo=itemView.findViewById(R.id.prm);
           title=itemView.findViewById(R.id.title);




        }








    }
    public static class DisplayFilter
    {
        TorrentStateCode constraintCode;

        /* Without filtering */
        public DisplayFilter() { }

        public DisplayFilter(TorrentStateCode constraint)
        {
            constraintCode = constraint;
        }

        public List<TorrentListItem> filter(Collection<TorrentListItem> items)
        {
            Log.w(TAG,"filtering");
            List<TorrentListItem> filtered = new ArrayList<>();
            if (items != null) {
                if (constraintCode != null) {
                    if(constraintCode==TorrentStateCode.SEEDING)
                    for (TorrentListItem item : items) {

                        if (constraintCode == TorrentStateCode.SEEDING) {

                            if (item.progress >= 100) {
                                filtered.add(item);

                            }
                        }
                        if (item.stateCode == constraintCode) {
                            filtered.add(item);
                        }

                    }
                } else {
                    filtered.addAll(items);
                }
            }

            return filtered;
        }

        public TorrentListItem filter(TorrentListItem item)
        {

            if (item == null)
                return null;

            if (constraintCode != null) {

                if (item.stateCode == constraintCode)
                    return item;
            } else {
                return item;
            }

            return null;
        }
    }

    private class SearchFilter extends Filter
    {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence)
        {
            currentItems.clear();

            if (charSequence.length() == 0) {
                currentItems.addAll(displayFilter.filter(allItems.values()));
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for (TorrentListItem item : allItems.values())
                    if (item.name.toLowerCase().contains(filterPattern))
                        currentItems.add(item);
            }
            Collections.sort(currentItems, sorting);

            return new FilterResults();
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults)
        {
            notifyDataSetChanged();
        }
    }


}
