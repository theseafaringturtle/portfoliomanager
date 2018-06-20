package derp.portfoliomanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {


    final String TAG = "PORTMAN";
    boolean DEBUG_NOLGIN = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);//spinner progress bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(DEBUG_NOLGIN) {
            switchToClientList();
        }
        else{
            //todo get lastUser from prefs
            LoginFragment newFragment = LoginFragment.newInstance("");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, newFragment);
            transaction.commit();
        }
    }
    void switchToClientList(){
        ClientPickerFragment newFragment = ClientPickerFragment.newInstance("", "");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, newFragment);
        transaction.commit();
    }

    boolean checkFragmentVisible(Class required){
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if(currentFragment.getClass() != required)//if (!(currentFragment instanceof PortfolioOverview))
            return false;
        if (!currentFragment.isVisible())
            return false;
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //'this' inside a listener refers to the listener, so we need this
        final MainActivity that = this;//cheap trick
        switch(requestCode){
            case 1337: //user has selected an excel file to import
                if(data==null) //no file selected!
                    return;
                final Uri uri = data.getData();
                Toast.makeText(this,"Uploading file to server...",Toast.LENGTH_SHORT).show();
                // Volley does not support attaching raw files to POST requests, so I need to
                // open the file and convert it to a base64 text string for upload
                final String excelContents = uriTobase64(uri);
                if(excelContents == null)
                    return;
                RequestQueue queue = Volley.newRequestQueue(this);
                final String description = PortfolioList.description;
                final int clientId = PortfolioList.clientId;
                String url = "http://54.36.182.56:5000/port/upload?cid="+clientId;
                StringRequest req = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(that,response,Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Utils.showError(that,"Failed to fetch portfolio details", error.toString(), false);
                            }
                        }){
                    //add parameters to send
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("desc", description);
                        parameters.put("excel",excelContents);
                        return parameters;
                    }
                };
                queue.add(req);
        }
    }
    String uriTobase64(Uri uri){
        //convert byte contents of file to base64 string
        try {
            InputStream instream = getContentResolver().openInputStream(uri);
            final byte[] buffer = new byte[1024];
            //final StringBuilder contents = new StringBuilder();
            int rsz;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((rsz = instream.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer,0,rsz);
            }
            return Base64.encodeToString(baos.toByteArray(),Base64.DEFAULT);
        }
        catch(Exception ex) {
            Toast.makeText(this, "File retrieval failed", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
