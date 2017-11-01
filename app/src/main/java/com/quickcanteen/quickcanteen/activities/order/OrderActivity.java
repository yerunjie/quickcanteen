package com.quickcanteen.quickcanteen.activities.order;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import com.quickcanteen.quickcanteen.R;
import com.quickcanteen.quickcanteen.actions.location.ILocationAction;
import com.quickcanteen.quickcanteen.actions.location.impl.LocationActionImpl;
import com.quickcanteen.quickcanteen.actions.orders.IOrderAction;
import com.quickcanteen.quickcanteen.actions.orders.impl.OrderActionImpl;
import com.quickcanteen.quickcanteen.activities.BaseActivity;
import com.quickcanteen.quickcanteen.activities.LocationsActivity;
import com.quickcanteen.quickcanteen.activities.canteen.GoodsItem;
import com.quickcanteen.quickcanteen.bean.LocationBean;
import com.quickcanteen.quickcanteen.bean.OrderBean;
import com.quickcanteen.quickcanteen.utils.BaseJson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class OrderActivity extends BaseActivity {

    private Date orderTime;
    private String orderTimeContent;
    private NumberFormat numberFormat;
    private int totalQuantity = 0;
    private double totalPrice = 0;
    private ListView dishesView;
    private TextView totalQuantityTextView, totalPriceTextView, companyNameTextView;
    private static Handler handler = new Handler();
    private int companyID;
    private String message, companyName;
    private ArrayList<GoodsItem> list;
    private int ordersID;
    private String selected = "取餐";
    private RadioGroup typeGroup;
    private RadioButton checkedType;
    private TextView timeSlotTitle;
    private RadioGroup timeSlotGroup;
    private TextView deliverTitle;
    private RadioGroup locationGroup;
    private String[] timeSlot = {"11:00-11:30", "11:30-11:45", "11:45-12:00", "12:00-12:30", "12:30-13:00", "16:30~17:00", "17:00~18:00", "18:00~19:00"};
    private IOrderAction orderAction = new OrderActionImpl(this);
    private ILocationAction locationAction = new LocationActionImpl(this);
    private OrderBean orderBean;
    private boolean isLegal;
    private String choosetimeslot;
    private List<LocationBean> locationBeans = new ArrayList<>();
    private LocationBean locationBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        BaseActivity.initializeTop(this, true, "确认订单");

        numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setMaximumFractionDigits(2);
        dishesView = (ListView) findViewById(R.id.dishesView);
        totalQuantityTextView = (TextView) findViewById(R.id.totalQuantity);
        totalPriceTextView = (TextView) findViewById(R.id.totalPrice);
        companyNameTextView = (TextView) findViewById(R.id.companyName);
        isLegal = false;
        choosetimeslot = "";
        /*choose take meal type*/
        typeGroup = (RadioGroup) findViewById(R.id.typeGroup);
        timeSlotTitle = (TextView) findViewById(R.id.timeSlotTitle);
        timeSlotGroup = (RadioGroup) findViewById(R.id.TimeSlotGroup);
        locationGroup = (RadioGroup) findViewById(R.id.locationGroup);
        int timeSlotCount = timeSlot.length;
        for (int i = 0; i < timeSlotCount; i++) {
            RadioButton timeSlotEach = new RadioButton(this);
            timeSlotEach.setId(i + 1);
            timeSlotEach.setText(timeSlot[i]);
            timeSlotGroup.addView(timeSlotEach);
        }

        typeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //点击事件获取的选择对象
                checkedType = (RadioButton) typeGroup.findViewById(checkedId);
                switch (checkedId) {
                    case R.id.invite:
                        isLegal = false;
                        selected = "取餐";
                        timeSlotTitle.setText("选择到窗取餐的时间：");
                        timeSlotTitle.setVisibility(View.VISIBLE);
                        timeSlotGroup.setVisibility(View.VISIBLE);
                        locationGroup.setVisibility(View.GONE);
                        break;
                    case R.id.distribution:
                        isLegal = true;
                        selected = "配送";
                        timeSlotTitle.setText("大约会在20分钟后送达");
                        timeSlotTitle.setVisibility(View.VISIBLE);
                        timeSlotGroup.setVisibility(View.GONE);
                        locationGroup.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });

        timeSlotGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //点击事件获取的选择对象
                isLegal = true;
                choosetimeslot = String.valueOf(checkedId + 1);
                orderBean.setTimeSlot(String.valueOf(checkedId + 1));
            }
        });

        locationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //点击事件获取的选择对象
                isLegal = true;
                locationBean = locationBeans.get(checkedId - 1);
            }
        });
        new Thread(new LocationThread()).start();

        Bundle bundle = getIntent().getExtras();
        orderBean = (OrderBean) bundle.getSerializable("orderBean");
        companyID = orderBean.getCompanyId();
        ordersID = orderBean.getOrderId();
        companyName = orderBean.getCompanyName();
        list = GoodsItem.getGoodsItemList(orderBean.getDishesBeanList());

        companyNameTextView.setText(companyName);

        ArrayList<HashMap<String, Object>> arrayList = getData(list);

        SimpleAdapter adapter = new SimpleAdapter(this, arrayList, R.layout.dishes_view_content,
                new String[]{"name", "quantity", "price"},
                new int[]{R.id.dishName, R.id.dishQuantity, R.id.dishPrice});
        dishesView.setAdapter(adapter);

        totalQuantityTextView.setText(String.valueOf(totalQuantity));
        totalPriceTextView.setText(numberFormat.format(totalPrice));


        Button button_to_pay = (Button) findViewById(R.id.payButton);
        button_to_pay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new PayThread()).start();
            }
        });
    }


    class PayThread implements Runnable {
        @Override
        public void run() {
            message = "支付成功";
            if (isLegal) {
                try {
                    BaseJson baseJson = orderAction.pay(ordersID, selected);
                    int result = baseJson.getSingleIntegerResult();
                    switch (result) {
                        case 0:
                            Intent intent = new Intent();
                            if (selected.equals("取餐")) {
                                BaseJson baseJson1 = orderAction.updateTimeSlot(ordersID, choosetimeslot);
                                int result2 = baseJson.getSingleIntegerResult();
                            } else {
                                orderAction.payWithDeliverAddress(ordersID, locationBean);
                            }
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("orderBean", orderBean);
                            intent.putExtras(bundle);
                            intent.setClass(OrderActivity.this, SuccessActivity.class);
                            startActivity(intent);
                            finish();
                            break;
                        default:
                            message = baseJson.getErrorMessage();
                            break;
                    }
                } catch (Exception e) {
                    message = "连接错误";
                }
            } else {
                message = "请完善取餐信息";
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(OrderActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public class LocationThread implements Runnable {
        @Override
        public void run() {
            try {
                final BaseJson baseJson = locationAction.getCurrentUserLocations();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            locationBeans = new ArrayList<>();
                            JSONArray jsonArray = baseJson.getJSONArray();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject tempJsonObject = jsonArray.getJSONObject(i);
                                LocationBean locationBean = new LocationBean(tempJsonObject);
                                locationBeans.add(locationBean);
                                RadioButton locationEach = new RadioButton(OrderActivity.this);
                                locationEach.setId(i + 1);
                                locationEach.setText(locationBean.getAddress());
                                locationGroup.addView(locationEach);
                            }
                        } catch (Exception e) {

                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(OrderActivity.this, "连接错误", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
        }
    }

    private ArrayList<HashMap<String, Object>> getData(ArrayList<GoodsItem> list) {
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();

        for (GoodsItem temp : list) {
            arrayList.add(getElement(temp));
        }

        return arrayList;
    }

    private HashMap<String, Object> getElement(GoodsItem item) {
        HashMap<String, Object> tempHashMap = new HashMap<String, Object>();
        tempHashMap.put("name", item.name);
        tempHashMap.put("quantity", item.count);
        totalQuantity += item.count;
        tempHashMap.put("price", numberFormat.format(item.price));
        totalPrice += item.price * item.count;
        return tempHashMap;
    }

}
