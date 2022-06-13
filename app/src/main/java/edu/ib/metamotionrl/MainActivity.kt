package edu.ib.metamotionrl

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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


class MainActivity : AppCompatActivity(), ServiceConnection {
    private var serviceBinder: BtleService.LocalBinder? = null
    lateinit var accelerometer: Accelerometer
    lateinit var deviceData: TextView
    lateinit var chart: LineChart
    var dataD = "{x: , y:, z: }"
    var dataX = ArrayList<Float>()
    var dataY = ArrayList<Float>()
    var dataZ = ArrayList<Float>()
    var dataT = ArrayList<Float>()
    val f = 10F
    private var dataXScal = 0F
    private var dataYScal = 0F
    private var dataZScal = 0F
    private val lineEntryX = ArrayList<Entry>()
    private val lineEntryY = ArrayList<Entry>()
    private val lineEntryZ = ArrayList<Entry>()


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

        findViewById<View>(R.id.calibration).setOnClickListener {
            if (dataX.size > 0) {
                dataXScal += dataX[dataX.size - 1]
                dataYScal += dataY[dataY.size - 1]
                dataZScal += dataZ[dataZ.size - 1]

                dataX.clear()
                dataY.clear()
                dataZ.clear()
                dataT.clear()
                lineEntryX.clear()
                lineEntryY.clear()
                lineEntryZ.clear()
            }
        }

        findViewById<View>(R.id.stop).setOnClickListener {
            accelerometer.stop()
            accelerometer.acceleration().stop()
        }

        findViewById<View>(R.id.save).setOnClickListener {
            // inflate the layout of the popup window
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(R.layout.savedata, null)

            // create the popup window
            val width = LinearLayout.LayoutParams.WRAP_CONTENT
            val height = LinearLayout.LayoutParams.WRAP_CONTENT
            val focusable = true

            val popupWindow = PopupWindow(popupView, width, height, focusable)

            // show the popup window
            popupWindow.showAtLocation(it, Gravity.CENTER, 0, 0);

            val saveButton= popupView.findViewById(R.id.saveButton) as Button
            saveButton.setOnClickListener(){
                val fileName = popupView.findViewById(R.id.filename) as EditText
                val name = fileName.text.toString()
                if (name.isNotEmpty()) {
                    saveData(name)
                    popupWindow.dismiss()
                }
                else{
                    Toast.makeText(this, "Wrong filename", Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<View>(R.id.database).setOnClickListener {
            val intent = Intent(this, StoredData::class.java)
            this.startActivity(intent)
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
        // device mac addr
        retriveBoard("F6:3A:8F:2E:A7:79")
        }catch (e: Exception){
            Log.e("Device", "Failed to update chart")
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {}

    // device connection
    private fun retriveBoard(macAddr: String) {
        var board: MetaWearBoard? = null
        val btManager : BluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val remoteDevice: BluetoothDevice = btManager.adapter.getRemoteDevice(macAddr)

        board = serviceBinder?.getMetaWearBoard(remoteDevice)
        board?.connectAsync()?.onSuccessTask {
            Log.i("Device", "Connected to MetaMotionRL $macAddr")
            runOnUiThread {
                Toast.makeText(this, "Connected to MetaMotionRL $macAddr", Toast.LENGTH_LONG).show()
            }

            accelerometer = board.getModule(Accelerometer::class.java)
            accelerometer.configure()
                .odr(f) // sampling frequency 10Hz
                .commit()

            accelerometer.acceleration().addRouteAsync(RouteBuilder { source ->
                source.stream { data, env ->
                    dataX.add(data.value(Acceleration::class.java).x() - dataXScal)
                    dataY.add(data.value(Acceleration::class.java).y() - dataYScal)
                    dataZ.add(data.value(Acceleration::class.java).z() - dataZScal)
                    dataT.add(dataX.size / f)
                    dataD = "x: ${String.format("%.3f", dataX[dataX.size - 1])}, y: ${String.format("%.3f", dataY[dataY.size - 1])}, z: ${String.format("%.3f", dataZ[dataZ.size - 1])}"

                    Log.i("Accelerometer", dataD)
                    //setLineChartData()

                    runOnUiThread {
                        deviceData.text = dataD
                        setLineChartData()
                    }
                }
            })
        }?.continueWith(Continuation<Route?, Void?> { task ->
            if (task.isFaulted) {
                Log.w("Device", "Failed to configure app", task.error)
                Toast.makeText(this, "Failed to configure app", Toast.LENGTH_LONG).show()

            } else {
                Log.i("Device", "App configured")
                Toast.makeText(this, "App configured", Toast.LENGTH_LONG).show()
            }
            null
        })
    }

    // configure chart properties
    private fun configureLineChart() {
        chart.xAxis.labelCount = 5
        chart.xAxis.mAxisMaximum = 100F
        chart.xAxis.mAxisMinimum = 0F
    }

    // update chart
    private fun setLineChartData(){
        try {
            // add new points
            lineEntryX.add(Entry((dataT[dataX.size - 1]), dataX[dataX.size - 1]))
            lineEntryY.add(Entry((dataT[dataY.size - 1]), dataY[dataY.size - 1]))
            lineEntryZ.add(Entry((dataT[dataZ.size - 1]), dataZ[dataZ.size - 1]))

            // chart data format
            val lineDataSetX = LineDataSet(lineEntryX, "X")
            lineDataSetX.color = resources.getColor(R.color.red)
            lineDataSetX.setDrawCircles(false)
            val lineDataSetY = LineDataSet(lineEntryY, "Y")
            lineDataSetY.color = resources.getColor(R.color.green)
            lineDataSetY.setDrawCircles(false)
            val lineDataSetZ = LineDataSet(lineEntryZ, "Z")
            lineDataSetZ.color = resources.getColor(R.color.blue)
            lineDataSetZ.setDrawCircles(false)

            // create dataset
            val dataSet = ArrayList<LineDataSet>()
            dataSet.add(lineDataSetX)
            dataSet.add(lineDataSetY)
            dataSet.add(lineDataSetZ)

            val data = LineData(dataSet as List<LineDataSet>?)

            chart.data = data
            // draw chart
            runOnUiThread {
                chart.animateXY(1, 1)
                chart.setVisibleXRangeMaximum(10F)
                chart.moveViewToX(dataX.size.toFloat())
            }
        }catch (e: Exception){
            Log.e("Device", "Failed to update chart")
        }
    }

    private fun saveData(name: String){
        val database = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null)
        val sqlDB = "CREATE TABLE IF NOT EXISTS StoredData (name String, time String, valueX String, valueY String, valueZ String)"
        database.execSQL(sqlDB)
        val sql = "INSERT INTO StoredData VALUES (?,?,?,?,?)"
        val statement = database.compileStatement(sql)

        statement.bindString(1, name)
        statement.bindString(2, dataT.toString())
        statement.bindString(3, dataX.toString())
        statement.bindString(4, dataY.toString())
        statement.bindString(5, dataZ.toString())
        statement.executeInsert()

        Toast.makeText(this, "Data saved", Toast.LENGTH_LONG).show()
    }
}