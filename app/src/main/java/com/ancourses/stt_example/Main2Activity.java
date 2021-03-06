package com.ancourses.stt_example;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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

public class Main2Activity extends AppCompatActivity {

    ImageButton btnRecord;

    EditText etResult;

    ProgressBar pb;

    Button btnConvert;

    TextView tvSearchResult;

    TextView tvQuestion;

    String currentType = "";

    MainActivityPresenter mPresenter;

    ImageView ivLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        pb = findViewById(R.id.pb);

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
                    Toast.makeText(Main2Activity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
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

                    Toast.makeText(Main2Activity.this, "You must enter Text", Toast.LENGTH_SHORT).show();
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


        //ivLocation = findViewById(R.id.iv_location);
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


    private void callWebService(String text) {

        try {

            tvQuestion.setVisibility(View.VISIBLE);
            tvQuestion.setText(text);

            text = text.replace("؟", "");

            String[] result = text.split("\\s+");

            String keyWord = "";

            if (result[0].trim().equals("اين") || result[0].trim().equals("وين")) {

                currentType = "1";

                if (result[1].trim().equals("محل") || result[1].trim().equals("مكان"))
                    keyWord = result[2];
                else
                    keyWord = result[1];

                GetDataAsync getDataAsync = new GetDataAsync();
                getDataAsync.execute(keyWord);


            } else if (result[0].trim().equals("متى") && result[1].trim().equals("يفتح")) {

                if (result[2].trim().equals("محل") || result[2].trim().equals("مكان"))
                    keyWord = result[3];
                else
                    keyWord = result[2];


                currentType = "2";

                GetDataAsync getDataAsync = new GetDataAsync();
                getDataAsync.execute(keyWord);

            } else if (result[0].trim().equals("ما") && result[1].trim().equals("هى")) {

                if (result[2].trim().equals("المحلات")) {
                    keyWord = result[3];

                    GetMultiDataAsync getMultiDataAsync = new GetMultiDataAsync();
                    getMultiDataAsync.execute(keyWord);
                }

            }

        } catch (Exception ex) {
            Toast.makeText(Main2Activity.this, "Your Words not Valid", Toast.LENGTH_SHORT).show();
        }

    }


    private class GetDataAsync extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... data) {

            String shopsUrl = "https://samall.000webhostapp.com/searchShops.php?name=" + data[0] + "&type=" + currentType;

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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main2Activity.this, "Error in connect to Server", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("shops");

                    String result = "";

                    if (currentType.trim().equals("1")) {

                        result = shopsArray.getJSONObject(0).getString("description");

                        String imageUrl = shopsArray.getJSONObject(0).getString("image_location");
                        Glide.with(Main2Activity.this).load(imageUrl).into(ivLocation);

                    } else if (currentType.trim().equals("2")) {

                        result = shopsArray.getJSONObject(0).getString("open_time");
                    }


                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);

                    mPresenter.startSpeak(result);


                } else {

                    Toast.makeText(Main2Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetMultiDataAsync extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... data) {

            String shopsUrl = "https://samall.000webhostapp.com/searchMultiShops.php?category=" + data[0];

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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pb.setVisibility(View.INVISIBLE);

            if (s == null) {

                Toast.makeText(Main2Activity.this, "Error in connect to Server", Toast.LENGTH_LONG).show();
                return;
            }

            //parse json
            try {

                JSONObject rootJson = new JSONObject(s);

                int status = rootJson.getInt("status");

                if (status == 1) {

                    JSONArray shopsArray = rootJson.getJSONArray("shops");

                    String result = "";

                    for (int i =0; i < shopsArray.length(); i++) {

                        JSONObject currentJSON = shopsArray.getJSONObject(i);

                        result += "\n" + currentJSON.getString("name") + "\n ------------------";

                    }


                    tvSearchResult.setText(result);
                    tvSearchResult.setVisibility(View.VISIBLE);

                    mPresenter.startSpeak(result);


                } else {

                    Toast.makeText(Main2Activity.this, rootJson.getString("msg"), Toast.LENGTH_LONG).show();

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
