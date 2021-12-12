package com.anand.memeszone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MemeAdapter(private val listener: MemeShare, private val downloadListener: MemeDownload ): RecyclerView.Adapter<MemeAdapter.MemeViewHolder>() {
    private val items: ArrayList<Meme> = ArrayList()
    class MemeViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val meme: ImageView = itemView.findViewById(R.id.ivMeme)
        val like: ImageView = itemView.findViewById(R.id.ivLike)
        val share: ImageView = itemView.findViewById(R.id.ivShare)
        val download: ImageView = itemView.findViewById(R.id.ivDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemeViewHolder {
        return MemeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_meme, parent, false))
    }

    override fun onBindViewHolder(holder: MemeViewHolder, position: Int) {
        val currentItems = items[position]
        Glide.with(holder.itemView.context).load(currentItems.url).into(holder.meme)
        holder.like.setImageResource(R.drawable.ic_unlike)
        holder.meme.setOnClickListener(object : DoubleClickListener(){
            override fun onDoubleClick(v: View?) {
                holder.like.setImageResource(R.drawable.ic_like)
            }
        })

        holder.like.setOnClickListener{
            holder.like.setImageResource(R.drawable.ic_unlike)
        }

        holder.share.setOnClickListener{
            listener.MemeShareClicked(currentItems.url)
        }

        holder.download.setOnClickListener{
            downloadListener.MemeDownloadClicked(currentItems.url)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateMemes(updateMemes: ArrayList<Meme>) {
        items.clear()
        items.addAll(updateMemes)

        notifyDataSetChanged()
    }
}

interface MemeShare {
    fun MemeShareClicked(MemeUrl: String)
}

interface MemeDownload {
    fun MemeDownloadClicked(MemeUrl: String)
}

abstract class DoubleClickListener : View.OnClickListener{
    private var lastCLickTime: Long = 0
    override fun onClick(v: View?) {
        val clickTime = System.currentTimeMillis()
        if(clickTime - lastCLickTime < DOUBLE_CLICK_TIME_DELTA ){
            onDoubleClick(v)
        }
        lastCLickTime = clickTime
    }

    abstract fun onDoubleClick(v: View?)

    companion object {
        private const val DOUBLE_CLICK_TIME_DELTA: Long = 300
    }
}
