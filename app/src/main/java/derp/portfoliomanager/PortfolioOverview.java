package derp.portfoliomanager;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class PortfolioOverview extends Fragment {

    private int portfolioId;
    String description = "";

    HoldingsAdapter holdingsAdapter;
    ArrayList<Holding> holdings = new ArrayList<>();

    public PortfolioOverview() {
        // Required empty public constructor
    }

    public static PortfolioOverview newInstance(int id, String desc) {
        PortfolioOverview fragment = new PortfolioOverview();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putString("desc", desc);
        fragment.setArguments(args);
        return fragment;
    }
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        fetchPortfolioDetails((MainActivity)getActivity());
        holdingsAdapter = new HoldingsAdapter(getActivity(),R.layout.item_holding,holdings);
        ListView listView = (ListView) getActivity().findViewById(R.id.holdingsList);
        listView.setAdapter(holdingsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openPriceHistoryScreen(position);
            }
        });
    }

    void openPriceHistoryScreen(int listNum){
        String ticker = ((Holding)holdingsAdapter.getItem(listNum)).ticker;
        //set action bar title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(((Holding) holdingsAdapter.getItem(listNum)).name);
        PriceHistory newFragment = PriceHistory.newInstance(ticker);
        //switch screen
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, newFragment);
        //add the transaction to the back stack so the user can navigate back
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
    }

    void fetchPortfolioDetails(final MainActivity activity) {
        //perform HTTP GET request for portfolio contents
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "http://54.36.182.56:5000/port/view?pid=" + portfolioId;
        //onresponse callback: parse json response, insert downloaded holdings into list
        //onerror callback: display error response
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                //check if response came too late and we've already left this screen
                if (!activity.checkFragmentVisible(PortfolioOverview.class))
                    return;
                //parse response
                try {
                    if(response.length() == 0){// no holdings - display message
                        getView().findViewById(R.id.noHoldingsText).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.holdingsList).setVisibility(View.GONE);
                        return;
                    }
                    holdings.clear();
                    float totalValue = 0;
                    for (int i = 0; i < response.length(); i++) {
                        //ISIN,value,currency,description
                        JSONArray item = response.getJSONArray(i);
                        Holding holding = new Holding();
                        holding.ISIN = item.getString(0);
                        holding.name = item.getString(1);
                        holding.currency = item.getString(2);
                        holding.value = item.getDouble(3);
                        totalValue += holding.value;
                        holding.perf = item.getDouble(4);
                        holding.ticker = item.getString(5);
                        holdings.add(holding);
                    }
                    //format total value
                    String val = String.format(Locale.UK,"%s%,.2f","€",totalValue);
                    //add total value of portfolio to title
                    ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(description+ " - "+val);
                    //todo change assume EUR across all products
                    holdingsAdapter.notifyDataSetChanged();
                } catch (JSONException ex) {
                    Utils.showError(getContext(),"Failed to fetch portfolio details", ex.toString(), false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.showError(getContext(),"Failed to fetch portfolio details", error.toString(), false);
            }
        });
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonArrayRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(description);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            portfolioId = getArguments().getInt("id");
            description = getArguments().getString("desc");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_portfolio_overview, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
//ISIN,qty,value,name,currency,performance indicator

class Holding
{
    String ISIN;
    double value;
    String name;
    String currency;
    double perf;
    String ticker;

    TextView nameView;
    TextView valueView;
    TextView perfView;
}
class HoldingsAdapter<Object> extends ArrayAdapter<Object> {
    FragmentActivity activity;
    private static LayoutInflater inflater=null;
    public final Map<String, String> CURRENCIES = new HashMap<String, String>(){
        {
            put("EUR","€");
            put("USD","$");
            put("GBP","£");
            put("JPY","¥");
        }
    };

    public HoldingsAdapter(FragmentActivity act, int layoutResourceId, ArrayList<Object> holdings) {
        super(act,layoutResourceId,holdings);
        activity = act;
        inflater = ( LayoutInflater ) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //initialise list item and set text description
        Holding holding = (Holding)getItem(position);
        View rowView;
        rowView = inflater.inflate(R.layout.item_holding, null);
        holding.nameView = (TextView) rowView.findViewById(R.id.holdingName);
        holding.nameView.setText(holding.name);

        holding.valueView = (TextView) rowView.findViewById(R.id.holdingValue);
        //format value of holding to 2 decimal places, prepend currency
        //todo EUR default
        //Currency curr = Currency.getInstance(Locale.UK);
        String curr = CURRENCIES.get(holding.currency);
        String val = String.format(Locale.UK,"%s%,.2f",curr,holding.value);
        holding.valueView.setText(val);

        holding.perfView = (TextView) rowView.findViewById(R.id.holdingPerf);
        if(holding.perf > 0) {
            holding.perfView.setText("+" + holding.perf);
            holding.perfView.setTextColor(Color.parseColor("#09DE09"));//green
        }
        else {
            holding.perfView.setText("" + holding.perf);
            holding.perfView.setTextColor(Color.parseColor("#D7290E"));//red
        }
        return rowView;
    }
}
