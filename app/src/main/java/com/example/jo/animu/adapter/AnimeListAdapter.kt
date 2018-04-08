package com.example.jo.animu.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jo.animu.R
import com.example.jo.animu.model.Anime

class AnimeListAdapter(var animeList: MutableList<Anime>, val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val loader = Anime(-1,null,null,null)

    init {
        animeList.add(loader)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (animeList[position].id?.equals(-1)!!) {
            return
        }

        val vholder = holder as ViewHolder
        vholder.txtTitle?.text = animeList[position].title
        vholder.txtScore?.text = animeList[position].score?.toString()

        animeList[position].coverImage?.let {
            vholder.imgCover?.let {

                val options = RequestOptions()
                options.centerCrop()

                Glide.with(context)
                        .load(animeList[position].coverImage)
                        .apply(options)
                        .into(it)
            }
        }
    }

    override fun getItemCount(): Int {
        return animeList.size
    }

    fun add (newAnimeList: MutableList<Anime>) {
        val currSize = animeList.size - 1
        animeList.removeAt(currSize)
        notifyItemRemoved(currSize)
        animeList.addAll(newAnimeList)
        animeList.add(loader)
        notifyItemRangeChanged(currSize,animeList.size+1)
    }

    fun changeDataSet (newAnimeList: MutableList<Anime>) {
        animeList = newAnimeList
        animeList.add(loader)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (animeList.size -1 == position) {
            return 2
        }
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)

        if (viewType == 2) {
            return ViewHolder(
                    inflater.inflate(
                            R.layout.loader, parent, false)
            )
        }

        return ViewHolder(
                inflater.inflate(
                        R.layout.anime_card, parent, false))
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imgCover = itemView.findViewById<ImageView>(R.id.imgCover)
        val txtTitle = itemView.findViewById<TextView>(R.id.txtTitle)
        val txtScore = itemView.findViewById<TextView>(R.id.txtScore)
    }

    class LoaderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

}