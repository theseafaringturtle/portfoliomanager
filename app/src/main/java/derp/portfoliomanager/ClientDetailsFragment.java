package derp.portfoliomanager;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ClientDetailsFragment extends Fragment {

    //private OnFragmentInteractionListener mListener;
    int currentClientId = -1;
    String fetchedName= "";
    String fetchedSurname= "";
    String fetchedPhone= "";
    String fetchedEmail= "";

    ArrayList<PortfolioDescHolder> portfolios = new ArrayList<>();
    ArrayAdapter<PortfolioDescHolder> portfoliosAdapter;

    public ClientDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ClientDetailsFragment.
     */
    public static ClientDetailsFragment newInstance() {
        ClientDetailsFragment fragment = new ClientDetailsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {//did we create this fragment to edit an existing client?
            currentClientId = getArguments().getInt("id");
            if(currentClientId != -1)
                fetchClientDetails((MainActivity)getActivity());
        }
        getActivity().findViewById(R.id.submitClientDetails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitClientDetails((MainActivity)getActivity());
            }
        });
        setupPortfolioListView();
        getActivity().findViewById(R.id.addNewPortfolio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickExcelFile();
            }
        });
    }

    void fetchClientDetails(final MainActivity activity) {
        //perform HTTP GET request for details of an individual client
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "http://54.36.182.56:5000/client/view?id="+currentClientId;
        //onresponse callback: parse json response, insert downloaded client details into form
        //onerror callback: display error response
        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //check if response came too late and we've already left this screen
                if (!activity.checkFragmentVisible(ClientDetailsFragment.class))
                    return;
                //parse response
                try {
                    JSONArray details = response.getJSONArray("contactDetails");
                    //store downloaded details to later compare them to check for any changes
                    fetchedName = details.getString(1);
                    fetchedSurname = details.getString(2);
                    fetchedPhone = details.getString(3);
                    fetchedEmail = details.getString(4);
                    //herp derp
                    //set form input text
                    ((EditText) activity.findViewById(R.id.detailsFName)).setText(fetchedName);
                    ((EditText) activity.findViewById(R.id.detailsLName)).setText(fetchedSurname);
                    ((EditText) activity.findViewById(R.id.detailsPhone)).setText(fetchedPhone);
                    ((EditText) activity.findViewById(R.id.detailsEmail)).setText(fetchedEmail);
                    JSONArray ports = response.getJSONArray("portfolios");
                    portfolios.clear();
                    for(int i = 0;i<ports.length();i++){//initialise data container with database id and description
                        portfolios.add(new PortfolioDescHolder(ports.getJSONArray(i).getInt(0),ports.getJSONArray(i).getString(1)));
                    }
                    if(ports.length() == 0)
                        portfolios.add(new PortfolioDescHolder(-1,"No portfolios found"));
                    portfoliosAdapter.notifyDataSetChanged();

                } catch (JSONException ex) {
                    activity.showError("Failed to fetch client details",ex.toString(),false);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                activity.showError("Failed to fetch client details",error.toString(),false);
            }
        });
        //attempt request again after 4 seconds
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(4000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(jsonArrayRequest);
    }


    public void submitClientDetails(final MainActivity activity){
        //perform HYTP POST request to add or update details of a client
        //get text values from form
        String firstName = ((EditText) activity.findViewById(R.id.detailsFName)).getText().toString();
        String lastName = ((EditText) activity.findViewById(R.id.detailsLName)).getText().toString();
        String phoneNumber = ((EditText) activity.findViewById(R.id.detailsPhone)).getText().toString();
        String email = ((EditText) activity.findViewById(R.id.detailsEmail)).getText().toString();
        if(firstName.equals("") || lastName.equals("") || phoneNumber.equals("") || email.equals("")){
            Toast.makeText(activity,"Fields cannot be left empty",Toast.LENGTH_LONG).show();
            return;
        }
        String url = "";
        if(currentClientId == -1) {//adding new client
            url = "http://54.36.182.56:5000/client/add?fname="+firstName+"&lname="+lastName+
                    "&phone="+phoneNumber+"&email="+email;
        }
        else{//updating existing client
            String changes = "";
            if(!fetchedName.equals(firstName))
                changes+="&fname="+firstName;
            if(!fetchedSurname.equals(lastName))
                changes+="&lname="+lastName;
            if(!fetchedPhone.equals(phoneNumber))
                changes+="&phone="+phoneNumber;
            if(!fetchedEmail.equals(email))
                changes+="&email="+email;
            if(changes.equals("")) {//no changes detected, no need to send request
                returnToClientPicker();
                return;
            }
            url = "http://54.36.182.56:5000/client/update?id="+currentClientId+changes;
        }
        RequestQueue queue = Volley.newRequestQueue(activity);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(activity,response,Toast.LENGTH_LONG).show();
                        ClientPickerFragment pickerFragment = (ClientPickerFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (pickerFragment != null && pickerFragment.isVisible())
                            pickerFragment.fetchClientsList(activity);//update clients list
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse.statusCode == 400){//serverside validation reported an invalid request
                    String message = new String(error.networkResponse.data);//e.g. adding an email without an @
                    Toast.makeText(activity,message,Toast.LENGTH_LONG).show();// display error message
                }else
                    //unexpected serverside error - display error message
                    activity.showError("Failed to submit client details",error.toString(),false);
            }
        });
        queue.add(stringRequest);
        returnToClientPicker();
    }
    void setupPortfolioListView(){
        portfoliosAdapter = new PortfoliosAdapter<>(getActivity(),R.layout.item_portfolio,portfolios);
        ListView listView = (ListView) getActivity().findViewById(R.id.portList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(portfoliosAdapter.getItem(position).DBid == -1)// no portfolios
                    return;
                final AppCompatActivity act = (AppCompatActivity) getActivity();
                act.getSupportActionBar().setTitle(portfoliosAdapter.getItem(position).desc);
                PortfolioOverview newFragment = PortfolioOverview.newInstance(portfoliosAdapter.getItem(position).DBid);
                //switch screen
                FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragmentContainer, newFragment);
                //add the transaction to the back stack so the user can navigate back
                transaction.addToBackStack(null);
                // Commit the transaction
                transaction.commit();
            }
        });
        listView.setAdapter(portfoliosAdapter);
    }

    void returnToClientPicker(){//navigate back to client picker screen
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.popBackStack();
    }

    void pickExcelFile(){
        //display instructions message
        Toast.makeText(getContext(),"Pick an excel file",Toast.LENGTH_LONG).show();
        //launch secondary activity to select an excel file using android's default content picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        String [] mimeTypes = {"text/csv", "text/comma-separated-values",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",// .xlsx
                "application/vnd.ms-excel"};// .xls
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        //the result will be handled in our activity's onActivityResult callback
        getActivity().startActivityForResult(intent, 1337);
    }

    @Override
    public void onResume(){
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Client details");
    }

    //autogenerated boilerplate below
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_details, container, false);
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }
    //end of boilerplate
}

class PortfolioDescHolder
{
    TextView tv;
    String desc;
    int DBid;
    PortfolioDescHolder(int id,String desc){
        this.DBid = id;
        this.desc = desc;
    }

}
class PortfoliosAdapter<Object> extends ArrayAdapter<Object> {
    FragmentActivity activity;
    private static LayoutInflater inflater=null;
    public PortfoliosAdapter(FragmentActivity act, int layoutResourceId, ArrayList<Object> ports) {
        super(act,layoutResourceId,ports);
        activity = act;
        //boilerplate
        inflater = ( LayoutInflater ) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
