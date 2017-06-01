package glumo.com.glumo.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.GlucoseRead;
import glumo.com.glumo.util.Appearance;

public class GlucoseReadAdapter extends RecyclerView.Adapter<GlucoseReadAdapter.GlucoseReadAdapterViewHolder> {

    List<GlucoseRead> glucoseReads;
    static int [] thresholds;
    Context ctx = GlumoApplication.getContext();

    public static class GlucoseReadAdapterViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout glucoseValueContainer;
        private TextView glucoseValue;
        //private ImageView glucoseValueArrow;
        private TextView getGlucoseValueTime;

        public GlucoseReadAdapterViewHolder(View view) {
            super(view);
            // values that have been set for hypoglycemia and hyperglicemia
            int [] thr = {GlumoApplication.getIntPreference(R.string.hypoglycemia_preference), GlumoApplication.getIntPreference(R.string.hyperglycemia_preference)};;
            thresholds = thr;

            glucoseValueContainer = (LinearLayout) view.findViewById(R.id.glucose_value_container);
            glucoseValue = (TextView) view.findViewById(R.id.glucose_value);
            //glucoseValueArrow = (ImageView) view.findViewById(R.id.glucose_value_arrow);
            getGlucoseValueTime = (TextView) view.findViewById(R.id.glucose_value_time);
        }
    }

    @Override
    public GlucoseReadAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.glucose_read_row;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new GlucoseReadAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GlucoseReadAdapterViewHolder glucoseReadAdapterViewHolder, int position) {
        GlucoseRead glucoseRead = glucoseReads.get(position);

        glucoseReadAdapterViewHolder.glucoseValueContainer.setBackgroundColor(ContextCompat.getColor(ctx, Appearance.getColorBasedOnThresholds(glucoseRead.getGlucose(), thresholds)));
        glucoseReadAdapterViewHolder.glucoseValue.setText(String.valueOf(glucoseRead.getGlucose()));
        //glucoseReadAdapterViewHolder.glucoseValueArrow.setRotation();
        glucoseReadAdapterViewHolder.getGlucoseValueTime.setText(glucoseRead.getTime());
    }

    @Override
    public int getItemCount() {
        if (glucoseReads == null)
            return 0;
        return glucoseReads.size();
    }

    public void setGlucoseReads(List<GlucoseRead> glucoseReads) {
        this.glucoseReads = glucoseReads;
        notifyDataSetChanged();
    }

}
