package edu.ib.metamotionrl

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.*

class ShowData : AppCompatActivity(){
    lateinit var chart: LineChart
    var dataX = ArrayList<Float>()
    var dataY = ArrayList<Float>()
    var dataZ = ArrayList<Float>()
    var dataT = ArrayList<Float>()

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.showdata)

        val i = intent
        val name = i.getStringExtra("name")
        val nameDisp = findViewById<TextView>(R.id.name)
        nameDisp.text = name
        val list = findViewById<ListView>(R.id.listV)
        chart = findViewById(R.id.linechart)
        chart.description.isEnabled = false

        val database = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null)
        val sqlDB = "CREATE TABLE IF NOT EXISTS StoredData (name String NOT NULL PRIMARY KEY, time String, valueX String, valueY String, valueZ String)"
        database.execSQL(sqlDB)

        var dataXRead = String()
        var dataYRead = String()
        var dataZRead = String()
        var dataTRead = String()

        val c = database.rawQuery(
                "SELECT time, valueX, valueY, valueZ FROM StoredData where name = '$name'",
                null
        )

        if (c.moveToFirst()) {
            do {
                dataTRead  = c.getString(c.getColumnIndex("time"))
                dataXRead = c.getString(c.getColumnIndex("valueX"))
                dataYRead = c.getString(c.getColumnIndex("valueY"))
                dataZRead = c.getString(c.getColumnIndex("valueZ"))
            } while (c.moveToNext())
        }
        c.close()

        // convert String to ArrayList
        dataX = makeArray(dataXRead)
        dataY = makeArray(dataYRead)
        dataZ = makeArray(dataZRead)
        dataT = makeArray(dataTRead)
        setLineChartData()

        // add data to listView
        val showData = ArrayList<String>()
        dataT.forEachIndexed { index, element ->
            showData.add("Time: $element, X: ${String.format("%.3f", dataX[index])}, Y: ${String.format("%.3f", dataY[index])}, Z: ${String.format("%.3f", dataZ[index])}")
        }
        val adapter = ArrayAdapter<String>(this, R.layout.showdata_row, showData)
        list.adapter = adapter

    }

    private fun setLineChartData(){

        val lineEntryX = ArrayList<Entry>()
        val lineEntryY = ArrayList<Entry>()
        val lineEntryZ = ArrayList<Entry>()

            // create chart data
            dataT.forEachIndexed { index, element ->
                lineEntryX.add(Entry((element), dataX[index]))
                lineEntryY.add(Entry((element), dataY[index]))
                lineEntryZ.add(Entry((element), dataZ[index]))
            }

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

            // display data on chart
            chart.data = data
            chart.animateXY(1, 1)
    }

    // back button
    fun onBack(view: View) {
        val intent = Intent(this, StoredData::class.java)
        this.startActivity(intent)
    }

    // convert String data to ArrayList
    private fun makeArray(st: String): ArrayList<Float> {
        val s = st.substring(1,st.length-1)
        val tokens = StringTokenizer(s, ",")
        var newArray = ArrayList<Float>()
        while(tokens.hasMoreTokens()){
            newArray.add(tokens.nextToken().trim().toFloat());
        }
        return newArray
    }
}