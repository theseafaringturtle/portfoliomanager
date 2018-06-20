package derp.portfoliomanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;


public class PortfolioList extends DialogFragment {

    static int clientId = -1;
    static String description = "";
    ArrayList<PortfolioDescHolder> portfolios = new ArrayList<>();
    ArrayAdapter<PortfolioDescHolder> portfoliosAdapter;


    public PortfolioList() {
        // Required empty public constructor
    }

    public static PortfolioList newInstance(int clientId) {
        PortfolioList fragment = new PortfolioList();
        Bundle args = new Bundle();
        args.putInt("cid", clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            clientId = getArguments().getInt("cid");
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //don't show the list container until we've received the data for the list from the server
        view.setVisibility(View.GONE);
        //getActivity().setProgressBarIndeterminateVisibility(true);
        setupPortfolioListView();
        getView().findViewById(R.id.addNewPortfolio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askforPortfolioDescription();
            }
        });
        fetchPortfolios((MainActivity)getActivity());
    }

    void fetchPortfolios(final MainActivity activity) {
        //perform HTTP GET request for details of an individual client
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "http://54.36.182.56:5000/port/list?cid=" + clientId;
        //onresponse callback: parse json response, insert downloaded portfolio details into listview
        //onerror callback: display error response
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // make ListView visible since data has arrived
                if(getView() != null)
                    getView().setVisibility(View.VISIBLE);
                //parse response
                try {
                    portfolios.clear();
                    for (int i = 0; i < response.length(); i++) {//initialise data container with database id and description
                        portfolios.add(new PortfolioDescHolder(response.getJSONArray(i).getInt(0), response.getJSONArray(i).getString(1)));
                    }
                    if (response.length() == 0)// add dummy item
                        portfolios.add(new PortfolioDescHolder(-1, "No portfolios"));
                    portfoliosAdapter.notifyDataSetChanged();
                    if(getShowsDialog()){//this is a popup dialog, we've clicked directly on a client
                        if(portfolios.size() == 1) {// this clients owns only one or no portfolios
                            switchToPortfolioScreen(0);// take us directly to that one
                        }
                    }
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
        queue.add(jsonArrayRequest);

    }
    void setupPortfolioListView(){
        portfoliosAdapter = new PortfoliosAdapter<>(getContext(),R.layout.item_portfolio,portfolios);
        ListView listView = (ListView) getView().findViewById(R.id.portList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchToPortfolioScreen(position);
            }
        });
        listView.setAdapter(portfoliosAdapter);
    }
    void switchToPortfolioScreen(int listNum){
        if(portfoliosAdapter.getItem(listNum).DBid == -1)// no portfolios
            return;
        final AppCompatActivity act = (AppCompatActivity) getActivity();
        if(act == null)
            return;
        //create new fragment instance
        PortfolioOverview newFragment = PortfolioOverview.newInstance(portfoliosAdapter.getItem(listNum).DBid,portfoliosAdapter.getItem(listNum).desc);
        //switch screen
        FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, newFragment);
        //add the transaction to the back stack so the user can navigate back
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
        dismiss();
    }
    //create a dialog with a text field which will provide the description for the new portfolio
    void askforPortfolioDescription(){
        final EditText txtInput = new EditText(getContext());
        txtInput.setSingleLine(true);
        new AlertDialog.Builder(getContext())
                .setTitle("Import Portfolio")
                .setMessage("Enter description")
                .setView(txtInput)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        description = txtInput.getText().toString();
                        pickExcelFile();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    void pickExcelFile(){
        //display instructions message
        Toast.makeText(getContext(),"Pick an excel file",Toast.LENGTH_LONG).show();
        //launch secondary activity to select an excel file using android's default content picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //pick excel format files, filter out the rest
        String [] mimeTypes = {"text/csv", "text/comma-separated-values",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",// .xlsx
                "application/vnd.ms-excel"};// .xls
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        //the result will be handled in our activity's onActivityResult callback
        getActivity().startActivityForResult(intent, 1337);
        dismiss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio_list, container, false);
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
class PortfolioDescHolder
{
    //holds the description and corresponding database id for each portfolio
    TextView tv;
    String desc;
    int DBid;
    PortfolioDescHolder(int id,String desc){
        this.DBid = id;
        this.desc = desc;
    }

}
class PortfoliosAdapter<Object> extends ArrayAdapter<Object> {
    //adapter for the portfolio ListView
    private static LayoutInflater inflater=null;
    public PortfoliosAdapter(Context context,int layoutResourceId, ArrayList<Object> ports) {
        super(context,layoutResourceId,ports);
        //boilerplate
        inflater = ( LayoutInflater ) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //initialise list item and set text description
        PortfolioDescHolder holder = (PortfolioDescHolder)getItem(position);
        View rowView;
        rowView = inflater.inflate(R.layout.item_portfolio, null);
        holder.tv = (TextView) rowView.findViewById(R.id.portfolioDescription);
        holder.tv.setText(holder.desc);
        return rowView;
    }
}
