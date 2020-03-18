package com.ancourses.stt_example;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main3Activity extends AppCompatActivity {

    ImageButton btnRecord;

    EditText etResult;

    ProgressBar pb;

    Button btnConvert, btnGo;

    TextView tvSearchResult;

    TextView tvQuestion;

    String currentIntent = "",  Slot_String="";

    MainActivityPresenter mPresenter;

    ImageView ivLocation;
    String imageUrl;
    ArrayList<RegularData> regularData = new ArrayList<>();
    ArrayList<String> temp = new ArrayList<>();
    int start,index;
    int [] images ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        fillRegularData();

        pb = findViewById(R.id.pb);
        btnGo = findViewById(R.id.btn_go);

        btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");


                try {
                    startActivityForResult(i, 1000);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(Main3Activity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
                }

            }
        });

        etResult = findViewById(R.id.et_result);

        btnConvert = findViewById(R.id.btn_convert);
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String text = etResult.getText().toString();

                if (text.trim().equals("")) {

                    Toast.makeText(Main3Activity.this, "You must enter Text", Toast.LENGTH_SHORT).show();
                    return;

                }

                callWebService(text);

            }
        });


        tvSearchResult = findViewById(R.id.tv_search_result);

        tvQuestion = findViewById(R.id.tv_question);

        mPresenter = new MainActivityPresenter();
        mPresenter.initGCPTTSSettings();
        mPresenter.initAndroidTTSSetting();

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Main3Activity.this,ImageLocation.class);
                i.putExtra("images",imageUrl);
                startActivity(i);
            }
        });

    }

    /**
     * This method reads from a regular expression json file and fill regularData
     * array.
     *
     */
    private void fillRegularData() {
        String fileContent = loadData("regulars.txt");

        //parse json
        try {

            JSONArray rootJson = new JSONArray(fileContent);


            for (int i = 0; i < rootJson.length(); i++) {

                JSONObject currentJSON = rootJson.getJSONObject(i);

                RegularData mRegularData = new RegularData();
                mRegularData.setRule(currentJSON.getString("rule"));
                mRegularData.setIntent(currentJSON.getInt("intent"));
                mRegularData.setSlot(currentJSON.getInt("slot"));


                regularData.add(mRegularData);
            }



        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    /**
     * Find matched pattern with user's question to extract intent and slot.
     * @param question
     * @return matched RegularData
     * else
     * @return null
     */
    private RegularData getMatchedRegular(String question) {

        for (int i = 0; i < regularData.size(); i++) {

            Pattern pattern = Pattern.compile(regularData.get(i).getRule());

            Matcher matcher = pattern.matcher(question);

            if (matcher.matches()) {
                return regularData.get(i);
            }

        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000) {

            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                String r = res.get(0);

                etResult.setText(r);

                callWebService(r);


            }
        }
    }

    /**
     *This method analyze the user question by the following steps:
     * First: Remove the question mark to analyze.
     * Second: get matched Object from RegularData.
     *  if there isn't, the system shall response with error msg
     * Third: splitting the question to extract the slot
     * Third: calling webService by the requiring Intent and Slot.
     * if there isn't, the system shall response with error msg
     * @param question
     */

    private void callWebService(String question) {
        try {
            tvQuestion.setVisibility(View.VISIBLE);
            tvQuestion.setText(question);
           // editimg.setVisibility(View.VISIBLE);
            question = question.replace("؟", "");
            RegularData selectedRegularData = getMatchedRegular(question);

            if (selectedRegularData == null) {
                Toast.makeText(Main3Activity.this, "أرجوا إعادة صياغة السؤال", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] split_question = question.split("\\s+");
            String Slot = split_question[selectedRegularData.getSlot()];
            String [] new_slot_array;
            // <====counter to see the slot's position ====>
            for(int counter = 0; counter <= split_question.length-1;counter++){
                if(split_question[counter].equals(Slot)){
                    start = counter;
                    // loop to add the slots =====>
                    for(int j = start ; j <= split_question.length-1; j++){
                        temp.add(split_question[j]);
                        index++;
                    }// end for loop
                } // end if
            } // end for
            new_slot_array = temp.toArray(new String[temp.size()]);
            // make slot array into word
            for(int x = 0; x < new_slot_array.length;x++){
                Slot_String+= "%20"+new_slot_array[x];
            }// end for loop
            Slot_String = Slot_String.substring(3,Slot_String.length());

            if (selectedRegularData.getIntent() == -1) {

                GetPlacesByCategoryAsync getPlacesByCategoryAsync = new GetPlacesByCategoryAsync();
                getPlacesByCategoryAsync.execute(Slot);

            }
            else if (selectedRegularData.getIntent() == -2) {

                GetMultiOffersAsyc getMultiOffersAsyc = new GetMultiOffersAsyc();
                getMultiOffersAsyc.execute(Slot);

            }
            else if (selectedRegularData.getIntent() == -3) {

                GetMultiEventsAsyc getMultiEventsAsyc = new GetMultiEventsAsyc();
                getMultiEventsAsyc.execute(Slot);

            }else if (selectedRegularData.getIntent() == -4) {

                GetMultiFoodAsyc getMultiFoodAsyc = new GetMultiFoodAsyc();
                getMultiFoodAsyc.execute(Slot);

            }
            else {

                currentIntent = selectedRegularData.getIntent() + "";
                GetDataAsync getDataAsync = new GetDataAsync();
                getDataAsync.execute(Slot_String);
            }
        } catch (Exception ex) {
            Toast.makeText(Main3Activity.this, "هل يمكنك إعادة السؤال؟", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method provide the execution of callWebService
     * Initiated by 3 methods
     */
    private class GetDataAsync extends AsyncTask<String, Void, String> {

        /**
         * This method is to provide the progress for the user
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        /**
         * This method is to establish a http connection.
         * @param slot
         * @return result
         */

        @Override
        protected String doInBackground(String... slot) {

            String shopsUrl = "https://samall.000webhostapp.com/searchShops.php?name="+slot[0] +"&intent=" + currentIntent;

            try {

                URL url = new URL(shopsUrl);
                // Open url connection
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.connect();
                // set request method
                httpConnection.setRequestMethod("GET");
                // open input stream and read server response data
                InputStream inputStream = httpConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String result = bufferedReader.readLine();
                bufferedReader.close();
                // close connection
                httpConnection.disconnect();

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        /**
         *This method provide json fetching data to the user's question by the currentIntent
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main3Activity.this, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("shops");

                    String Answer = "";


                    if (currentIntent.trim().equals("1")) {

                        Answer = shopsArray.getJSONObject(0).getString("description");

                        imageUrl = shopsArray.getJSONObject(0).getString("location");
                       // Glide.with(Main3Activity.this).load(imageUrl).into(ivLocation);

                    } else if (currentIntent.trim().equals("2")) {

                        Answer = shopsArray.getJSONObject(0).getString("open_time");
                    }else if (currentIntent.trim().equals("3")){
                        Answer = "عبارة عن " + shopsArray.getJSONObject(0).getString("description") ;
                    }
                    else if (currentIntent.trim().equals("4")) {
                        for (int i = 0; i < shopsArray.length(); i++) {

                            JSONObject currentJSON = shopsArray.getJSONObject(i);

                            Answer += "تبدأ في" +  currentJSON.getString("start_date")  +"\n"+ "و تنتهي في"    +  currentJSON.getString("end_date")+ "\n" +currentJSON.getString("time");

                        }
                    }else if (currentIntent.trim().equals("5")){
                        Answer = "عبارة عن " + shopsArray.getJSONObject(0).getString("description") ;
                    }
                    else if (currentIntent.trim().equals("7")) {
                        for (int i = 0; i < shopsArray.length(); i++) {

                            JSONObject currentJSON = shopsArray.getJSONObject(i);
                            
                            Answer +=  "تبدأ في" + "\n"+ currentJSON.getString("start_date") +"\n" + "و تنتهي في" + "\n" + currentJSON.getString("end_date");

                        }
                    }else if (currentIntent.trim().equals("9")) {

                        for (int i = 0; i < shopsArray.length(); i++) {

                            JSONObject currentJSON = shopsArray.getJSONObject(i);

                            Answer +=  "هو"    +  currentJSON.getString("category")+ "\n"+currentJSON.getString("outdoor");

                        }

                    }
                    tvSearchResult.setText(Answer);
                    tvSearchResult.setVisibility(View.VISIBLE);
                    mPresenter.startSpeak(Answer);



                } else {

                    Toast.makeText(Main3Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method provide the execution of callWebService for multiple data of the place's category
     * Initiated by 3 methods
     */

    private class GetPlacesByCategoryAsync extends AsyncTask<String, Void, String> {
        /**
         * This method is to provide the progress for the user
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        /**
         * This method is to establish a http connection.
         * @param slot
         * @return result
         */

        @Override
        protected String doInBackground(String... slot) {

            String shopsUrl = "https://samall.000webhostapp.com/searchMultiShops.php?category=" + slot[0];

            try {

                URL url = new URL(shopsUrl);

                // Open url connection
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                // set request method
                httpConnection.setRequestMethod("GET");

                // open input stream and read server response data
                InputStream inputStream = httpConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String result = bufferedReader.readLine();


                // close connection
                httpConnection.disconnect();

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        /**
         *This method provide json fetching data to the user's question by the currentIntent
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main3Activity.this, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("stores");

                    String result = "";
                    String talk = "توجد هذه المحلات";

                    for (int i = 0; i < shopsArray.length(); i++) {

                        JSONObject currentJSON = shopsArray.getJSONObject(i);
                        result += "\n" + currentJSON.getString("place.name");

                    }


                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);

                    mPresenter.startSpeak(talk);


                } else {

                    Toast.makeText(Main3Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This method provide the execution of callWebService for multiple data of the offers
     * Initiated by 3 methods
     */
    private class GetMultiOffersAsyc extends AsyncTask<String,Void, String> {
        /**
         * This method is to provide the progress for the user
         */

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        /**
         * This method is to establish a http connection.
         * @param slots
         * @return result
         */
        @Override
        protected String doInBackground(String... slots) {
            String shopsUrl = "https://samall.000webhostapp.com/searchMultiOffers.php?intent="+currentIntent;

            try {

                URL url = new URL(shopsUrl);

                // Open url connection
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                // set request method
                httpConnection.setRequestMethod("GET");

                // open input stream and read server response data
                InputStream inputStream = httpConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String result = bufferedReader.readLine();


                // close connection
                httpConnection.disconnect();

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        /**
         *This method provide json fetching data to the user's question by the currentIntent
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main3Activity.this, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("offers");

                    String result = "";

                    for (int i = 0; i < shopsArray.length(); i++) {

                        JSONObject currentJSON = shopsArray.getJSONObject(i);

                        result += "\n" + currentJSON.getString("name") + "\n ------------------";

                    }


                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);

                    mPresenter.startSpeak(result);


                } else {

                    Toast.makeText(Main3Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method provide the execution of callWebService for multiple data of events
     * Initiated by 3 methods
     */

    private class GetMultiEventsAsyc extends AsyncTask<String,Void, String> {
        /**
         * This method is to establish a http connection.
         * @param slots
         * @return result
         */

        @Override
        protected String doInBackground(String... slots) {
            String shopsUrl = "https://samall.000webhostapp.com/searchMultiEvents.php?intent="+ currentIntent;

            try {

                URL url = new URL(shopsUrl);

                // Open url connection
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                // set request method
                httpConnection.setRequestMethod("GET");

                // open input stream and read server response data
                InputStream inputStream = httpConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String result = bufferedReader.readLine();


                // close connection
                httpConnection.disconnect();

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }
        /**
         * This method is to provide the progress for the user
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        /**
         *This method provide json fetching data to the user's question by the currentIntent
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main3Activity.this, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("events");

                    String result = "";

                    for (int i = 0; i < shopsArray.length(); i++) {

                        JSONObject currentJSON = shopsArray.getJSONObject(i);

                        result += "\n" + currentJSON.getString("name") + "\n ------------------";

                    }
                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);
                    mPresenter.startSpeak(result);
                } else {

                    Toast.makeText(Main3Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method provide the execution of callWebService for multiple data of the offers
     * Initiated by 3 methods
     */
    private class GetMultiFoodAsyc extends AsyncTask<String,Void, String> {
        /**
         * This method is to provide the progress for the user
         */

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        /**
         * This method is to establish a http connection.
         * @param slots
         * @return result
         */
        @Override
        protected String doInBackground(String... slots) {
            String shopsUrl = "https://samall.000webhostapp.com/searchMultiFood.php?category=" + slots[0];

            try {

                URL url = new URL(shopsUrl);

                // Open url connection
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                // set request method
                httpConnection.setRequestMethod("GET");

                // open input stream and read server response data
                InputStream inputStream = httpConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String result = bufferedReader.readLine();


                // close connection
                httpConnection.disconnect();

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        /**
         *This method provide json fetching data to the user's question by the currentIntent
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main3Activity.this, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("food");

                    String result = "";

                    for (int i = 0; i < shopsArray.length(); i++) {

                        JSONObject currentJSON = shopsArray.getJSONObject(i);

                        result += "\n" + currentJSON.getString("place.name") + "\n ------------------";

                    }


                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);

                    mPresenter.startSpeak(result);


                } else {

                    Toast.makeText(Main3Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public String loadData(String inFile) {
        String tContents = "";

        try {
            InputStream stream = getAssets().open(inFile);

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
            // Handle exceptions here
        }

        return tContents;

    }
}
