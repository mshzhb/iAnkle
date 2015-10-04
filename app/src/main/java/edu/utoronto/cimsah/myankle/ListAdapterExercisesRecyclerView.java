package edu.utoronto.cimsah.myankle;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapterExercisesRecyclerView extends RecyclerView.Adapter<ListAdapterExercisesRecyclerView.ViewHolder> {
    public static final String TAG = ListAdapterExercisesRecyclerView.class.getSimpleName();
    private Context mContext;
    private ViewHolder.OnRowClickedListener mListener;
    private int mLayoutId;
    private ArrayList<Exercise> mExerciseList;

    /**
     * Constructor for Adapter
     * @param exercises AL of all Exercise objects to be displayed
     * @param layoutId Resource ID of layout file for cards
     * @param context
     */
    ListAdapterExercisesRecyclerView(ArrayList<Exercise> exercises, int layoutId, Context context) {
        mExerciseList = exercises;
        mContext = context;
        mLayoutId = layoutId;
    }

    /**
     * Replaces existing Exercise list
     * Must call notifyDataSetChanged if new list will be smaller than existing
     * @param newExercises
     */
    public void changeExercises(ArrayList<Exercise> newExercises) {
        mExerciseList = newExercises;
    }

    public void setOnRowClickedListener(ViewHolder.OnRowClickedListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //Inflates chosen row layout
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(mLayoutId, viewGroup, false);
        return new ViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        Exercise exercise = mExerciseList.get(i);

        //Extract data from Exercise
        int id = mContext.getResources().getIdentifier(exercise.getPhotoId() , "drawable", mContext.getPackageName());
        String exerciseName = exercise.getExerciseName();
        String exerciseEquipment = exercise.getExerciseEquipment();
        String exerciseEyeState = exercise.getExerciseEyeState();
        String exerciseDifficulty = exercise.getExerciseDifficulty();

        //Set Exercise information to display
        viewHolder.mImageExerciseIcon_Iv.setImageDrawable(mContext.getDrawable(id));
        viewHolder.mTextExerciseName_Tv.setText(exerciseName);
        viewHolder.mTextExerciseEquipment_Tv.setText(exerciseEquipment);
        viewHolder.mTextExerciseEyeState_Tv.setText("Eyes " + exerciseEyeState);
        viewHolder.mExerciseDifficulty_Tv.setText(exerciseDifficulty);
    }

    @Override
    public int getItemCount() {
        if (mExerciseList == null)
            return 0;

        else
            return mExerciseList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        View mItemView;
        ImageView mImageExerciseIcon_Iv;
        TextView mTextExerciseName_Tv;
        TextView mTextExerciseEquipment_Tv;
        TextView mTextExerciseEyeState_Tv;
        TextView mExerciseDifficulty_Tv;

        public OnRowClickedListener mListener;

        public ViewHolder(View itemView, OnRowClickedListener listener) {
            super(itemView);
            mItemView = itemView;
            mListener = listener;

            mItemView.setOnClickListener(this);

            //Identify UI elements
            mImageExerciseIcon_Iv = (ImageView) itemView.findViewById(R.id.layout_exercise_list_row_imageview_icon);
            mTextExerciseName_Tv = (TextView) itemView.findViewById(R.id.layout_exercise_list_row_textview_name);
            mTextExerciseEquipment_Tv = (TextView) itemView.findViewById(R.id.layout_exercise_list_row_textview_equipment);
            mTextExerciseEyeState_Tv = (TextView) itemView.findViewById(R.id.layout_exercise_list_row_textview_eye_state);
            mExerciseDifficulty_Tv = (TextView) itemView.findViewById(R.id.layout_exercise_list_row_textview_difficulty);

        }

        @Override
        public void onClick(View v) {
            mListener.onSelect(mItemView);
        }

        public interface OnRowClickedListener {
            void onSelect(View itemView);
        }

    }

    //Object for containing all info for exercise
    public static class Exercise {
        private String mExerciseName;
        private String mExerciseEquipment;
        private String mExerciseEyeState;
        private String mExerciseDifficulty;
        private String mPhotoId;
        

        private int mId;

        /**
         * @param exerciseName Name displayed (e.g. "Feet Apart")
         * @param id Must match ID in database: 1-30 = Eyes Open, 31-60 = Eyes Closed, 61-70 = Debugging
         * @param exerciseEquipment Equipment displayed (e.g. Balance Disk")
         * @param exerciseEyeState "Open" or "Closed"
         * @param exerciseDifficulty e.g. "Moderate"
         * @param photoId Must match photo file name in resources (e.g. "exercise_07")
         */
        public Exercise(String exerciseName, int id, String exerciseEquipment, String exerciseEyeState, String exerciseDifficulty, String photoId) {
            this.mExerciseName = exerciseName;
            this.mId = id;
            this.mExerciseEquipment = exerciseEquipment;
            this.mExerciseEyeState = exerciseEyeState;
            this.mExerciseDifficulty = exerciseDifficulty;
            this.mPhotoId = photoId;
        }

        public String getPhotoId() {
            return mPhotoId;
        }

        public String getExerciseName() {
            return mExerciseName;
        }

        public String getExerciseEquipment() {
            return mExerciseEquipment;
        }

        public String getExerciseEyeState() {
            return mExerciseEyeState;
        }

        public String getExerciseDifficulty() {
            return mExerciseDifficulty;
        }


        public int getId() {
            return mId;
        }

    }
}