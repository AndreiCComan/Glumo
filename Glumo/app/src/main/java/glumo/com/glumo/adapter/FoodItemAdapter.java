package glumo.com.glumo.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.FoodItem;
import glumo.com.glumo.util.Appearance;

public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodItemAdapterViewHolder> {

    private TextView numberOfItemsInList;
    private List<FoodItem> foodItems = new ArrayList<FoodItem>();
    private List<FoodItem> listOfItemsWithinList;
    private FoodItemsInListAdapter foodItemsInListAdapter;
    private LinearLayout totalCarbohydratesContainer;
    private TextView totalCarbohydratesValue;

    public FoodItemAdapter(TextView numberOfItems_number,
                           LinearLayout totalCarbohydratesContainer,
                           TextView totalCarbohydratesValue,
                           FoodItemsInListAdapter foodItemsInListAdapter) {
        this.numberOfItemsInList = numberOfItems_number;
        this.totalCarbohydratesContainer = totalCarbohydratesContainer;
        this.totalCarbohydratesValue = totalCarbohydratesValue;
        this.foodItemsInListAdapter = foodItemsInListAdapter;
    }

    public static class FoodItemAdapterViewHolder extends RecyclerView.ViewHolder {
        public final TextView foodName;
        public final TextView carbohydrateValue;
        public final LinearLayout carbohydrateValueContainer;
        public final LinearLayout foodDescription;
        public final TextView carbohydrateValueWithinNutrients;
        public final TextView fatValueWithinNutrients;
        public final TextView proteinValueWithinNutrients;
        public final EditText foodItemGramsEditText;
        public final Button addToMealButton;

        public FoodItemAdapterViewHolder(View view) {
            super(view);
            this.foodName = (TextView) view.findViewById(R.id.food_name);
            this.carbohydrateValue = (TextView) view.findViewById(R.id.carbohydrate_value);
            this.carbohydrateValueContainer = (LinearLayout) view.findViewById(R.id.carbohydrate_value_container);
            this.foodDescription = (LinearLayout) view.findViewById(R.id.food_description);
            this.carbohydrateValueWithinNutrients = (TextView) view.findViewById(R.id.carbohydrate_value_within_nutrients);
            this.fatValueWithinNutrients = (TextView) view.findViewById(R.id.fat_value_within_nutrients);
            this.proteinValueWithinNutrients = (TextView) view.findViewById(R.id.protein_value_within_nutrients);
            this.foodItemGramsEditText = (EditText) view.findViewById(R.id.food_item_grams);
            this.addToMealButton = (Button) view.findViewById(R.id.add_to_meal_button);
        }
    }

    @Override
    public FoodItemAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.food_item_row;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new FoodItemAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FoodItemAdapterViewHolder foodItemAdapterViewHolder, int position) {
        final FoodItem foodItem = foodItems.get(position);
        int carbohydrateValue = (int) Math.ceil(foodItem.getCarbohydrateValue());

        int colorResource = Appearance.getColorBasedOnCarbohydrateValue(carbohydrateValue);
        foodItemAdapterViewHolder.carbohydrateValueContainer.setBackgroundColor(ContextCompat.getColor(GlumoApplication.getContext(), colorResource));

        String unit = GlumoApplication.getContext().getString(R.string.grams_unit);
        foodItemAdapterViewHolder.foodName.setText(foodItem.getFoodName());
        foodItemAdapterViewHolder.carbohydrateValue.setText(String.valueOf(carbohydrateValue));
        foodItemAdapterViewHolder.carbohydrateValueWithinNutrients.setText(foodItem.getCarbohydrateValue() + " " + unit);
        foodItemAdapterViewHolder.fatValueWithinNutrients.setText(foodItem.getFatValue() + " " + unit);
        foodItemAdapterViewHolder.proteinValueWithinNutrients.setText(foodItem.getProteinValue() + " " + unit);

        // State keeper
        if (foodItem.isExpanded()) {
            foodItemAdapterViewHolder.foodDescription.setVisibility(View.VISIBLE);
            foodItem.setExpanded(true);
        } else {
            foodItemAdapterViewHolder.foodDescription.setVisibility(View.GONE);
            foodItem.setExpanded(false);
        }

        foodItemAdapterViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean shouldExpand = foodItemAdapterViewHolder.foodDescription.getVisibility() == View.GONE;
                if (shouldExpand) {
                    Appearance.expand(foodItemAdapterViewHolder.foodDescription);
                    foodItem.setExpanded(true);
                } else {
                    Appearance.collapse(foodItemAdapterViewHolder.foodDescription);
                    foodItem.setExpanded(false);
                }
                ViewGroup viewGroup = (ViewGroup) foodItemAdapterViewHolder.itemView;
                foodItemAdapterViewHolder.foodDescription.setActivated(shouldExpand);
            }
        });

        foodItemAdapterViewHolder.addToMealButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOrUpdateItemWithinList(foodItemAdapterViewHolder, foodItem);
            }
        });

        foodItemAdapterViewHolder.foodItemGramsEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addOrUpdateItemWithinList(foodItemAdapterViewHolder, foodItem);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }

    public void setFoodItems(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
        notifyDataSetChanged();
    }

    public void setFoodItemsInList(List<FoodItem> listOfItemsWithinList){
        this.listOfItemsWithinList = listOfItemsWithinList;
    }

    private int searchForItem(FoodItem foodItem) {
        int outcome = -1;
        for (int index = 0; index < listOfItemsWithinList.size(); index++) {
            FoodItem item = listOfItemsWithinList.get(index);
            if (item.getFoodName().equals(foodItem.getFoodName())) {
                return index;
            }
        }
        return outcome;
    }

    private void addOrUpdateItemWithinList(FoodItemAdapterViewHolder foodItemAdapterViewHolder, FoodItem foodItem){
        String foodGramsText = foodItemAdapterViewHolder.foodItemGramsEditText.getText().toString();
        if (foodGramsText != null && !foodGramsText.equals("")) {
            double foodGramsDouble = Double.valueOf(foodGramsText);
            if(foodGramsDouble > 0){
                double foodCarbsDouble = foodGramsDouble / 100 * foodItem.getCarbohydrateValue();
                foodCarbsDouble = Math.floor(foodCarbsDouble * 100) / 100;
                foodItemAdapterViewHolder.foodItemGramsEditText.getText().clear();
                Context context = GlumoApplication.getContext();
                foodItem.setTotalGramsValue(foodGramsDouble);
                foodItem.setTotalCarbohydratesValue(foodCarbsDouble);
                int itemIndex = searchForItem(foodItem);
                if (itemIndex != -1) {
                    FoodItem item = listOfItemsWithinList.get(itemIndex);
                    item.setTotalGramsValue(item.getTotalGramsValue() + foodGramsDouble);
                    item.setTotalCarbohydratesValue(item.getTotalCarbohydratesValue() + foodCarbsDouble);
                    Toast toast = Toast.makeText(context, context.getString(R.string.item_updated_within_list), Toast.LENGTH_SHORT);
                    toast.show();
                }else{
                    listOfItemsWithinList.add(foodItem);
                    int itemsCount = foodItemsInListAdapter.getItemCount();
                    if(itemsCount!=1){
                        numberOfItemsInList.setText(String.valueOf(itemsCount) + " " + GlumoApplication.getContext().getString(R.string.items_in_list));
                    }else {
                        numberOfItemsInList.setText("1" + " " + GlumoApplication.getContext().getString(R.string.item_in_list));
                    }
                    Toast toast = Toast.makeText(context, context.getString(R.string.item_added_to_list), Toast.LENGTH_SHORT);
                    toast.show();
                }
                totalCarbohydratesContainer.setVisibility(View.VISIBLE);
                double intialValue = Double.valueOf(totalCarbohydratesValue.getText().toString());
                intialValue = Math.floor(intialValue * 100) / 100;
                double totalValue = Math.floor((intialValue+foodCarbsDouble) * 100) / 100;
                totalCarbohydratesValue.setText(String.valueOf(totalValue));
                foodItemsInListAdapter.notifyDataSetChanged();
            }
        }
        Appearance.hideKeyboard(foodItemAdapterViewHolder.itemView);
    }
}
