package edu.ib.metamotionrl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi

class DatabaseAdapter(
    var cont: Context,
    var layoutResourceId: Int,
    data: ArrayList<String>
): ArrayAdapter<String>(cont, layoutResourceId, data) {
    var data: java.util.ArrayList<String>? = null
    var dbHelper = DBHelper(getContext())
    var database = dbHelper.writableDatabase
    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
    ): View {
        var row = convertView
        var holder: RowBeanHolder? = null
        if (row == null) {
            val inflater = (cont as Activity).layoutInflater
            row = inflater.inflate(layoutResourceId, parent, false)
            holder = RowBeanHolder()
            holder.name = row.findViewById<View>(R.id.name) as TextView
        } else {
            holder = row.tag as RowBeanHolder
        }

        holder.name!!.text = data!![position]

        // buttons
        val show = row!!.findViewById<View>(R.id.show) as Button
        val delete = row!!.findViewById<View>(R.id.delete) as Button

        show.setOnClickListener {
            val intent = Intent(cont, ShowData::class.java)
            intent.putExtra("name", holder.name?.text)
            cont.startActivity(intent)
        }

        delete.setOnClickListener {
            database.execSQL("CREATE TABLE IF NOT EXISTS StoredData (name String NOT NULL PRIMARY KEY, time String, valueX String, valueY String, valueZ String)")
            val name = holder.name?.text
            val sqlStudent = "delete from StoredData where name = '$name'"
            val statement = database.compileStatement(sqlStudent)
            statement.executeInsert()
            val intent = Intent(cont, StoredData::class.java)
            cont.startActivity(intent)
        }

        return row
    }

    internal class RowBeanHolder {
        var name: TextView? = null
    }

    init {
        this.data = data
    }
}