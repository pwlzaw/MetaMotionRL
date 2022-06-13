package edu.ib.metamotionrl

import android.content.Context
import android.widget.ArrayAdapter

class DatabaseAdapter(
    var cont: Context,
    var layoutResourceId: Int,
    data: ArrayList<String>
): ArrayAdapter<String>(cont, layoutResourceId, data) {
    var data: java.util.ArrayList<String>? = null
    var dbHelper = DBHelper(getContext())
    var database = dbHelper.writableDatabase
}