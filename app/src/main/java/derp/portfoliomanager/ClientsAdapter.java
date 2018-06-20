package derp.portfoliomanager;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 03/12/2017.
 */

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ViewHolder> implements Filterable {
    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public Button nameTextView;
        public Button editButton;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (Button) itemView.findViewById(R.id.clientName);
            editButton = (Button) itemView.findViewById(R.id.editClientButton);
        }
    }
    private List<ShortClient> mClients;
    private List<ShortClient> mFilteredClients;
    // Store the context for easy access
    private Context mContext;

    // Pass in the contact array into the constructor
    public ClientsAdapter(Context context, List<ShortClient> contacts) {
        mClients = contacts;
        mFilteredClients = contacts;
        mContext = context;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    @Override
    public ClientsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_client, parent, false);
        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // UI hook for each item in clients list
    @Override
    public void onBindViewHolder(ClientsAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        final ShortClient client = mFilteredClients.get(position);
        // Set item text
        Button nameButton = viewHolder.nameTextView;
        nameButton.setText(client.getName());
        nameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PortfolioList pList = PortfolioList.newInstance(client.getId());
                pList.show(((FragmentActivity)getContext()).getSupportFragmentManager().beginTransaction(), "PortfolioList");
            }
        });
        //add handler for edit button
        Button button = viewHolder.editButton;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openClientDetails(client.getId());
            }
        });
    }
    public void openClientDetails(int id){
        // -1 = new client
        final AppCompatActivity act = (AppCompatActivity) mContext;
        ClientDetailsFragment newFragment = ClientDetailsFragment.newInstance();
        // no arguments if the client is new
        if(id != -1) {
            Bundle args = new Bundle();
            args.putInt("id", id);
            newFragment.setArguments(args);
        }
        //switch screen
        FragmentTransaction transaction = act.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, newFragment);
        //add the transaction to the back stack so the user can navigate back
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
    }

    //filter function for the search box
    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    //no search - show all clients
                    mFilteredClients = mClients;
                } else {
                    //check if every client contains the text in the search box
                    ArrayList<ShortClient> filteredList = new ArrayList<>();
                    for (ShortClient client : mClients) {
                        if (client.getName().toLowerCase().contains(charString.toLowerCase())) {
                            //append it to filtered list
                            filteredList.add(client);
                        }
                    }
                    mFilteredClients = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredClients;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredClients = (ArrayList<ShortClient>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mFilteredClients.size();
    }


}
