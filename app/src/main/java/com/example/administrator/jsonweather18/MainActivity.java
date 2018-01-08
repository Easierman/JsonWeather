package com.example.administrator.jsonweather18;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;




import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity{
    HttpURLConnection httpConn = null;
    InputStream din =null;
    //任务四
//    Button find = null;
//    EditText value = null;
//    TextView tv_show = null;
    private Button find;
    private EditText value;
    private TextView tv_show;
    private String cityname="广州";
    private AutoCompleteTextView mCityname;
    String db_name = "weather";
    String db_path = "data/data/com.example.administrator.jsonweather18/database/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("天气查询Json解析");
        //任务4️
        copydb();
        find = (Button)findViewById(R.id.find);
        value = (EditText)findViewById(R.id.value);
//        value.setText("广州");//初始化，给个初值，方便测试
        tv_show = (TextView)findViewById(R.id.tv_show);

        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_show.setText("");//清空数据
                cityname = mCityname.getText().toString();
                Toast.makeText(MainActivity.this, "正在查询天气信息", Toast.LENGTH_SHORT).show();
                GetJson gd = new GetJson(cityname);//调用线程类创建的对象
                gd.start();//运行线程对象
            }
        });
        mCityname = (AutoCompleteTextView) findViewById(R.id.value);
        mCityname.setThreshold(1);
        mCityname.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(db_path+db_name,null);
                Cursor cursor = null;
                try{
                    cursor = db.rawQuery("select area_name from weathers where area_name like '%"+str+"%'", null);
                }catch(Exception e){
                    e.printStackTrace();
                }
                List<String> list = new ArrayList<String>();
                String pro="";
                while(cursor.moveToNext()){
                    pro = cursor.getString(cursor.getColumnIndex("area_name"));
                    list.add(pro);

                }
                cursor.close();
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,list);
                mCityname.setAdapter(adapter);

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }



    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 123:
                    showData((String)msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private  void showData(String jData){
        tv_show.setText(jData);
        //这里我直接显示json数据，没解析。解析的方法，请参考教材或网上相应的代码
        try {
            JSONObject jobj = new JSONObject(jData);
            JSONObject weather = jobj.getJSONObject("data");
            StringBuffer wbf = new StringBuffer();
            wbf.append("天气提示："+weather.getString("ganmao")+"\n");
            wbf.append("当前温度："+weather.getString("wendu")+"\n");
            JSONArray jary = weather.getJSONArray("forecast");
            for(int i=0;i<jary.length();i++){
                JSONObject pobj = (JSONObject)jary.opt(i);
                wbf.append("日期："+pobj.getString("date")+"\n");
                wbf.append("最高温："+pobj.getString("high")+"\n");
                wbf.append("最低温度："+pobj.getString("low")+"\n");
                wbf.append("风向："+pobj.getString("fengxiang")+"\n");
                String fengli=pobj.getString("fengli");
                int ep=fengli.indexOf("]]>");
                fengli=fengli.substring(9,ep);
                wbf.append("风力："+fengli+"\n");
            }
            tv_show.setText(wbf.toString());
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    private void copydb(){
        File db_file = new File(db_path+db_name);
        Log.i("weather","数据库创建");
        if(!db_file.exists()){
            File db_dir= new File(db_path);
            if(!db_dir.exists()){
                db_dir.mkdir();
            }
            InputStream is = getResources().openRawResource(R.raw.weather);
            try {
                OutputStream os = new FileOutputStream(db_path+db_name);
                byte[]buff = new byte[1024];
                int length = 0;
                while((length=is.read(buff))>0){
                    os.write(buff,0,length);
                }
                os.flush();
                os.close();
                is.close();
            }catch (Exception ee){
                ee.printStackTrace();
            }

        }

    }
    class GetJson extends Thread{

        private String urlstr =  "http://wthrcdn.etouch.cn/weather_mini?city=";
        public GetJson(String cityname){
            try{
                urlstr = urlstr+URLEncoder.encode(cityname,"UTF-8");

            }catch (Exception ee){

            }
        }
        @Override
        public void run() {
            try {
                URL url = new URL(urlstr);
                httpConn = (HttpURLConnection)url.openConnection();
                httpConn.setRequestMethod("GET");
                din = httpConn.getInputStream();
                InputStreamReader in = new InputStreamReader(din);
                BufferedReader buffer = new BufferedReader(in);
                StringBuffer sbf = new StringBuffer();
                String line = null;
                while( (line=buffer.readLine())!=null) {
                    sbf.append(line);
                }
                Message msg = new Message();
                msg.obj = sbf.toString();
                msg.what = 123;
                handler.sendMessage(msg);
                Looper.prepare(); //在线程中调用Toast，要使用此方法，这里纯粹演示用:)
                Toast.makeText(MainActivity.this,"获取数据成功",Toast.LENGTH_LONG).show();
                Looper.loop(); //在线程中调用Toast，要使用此方法



            }catch (Exception ee){
                Looper.prepare(); //在线程中调用Toast，要使用此方法
                Toast.makeText(MainActivity.this,"获取数据失败，网络连接失败或输入有误",Toast.LENGTH_LONG).show();
                Looper.loop(); //在线程中调用Toast，要使用此方法
                ee.printStackTrace();
            }finally {
                try{
                    httpConn.disconnect();
                    din.close();

                }catch (Exception ee){
                    ee.printStackTrace();
                }
            }
        }
    }

}
