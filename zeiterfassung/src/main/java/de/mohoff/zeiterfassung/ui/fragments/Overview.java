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

import de.mohoff.zeiterfassung.ui.AdapterOverview;
import de.mohoff.zeiterfassung.ui.MyItemAnimator;
import de.mohoff.zeiterfassung.R;

public class Overview extends Fragment {
    AdapterOverview adapter;
    RecyclerView recList;

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

        // Disable ItemAnimator to prevent a visual bug by calling updateFirstCardPeriodically().
        //recList.setItemAnimator(new MyItemAnimator());
        recList.setItemAnimator(null);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        //recList.setHasFixedSize(true); // allows for optimizations
        adapter = new AdapterOverview(getActivity());
        recList.setAdapter(adapter);
        recList.setLayoutManager(llm);

        updateFirstCardPeriodically();
    }

    private void updateFirstCardPeriodically() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(getActivity() != null){
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
}
