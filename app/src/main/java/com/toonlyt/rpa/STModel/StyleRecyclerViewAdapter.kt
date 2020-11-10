package com.toonlyt.rpa.STModel

import android.content.Context
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.toonlyt.rpa.STModel.StyleFragment.OnListFragmentInteractionListener
import org.tensorflow.lite.examples.rpa.R

/**
 * [StyleRecyclerViewAdapter] that can display a [StyleItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class StyleRecyclerViewAdapter(
  private val styles: List<String>,
  private val context: Context,
  private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<StyleRecyclerViewAdapter.StyleItemViewHolder>() {

  private val mOnClickListener: View.OnClickListener

  init {
    mOnClickListener = View.OnClickListener { v ->
      val item = v.tag as String
      // Notify the active callbacks interface (the activity, if the fragment is attached to
      // one) that an item has been selected.
      mListener?.onListFragmentInteraction(item)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleItemViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.image_item, parent, false)
    return StyleItemViewHolder(view)
  }

  override fun onBindViewHolder(holder: StyleItemViewHolder, position: Int) {
    val imagePath = styles[position]

    Glide.with(context)
      .load(Uri.parse("file:///android_asset/thumbnails/$imagePath"))
      .centerInside()
      .into(holder.imageView)


    with(holder.mView) {
      tag = imagePath
      setOnClickListener(mOnClickListener)
    }
  }

  override fun getItemCount(): Int = styles.size

  inner class StyleItemViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
    var imageView: ImageView = mView.findViewById(R.id.image_view)
  }
}
