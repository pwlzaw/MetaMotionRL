package edu.ib.metamotionrl

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
        val sqlDB = "CREATE TABLE IF NOT EXISTS StoredData (name String NOT NULL PRIMARY KEY, time String, valueX String, valueY String, valueZ String)"
        database.execSQL(sqlDB)

        results = ArrayList()
        val c = database.rawQuery(
                "SELECT name FROM StoredData",
                null
        )

        if (c.moveToFirst()) {
            do {
                val name = c.getString(c.getColumnIndex("name"))
                results.add(name)
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

    fun onBack(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        this.startActivity(intent)
    }
}