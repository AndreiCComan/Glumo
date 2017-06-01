package glumo.com.glumo.adapter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.FoodItem;

public class FoodItemLoader extends AsyncTaskLoader<List<FoodItem>> {
    private String foodItemQuery;

    public FoodItemLoader(Context context, String foodItemQuery) {
        super(context);
        this.foodItemQuery = foodItemQuery;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<FoodItem> loadInBackground() {
        if (foodItemQuery == null) {
            return null;
        }
        return getItems();
    }

    private List<FoodItem> getItems() {
        List<FoodItem> foodItems = null;
        Uri baseUri = Uri.parse(GlumoApplication.USDA_REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendPath(GlumoApplication.SEARCH_BY_QUERY);
        uriBuilder.appendQueryParameter(GlumoApplication.FORMAT_PARAM, GlumoApplication.FORMAT_VALUE);
        uriBuilder.appendQueryParameter(GlumoApplication.DATA_SOURCE_PARAM, GlumoApplication.DATA_SOURCE_VALUE);
        uriBuilder.appendQueryParameter(GlumoApplication.MAX_ROWS_PARAM, GlumoApplication.MAX_ROWS_VALUE);
        uriBuilder.appendQueryParameter(GlumoApplication.QUERY_PARAM, foodItemQuery);
        uriBuilder.appendQueryParameter(GlumoApplication.API_KEY_PARAM, GlumoApplication.API_KEY_VALUE);
        try {
            String jsonResponse = makeHTTPSRequest(new URL(uriBuilder.build().toString()));
            if(jsonResponse!=null){
                foodItems = new ArrayList<FoodItem>();
                JSONObject root = new JSONObject(jsonResponse).getJSONObject("list");
                JSONArray itemsArray = root.getJSONArray("item");
                for(int index = 0; index < itemsArray.length(); index++){
                    JSONObject item = itemsArray.getJSONObject(index);
                    FoodItem foodItem = new FoodItem();
                    foodItem.setFoodName(item.getString("name"));
                    foodItem.setNdbno(item.getString("ndbno"));
                    foodItems.add(foodItem);
                    completeFoodItemsInformation(foodItems);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return foodItems;
    }

    private String makeHTTPSRequest(URL url) {
        String jsonResponse = null;
        HttpsURLConnection httpsURLConnection = null;
        InputStream inputStream = null;
        try {
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setReadTimeout(20000);
            httpsURLConnection.setConnectTimeout(20000);
            httpsURLConnection.connect();
            inputStream = httpsURLConnection.getInputStream();
            StringBuilder stringBuilder = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
            }
            jsonResponse = stringBuilder.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    private void completeFoodItemsInformation(List<FoodItem> foodItems){
        for(FoodItem foodItem : foodItems){
            Uri baseUri = Uri.parse(GlumoApplication.USDA_REQUEST_URL);
            Uri.Builder uriBuilder = baseUri.buildUpon();
            uriBuilder.appendPath(GlumoApplication.SEARCH_BY_NDBNO);
            uriBuilder.appendQueryParameter(GlumoApplication.FORMAT_PARAM, GlumoApplication.FORMAT_VALUE);
            uriBuilder.appendQueryParameter(GlumoApplication.NDBNO_PARAM, foodItem.getNdbno());
            uriBuilder.appendQueryParameter(GlumoApplication.REPORT_TYPE_PARAM, GlumoApplication.REPORT_TYPE_VALUE);
            uriBuilder.appendQueryParameter(GlumoApplication.API_KEY_PARAM, GlumoApplication.API_KEY_VALUE);
            try {
                String jsonResponse = makeHTTPSRequest(new URL(uriBuilder.build().toString()));
                if(jsonResponse!=null){
                    JSONObject root = new JSONObject(jsonResponse).getJSONObject("report").getJSONObject("food");
                    JSONArray nutrientsArray = root.getJSONArray("nutrients");
                    for(int index = 0; index < nutrientsArray.length(); index++){
                        JSONObject nutrient = nutrientsArray.getJSONObject(index);
                        switch (nutrient.getString("nutrient_id")){
                            case GlumoApplication.CARBOHYDRATE_NUTRIENT_ID:{
                                foodItem.setCarbohydrateValue(nutrient.getDouble("value"));
                                break;
                            }
                            case GlumoApplication.FAT_NUTRIENT_ID:{
                                foodItem.setFatValue(nutrient.getDouble("value"));
                                break;
                            }
                            case GlumoApplication.PROTEIN_NUTRIENT_ID:{
                                foodItem.setProteinValue(Math.floor(nutrient.getDouble("value") * 100) / 100);
                                break;
                            }
                        }
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
