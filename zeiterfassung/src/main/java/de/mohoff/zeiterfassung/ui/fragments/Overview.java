package de.mohoff.zeiterfassung.ui.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mohoff.zeiterfassung.ui.CardAdapterOverview;
import de.mohoff.zeiterfassung.ui.MyItemAnimator;
import de.mohoff.zeiterfassung.R;

public class Overview extends Fragment {

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
        View view =inflater.inflate(R.layout.fragment_overview, container, false);
        RecyclerView recList = (RecyclerView) view.findViewById(R.id.cardList);
        recList.setItemAnimator(new MyItemAnimator());

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        //recList.setHasFixedSize(true); // allows for optimizations
        recList.setAdapter(new CardAdapterOverview(getActivity()));
        //recList.setLayoutManager(new LinearLayoutManager(getActivity()));


        recList.setLayoutManager(llm);

        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
