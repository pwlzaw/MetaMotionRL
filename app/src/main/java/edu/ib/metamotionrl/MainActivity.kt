package edu.ib.metamotionrl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import bolts.Continuation
import bolts.Task
import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.Route
import com.mbientlab.metawear.android.BtleService
import com.mbientlab.metawear.android.BtleService.LocalBinder
import com.mbientlab.metawear.builder.RouteBuilder
import com.mbientlab.metawear.data.Acceleration
import com.mbientlab.metawear.module.Accelerometer


class MainActivity : AppCompatActivity(), ServiceConnection {
    private var serviceBinder: BtleService.LocalBinder? = null
    lateinit var accelerometer: Accelerometer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ///< Bind the service when the activity is created
        applicationContext.bindService(
            Intent(this, BtleService::class.java),
            this, BIND_AUTO_CREATE
        )

        findViewById<View>(R.id.start).setOnClickListener {
            accelerometer.start()
            accelerometer.acceleration().start()
        }

        findViewById<View>(R.id.stop).setOnClickListener {
            accelerometer.stop()
            accelerometer.acceleration().stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ///< Unbind the service when the activity is destroyed
        applicationContext.unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        ///< Typecast the binder to the service's LocalBinder class
        serviceBinder = service as LocalBinder

        // mac addr z czujnika
        retriveBoard("F6:3A:8F:2E:A7:79")
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {}

    // połączenie z czujnikiem
    private fun retriveBoard(macAddr: String) {
        var board: MetaWearBoard? = null
        val btManager : BluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val remoteDevice: BluetoothDevice = btManager.adapter.getRemoteDevice(macAddr)

        board = serviceBinder?.getMetaWearBoard(remoteDevice)
        board?.connectAsync()?.onSuccessTask {
            Log.i("Device", "Connected to MetaMotionRL $macAddr")

            accelerometer = board.getModule(Accelerometer::class.java)
            accelerometer.configure()
                .odr(25f) // sampling frequency 25Hz
                .commit()

            accelerometer.acceleration().addRouteAsync(RouteBuilder { source ->
                source.stream { data, env ->
                    Log.i("Accelerometer", data.value(Acceleration::class.java).toString())
                }
            })
        }?.continueWith(Continuation<Route?, Void?> { task ->
            if (task.isFaulted) {
                Log.w("Device", "Failed to configure app", task.error)
            } else {
                Log.i("Device", "App configured")
            }
            null
        })
    }
}