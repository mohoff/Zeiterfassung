package de.mohoff.zeiterfassung.ui.components.about;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class SendFeedback extends Fragment {
    private static int SEND_FEEDBACK_REQ_CODE = 1;

    MainActivity context;
    EditText feedback;

    public SendFeedback() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // For mail sending intents, resultCode per design doesn't work reliably
        if(requestCode == SEND_FEEDBACK_REQ_CODE){// && resultCode == Activity.RESULT_OK){
            Snackbar.make(
                    context.coordinatorLayout,
                    "Feedback sent.",
                    Snackbar.LENGTH_LONG)
            .show();
            getActivity().onBackPressed();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about_send_feedback, container, false);
        feedback = (EditText) v.findViewById(R.id.msg);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = (MainActivity) getActivity();

        context.getDrawerToggle().setDrawerIndicatorEnabled(false);
        context.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context.fab.show();
        context.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_send));
        context.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = feedback.getText().toString();
                if(msg.equals("") || msg.equals("Hi, if I may speak freely: ")){
                    Snackbar.make(
                            context.coordinatorLayout,
                            "Please write a message first.",
                            Snackbar.LENGTH_LONG)
                    .show();
                } else if(msg.length() < 50){
                    Snackbar.make(
                            context.coordinatorLayout,
                            "A few more words please...",
                            Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    // Send feedback mail
                    Intent mail = new Intent(Intent.ACTION_SEND);
                    mail.setType("message/rfc822");
                    mail.putExtra(Intent.EXTRA_EMAIL, new String[] { "hoffmamo@gmail.com" });
                    mail.putExtra(Intent.EXTRA_SUBJECT, "ZeiterfassungApp, Feedback");
                    mail.putExtra(Intent.EXTRA_TEXT, msg);
                    // Send intent and show custom message for app picker dialog.
                    startActivityForResult(Intent.createChooser(mail, "Send Feedback:"), SEND_FEEDBACK_REQ_CODE);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Called when the up arrow/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
