package com.hnxlabs.csnt.youstream;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.ui.PagedListView;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PromotedItem;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.hnxlabs.csnt.youstream.adapters.CardsAdapter;
import com.hnxlabs.csnt.youstream.data.TrackItem;
import com.hnxlabs.csnt.youstream.listeners.FragmentsLifecyleListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ahmed abrar on 2/4/18.
 */

public class SearchFragment extends CarFragment {
    private final String TAG = "SearchFragment";
    private CardsAdapter mAdapter;
    private ArrayList<TrackItem> searchList;
    private PagedListView mPagedListView;
    private int selectedPos = RecyclerView.NO_POSITION;
    private String mSelectedVideoId = "";
    private YouTube mYoutube;
    private OnMotion onMotion;
    private SpinKitView mProgressBar;

    public SearchFragment() {
        // Required empty public constructor
        searchList = new ArrayList<>();
        mAdapter = new CardsAdapter();
    }

    public String getSelectedVideoId(){
        return this.mSelectedVideoId;
    }

    private void setFocus(){
        if(mPagedListView != null)
            mPagedListView.requestFocus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setFocus();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
        setTitle(R.string.app_name);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.searchview_fragment, container, false);
        mProgressBar = v.findViewById(R.id.youtubeLoading);
        mPagedListView = v.findViewById(R.id.pagedView);
        mAdapter.setLifecyleListener(getFragmentsLifecyleListener());
        mPagedListView.setAdapter(mAdapter);
        mPagedListView.setFocusable(true);
        mPagedListView.setEnabled(true);
        onMotion = new OnMotion();
        mPagedListView.setOnGenericMotionListener(onMotion);
        setFocus();
        return v;
    }

    public void startSearch(String query){
        mProgressBar.setVisibility(View.VISIBLE);
        new Tasker(mAdapter).execute(query);
    }

    private class OnMotion implements View.OnGenericMotionListener {

        @Override
        public boolean onGenericMotion(View view, MotionEvent motionEvent) {
            Log.d(TAG, "setting view selected");
            float direction = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL);
            // set the current position within bounds
            if (selectedPos > mAdapter.getItemCount())
                selectedPos = mAdapter.getItemCount();

            if (selectedPos < 0)
                selectedPos = -1;

            if (direction == 1.0f) { //direction down
                mAdapter.select(selectedPos, true);
                mPagedListView.scrollToPosition(++selectedPos);
            } else if (direction == -1.0f && selectedPos >=0) {
                mAdapter.select(selectedPos, false);
                mPagedListView.scrollToPosition(--selectedPos);
            }
            mSelectedVideoId = mAdapter.getVideoId(selectedPos);
            return true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        getCarUiController().getSearchController().showSearchBox();
        getCarUiController().getStatusBarController().showAppHeader();
        getCarUiController().getStatusBarController().showConnectivityLevel();
        getCarUiController().getStatusBarController().setAppBarAlpha(0.8f);
        setFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        setFocus();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    public class Tasker extends AsyncTask<String, String, CardsAdapter> {

        CardsAdapter adapter;
        public Tasker(CardsAdapter adapter) {
            this.adapter = adapter;
        }
        @Override
        protected CardsAdapter doInBackground(String... strings) {
            try {
                mYoutube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) throws IOException {
                    }
                }).setApplicationName("youtube-39568").build();
                YouTube.Search.List search = mYoutube.search().list("id,snippet");
                search.setKey("AIzaSyAOEG8BIIFYoS8NZ-gUqVertSHj01rE18g");
                search.setQ(strings[0]);
                search.setType("video");
                search.setMaxResults(30l);
                search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url, snippet/channelTitle)");
                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResultList = searchResponse.getItems();
                this.adapter.clearAll();
                selectedPos = RecyclerView.NO_POSITION;
                for(SearchResult s: searchResultList) {
                    this.adapter.add(new TrackItem(s));
                }

            }catch (Exception e) {
                Log.e("searchgrag", e.getMessage());
            }
            return this.adapter;
        }

        protected void onPostExecute(CardsAdapter result) {
            mProgressBar.setVisibility(View.GONE);
            result.notifyDataSetChanged();
        }
    }


}