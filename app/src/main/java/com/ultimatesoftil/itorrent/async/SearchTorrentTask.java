package com.ultimatesoftil.itorrent.async;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import com.ultimatesoftil.itorrent.adapters.SearchListItem;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

public class SearchTorrentTask extends AsyncTask <String,Void, ArrayList<SearchListItem>>{
  private String TAG="SearchTorrentTask";
  private SearchTorrentCallBack mCallBack;

    @Override
    protected void onPostExecute(ArrayList<SearchListItem> items) {
        super.onPostExecute(items);
        if(mCallBack!=null)
            mCallBack.onSuccess(items);
    }

    @Override
    protected ArrayList<SearchListItem> doInBackground(String... u) {
        ArrayList<SearchListItem> items= new ArrayList<>();
            try {

                URL url = new URL(u[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                InputStream stream = conn.getInputStream();
                Gson gson= new Gson();
                String data = convertStreamToString(stream);
                JsonArray jsonArray = gson.fromJson(data,JsonArray.class);
                for (int i = 0; i < jsonArray.size(); i++) {
                    SearchListItem item= gson.fromJson(jsonArray.get(i),SearchListItem.class);

                    items.add(item);

                }


                stream.close();


            }catch(SocketTimeoutException e){
                if(mCallBack!=null)
                    mCallBack.onError(e.getMessage());
                e.printStackTrace();

            }
            catch (Exception e) {
                if(mCallBack!=null)
                    mCallBack.onError(e.getMessage());
                e.printStackTrace();
            }


            //Your code goes here

        return items;
    }
    public void setmCallBack(SearchTorrentCallBack mCallBack) {
        this.mCallBack = mCallBack;
    }
    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    public interface SearchTorrentCallBack {
        public void onSuccess(ArrayList<SearchListItem> items);
        public void onError(String successMessage);
    }
}
