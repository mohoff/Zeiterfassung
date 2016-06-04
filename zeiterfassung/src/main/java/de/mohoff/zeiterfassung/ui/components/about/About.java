package de.mohoff.zeiterfassung.ui.components.about;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class About extends Fragment {
    MainActivity context;
    Button sendFeedback;

    public About() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);
        sendFeedback = (Button) v.findViewById(R.id.sendFeedback);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = (MainActivity) getActivity();
        context.fab.hide();

        sendFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment nextFragment = new SendFeedback();
                context.replaceFragment(nextFragment, true);
            }
        });
    }

    @Override
    public void onResume() {
        // TODO: What's up with that snippet? Still needed in NavigationView? -- Current status: NEEDED!
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            ((MainActivity) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        } else {
            ((MainActivity) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        }
        super.onResume();
    }
}
