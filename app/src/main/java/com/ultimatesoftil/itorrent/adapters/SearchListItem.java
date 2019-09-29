package com.ultimatesoftil.itorrent.adapters;

public class SearchListItem extends TorrentListItem{

    private String size;
    private String time;
    private String seeds;
    protected String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMagnet() {
        return magnet;
    }

    public void setMagnet(String magnet) {
        this.magnet = magnet;
    }

    private String magnet;

    public SearchListItem() {
    }

    public SearchListItem(String title, String size, String date, int peers, String seeds) {
        this.peers=peers;

        this.size = size;
        this.time = date;
        this.seeds = seeds;
    }



    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDate() {
        return time;
    }

    public void setDate(String date) {
        this.time = date;
    }



    public String getSeeds() {
        return seeds;
    }

    public void setSeeds(String seeds) {
        this.seeds = seeds;
    }


}
