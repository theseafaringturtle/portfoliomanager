package derp.portfoliomanager;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;


public class PriceHistory extends Fragment {

    private String ticker;
    public PriceHistory() {
        // Required empty public constructor
    }
    //actual constructor
    public static PriceHistory newInstance(String ticker) {
        PriceHistory fragment = new PriceHistory();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ticker = getArguments().getString("ticker");
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        fetchPriceHistory();
    }

    void fetchPriceHistory(){
        //loading message
        final Toast loadingMsg = Toast.makeText(getContext(),"Fetching price history...",Toast.LENGTH_LONG);
        loadingMsg.show();
        //perform HTTP GET request for price history
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = "http://54.36.182.56:5000/price/view?sym=" + ticker+".MI";
        //onresponse callback: parse json response, insert downloaded holdings into list
        //onerror callback: display error response
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //check if response came too late and we've already left this screen
                if (!((MainActivity)getActivity()).checkFragmentVisible(PriceHistory.class))
                    return;
                try {
                    loadingMsg.cancel();
                    parsePriceHistory(response);
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
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    void parsePriceHistory(JSONObject jsonResponse) throws JSONException{
        try {
            Iterator it = jsonResponse.keys();
            List<PointValue> values = new ArrayList<PointValue>();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");// + HH:mm:ss for intraday
            while(it.hasNext()){
                String dateStr = (String)it.next();
                Date date = formatter.parse(dateStr);
                PointValue pointValue = new PointValue(date.getTime(), ((float) jsonResponse.getDouble(dateStr)));
                values.add(pointValue);
            }
            displayChart(values);
        }
        catch(Exception ex){
            Utils.showError(getContext(),"Could not parse price data",ex.toString(),false);
        }
    }

    void displayChart(List<PointValue> values){
        Collections.sort(values,new Comparator<PointValue>() {
            @Override
            public int compare(PointValue left, PointValue right) {
                if(left.getX()> right.getX())//left data point is older
                    return 1;
                else if(left.getX() == right.getX())//equal
                    return 0;
                else
                    return -1;//newer, swap
            }
        });
        //Collections.reverse(values);
        LineChartData lineChartData = new LineChartData();
        Line line = new Line(values).setColor(Color.BLUE);
        List<Line> lines = new ArrayList<Line>();
        lines.add(line);
        lineChartData.setLines(lines);
        setAxisValues(lineChartData,values);
        LineChartView lView = (LineChartView) getView().findViewById(R.id.priceChart);
        lView.setInteractive(true);
        lView.setLineChartData(lineChartData);
        lView.setMaxZoom(10f);
        //lView.setZoomLevel(lView.getMaximumViewport().right, values.get(values.size()-1).getY(), 5.0f);
        //todo split by years?
        lView.setOnValueTouchListener(new LineChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
                TextView priceLabel = (TextView)getActivity().findViewById(R.id.priceLabel);
                TextView dateLabel = (TextView)getActivity().findViewById(R.id.dateLabel);
                //X values are float dates - convert them back to human readable format
                Calendar calendar = floatToDate(value.getX());
                String dateStr = calendar.get(Calendar.YEAR)+"-"+(calendar.get(Calendar.MONTH)+1)+"-"+calendar.get(Calendar.DAY_OF_MONTH);
                //set label values with format
                //Y value = price at given date
                Spanned dateSpan = Html.fromHtml("<b>Date: </b>"+dateStr);
                dateLabel.setText(dateSpan);
                Spanned priceSpan = Html.fromHtml("<b>Price: </b> €"+value.getY());
                priceLabel.setText(priceSpan);
            }
            @Override
            public void onValueDeselected() {
            }
        });
    }
    //convert float date back to string
    Calendar floatToDate(float floatValue){
        long longEpoch = (long) (floatValue) ;
        Date date = new Date(longEpoch);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    void setAxisValues(LineChartData lineChartData, List<PointValue> values){
        //set X axis values to years
        List<AxisValue> XaxisValues = new ArrayList<AxisValue>();
        float startValue = values.get(0).getX();
        float endValue = values.get(values.size()-1).getX();
        Calendar endCal = floatToDate(endValue);
        int lastYear = endCal.get(Calendar.YEAR);
        Calendar startCal = floatToDate(startValue);
        int yearAfterStart = startCal.get(Calendar.YEAR);
        if(yearAfterStart != lastYear)
            yearAfterStart += 1;
        for(int year = yearAfterStart; year<=lastYear; year++){
            //set float date value to first of the year
            startCal.set(year,0,1);
            //every AxisValue contains the actual int/float value and a label to display on the axis
            //add 01/01/year date to list as in floating point format and year string as label
            XaxisValues.add(new AxisValue(startCal.getTime().getTime(), String.format("%d", year).toCharArray()));
        }
        Axis axisX = new Axis(XaxisValues);
        lineChartData.setAxisXBottom(axisX);

        //set Y axis values to highest-lowest price interval
        List<AxisValue> YaxisValues = new ArrayList<AxisValue>();
        float maxValue = 0;
        float minValue = 0;
        for(PointValue v : values){
            if(v.getY()>maxValue)
                maxValue = v.getY();
            else if(v.getY()<minValue || minValue == 0)
                minValue = v.getY();
        }
        //(int) (maxValue-minValue) / 10;//discarded
        int interval = 2;
        int i = (int)minValue;
        while(i <= maxValue){
            //add mod 2 of the result to the incrementer to include only even numbers
            i+= interval + (i + interval ) %2;
            //add new intermediate value to list with integer value i and i to string as label
            YaxisValues.add(new AxisValue(i,String.format("%d", i).toCharArray()));
        }
        Axis axisY = new Axis(YaxisValues);
        lineChartData.setAxisYLeft(axisY);
    }

    //autogenerated boilerplate
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_price_history, container, false);
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
