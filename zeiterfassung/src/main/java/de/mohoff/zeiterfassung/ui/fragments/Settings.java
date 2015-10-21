package de.mohoff.zeiterfassung.ui.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class Settings extends Fragment {
    Context context;
    DatabaseHelper dbHelper;

    Button deleteAllTimeslotsButton;

    public Settings() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        deleteAllTimeslotsButton = (Button) view.findViewById(R.id.deleteAllTimeslotsButton);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getActivity();
        getDbHelper(context);

        deleteAllTimeslotsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Execute delete on DB
                                if(dbHelper.deleteAllTimeslots() == 1){
                                    GeneralHelper.showToast(context, "Deleted entries successfully.");
                                    dialog.dismiss();
                                } else {
                                    GeneralHelper.showToast(context, "Could not delete entries.");
                                }
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Cancel action
                                dialog.dismiss();
                            }
                        })
                        .setTitle("Delete all tracking entries")
                        .setMessage("Do you really want to delete all tracking entries visible in 'Overview'?")
                        .create();
                alertDialog.show();
            }
        });
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
