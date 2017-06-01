package glumo.com.glumo.object;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Just a simple util class for binding glucose_read_value and time_stamp
 */
public class GlucoseRead implements Parcelable {

    // time and glucose attributes
    private String time;
    private int glucose;

    public GlucoseRead(){}

    /**
     * This constructor method simply sets the attributes values
     * @param time time value
     * @param glucose glucose value
     */
    public GlucoseRead(String time, int glucose){
        this.time = time;
        this.glucose = glucose;
    }

    /**
     * This constructor method simply sets the attributes values by using the
     * parcel given as parameters
     * @param in parcel
     */
    protected GlucoseRead(Parcel in) {
        time = in.readString();
        glucose = in.readInt();
    }

    /**
     * This method simply creates new glucose read objects
     */
    public static final Creator<GlucoseRead> CREATOR = new Creator<GlucoseRead>() {
        @Override
        public GlucoseRead createFromParcel(Parcel in) {
            return new GlucoseRead(in);
        }

        @Override
        public GlucoseRead[] newArray(int size) {
            return new GlucoseRead[size];
        }
    };

    // sets time value
    public void setTime(String time){
        this.time = time;
    }

    // sets glucose value
    public void setGlucose(int glucose){
        this.glucose = glucose;
    }

    // gets time value
    public String getTime(){
        return time;
    }

    // gets glucose value
    public int getGlucose(){
        return glucose;
    }

    /**
     * This method simply returns the glucose value of the object
     * @return glucose value
     */
    @Override
    public int describeContents() {
        return this.getGlucose();
    }

    /**
     * This method simply writes a parcel with the values of the attributes
     * @param dest pracel
     * @param flags flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getTime());
        dest.writeInt(this.getGlucose());
    }
}