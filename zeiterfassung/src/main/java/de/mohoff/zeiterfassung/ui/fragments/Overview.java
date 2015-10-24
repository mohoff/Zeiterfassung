package de.mohoff.zeiterfassung.ui.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

import de.mohoff.zeiterfassung.locationservice.ServiceChangeListener;
import de.mohoff.zeiterfassung.locationservice.TimeslotEventListener;
import de.mohoff.zeiterfassung.ui.AdapterOverview;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.MyItemAnimator;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationDrawerListener;

public class Overview extends Fragment implements TimeslotEventListener, ServiceChangeListener{
    MainActivity parentActivity;
    AdapterOverview adapter;
    RecyclerView recList;
    LinearLayoutManager llm;

    public Overview() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);
        recList = (RecyclerView) view.findViewById(R.id.cardList);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        parentActivity = (MainActivity) getActivity();

        // Disable ItemAnimator to prevent a visual bug by calling updateFirstCardPeriodically().
        //recList.setItemAnimator(new MyItemAnimator());
        recList.setItemAnimator(null);

        llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        //recList.setHasFixedSize(true); // allows for optimizations
        adapter = new AdapterOverview(getActivity());
        recList.setAdapter(adapter);
        recList.setLayoutManager(llm);

        updateFirstCardPeriodically();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set listeners
        parentActivity.setOnTimeslotEventListener(this);
        parentActivity.serviceStatus.addListener(this);

        // Update adapter model and update UI
        adapter.updateData();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remove listeners
        parentActivity.removeOnTimeslotEventListener();
        parentActivity.serviceStatus.removeListener(this);
    }

    private void updateFirstCardPeriodically() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(getActivity() != null && adapter != null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyItemChanged(adapter.getItemCount()-1);
                        }
                    });
                }
            }
        }, 0, 1000 * 10); // Update every 10sec.
    }

    @Override
    public void onNewTimeslot(int id) {
        // Adapter retrieves updated data from DB and updates its model accordingly.
        adapter.updateData();
        // The current implementation only expects inserts, no deletions.
        // Thus the only action is to notify the adapter that an item is added.
        adapter.notifyItemInserted(adapter.getItemCount()-1);
        // Give visual feedback that a new item has beed added.
        recList.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onTimeslotSealed(int id) {
        // Only most recent Timeslot can be possibly sealed. For that the item position is known anytime.
        adapter.notifyItemChanged(adapter.getItemCount()-1);
    }

    @Override
    public void onServiceStatusEvent(boolean isRunning) {
        adapter.notifyDataSetChanged();
    }

    /*@Override
    public void StartButtonClicked() {
        // TODO: when click start->stop often repeatedly, it gets async. To fix.
        adapter.setIsServiceRunning(true);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void StopButtonClicked() {
        adapter.setIsServiceRunning(false);
        adapter.notifyDataSetChanged();
        // TODO: below not working yet.
        if(llm.findFirstVisibleItemPosition() > (adapter.getItemCount()-3)){
            recList.scrollToPosition(0);
        }
    }
    */
}
