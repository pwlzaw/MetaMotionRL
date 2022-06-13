package edu.ib.metamotionrl

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.util.ArrayList

class StoredData: AppCompatActivity() {
    lateinit var database: SQLiteDatabase
    private var adapter: DatabaseAdapter? = null
    private var results = ArrayList<String>()
    private var filteredList = ArrayList <String>()


    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.database)

        database = this.openOrCreateDatabase("Database", Context.MODE_PRIVATE, null)
        val sqlDB = "CREATE TABLE IF NOT EXISTS StoredData (name String, indx float, valueX float, valueY float, valueZ float)"
        database.execSQL(sqlDB)

        results = ArrayList<String>()
        val c = database.rawQuery(
                "SELECT DISTINCT name FROM UsersPlans",
                null
        )

        if (c.moveToFirst()) {
            do {
                val ind = c.getString(c.getColumnIndex("name"))
                results.add(ind)
            } while (c.moveToNext())
        }

        filteredList.addAll(results)
        adapter = DatabaseAdapter(
                this,
                R.layout.database_row, filteredList
        )

        val listView =
                findViewById<View>(R.id.list) as ListView
        listView.adapter = adapter
        c.close()
    }
}