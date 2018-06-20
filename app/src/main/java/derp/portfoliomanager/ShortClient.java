package derp.portfoliomanager;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by User on 03/12/2017.
 */
class ShortClient {
    //this class holds the preview for each client inside the client picker screen
    //instead of containing all the details, it holds a name+lastname string and corresponding
    //database id to be used in case we want to fetch all the details
    private String name;
    private int id;

    public ShortClient(int id,String name) {
        this.id = id;
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public int getId() {
        return id;
    }


}
