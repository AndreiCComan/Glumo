package glumo.com.glumo.object;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class handles the object FOOD ITEM
 */
public class FoodItem implements Parcelable{

    // attributes
    private String foodName;
    private String ndbno;
    private double carbohydrateValue;
    private double proteinValue;
    private double fatValue;
    private boolean expanded = false;
    private double totalGramsValue;
    private double totalCarbohydratesValue;

    // constructor
    public FoodItem(){

    }

    // gets carbohydrates value
    public double getCarbohydrateValue() {
        return carbohydrateValue;
    }

    // sets carbohydrates value
    public void setCarbohydrateValue(double carbohydrateValue) {
        this.carbohydrateValue = carbohydrateValue;
    }

    // gets food name
    public String getFoodName() {
        return foodName;
    }

    // sets carbohydrates value
    public FoodItem setFoodName(String foodName) {
        this.foodName = foodName;
        return this;
    }

    // gets id
    public String getNdbno() {
        return ndbno;
    }

    // sets id
    public FoodItem setNdbno(String ndbno) {
        this.ndbno = ndbno;
        return this;
    }

    // gets proteins value
    public double getProteinValue() {
        return proteinValue;
    }

    // sets proteins value
    public FoodItem setProteinValue(double proteinValue) {
        this.proteinValue = proteinValue;
        return this;
    }

    // gets fats value
    public double getFatValue() {
        return fatValue;
    }

    // sets carbohydrates value
    public FoodItem setFatValue(double fatValue) {
        this.fatValue = fatValue;
        return this;
    }

    // returns boolean value of EXPANDED
    public boolean isExpanded() {
        return expanded;
    }

    // sets boolean value of EXPANDED
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    // gets total grams value
    public double getTotalGramsValue() {
        return totalGramsValue;
    }

    // sets total grams value
    public FoodItem setTotalGramsValue(double totalGramsValue) {
        this.totalGramsValue = totalGramsValue;
        return this;
    }

    // gets total carbohydrates value
    public double getTotalCarbohydratesValue() {
        return totalCarbohydratesValue;
    }

    // sets carbohydrates value
    public FoodItem setTotalCarbohydratesValue(double totalCarbohydratesValue) {
        this.totalCarbohydratesValue = totalCarbohydratesValue;
        return this;
    }

    /**
     * @return 0
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * This method simply sets the attributes on the basis of the parcel given as parameter
     * @param in parcel
     */
    public FoodItem(Parcel in){
        this.foodName = in.readString();
        this.ndbno = in.readString();
        this.carbohydrateValue = in.readDouble();
        this.proteinValue = in.readDouble();
        this.fatValue = in.readDouble();
        this.expanded = in.readByte() != 0;
        this.totalGramsValue = in.readDouble();
        this.totalCarbohydratesValue = in.readDouble();
    }

    /**
     * This method simply writes the parcel given as parameters with the values of the attributes
     * @param out parcel
     * @param flags flags
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.foodName);
        out.writeString(this.ndbno);
        out.writeDouble(this.carbohydrateValue);
        out.writeDouble(this.proteinValue);
        out.writeDouble(this.fatValue);
        out.writeByte((byte) (expanded ? 1 : 0));
        out.writeDouble(this.totalGramsValue);
        out.writeDouble(this.totalCarbohydratesValue);
    }

    /**
     * This method simply creates new food item objects
     */
    public static final Parcelable.Creator<FoodItem> CREATOR = new Parcelable.Creator<FoodItem>() {
        public FoodItem createFromParcel(Parcel in) {
            return new FoodItem(in);
        }

        public FoodItem[] newArray(int size) {
            return new FoodItem[size];
        }
    };
}
