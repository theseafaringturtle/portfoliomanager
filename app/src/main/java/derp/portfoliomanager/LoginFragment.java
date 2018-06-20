package derp.portfoliomanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;


public class LoginFragment extends Fragment {

    private String lastUser = null;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance(String user) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString("lastUser", user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            lastUser = getArguments().getString("lastUser");
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if(lastUser!=null)
            ((EditText)view.findViewById(R.id.nameInput)).setText(lastUser);
        view.findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin((MainActivity)getActivity());
            }
        });
    }
    void attemptLogin(final MainActivity activity){
        //perform HTTP GET request for portfolio contents
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "http://54.36.182.56:5000/login";
        final String userName = ((TextView)getActivity().findViewById(R.id.nameInput)).getText().toString();
        final String password = ((TextView)getActivity().findViewById(R.id.passInput)).getText().toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST,url,new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(activity,response,Toast.LENGTH_LONG).show();//login successful
                if ( ((CheckBox)activity.findViewById(R.id.rememberUser)).isChecked() )
                    rememberLastUser();
                activity.switchToClientList();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse.statusCode == 403)//login attempt failed
                    Toast.makeText(activity,error.toString(),Toast.LENGTH_LONG).show();
                else
                    Utils.showError(activity,"Network Error",error.toString(),false);
            }
        }){
        //add parameters to request body
        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("user", userName);
            parameters.put("pass", password);
            return parameters;
        }};
        queue.add(stringRequest);
    }

    void rememberLastUser(){
        SharedPreferences prefs;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
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
