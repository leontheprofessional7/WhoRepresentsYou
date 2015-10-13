package com.leontheprofessional.test.whorepresentsyou.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.leontheprofessional.test.whorepresentsyou.R;
import com.leontheprofessional.test.whorepresentsyou.activity.fragment.DisplayFragment;
import com.leontheprofessional.test.whorepresentsyou.adapter.CustomAdapter;
import com.leontheprofessional.test.whorepresentsyou.helper.GeneralHelper;
import com.leontheprofessional.test.whorepresentsyou.jsonparsing.WhoRepresentsYouApi;
import com.leontheprofessional.test.whorepresentsyou.model.MemberModel;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;

import static com.leontheprofessional.test.whorepresentsyou.helper.GeneralHelper.isZipCode;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String CUSTOM_SEARCH_INTENT_FILTER = "com.leontheprofessional.test.CustomSearchIntentFilter";

    private ListView listView;

    private CustomAdapter customAdapter;

    private ArrayList<MemberModel> members;

    private String zipcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        members = new ArrayList<>();

        if (savedInstanceState != null && savedInstanceState.containsKey(getString(R.string.save_instance_state_main_activity))) {
            members = savedInstanceState.getParcelableArrayList(getString(R.string.save_instance_state_main_activity));
        } else if (GeneralHelper.isNetworkConnectionAvailable(MainActivity.this)) {
            refreshPage(getIntent());
        } else {
            showFavorite();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putParcelableArrayList(getString(R.string.save_instance_state_main_activity), members);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_search:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }

    private void refreshPage(Intent intent) {

        Log.v(LOG_TAG, "handleIntent() executed");

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            zipcode = intent.getStringExtra(SearchManager.QUERY);
        } else if (CUSTOM_SEARCH_INTENT_FILTER.equals(intent.getAction())) {
            zipcode = intent.getStringExtra(getString(R.string.search_keyword_identifier));
            Log.v(LOG_TAG, "zipcode: " + zipcode);
        }

        if (GeneralHelper.isNetworkConnectionAvailable(MainActivity.this)) {
            if (isZipCode(zipcode)) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {

                        WhoRepresentsYouApi whoRepresentsYouApi = new WhoRepresentsYouApi();
                        try {
                            members = whoRepresentsYouApi.getAllMemberByZipCode(MainActivity.this, zipcode);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }


                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                        if (GeneralHelper.isTablet(MainActivity.this)) {
                            Log.v(LOG_TAG, "This is a tablet");
                            setContentView(R.layout.fragment_main);
                            Bundle arguments = new Bundle();
                            arguments.putParcelableArrayList(getString(R.string.fragment_argument_identifier), members);
                            DisplayFragment displayFragment = new DisplayFragment();
                            displayFragment.setArguments(arguments);
                            FragmentManager fragmentManager = getFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            fragmentTransaction.replace(R.id.container1, displayFragment);
                            fragmentTransaction.addToBackStack(null);

                            fragmentTransaction.commit();
                        } else {
                            setContentView(R.layout.activity_main);
                            listView = (ListView) findViewById(R.id.listview_main_activity);

                            customAdapter = new CustomAdapter(MainActivity.this, members);
                            listView.setAdapter(customAdapter);
                            TextView emptyTextView = new TextView(MainActivity.this);
                            emptyTextView.setText(getString(R.string.empty_textview));
                            listView.setEmptyView(emptyTextView);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Intent intent = new Intent(MainActivity.this, MemberDetailsActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable(getString(R.string.parcelable_identifier), members.get(position));
                                    intent.putExtra(getString(R.string.bundle_identifier), bundle);
                                    intent.setAction(CUSTOM_SEARCH_INTENT_FILTER);
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                }.execute();
            } else {
                Log.e(LOG_TAG, "Zipcode is incorrect.");
            }
        } else {
            Toast.makeText(MainActivity.this, getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavorite() {

    }
}