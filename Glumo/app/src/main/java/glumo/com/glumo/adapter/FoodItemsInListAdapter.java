package glumo.com.glumo.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;

import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.FoodItem;

public class FoodItemsInListAdapter extends RecyclerView.Adapter<FoodItemsInListAdapter.FoodItemInListViewHolder> {
    // This object helps you save/restore the open/close state of each view
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private TextView numberOfItemsInList;
    private LinearLayout totalCarbohydratesContainer;
    private TextView totalCarbohydratesValue;
    private List<FoodItem> listOfFoodItemsWithinList;


    public FoodItemsInListAdapter(TextView numberOfItemsInList,
                                  LinearLayout totalCarbohydratesContainer,
                                  TextView totalCarbohydratesValue) {
        this.numberOfItemsInList = numberOfItemsInList;
        this.totalCarbohydratesContainer = totalCarbohydratesContainer;
        this.totalCarbohydratesValue = totalCarbohydratesValue;
        // Comment the line below if you want to open only one row at a time
        viewBinderHelper.setOpenOnlyOne(true);
    }

    public static class FoodItemInListViewHolder extends RecyclerView.ViewHolder {
        private TextView foodItemInListName;
        private TextView foodItemInListGrams;
        private TextView foodItemInListCarbohydrates;
        private ImageView deleteFoodItemBin;
        private SwipeRevealLayout swipeRevealLayout;
        public FoodItemInListViewHolder(View view) {
            super(view);
            foodItemInListName = (TextView) view.findViewById(R.id.food_item_in_list_name);
            foodItemInListGrams = (TextView) view.findViewById(R.id.food_item_in_list_name_grams);
            foodItemInListCarbohydrates = (TextView) view.findViewById(R.id.food_item_in_list_name_carbohydrates);
            deleteFoodItemBin = (ImageView) view.findViewById(R.id.delete_food_item_bin);
            swipeRevealLayout = (SwipeRevealLayout) view.findViewById(R.id.swipe_layout);
        }
    }

    @Override
    public FoodItemInListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.food_item_in_list_row;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;
        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new FoodItemInListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FoodItemInListViewHolder foodItemInListViewHolder, final int position) {
        final FoodItem foodItem = listOfFoodItemsWithinList.get(position);
        viewBinderHelper.bind(foodItemInListViewHolder.swipeRevealLayout, foodItem.getNdbno());
        foodItemInListViewHolder.foodItemInListName.setText(foodItem.getFoodName());
        foodItemInListViewHolder.foodItemInListGrams.setText(foodItem.getTotalGramsValue() + " grams");
        foodItemInListViewHolder.foodItemInListCarbohydrates.setText(foodItem.getTotalCarbohydratesValue() + " carbs");
        foodItemInListViewHolder.deleteFoodItemBin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double carbohydrateToBeRemoved = listOfFoodItemsWithinList.get(position).getTotalCarbohydratesValue();
                carbohydrateToBeRemoved = Math.floor(carbohydrateToBeRemoved * 100) / 100;
                double intialValue = Double.valueOf(totalCarbohydratesValue.getText().toString());
                intialValue = Math.floor(intialValue * 100) / 100;
                double newValue = intialValue - carbohydrateToBeRemoved;
                newValue = Math.floor(newValue * 100) / 100;
                totalCarbohydratesValue.setText(String.valueOf(newValue));
                removeAt(position);
                int itemsCount = getItemCount();
                if(itemsCount!=1){
                    numberOfItemsInList.setText(String.valueOf(itemsCount) + " " + GlumoApplication.getContext().getString(R.string.items_in_list));
                }else {
                    numberOfItemsInList.setText("1" + " " + GlumoApplication.getContext().getString(R.string.item_in_list));
                }
                if(itemsCount==0){
                    totalCarbohydratesContainer.setVisibility(View.GONE);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return listOfFoodItemsWithinList.size();
    }

    private void removeAt(int position) {
        listOfFoodItemsWithinList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    public void setFoodItems(List<FoodItem> listOfItemsWithinList) {
        this.listOfFoodItemsWithinList = listOfItemsWithinList;
        notifyDataSetChanged();
    }

    public void saveStates(Bundle outState) {
        viewBinderHelper.saveStates(outState);
    }

    public void restoreStates(Bundle inState) {
        viewBinderHelper.restoreStates(inState);
    }
}
