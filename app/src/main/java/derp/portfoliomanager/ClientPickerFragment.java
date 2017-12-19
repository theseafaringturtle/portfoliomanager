package derp.portfoliomanager;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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
import java.util.List;

import derp.portfoliomanager.ShortClient;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ClientPickerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ClientPickerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ClientPickerFragment extends Fragment {

    RecyclerView recyclerView;
    ClientsAdapter adapter = null;
    ArrayList<ShortClient> clients = new ArrayList<>();

    private OnFragmentInteractionListener mListener;

    public ClientPickerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ClientPickerFragment.
     */
    public static ClientPickerFragment newInstance(String param1, String param2) {
        ClientPickerFragment fragment = new ClientPickerFragment();
        return fragment;
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        recyclerView = (RecyclerView) getActivity().findViewById(R.id.clientsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ClientsAdapter(getContext(), clients);
        recyclerView.setAdapter(adapter);
        final MainActivity act = (MainActivity)getActivity();
        fetchClientsList(act);
        getActivity().findViewById(R.id.addClientButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter != null) {//check if list adapter has not been initialised yet
                    adapter.openClientDetails(-1);//new client - no id set yet
                }
            }
        });
        addSearchFunction();
        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    public void fetchClientsList(final Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://54.36.182.56:5000/client/list";
        //onresponse callback: parse json response, add clients to list named 'clients', update recyclerview
        //onerror callback: display error response in a toast message
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                clients.clear();
                for (int i = 0; i != response.length(); i++) {
                    try {
                        JSONArray cl = response.getJSONArray(i);
                        //client preview initialised with #id,name+lastname
                        clients.add(new ShortClient(cl.getInt(0),cl.getString(1)+" "+cl.getString(2)));
                    }catch (JSONException ex){
                        Toast.makeText(getContext(),ex.toString(),Toast.LENGTH_LONG).show();
                    }
                }
                adapter.notifyDataSetChanged();
                //adapter = new ClientsAdapter(getContext(), clients);
                //recyclerView.setAdapter(adapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context,error.toString(),Toast.LENGTH_LONG).show();
            }
        });
        //attempt request again after 5 seconds
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonArrayRequest);
    }

    void addSearchFunction(){
        ((SearchView)getActivity().findViewById(R.id.searchBox)).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Select a client");
    }

    // autogenerated boilerplate below
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_picker, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
