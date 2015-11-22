package de.mohoff.zeiterfassung.ui.components.overview;

import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

import de.mohoff.zeiterfassung.locationservice.ServiceChangeListener;
import de.mohoff.zeiterfassung.locationservice.TimeslotEventListener;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.R;

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

        // Set FAB colorBarIcon and click listener
        parentActivity.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_search_black_24dp));
        parentActivity.fab.show();
        /*context.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    centerMapTo(currentLocation.getPosition());
                }
            }
        });*/



        updateFirstCardPeriodically();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!parentActivity.serviceStatus.isRunning()){
            Snackbar.make(
                    parentActivity.coordinatorLayout,
                    getString(R.string.service_not_running),
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        // Set listeners
        parentActivity.setOnTimeslotEventListener(this);
        parentActivity.serviceStatus.addListener(this);

        // Update adapter model and update UI
        adapter.updateData();
        adapter.notifyDataSetChanged();
        // Scroll to top
        recList.scrollToPosition(adapter.getItemCount()-1);
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
        adapter.notifyItemInserted(adapter.getItemCount()-2);
        // Update the 2nd list element. Its endtime and enddate might have TYPEFACE.ITALIC still.
        adapter.notifyItemChanged(adapter.getItemCount()-2);
        adapter.notifyItemChanged(adapter.getItemCount()-3);
        // Give visual feedback that a new item has beed added: Scroll smoothly to top
        recList.smoothScrollToPosition(adapter.getItemCount()-1);
    }

    @Override
    public void onTimeslotSealed(int id) {
        // Only most recent Timeslot can be possibly sealed. For that the item position is known anytime.
        //adapter.notifyItemRangeChanged(adapter.getItemCount());
        adapter.notifyItemChanged(adapter.getItemCount()-1);
        adapter.notifyItemChanged(adapter.getItemCount()-2);
    }

    @Override
    public void onServiceStatusEvent(boolean isRunning) {
        if(isRunning){
            adapter.notifyItemRemoved(adapter.getItemCount() - 1);
        } else {
            adapter.notifyItemInserted(adapter.getItemCount() - 1);
        }

        //adapter.notifyItemChanged(adapter.getItemCount()-2);
        if(!isRunning){
            // Because a reverse layout is used, the most upper card has index n and
            // the card at the bottom of the list has index 0. Taking screen height
            // into account, firstVisible is approx. n-4 by default.
            // The list will be scrolled top when current scroll position is not too far
            // away from the very top.

            // findFirstVisibleItemPosition() returns first (partly) visible item position
            int firstVisible = llm.findFirstVisibleItemPosition();
            int itemCount = adapter.getItemCount();
            if(firstVisible > itemCount-10){
                //recList.scrollToPosition(itemCount-1);
                recList.smoothScrollToPosition(itemCount-1);
            }
        }
    }
}
