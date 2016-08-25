
package com.panshi.api.baidu;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yangshoule on 2016/8/25.
 */
public class BaiduMapActivity extends Activity {

    public final static String LATITUDE = "latitude";
    public final static String LONGITUDE = "longitude";
    public final static String ADDRESS = "address";
    public final static String NAME = "name";


    private ImageView original;

    private BaiduMapAdatper adatper;

    private LatLng originalLL, currentLL;//初始化时的经纬度和地图滑动时屏幕中央的经纬度

    static MapView mMapView = null;
    private GeoCoder mSearch = null;
    private LocationClient mLocClient;// 定位相关
    public MyLocationListenner myListener = new MyLocationListenner();

    private LinearLayout sendButton = null;
    private PoiSearch mPoiSearch;

    private List<PoiInfo> datas;
    private PoiInfo lastInfo = null;
    public static BaiduMapActivity instance = null;
    private ProgressDialog progressDialog;
    private BaiduMap mBaiduMap;
    private MapStatusUpdate myselfU;

    private ListView listView;

    private boolean changeState = true;//当滑动地图时再进行附近搜索

    private int preCheckedPosition = 0;//点击的前一个位置

    private TextView refreshText;


    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    public class BaiduSDKReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            String st1 = "Network error";
            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {

                String st2 = "key validation error!Please on AndroidManifest.xml file check the key set";
                Toast.makeText(instance, st2, Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                Toast.makeText(instance, st1, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BaiduSDKReceiver mBaiduReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        instance = this;
        setContentView(R.layout.activity_baidumap);
        setTitle();
        init();

    }

    private void init() {
        original = (ImageView) findViewById(R.id.bmap_local_myself);
        listView = (ListView) findViewById(R.id.bmap_listview);
        mMapView = (MapView) findViewById(R.id.bmap_View);
        mSearch = GeoCoder.newInstance();
        sendButton = (LinearLayout) findViewById(R.id.right_title_layout);
        refreshText = (TextView) findViewById(R.id.bmap_refresh);
        ImageView centerIcon = (ImageView) findViewById(R.id.bmap_center_icon);

        datas = new ArrayList<PoiInfo>();
        adatper = new BaiduMapAdatper(BaiduMapActivity.this,
                datas, R.layout.adapter_baidumap_item);
        listView.setAdapter(adatper);
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra(LATITUDE, 0);
        LocationMode mCurrentMode = LocationMode.NORMAL;
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(msu);
        mPoiSearch = PoiSearch.newInstance();
        mMapView.setLongClickable(true);
        // 隐藏百度logo ZoomControl
        int count = mMapView.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mMapView.getChildAt(i);
            if (child instanceof ImageView || child instanceof ZoomControls) {
                child.setVisibility(View.INVISIBLE);
            }
        }
        // 隐藏比例尺
        mMapView.showScaleControl(false);
        if (latitude == 0) {
            mMapView = new MapView(this, new BaiduMapOptions());
            mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
                    mCurrentMode, true, null));
            mBaiduMap.setMyLocationEnabled(true);
            showMapWithLocationClient();
            setOnclick();
        } else {
            double longtitude = intent.getDoubleExtra(LONGITUDE, 0);
            String address = intent.getStringExtra(ADDRESS);
            LatLng p = new LatLng(latitude, longtitude);
            mMapView = new MapView(this,
                    new BaiduMapOptions().mapStatus(new MapStatus.Builder()
                            .target(p).build()));
            listView.setVisibility(View.GONE);
            refreshText.setVisibility(View.GONE);
            original.setVisibility(View.GONE);
            centerIcon.setVisibility(View.GONE);
            showMap(latitude, longtitude, address.split("|")[1]);
        }

        // 注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mBaiduReceiver = new BaiduSDKReceiver();
        registerReceiver(mBaiduReceiver, iFilter);


    }

    /**
     * 设置点击事件
     */
    private void setOnclick() {
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                changeState = true;
            }
        });
        original.setOnClickListener(new MyOnClickListener());
        listView.setOnItemClickListener(new MyItemClickListener());
        mPoiSearch.setOnGetPoiSearchResultListener(new MyGetPoiSearchResult());
        mBaiduMap.setOnMapStatusChangeListener(new MyMapStatusChangeListener());
        mSearch.setOnGetGeoCodeResultListener(new MyGetGeoCoderResultListener());
    }

    private boolean isSearchFinished;
    private boolean isGeoCoderFinished;

    private void refreshAdapter() {
        if (isSearchFinished && isGeoCoderFinished) {
            adatper.notifyDataSetChanged();
            refreshText.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            isSearchFinished = false;
            isGeoCoderFinished = false;
        }
    }


    /**
     * 根据关键字查找附近的位置信息
     */
    private class MyGetPoiSearchResult implements OnGetPoiSearchResultListener {

        @Override
        public void onGetPoiResult(PoiResult poiResult) {

            datas.addAll(poiResult.getAllPoi());
            preCheckedPosition = 0;
            isSearchFinished = true;

            refreshAdapter();
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }
    }

    /**
     * 根据经纬度进行反地理编码
     */
    private class MyGetGeoCoderResultListener implements OnGetGeoCoderResultListener {

        @Override
        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                return;
            }

            lastInfo.address = result.getAddress();
            lastInfo.location = result.getLocation();
            lastInfo.name = "[位置]";
            datas.add(lastInfo);
            preCheckedPosition = 0;
            adatper.setSelection(0);
            isGeoCoderFinished = true;
            refreshAdapter();

        }
    }

    /**
     * 监听位置发生了变化
     */
    private class MyMapStatusChangeListener implements BaiduMap.OnMapStatusChangeListener {

        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus) {
            if (changeState) {
                datas.clear();
                refreshText.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            }

        }

        @Override
        public void onMapStatusChange(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeFinish(MapStatus mapStatus) {
            if (changeState) {
                boolean isFirstLoad = true;
                if (isFirstLoad) {
                    originalLL = mapStatus.target;
                }
                currentLL = mapStatus.target;
                // 反Geo搜索
                mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(currentLL));
                mPoiSearch.searchNearby(new PoiNearbySearchOption().keyword("小区")
                        .location(currentLL).radius(1000));
            }
        }
    }


    /**
     * 查看别人发过来，或者已经发送出去的位置信息
     *
     * @param latitude   维度
     * @param longtitude 经度
     * @param address    详细地址信息
     */
    private void showMap(double latitude, double longtitude, String address) {
        sendButton.setVisibility(View.GONE);
        LatLng llA = new LatLng(latitude, longtitude);
        OverlayOptions ooA = new MarkerOptions().position(llA).icon(BitmapDescriptorFactory
                .fromResource(R.drawable.icon_yourself_lication))
                .zIndex(4).draggable(true);
        mBaiduMap.addOverlay(ooA);
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(llA, 17.0f);
        mBaiduMap.animateMapStatus(u);
    }

    /**
     * 显示当前的位置信息
     */
    private void showMapWithLocationClient() {
        String str1 = "正在刷新";
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(str1);
        progressDialog.setOnCancelListener(new OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                finish();
            }
        });

        progressDialog.show();

        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("gcj02");
        option.setIsNeedAddress(true);
        option.setScanSpan(10000);
        mLocClient.setLocOption(option);
        mLocClient.start();


    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        if (mLocClient != null) {
            mLocClient.stop();
        }
        super.onPause();
        lastInfo = null;
    }


    @Override
    protected void onDestroy() {
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.onDestroy();
        unregisterReceiver(mBaiduReceiver);
        super.onDestroy();
    }


    /**
     * 监听函数，有新位置的时候，格式化成字符串，输出到屏幕中
     */
    public class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }
            sendButton.setEnabled(true);
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            if (lastInfo != null) {
                return;
            }
            lastInfo = new PoiInfo();
            mBaiduMap.clear();
            LatLng llA = new LatLng(location.getLatitude(), location.getLongitude());

            lastInfo.location = llA;
            lastInfo.address = location.getAddrStr();
            lastInfo.name = "[位置]";


            LatLng ll = new LatLng(location.getLatitude() - 0.0002, location.getLongitude());
            CoordinateConverter converter = new CoordinateConverter();//坐标转换工具类
            converter.coord(ll);//设置源坐标数据
            converter.from(CoordinateConverter.CoordType.COMMON);//设置源坐标类型
            LatLng convertLatLng = converter.convert();
            OverlayOptions myselfOOA = new MarkerOptions().position(convertLatLng).icon(BitmapDescriptorFactory
                    .fromResource(R.drawable.icon_yourself_lication))
                    .zIndex(4).draggable(true);
            mBaiduMap.addOverlay(myselfOOA);
            myselfU = MapStatusUpdateFactory.newLatLngZoom(convertLatLng, 17.0f);
            mBaiduMap.animateMapStatus(myselfU);

        }

    }


    private void showRightWithText(String str,
                                   View.OnClickListener clickListener) {
        TextView rightText = (TextView) findViewById(R.id.right_title_text);
        rightText.setVisibility(View.VISIBLE);
        rightText.setText(str);

        //设置点击区域
        LinearLayout rightClickRange = (LinearLayout) findViewById(R.id.right_title_layout);
        rightClickRange.setOnClickListener(clickListener);
    }

    protected void showLeftWithImage(int resId,
                                     View.OnClickListener clickListener) {
        ImageView leftImage = (ImageView) findViewById(R.id.left_title_image);
        leftImage.setVisibility(View.VISIBLE);
        leftImage.setImageResource(resId);

        //设置点击区域
        LinearLayout leftClickRange = (LinearLayout) findViewById(R.id.left_title_layout);
        leftClickRange.setOnClickListener(clickListener);
    }

    private void setTitle() {
        TextView title = (TextView) findViewById(R.id.center_title);
        title.setText("位置信息");

        showRightWithText("发送", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = BaiduMapActivity.this.getIntent();
                intent.putExtra(LATITUDE, lastInfo.location.latitude);
                intent.putExtra(LONGITUDE, lastInfo.location.longitude);
                intent.putExtra(ADDRESS, lastInfo.address);
                intent.putExtra(NAME, lastInfo.name);
                BaiduMapActivity.this.setResult(RESULT_OK, intent);
                finish();
            }
        });
        showLeftWithImage(R.drawable.btn_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    /**
     * 点击相应的位置，移动到该位置
     */
    private class MyItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (preCheckedPosition != position) {
                adatper.setSelection(position);
                View view1 = listView.getChildAt(preCheckedPosition - listView.getFirstVisiblePosition());
                ImageView checked = null;
                if (view1 != null) {
                    checked = (ImageView) view1.findViewById(R.id.adapter_baidumap_location_checked);
                    checked.setVisibility(View.GONE);
                }
                preCheckedPosition = position;
                changeState = false;
                PoiInfo info = datas.get(position);
                LatLng llA = info.location;
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(llA, 17.0f);
                mBaiduMap.animateMapStatus(u);
                lastInfo = info;
                checked = (ImageView) view.findViewById(R.id.adapter_baidumap_location_checked);
                checked.setVisibility(View.VISIBLE);
            }

        }
    }


    private class MyOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (currentLL != originalLL) {
                changeState = true;
                mBaiduMap.animateMapStatus(myselfU);
            }
        }
    }

}
