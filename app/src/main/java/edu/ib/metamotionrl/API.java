package edu.ib.metamotionrl;

import android.app.AppOpsManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;

import bolts.Continuation;
import bolts.Task;

public class API {
    MetaWearBoard board;
    Accelerometer accelerometer;
    BtleService.LocalBinder serviceBinder;
    Context mContext;

    void retriveBoard(String macAddr, Context mContext, BtleService.LocalBinder serviceBinder){
        this.mContext = mContext;
        this.serviceBinder = serviceBinder;
    final BluetoothManager btManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr);


        board = serviceBinder.getMetaWearBoard(remoteDevice);
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
        @Override
        public Task<Route> then(Task<Void> task) throws Exception {
            Log.i("Device", "Connected to MetaMotionRL " + macAddr);


            accelerometer = board.getModule(Accelerometer.class);
            accelerometer.configure()
                    .odr(25f) // sampling frequency 25Hz
                    .commit();

            return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            Log.i("Accelerometer", data.value(Acceleration.class).toString());
                        }
                    });
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            if (task.isFaulted()) {
                Log.w("Device", "Failed to configure app", task.getError());
            }else{
                Log.i("Device", "App configured");
            }

            return null;
        }
    });
    }
}
