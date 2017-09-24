package io.almayce.dev.fancysticker

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView


/**
 * Created by almayce on 30.05.17.
 */

class CustomGridViewAdapter(context: Context, var list: ArrayList<Bitmap>) : BaseAdapter() {

    override fun getCount(): Int = list.size
    override fun getItem(position: Int): Any = list.get(position)
    override fun getItemId(position: Int): Long = position.toLong()

    var inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view =  inflater.inflate(R.layout.item, parent, false)
        view.findViewById<ImageView>(R.id.ivSticker).setImageBitmap(list.get(position))
        return view
    }
}
