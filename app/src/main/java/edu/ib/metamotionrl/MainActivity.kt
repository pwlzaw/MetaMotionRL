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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import bolts.Continuation
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.Route
import com.mbientlab.metawear.android.BtleService
import com.mbientlab.metawear.android.BtleService.LocalBinder
import com.mbientlab.metawear.builder.RouteBuilder
import com.mbientlab.metawear.data.Acceleration
import com.mbientlab.metawear.module.Accelerometer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), ServiceConnection {
    private var serviceBinder: BtleService.LocalBinder? = null
    lateinit var accelerometer: Accelerometer
    lateinit var deviceData: TextView
    lateinit var chart: LineChart
    var dataD = "{x: , y:, z: }"
    var dataX = ArrayList<Float>()
    var dataY = ArrayList<Float>()
    var dataZ = ArrayList<Float>()
    var dataT = ArrayList<Date>()
    val dataXScal = 0.021.toFloat()
    val dataYScal = 0.001.toFloat()
    val dataZScal = 0.979.toFloat()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        deviceData = findViewById<View>(R.id.data) as TextView
        chart = findViewById(R.id.activity_main_linechart);
        configureLineChart()

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

        try {
        // mac addr z czujnika
        retriveBoard("F6:3A:8F:2E:A7:79")
        }catch (e: Exception){
            Log.e("Device", "Failed to update chart")
        }
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
                .odr(10f) // sampling frequency 10Hz
                .commit()

            accelerometer.acceleration().addRouteAsync(RouteBuilder { source ->
                source.stream { data, env ->
                    //dataD = data.value(Acceleration::class.java).toString()
                    dataX.add(data.value(Acceleration::class.java).x() + dataXScal)
                    dataY.add(data.value(Acceleration::class.java).y() + dataYScal)
                    dataZ.add(data.value(Acceleration::class.java).z() + dataZScal)
                    dataD = "x: ${(dataX[dataX.size - 1] * 1000).roundToInt().toFloat() / 1000}, y: ${(dataY[dataY.size - 1] * 1000).roundToInt().toFloat() / 1000}, z: ${(dataZ[dataZ.size - 1] * 1000).roundToInt().toFloat() / 1000}"

                    dataT.add(Calendar.getInstance().time)
                    Log.i("Accelerometer", dataD)
                    setLineChartData()

                    runOnUiThread {
                        deviceData.text = dataD
                        //setLineChartData()
                    }

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

    private fun configureLineChart() {
        chart.xAxis.labelCount = 5
        chart.xAxis.mAxisMaximum = 100F
        chart.xAxis.mAxisMinimum = 0F
    }

    // update chart
    private fun setLineChartData(){
        try {
            val lineEntryX = ArrayList<Entry>()
            val lineEntryY = ArrayList<Entry>()
            val lineEntryZ = ArrayList<Entry>()

            dataX.forEachIndexed(){index, item ->
                lineEntryX.add(Entry(index.toFloat(), item))
            }
            dataY.forEachIndexed(){index, item ->
                lineEntryY.add(Entry(index.toFloat(), item))
            }
            dataZ.forEachIndexed(){index, item ->
                lineEntryZ.add(Entry(index.toFloat(), item))
            }

        // chart data format
            val lineDataSetX = LineDataSet(lineEntryX, "X")
            lineDataSetX.color = resources.getColor(R.color.red)
            lineDataSetX.setDrawCircles(false)
            val lineDataSet = LineDataSet(lineEntryY, "Y")
            lineDataSet.color = resources.getColor(R.color.green)
            lineDataSet.setDrawCircles(false)
            val lineDataSetZ = LineDataSet(lineEntryZ, "Z")
            lineDataSetZ.color = resources.getColor(R.color.blue)
            lineDataSetZ.setDrawCircles(false)

            val dataset = ArrayList<LineDataSet>()
            dataset.add(lineDataSetX)
            dataset.add(lineDataSet)
            dataset.add(lineDataSetZ)

            val data = LineData(dataset as List<LineDataSet>?)

            chart.data = data
            runOnUiThread {
                chart.animateXY(1, 1)
                chart.setVisibleXRangeMaximum(100F)
                chart.moveViewToX(dataX.size.toFloat())
            }
        }catch (e: Exception){
            Log.e("Device", "Failed to update chart")
        }
    }
}