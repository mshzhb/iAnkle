package edu.utoronto.cimsah.myankle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.date.DatePickerDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.DialogStyleHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;

public class FragmentProfileInjuries extends Fragment implements OnCheckedChangeListener,
        OnClickListener, DatePickerDialog.OnDateSetListener {

    public static final String TAG = FragmentProfileInjuries.class.getSimpleName();
    public static final String ARG_PROFILE_MODE = "profile_mode";

    // The request codes with which to launch child activities
    public static final int TUTORIAL_REQUEST_CODE = 1;

    private String mDateInjuryLeft = "", mDateInjuryRight = "";
    private String mMostRecentInjuryLeft = "", mMostRecentInjuryRight = "";

    // Argument key-strings for populating the savedInstance bundle
    private static final String SAVED_LEFT_INJURY_DATE = "saved_left_injury_date";
    private static final String SAVED_RIGHT_INJURY_DATE = "saved_right_injury_date";
    private static final String SAVED_DISPLAY_LEFT_STRING = "saved_display_left_string";
    private static final String SAVED_DISPLAY_RIGHT_STRING = "saved_display_right_string";

    // User parameters
    private String mProfileMode = null;
    private int mUserId = -1;

    // Layout objects
    private TextView mLabelTitle, mLabelDesc, mLabelDateInjuryLeft, mLabelDateInjuryRight;
    private CheckBox mCheckboxLeft, mCheckboxRight;
    private Button mButtonDiscardLeft, mButtonDiscardRight, mButtonSubmit;

    // Miscellaneous objects
    private DatabaseHelper mDatabaseHelper = null;
    SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    /** Instantiate FragmentProfileInjuries */
    public static FragmentProfileInjuries newInstance(String profileMode) {

        FragmentProfileInjuries myFragment = new FragmentProfileInjuries();

        // Create and populate a bundle to instantiate the fragment with
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_MODE, profileMode);
        myFragment.setArguments(args);

        return myFragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the date-string and display labels to the bundle
        outState.putString(SAVED_LEFT_INJURY_DATE, mDateInjuryLeft);
        outState.putString(SAVED_RIGHT_INJURY_DATE, mDateInjuryRight);

        outState.putString(SAVED_DISPLAY_LEFT_STRING, mLabelDateInjuryLeft.getText().toString());
        outState.putString(SAVED_DISPLAY_RIGHT_STRING, mLabelDateInjuryRight.getText().toString());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and maintain a new DatabaseHelper object
        mDatabaseHelper = new DatabaseHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout file
        View view = inflater.inflate(R.layout.fragment_profile_injuries, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Fetch the User ID from SharedPreferences
        mUserId = PrefUtils.getIntPreference(getActivity(), PrefUtils.LOCAL_USER_ID);

        // Fetch the profile mode from the arguments
        mProfileMode = getArguments().getString(ARG_PROFILE_MODE);

        // Link the layout objects to the corresponding UI elements
        mLabelTitle = (TextView) getView().findViewById(R.id.fragment_profile_injuries_label_title);
        mLabelDesc = (TextView) getView().findViewById(R.id.fragment_profile_injuries_label_desc);
        mLabelDateInjuryLeft = (TextView) getView().findViewById(R.id.fragment_profile_injuries_label_left_date);
        mLabelDateInjuryRight = (TextView) getView().findViewById(R.id.fragment_profile_injuries_label_right_date);

        mCheckboxLeft = (CheckBox) getView().findViewById(R.id.fragment_profile_injuries_checkbox_left);
        mCheckboxRight = (CheckBox) getView().findViewById(R.id.fragment_profile_injuries_checkbox_right);

        mButtonDiscardLeft = (Button) getView().findViewById(R.id.fragment_profile_injuries_button_discard_left);
        mButtonDiscardRight = (Button) getView().findViewById(R.id.fragment_profile_injuries_button_discard_right);
        mButtonSubmit = (Button) getView().findViewById(R.id.fragment_profile_injuries_button_submit);

        // Register the button listeners
        mButtonDiscardLeft.setOnClickListener(this);
        mButtonDiscardRight.setOnClickListener(this);
        mButtonSubmit.setOnClickListener(this);

        // Register the check-box listeners
        mCheckboxLeft.setOnCheckedChangeListener(this);
        mCheckboxRight.setOnCheckedChangeListener(this);

        // The activity is launched in 'Create' mode
        if (mProfileMode.equals(ActivityProfile.CREATE_MODE)) {

            // Explicitly set the title label and description
            mLabelTitle.setText(getActivity().getString(R.string.profile_injuries_title_create));
            mLabelDesc.setText(getActivity().getString(R.string.profile_injuries_body_create));

        // The activity is launched in 'Update' mode
        } else if (mProfileMode.equals(ActivityProfile.UPDATE_MODE)) {

            // Explicitly set the title label and description
            mLabelTitle.setText(getActivity().getString(R.string.profile_injuries_title_update));
            mLabelDesc.setText(getActivity().getString(R.string.profile_injuries_body_update));

            // Get the most recent injury dates from the database
            mMostRecentInjuryLeft = getMostRecentInjuryDate("Left");
            mMostRecentInjuryRight = getMostRecentInjuryDate("Right");

            // Set the default ankle-injury labels
            mLabelDateInjuryLeft.setText(mMostRecentInjuryLeft);
            mLabelDateInjuryRight.setText(mMostRecentInjuryRight);
        }

        // If the fragment is being re-created from a saved state
        if(savedInstanceState != null) {

            // Retrieve the date-strings from the savedInstance bundle
            mDateInjuryLeft = savedInstanceState.getString(SAVED_LEFT_INJURY_DATE, "");
            mDateInjuryRight = savedInstanceState.getString(SAVED_RIGHT_INJURY_DATE, "");

            // Retrieve the display labels
            String displayStringLeft = savedInstanceState.getString(SAVED_DISPLAY_LEFT_STRING, "");
            String displayStringRight = savedInstanceState.getString(SAVED_DISPLAY_RIGHT_STRING, "");

            // If the left injury date-string was not empty
            if(!mDateInjuryLeft.isEmpty()) {

                // Make the 'Discard Left' button visible
                mButtonDiscardLeft.setVisibility(Button.VISIBLE);
            }

            // If the right injury date-string was not empty
            if(!mDateInjuryRight.isEmpty()) {

                // Make the 'Discard Right' button visible
                mButtonDiscardRight.setVisibility(Button.VISIBLE);
            }

            // Restore the corresponding text labels
            mLabelDateInjuryLeft.setText(displayStringLeft);
            mLabelDateInjuryRight.setText(displayStringRight);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDatabaseHelper.close();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            // When the 'Discard Left' button is pressed
            case R.id.fragment_profile_injuries_button_discard_left:

                // Un-check the 'Left' check-box
                mCheckboxLeft.setChecked(false);

                break;

            // When the 'Discard Right' button is pressed
            case R.id.fragment_profile_injuries_button_discard_right:

                // Un-check the 'Right' check-box
                mCheckboxRight.setChecked(false);

                break;

            // When the finish button is pressed
            case R.id.fragment_profile_injuries_button_submit:

                // The position of the Personal fragment in the ViewPager
                int personalFragmentPosition = 0;

                /* In 'Update' mode, offset the position (from 0) by the
                 * difference in number of fragments displayed in the two modes */
                if (mProfileMode.equals(ActivityProfile.UPDATE_MODE)) {
                    personalFragmentPosition = ActivityProfile.PERSONAL_STARTS_AT;
                }

                /* Fetch a reference to the Personal Fragment. Note: No sanity checks are
                 * performed to ensure existence of the fragment. Every instance of the
                 * fragment is expected to be tied to a profile (personal) fragment */
                FragmentProfilePersonal personalFragment = (FragmentProfilePersonal)
                        ActivityProfile.mSectionsPagerAdapter.getFragment(personalFragmentPosition);

                // Debug messages
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Left date : " + mDateInjuryLeft +
                               " and Right date : " + mDateInjuryRight);
                }

                // Check if the person's details on the previous page are valid
                if (personalFragment.hasFilledOut()) {

                    // If the activity is launched in 'Create' mode
                    if (mProfileMode.equals(ActivityProfile.CREATE_MODE)) {

                        // If the entered name is available (not taken by another user)
                        if (personalFragment.isAvailableName()) {

                            // In debug mode, skip the tutorial
                            if (BuildConfig.DEBUG) {

                                // Simulate successful completion of the tutorial
                                onActivityResult(TUTORIAL_REQUEST_CODE, Activity.RESULT_OK, null);

                            } else {

                                // Launch ActivityTutorial and listen for a result
                                Intent tutorialIntent = new Intent(getActivity(), ActivityTutorial.class);
                                startActivityForResult(tutorialIntent, TUTORIAL_REQUEST_CODE);
                            }

                        // Else if the username is taken
                        } else {

                            // Show an appropriate error
                            Toast.makeText(getActivity(), "A user with that name already exists!",
                                    Toast.LENGTH_SHORT).show();

                            // Return to the 'Personal' fragment
                            ActivityProfile.mViewPager.setCurrentItem(personalFragmentPosition);
                        }

                    // If the activity is launched in 'Update' mode
                    } else {

                        // If the name is either unchanged or is available, update the user's data
                        if (personalFragment.isNameUnchanged() || personalFragment.isAvailableName()) {

                            // Update the user in the database
                            personalFragment.createOrUpdateUser();

                            // Create a new entry in the 'Injuries' table for the entered injury
                            saveInjuryData();

                            // Create a toast indicating success and finish the current activity
                            Toast.makeText(getActivity(), "Profile updated successfully",
                                    Toast.LENGTH_SHORT).show();

                            getActivity().finish();

                        // The username has been changed, and is not available
                        } else {

                            // Show an appropriate error
                            Toast.makeText(getActivity(), "A user with that name already exists!",
                                    Toast.LENGTH_SHORT).show();

                            // Return to the 'Personal' fragment
                            ActivityProfile.mViewPager.setCurrentItem(personalFragmentPosition);
                        }
                    }

                } else {

                    // If the details are invalid, return to the previous page
                    ActivityProfile.mViewPager.setCurrentItem(personalFragmentPosition);
                }

                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        /* Retrieve the position of this fragment, and that of the
         * fragment currently being displayed in the ViewPager */
        int selectedItemPosition = ActivityProfile.mViewPager.getCurrentItem();
        int thisItemPosition = ActivityProfile.NUM_PERSONAL;

        // In 'Update' mode, add the offset (based on the number of preceding fragments
        if (mProfileMode.equals(ActivityProfile.UPDATE_MODE)) {
            thisItemPosition = ActivityProfile.INJURIES_STARTS_AT;
        }

        /* If the check-box was recently checked and this is the currently displayed fragment in
         * the activity's viewPager (this accounts for pre-loading of fragments in the pager) */
        if(isChecked && (thisItemPosition == selectedItemPosition)) {

            // Get the current date
            Calendar calendar = Calendar.getInstance();
            int thisYear = calendar.get(Calendar.YEAR);
            int thisMonth = calendar.get(Calendar.MONTH);
            int thisDay = calendar.get(Calendar.DAY_OF_MONTH);

            // Initialize a new DatePicker object with the current date
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                    this, thisYear, thisMonth, thisDay);

            // Configure the dialog properties
            datePickerDialog.setCancelable(false);
            datePickerDialog.setYearRange(thisYear - 1, thisYear);

            // If the 'Left' check-box was checked, show the corresponding date-picker dialog
            if(buttonView.getId() == R.id.fragment_profile_injuries_checkbox_left) {
                datePickerDialog.show(getActivity().getFragmentManager(), "date-picker-left");
            }

            // If the 'Right' check-box was checked, show the corresponding date-picker dialog
            if(buttonView.getId() == R.id.fragment_profile_injuries_checkbox_right) {
                datePickerDialog.show(getActivity().getFragmentManager(), "date-picker-right");
            }

        // If the check-box was recently un-checked, reset the corresponding TextView and date-string
        } else if(!isChecked){

            // The 'Left' check-box was un-checked
            if(buttonView.getId() == R.id.fragment_profile_injuries_checkbox_left) {

                // Reset the date-string and TextView
                mDateInjuryLeft = "";
				mLabelDateInjuryLeft.setText(mMostRecentInjuryLeft);
				mButtonDiscardLeft.setVisibility(Button.INVISIBLE);
            }

            // The 'Right' check-box was un-checked
            if(buttonView.getId() == R.id.fragment_profile_injuries_checkbox_right) {

                // Reset the date-string and TextView
				mDateInjuryRight = "";
				mLabelDateInjuryRight.setText(mMostRecentInjuryRight);
				mButtonDiscardRight.setVisibility(Button.INVISIBLE);
            }
        }
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int month, int day) {

        // Create and configure a Calendar object with the selected date
        Calendar selectedInjuryCalendar = Calendar.getInstance();
        selectedInjuryCalendar.clear();
        selectedInjuryCalendar.set(year, month, day);

        String selectedAnkleSide = "";
        String selectedInjuryDate = "";

        // The left-ankle date-selection dialog was displayed
        if(dialog.getTag().equals("date-picker-left")) {

            // Generate the selected injury date-string
            mDateInjuryLeft = DateFormat.format("yyyy-MM-dd",
                    selectedInjuryCalendar).toString();

            // Set the selected ankle-side and injury dates
            selectedAnkleSide = "Left";
            selectedInjuryDate = mDateInjuryLeft;

            // Update the corresponding (left) text-label
            mLabelDateInjuryLeft.setText(DateFormat.format("dd - MM - yyyy",
                    selectedInjuryCalendar).toString());

            // Make the 'Discard Left' button visible
            mButtonDiscardLeft.setVisibility(Button.VISIBLE);

        } else if(dialog.getTag().equals("date-picker-right")) {

            // Generate the selected injury date-string
            mDateInjuryRight = DateFormat.format("yyyy-MM-dd",
                    selectedInjuryCalendar).toString();

            // Set the selected ankle-side and injury dates
            selectedAnkleSide = "Right";
            selectedInjuryDate = mDateInjuryRight;

            // Update the corresponding (right) text-label
            mLabelDateInjuryRight.setText(DateFormat.format("dd - MM - yyyy",
                    selectedInjuryCalendar).toString());

            // Make the 'Discard Right' button visible
            mButtonDiscardRight.setVisibility(Button.VISIBLE);
        }

        // If the entered injury date is not within the last 180 days
        if(!inDayRange(year, month, day, 180)) {

            // Un-check the corresponding check-box
            if (selectedAnkleSide.equals("Left")) {
                mCheckboxLeft.setChecked(false);
            } else {
                mCheckboxRight.setChecked(false);
            }

            // Create a toast indicating why the injury was not recorded
            Toast.makeText(getActivity(), "That date was not in the past 6 months",
                    Toast.LENGTH_SHORT).show();

            // If the entered injury date is not the most recent one
        } else if(!isMostRecentInjury(selectedAnkleSide, selectedInjuryDate)) {

            // Un-check the corresponding check-box
            if (selectedAnkleSide.equals("Left")) {
                mCheckboxLeft.setChecked(false);
            } else {
                mCheckboxRight.setChecked(false);
            }

            // Create a toast indicating why the injury was not recorded
            Toast.makeText(getActivity(), "Entered injury was not the most recent one",
                    Toast.LENGTH_SHORT).show();

            // If the injury is very recent (last 30 days)
        } else if(inDayRange(year, month, day, 30)) {

            // Create an injury warning alert
            createInjuryWarningAlert();
        }
    }

    /** Check if the parametrized date is in the acceptable range */
    private boolean inDayRange(int year, int month, int day, int daysValid) {

        // Get the current date calendar
        Calendar calCurDate = Calendar.getInstance();

        // Make the injury date calendar
        Calendar calInjDate = Calendar.getInstance();
        calInjDate.set(year, month, day);

        // Get the number of days that have passed since the injury
        int days = Math.round(((calCurDate.getTime().getTime() - calInjDate.getTime().getTime()) / (1000 * 60 * 60 * 24)));

        // Injury date is valid if it's in the past and less than 6 months ago
        return ((days >=0) && (days <= daysValid));
    }

    /** Create a new entry in the 'injuries' table corresponding to the current user */
    private void saveInjuryData() {

        ContentValues args = new ContentValues();

        // Left ankle
        if(mCheckboxLeft.isChecked() && !mDateInjuryLeft.isEmpty()) {

            // Clear any existing arguments
            args.clear();

            // Bundle and insert the entered fields into the database
            args.put("userId", mUserId);
            args.put("ankleSide", "Left");
            args.put("injuryDate", mDateInjuryLeft);
            args.put("severity", 0);

            mDatabaseHelper.getWritableDatabase().insert("injuries", null, args);
        }

        // Right ankle
        if(mCheckboxRight.isChecked() && !mDateInjuryRight.isEmpty()) {

            // Clear any existing arguments
            args.clear();

            // Bundle and insert the entered fields into the database
            args.put("userId", mUserId);
            args.put("ankleSide", "Right");
            args.put("injuryDate", mDateInjuryRight);
            args.put("severity", 0);

            mDatabaseHelper.getWritableDatabase().insert("injuries", null, args);
        }
    }

    /** Checks if the injury date entered by the user is the most recent one. If so, returns true.
     * Else, returns false */
    private boolean isMostRecentInjury(String enteredAnkleSide, String enteredInjuryDateString) {

        boolean isMostRecent = true;
        String lastInjuryDateString = "";

        /* Fetch the most recent injury date from the database. note: this expects
         * the dates to be entered in the database in chronological order */
        Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
                "SELECT injuryDate FROM injuries WHERE userId = " + mUserId +
                        " AND ankleSide = " + "'" + enteredAnkleSide + "'" + " ORDER BY " +
                        " _id DESC", null);

        // If the cursor has at least one row entry, get the most recent one
        if(cur != null && cur.moveToFirst()) {
            lastInjuryDateString = cur.getString(0);
        }

        // Close the cursor
        cur.close();

        // If the latest injury date-string is not empty
        if(!lastInjuryDateString.isEmpty()) {

            try {

				// Parse the ankle-injury date-strings as date objects
                Date enteredInjuryDate = originalFormat.parse(enteredInjuryDateString);
                Date lastInjuryDate = originalFormat.parse(lastInjuryDateString);

                // If the entered date precedes or equals the last injury date
                if(!enteredInjuryDate.after(lastInjuryDate)) {
                    isMostRecent = false;
                }

            } catch (ParseException e) {

                // In debug mode, display the error
                if(BuildConfig.DEBUG) e.printStackTrace();
                isMostRecent = false;
            }
        }

        return isMostRecent;
    }

    /** Returns the most recent ankle-injury date-string from the database, corresponding to the
     * current user and parametrized ankle-side. If no such entry exists, returns an empty string */
    private String getMostRecentInjuryDate(String ankleSide) {

        // The date-string displayed to the user (must be an intuitive format)
        String displayRecentDateString = "";

        // Fetch the most recent injury date for that particular ankle from the database
        Cursor cursor = mDatabaseHelper.getReadableDatabase().rawQuery(
                "SELECT injuryDate FROM injuries WHERE userId = ? AND ankleSide = ? " +
                        " ORDER BY _id DESC",
                new String[] {String.valueOf(mUserId), ankleSide});

        // If at-least one recorded injury exists
        if(cursor != null && cursor.moveToFirst()) {

            try {

                // Parse the original date string using the specified format
                Date date = originalFormat.parse(cursor.getString(0));

                // Generate the date-string to be displayed
                displayRecentDateString = "Last Injury\n" + DateFormat.format("dd - MM - yyyy",
                        date).toString();

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Close the cursor
        cursor.close();

        // Return the formatted date string
        return displayRecentDateString;
    }

    @Override
    // Called when the child activity finishes with a response to the parent's call
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // The position of the Personal fragment in the ViewPager
        int personalFragmentPosition = 0;

        /* In 'Update' mode, offset the position (from 0) by the difference
         * in number of fragments displayed in the two modes */
        if (mProfileMode.equals(ActivityProfile.UPDATE_MODE)) {
            personalFragmentPosition = ActivityProfile.PERSONAL_STARTS_AT;
        }

        // Fetch a reference to the personal fragment. This is expected to exist (see above)
        FragmentProfilePersonal personalFragment = (FragmentProfilePersonal)
                ActivityProfile.mSectionsPagerAdapter.getFragment(personalFragmentPosition);

        // The tutorial activity finished with a response
        if(requestCode == TUTORIAL_REQUEST_CODE) {

            // Tutorial was completed successfully
            if(resultCode == Activity.RESULT_OK) {

                // Create a new user in the database
                personalFragment.createOrUpdateUser();

                // Get the created user's local ID from the database
                Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
                        "SELECT _id FROM users ORDER BY _id DESC", null);

                if(cur != null && cur.moveToFirst()) {

                    mUserId = cur.getInt(0);
                    cur.close();

                    // Create an entry in the 'injuries' table for the new injury
                    saveInjuryData();

                // Should not reach here
                } else {
                    if(BuildConfig.DEBUG) Log.e(TAG, "User creation unsuccessful!");
                }

                // Create a toast indicating success and finish the current activity
                Toast.makeText(getActivity(), "User created successfully!",
                        Toast.LENGTH_SHORT).show();

                getActivity().finish();
            }
        }
    }

    /** Creates an alert dialog warning the user that the indicated injury was within 30 days */
    private void createInjuryWarningAlert(){

        // Build a one-button dialog and display it
        AlertDialog.Builder dlgAlert= new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.profile_questions_injury_alert_title))
                .setMessage(getResources().getString(R.string.profile_questions_injury_alert_messagee))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.profile_questions_injury_alert_positive_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });

        // Create a new instance of DialogHelper, set the parameters and display the dialog
        DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
        box.setDialogButtonParams(null, -1, getActivity().getResources().getColor(R.color.blue));
        box.showDialog();
    }
}
