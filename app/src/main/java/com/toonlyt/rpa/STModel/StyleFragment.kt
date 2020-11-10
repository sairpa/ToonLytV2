package com.toonlyt.rpa.STModel

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tensorflow.lite.examples.rpa.R


class StyleFragment : DialogFragment() {

  private var listener: OnListFragmentInteractionListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_style_list, container, false)

    val styles = ArrayList<String>()
    styles.addAll(activity!!.assets.list("thumbnails")!!)

    // Set the adapter
    if (view is RecyclerView) {
      with(view) {
        layoutManager = GridLayoutManager(context, 2)
        adapter = StyleRecyclerViewAdapter(styles, context, listener)
      }
    }
    return view
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is OnListFragmentInteractionListener) {
      listener = context
    } else {
      throw RuntimeException("$context must implement OnListFragmentInteractionListener")
    }
  }

  override fun onDetach() {
    super.onDetach()
    listener = null
  }


  interface OnListFragmentInteractionListener {
    fun onListFragmentInteraction(item: String)
  }
}
