package de.mohoff.zeiterfassung.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.ArrayList;
import java.util.List;


public class ListAdapterTLA extends ArrayAdapter{

    private Context context;
    private DatabaseHelper dbHelper = null;
    List<TargetLocationArea> tlas;
    List<String> activityNames = new ArrayList<String>();
    List<EditText> editTextList = new ArrayList<EditText>();
    private boolean inEditMode = false;

    public ListAdapterTLA(Context context) {
        super(context, R.layout.activity_manage_tla_views);
        getDbHelper();
        this.tlas = dbHelper.getTLAs();
        this.activityNames = dbHelper.getDistinctActivityNames();
        this.context = context;

        //addFooterView() --> ADD NEW ACTIVITY
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return activityNames.size();
    }

    @Override
    public Object getItem(int position) {
        return activityNames.get(position);
    }

    @Override
    public boolean isEnabled(int position) {
        return super.isEnabled(position);
    }

    public static class ViewHolder {
        public ImageView iconView;
        public TextView locationView;
        public TextView activityView;
        public TextView radiusView;
        public TextView latView;
        public TextView lngView;
    }

    public void confirmEditDialog(View v, final String oldName, String newName){
        final EditText et = (EditText)v;
        new AlertDialog.Builder(context)
                .setTitle("Please confirm")
                .setMessage("Do you really want to change \"" + oldName + "\" to \"" + newName + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // update newName into DB
                    }})
                //.setNegativeButton(android.R.string.no, null).show();
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        et.setText(oldName);
                    }})
                .show();
    }

    /*public void confirmEditLocationDialog(View v, final String oldName, String newName, final ImageButton resetLocationIcon, final ImageButton deleteIcon, final ImageButton saveChangesIcon, final ImageButton discardChangesIcon){
        final EditText et = (EditText)v;
        new AlertDialog.Builder(context)
                .setTitle("Please confirm")
                .setMessage("Do you really want to change \"" + oldName + "\" to \"" + newName + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // update newName into DB
                        goStandardMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                    }})
                        //.setNegativeButton(android.R.string.no, null).show();
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        et.setText(oldName);
                        et.requestFocus();
                    }})
                .show();
    }*/

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        final String currentActivity = activityNames.get(position);
        final List<TargetLocationArea> relevantTLAs = new ArrayList<TargetLocationArea>();

        for(TargetLocationArea tla : tlas){
            if(tla.getActivityName().equals(currentActivity)){
                relevantTLAs.add(tla);
            }
        }

        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.activity_manage_tla_views, parent, false);

            LinearLayout locationWrapper = (LinearLayout) view.findViewById(R.id.locationWrapper);
            final EditText activityName = (EditText) view.findViewById(R.id.activityName);
            activityName.setText(currentActivity);
            editTextList.add(activityName);
            //final Drawable originalDrawable = activityName.getBackground();
            //activityName.setBackgroundColor(Color.TRANSPARENT);


            activityName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    //hideSoftKeyboard();
                    EditText et = (EditText) v;
                    if(!hasFocus){
                        String newActivityName = activityName.getText().toString();
                        if(!currentActivity.equals(newActivityName)){
                            confirmEditDialog(v, currentActivity, newActivityName);
                        }
                        //activityName.setEnabled(false);
                        //activityName.clearFocus();
                        et.setTextColor(Color.parseColor("#99FFFFFF"));
                    } else {
                        et.setTextColor(Color.parseColor("#FFFFFFFF"));
                    }

                }
            });

            for (int i = 0; i < relevantTLAs.size(); i++) {

                RelativeLayout relativeLayout = new RelativeLayout(context);
                RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
                relativeLayout.setLayoutParams(relativeLayoutParams);
                //relativeLayout.setBackgroundColor(Color.YELLOW);
                relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
                relativeLayout.setPadding(0,0,0,0);

                final EditText locationName = new EditText(context);
                final String locationNameString = relevantTLAs.get(i).getLocationName();
                locationName.setText(locationNameString);
                locationName.setTextColor(Color.parseColor("#99FFFFFF"));
                //locationName.setBackgroundColor(Color.TRANSPARENT);
                RelativeLayout.LayoutParams locationNameParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                locationNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                //locationNameParams.addRule(RelativeLayout.LEFT_OF, 2);
                locationName.setLayoutParams(locationNameParams);
                //locationName.setCursorVisible(true);

                editTextList.add(locationName);

                // Edit-ImageButton
                /*ImageButton editIcon = new ImageButton(context);
                editIcon.setImageResource(R.drawable.ic_action_edit);
                editIcon.setBackgroundColor(Color.TRANSPARENT);
                RelativeLayout.LayoutParams editIconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                editIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                //locationNameParams.addRule(RelativeLayout.LEFT_OF, 2);
                editIcon.setLayoutParams(editIconParams);
                editIcon.setId(i+1);
                //editIcon.setBackgroundColor(Color.BLUE);
                editIcon.setPadding(0,0,0,0);
                */

                final ImageButton deleteIcon = new ImageButton(context);
                deleteIcon.setImageResource(R.drawable.ic_action_discard);
                deleteIcon.setBackgroundColor(0x00000000);
                deleteIcon.setId(i+1);
                RelativeLayout.LayoutParams deleteIconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                deleteIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                deleteIcon.setLayoutParams(deleteIconParams);
                deleteIcon.setPadding(0,0,0,0);

                final ImageButton resetLocationIcon = new ImageButton(context);
                resetLocationIcon.setImageResource(R.drawable.ic_action_edit_location);
                resetLocationIcon.setBackgroundColor(Color.TRANSPARENT);
                resetLocationIcon.setId(i+2);
                RelativeLayout.LayoutParams editIconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                editIconParams.addRule(RelativeLayout.LEFT_OF, i+1);
                resetLocationIcon.setLayoutParams(editIconParams);
                resetLocationIcon.setPadding(0,0,0,0);

                final ImageButton discardChangesIcon = new ImageButton(context);
                discardChangesIcon.setImageResource(R.drawable.ic_action_remove);
                discardChangesIcon.setBackgroundColor(Color.TRANSPARENT);
                discardChangesIcon.setId(i+3);
                RelativeLayout.LayoutParams discardChangesIconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                discardChangesIconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                discardChangesIcon.setLayoutParams(discardChangesIconParams);
                discardChangesIcon.setPadding(0,0,0,0);

                final ImageButton saveChangesIcon = new ImageButton(context);
                saveChangesIcon.setImageResource(R.drawable.ic_action_save);
                saveChangesIcon.setBackgroundColor(Color.TRANSPARENT);
                saveChangesIcon.setId(i+4);
                RelativeLayout.LayoutParams saveChangesIconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                saveChangesIconParams.addRule(RelativeLayout.LEFT_OF, i+3);
                saveChangesIcon.setLayoutParams(saveChangesIconParams);
                saveChangesIcon.setPadding(0,0,0,0);

                goStandardMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);

                relativeLayout.addView(locationName);
                relativeLayout.addView(resetLocationIcon);
                relativeLayout.addView(deleteIcon);
                relativeLayout.addView(saveChangesIcon);
                relativeLayout.addView(discardChangesIcon);
                locationWrapper.addView(relativeLayout);

                /* listener */
                locationName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        //hideSoftKeyboard();
                        EditText et = (EditText) v;
                        if(!hasFocus){
                            inEditMode = false;
                            // LOGIK ist in save- und discard-Button drin. Um klick auf diese zu enforcen wird alles andere durch blurOutEverythingElse aug nicht-focusable gestellt
                            /*
                            String newLocationName = locationName.getText().toString();
                            if(!locationNameString.equals(newLocationName)){
                                confirmEditLocationDialog(v, locationNameString, newLocationName, resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                            } else {
                                goStandardMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                            }
                            */

                            // oder alternativ (alles in diesem scope löschen):
                            // und für saveIcon+discardIcon (im edit mode) click listener implementieren, die den dialog etc. implementieren
                        } else {
                            et.setTextColor(Color.parseColor("#FFFFFFFF"));
                            inEditMode = true;
                            goEditMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                            blurOutEverythingElse(et);
                        }
                    }
                });
                saveChangesIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newLocationName = locationName.getText().toString();
                        ViewGroup tmpLayout = (ViewGroup) v.getParent();
                        EditText exceptThis = (EditText)tmpLayout.getChildAt(0);
                        exceptThis.clearFocus();
                        exceptThis.setTextColor(Color.parseColor("#99FFFFFF"));

                        if(!locationNameString.equals(newLocationName)){
                            //confirmEditLocationDialog(v, locationNameString, newLocationName, resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);

                            // START database update with new value
                            // give feedback if update was successful or not
                        }
                        removeBlurFromEverything();
                        goStandardMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                        hideSoftKeyboard();
                    }
                });
                discardChangesIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        locationName.setText(locationNameString);
                        ViewGroup tmpLayout = (ViewGroup) v.getParent();
                        EditText exceptThis = (EditText)tmpLayout.getChildAt(0);
                        exceptThis.clearFocus();
                        exceptThis.setTextColor(Color.parseColor("#99FFFFFF"));

                        removeBlurFromEverything();
                        goStandardMode(resetLocationIcon, deleteIcon, saveChangesIcon, discardChangesIcon);
                        hideSoftKeyboard();
                    }
                });
            }
        }

        return view;
    }

    private void blurOutEverythingElse(EditText exceptThis){
        for(EditText iterateEditText : editTextList){
            if(!iterateEditText.equals(exceptThis)){
                ViewGroup parentLayout = (ViewGroup) iterateEditText.getParent();
                for(int i=0; i<parentLayout.getChildCount(); i++){
                    View tmpView = parentLayout.getChildAt(i);
                    tmpView.setAlpha(0.2f);
                    if(tmpView instanceof EditText){
                        tmpView.setFocusable(false);
                        tmpView.setFocusableInTouchMode(false);
                    }
                }
            }
        }
    }

    private void removeBlurFromEverything(){
        for(EditText iterateEditText : editTextList){
            ViewGroup parentLayout = (ViewGroup) iterateEditText.getParent();
            for(int i=0; i<parentLayout.getChildCount(); i++){
                View tmpView = parentLayout.getChildAt(i);
                tmpView.setAlpha(1);
                if(tmpView instanceof EditText){
                    tmpView.setFocusable(true);
                    tmpView.setFocusableInTouchMode(true);
                }
            }
        }
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

    public void goStandardMode(ImageButton resetLocationIcon, ImageButton deleteIcon, ImageButton saveChangesIcon, ImageButton discardChangesIcon){
        resetLocationIcon.setVisibility(View.VISIBLE);
        deleteIcon.setVisibility(View.VISIBLE);
        saveChangesIcon.setVisibility(View.GONE);
        discardChangesIcon.setVisibility(View.GONE);
    }

    public void goEditMode(ImageButton resetLocationIcon, ImageButton deleteIcon, ImageButton saveChangesIcon, ImageButton discardChangesIcon){
        resetLocationIcon.setVisibility(View.GONE);
        deleteIcon.setVisibility(View.GONE);
        saveChangesIcon.setVisibility(View.VISIBLE);
        discardChangesIcon.setVisibility(View.VISIBLE);
    }
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

}

