package com.aslam.carapiapp;

import android.Manifest;
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {

    private Car mCarApi;
    private CarPropertyManager mCarPropertyManager;

    private static final int KS_PERMISSIONS_REQUEST = 1;

    private final static String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Car.PERMISSION_ENERGY,
            Car.PERMISSION_SPEED,
    };

    private List<CarPropertyConfig> mPropertyList;
    private static String TAG ="MainActivity";
    private Set<String> mActivePermissions = new HashSet<String>();

    private final Map<Integer, CarPropertyValue> mValueMap = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermissions();

    }


    private void initCar() {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            return;
        }

        if (mCarApi != null && mCarApi.isConnected()) {
            mCarApi.disconnect();
            mCarApi = null;
        }

        mCarApi = Car.createCar(this, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                (Car car, boolean ready) -> {
                    if (ready) {
                        //initManagers(car);
                        Log.d("Aslam", "Car connected");

                    }
                });
    }




    @Override
    protected void onResume() {
        super.onResume();
        /*final Runnable r = this::initPermissions;

        requestRefreshManager(r,new Handler(getMainLooper()));*/
        initCar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCarApi.isConnected()) {
            mCarApi.disconnect();
        }
    }

    // Use AsyncTask to refresh Car*Manager after car service connected
    public void requestRefreshManager(final Runnable r, final Handler h) {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {

                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                h.post(r);
            }
        };
        task.execute();
    }

    private void initPermissions() {
        Set<String> missingPermissions = checkExistingPermissions();
        if (!missingPermissions.isEmpty()) {
            requestPermissions(missingPermissions);
            // The callback with premission results will take care of calling initSensors for us
        } else {
            initSensors();
        }
    }


    private Set<String> checkExistingPermissions() {
        Set<String> missingPermissions = new HashSet<String>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED) {
                mActivePermissions.add(permission);
            } else {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }

    private void requestPermissions(Set<String> permissions) {
        Log.d(TAG, "requesting additional permissions=" + permissions);

        requestPermissions(permissions.toArray(new String[permissions.size()]),
                KS_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult reqCode=" + requestCode);
        if (KS_PERMISSIONS_REQUEST == requestCode) {

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mActivePermissions.add(permissions[i]);
                }
            }
            initSensors();
        }
    }


    private void initSensors() {
        try {
            if (mCarPropertyManager == null) {
                mCarPropertyManager = (CarPropertyManager) mCarApi.getCarManager(
                        android.car.Car.PROPERTY_SERVICE);
            }
            ArraySet<Integer> set = new ArraySet<>();
            set.add(VehiclePropertyIds.PERF_VEHICLE_SPEED);
            /*set.add(VehiclePropertyIds.ENGINE_RPM);
            set.add(VehiclePropertyIds.PERF_ODOMETER);*/
            set.add(VehiclePropertyIds.FUEL_LEVEL);
           /* set.add(VehiclePropertyIds.FUEL_DOOR_OPEN);
            set.add(VehiclePropertyIds.IGNITION_STATE);
            set.add(VehiclePropertyIds.PARKING_BRAKE_ON);
            set.add(VehiclePropertyIds.GEAR_SELECTION);
            set.add(VehiclePropertyIds.NIGHT_MODE);
            set.add(VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE);
            set.add(VehiclePropertyIds.WHEEL_TICK);
            set.add(VehiclePropertyIds.ABS_ACTIVE);
            set.add(VehiclePropertyIds.TRACTION_CONTROL_ACTIVE);
            set.add(VehiclePropertyIds.EV_BATTERY_LEVEL);
            set.add(VehiclePropertyIds.EV_CHARGE_PORT_OPEN);
            set.add(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED);
            set.add(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE);*/
            set.add(VehiclePropertyIds.ENGINE_OIL_LEVEL);

            mPropertyList = mCarPropertyManager.getPropertyList(set);

            for (CarPropertyConfig property : mPropertyList) {
                float rate = CarPropertyManager.SENSOR_RATE_NORMAL;
                if (property.getChangeMode()
                        == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_ONCHANGE) {
                    rate = CarPropertyManager.SENSOR_RATE_ONCHANGE;
                }
                mCarPropertyManager.registerCallback(mCarPropertyEventCallback,
                        property.getPropertyId(), rate);
            }
        } catch (Exception e) {
            Log.e(TAG, "initSensors() exception caught SensorManager: ", e);
        }
    }



    private final CarPropertyManager.CarPropertyEventCallback mCarPropertyEventCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {

                        Log.v(TAG, "New car property value: " + value);
                    if (value.getStatus() == CarPropertyValue.STATUS_AVAILABLE) {
                        mValueMap.put(value.getPropertyId(), value);
                    } else {
                        mValueMap.put(value.getPropertyId(), null);
                    }
                   // refreshSensorInfoText();
                }
                @Override
                public void onErrorEvent(int propId, int zone) {
                    Log.e(TAG, "propId: " + propId + " zone: " + zone);
                }
            };


}