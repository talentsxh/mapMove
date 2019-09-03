package com.amap.map3d.demo.map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.map3d.demo.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * by sxh
 *
 * 如果没有进度的控制要求和速度要求
 * 建议使用平滑移动demo SmoothMoveActivity
 *
 */

public class ProgressMoveActivity extends Activity implements AMap.InfoWindowAdapter, View.OnClickListener {
    private ImageView mPlayBtn;
    private SeekBar mProgressBar;
    private SeekBar mSpeedBar;
    private MapView mMapView;
    private AMap mAMap;
    private Marker mMarker;
    //播放状态，时间
    private int mPlayState=0;
    private int mNotChangeTime=1000;//默认1000毫秒
    private int mSpeedTime=1000;
    //框提示
    private LinearLayout mInfoWindowLayout;
    private TextView mWinTitle;
    //总数据和经纬度
    private List<HistoryBean.ResultBean> mSubList =new ArrayList<>();
    private List<LatLng> mPointLists =new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_move);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        initViews();
        initDate();

    }

    /**
     * 初始化数据，建议接口，加上加载框，转圈圈体验更佳。
     */
    private void initDate() {
        StringBuilder newstringBuilder = new StringBuilder();
        InputStream inputStream = null;
        try {
            inputStream = getResources().getAssets().open("map_date.json");
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(isr);
            String jsonLine;
            while ((jsonLine = reader.readLine()) != null) {
                newstringBuilder.append(jsonLine);
            }
            reader.close();
            isr.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result =  newstringBuilder .toString();
        Log.d("json",result);

        HistoryBean obj = JSON.parseObject(result,HistoryBean.class);
        handlerDatas(obj);
    }


    public void initViews() {
        mPlayBtn=findViewById(R.id.play_btn);
        mPlayBtn.setOnClickListener(this);
        mProgressBar=findViewById(R.id.progress_bar);
        mSpeedBar=findViewById(R.id.speed_bar);

        if (mAMap == null) {
            mAMap = mMapView.getMap();
        }

        mAMap.getUiSettings().setScaleControlsEnabled(true);
        //进度监听
        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                //Log.e("size",progress+"--");
                if (mSubList==null || mSubList.size()==0){
                    return;
                }
                if (progress== mSubList.size()){
                    //设置按钮
                    mPlayBtn.setBackgroundResource(R.drawable.ybh_ybhz_icon_stop);
                    mPlayState=0;
                    //设置终点marker
                    if (mMarker!=null){
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ybh_ybhz_start_icon_green));
                    }
                }
                moveToPosition(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });
        //速度监听
        mSpeedBar.setMax(mNotChangeTime);
        mSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i=i-mNotChangeTime;
                if (Math.abs(i)<40){
                    i=40;
                }
                mSpeedTime=Math.abs(i);
                // Log.e("progress",i+"--");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }


    /**
     * 移动进度
     * @param progress
     */
    private void moveToPosition(int progress) {
        if (mSubList!=null && mSubList.size()>0) {
            if (progress >= mSubList.size()) {
                LatLng position = new LatLng(mSubList.get(mSubList.size() - 1).getLat(), mSubList.get(mSubList.size() - 1).getLng());
                //设置移动中心
                mAMap.animateCamera(CameraUpdateFactory.newLatLng(position));
                //设置marker的位置和对应信息
                mMarker.setPosition(position);
                if (!TextUtils.isEmpty(mSubList.get(mSubList.size() - 1).getDesc())) {
                    mMarker.setTitle(mSubList.get(mSubList.size() - 1).getDesc());
                }
                //设置速度方向
                if (!TextUtils.isEmpty(mSubList.get(mSubList.size() - 1).getC())) {
                    mMarker.setRotateAngle(Math.abs(Float.valueOf(mSubList.get(mSubList.size() - 1).getC()) - 360));
                }
            } else {
                LatLng position = new LatLng(mSubList.get(progress).getLat(), mSubList.get(progress).getLng());
                //设置移动中心
                mAMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                //设置速度方向
                if (!TextUtils.isEmpty(mSubList.get(progress).getC())) {
                    mMarker.setRotateAngle(Math.abs(Float.valueOf(mSubList.get(progress).getC()) - 360));
                }
                //设置marker的位置和对应信息
                mMarker.setPosition(position);
                if (!TextUtils.isEmpty(mSubList.get(progress).getDesc())) {
                    mMarker.setTitle(mSubList.get(progress).getDesc());
                    mMarker.showInfoWindow();
                }

            }
        }

    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();


    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mHandler!=null) {
            mHandler.removeMessages(1);
        }

    }


    /**
     * 自定义View并且绑定数据方法
     *
     * @param marker 点击的Marker对象
     * @return 返回自定义窗口的视图
     */
    private View getInfoWindowView(Marker marker) {
        if (mInfoWindowLayout == null) {
            mInfoWindowLayout = new LinearLayout(this);
            mInfoWindowLayout.setOrientation(LinearLayout.VERTICAL);
            mWinTitle = new TextView(this);
            mWinTitle.setTextSize(12);
            mWinTitle.setTextColor(Color.BLACK);
            mInfoWindowLayout.setBackgroundResource(R.drawable.infowindow_bg);
            mInfoWindowLayout.addView(mWinTitle);
        }
        if(mWinTitle!=null) {
            mWinTitle.setText(Html.fromHtml(marker.getTitle()));
        }

        return mInfoWindowLayout;
    }

    /**
     * 添加轨迹线
     */
    private void addPolyline() {
        if (mPointLists==null){
            return;
        }
        //画线 有需要可以参照其他画线功能添加图片
        PolylineOptions polylineOptions=new PolylineOptions();
        polylineOptions.setPoints(mPointLists);
        polylineOptions.color(0xff2dff50).width(6);
        mAMap.addPolyline(polylineOptions);

        // 读取轨迹点
        if (mPointLists==null || mPointLists.size()==0){
            Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        //添加marker到地图上
        MarkerOptions options=new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ybh_ybhz_start_icon));
        mMarker = mAMap.addMarker(options);
        //设置最大进度
        mProgressBar.setMax(mSubList.size());
        //设置图标起始位置
        mMarker.setPosition(mPointLists.get(0));
        // 设置  自定义的InfoWindow 适配器
        mAMap.setInfoWindowAdapter(this);
        // 显示 infowindow
        mMarker.showInfoWindow();
        //移动到起点
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mPointLists.get(0), 16));

    }

    /**
     * 读取坐标点
     *
     * @return
     */
    private void readLatLngs() {
        mPointLists.clear();
        for (int i = 0; i < mSubList.size(); i++) {
            mPointLists.add(new LatLng(mSubList.get(i).getLat(), mSubList.get(i).getLng()));
        }
        Log.e("map",mPointLists.size()+"--");
        Log.e("maplist",mSubList.size()+"--");
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what==1){
                //根据定时时间设置速度
                mProgressBar.setProgress(mProgressBar.getProgress()+1);
                if (mProgressBar.getProgress()!= mSubList.size()) {
                    sendEmptyMessageDelayed(1,mSpeedTime);
                }
            }else if (msg.what==2){
                removeMessages(1);
            }
        }
    };




    private void handlerDatas(HistoryBean obj) {
        if (obj == null || !obj.getStatus().equals("1")) {
            if (obj!=null && obj.getMsg()!=null){
                Toast.makeText(this,obj.getMsg(),Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (obj.getResult()==null){
            if (obj != null && obj.getMsg() != null) {
                Toast.makeText(this, obj.getMsg(), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        mSubList=obj.getResult();
        //获取经纬度
        readLatLngs();
        //设置路线
        addPolyline();
    }



    @Override
    public View getInfoWindow(Marker marker) {
        return getInfoWindowView(marker);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return getInfoWindowView(marker);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn:
                if (mSubList==null || mSubList.size()==0){
                    return;
                }
                if (mPlayState==0) {
                    if (mProgressBar.getProgress()== mSubList.size()) {
                        mProgressBar.setProgress(0);
                    }
                    mPlayBtn.setBackgroundResource(R.drawable.ybh_ybhz_icon_broadcast);
                    mPlayState=1;
                    mHandler.sendEmptyMessage(1);
                    if (mMarker!=null){
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ybh_ybhz_start_icon));
                    }
                }else{
                    mHandler.sendEmptyMessage(2);
                    mPlayBtn.setBackgroundResource(R.drawable.ybh_ybhz_icon_stop);
                    mPlayState=0;
                }
                break;
        }
    }
}
