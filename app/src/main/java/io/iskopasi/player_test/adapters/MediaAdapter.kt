package io.iskopasi.player_test.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.MediaListItemBinding
import io.iskopasi.player_test.models.MediaData


class MediaAdapter(
    private val initialActiveMap: Map<Int, Boolean>,
    val onClick: (Int, Int) -> Unit,
    val onLongPress: (MotionEvent, Int, Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    private var lastEvent: MotionEvent? = null
    var data: List<MediaData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val activeMap = mutableMapOf<Int, Boolean>().apply {
        putAll(initialActiveMap)
    }

    fun addActive(index: Int) {
        activeMap[index] = true
        notifyItemChanged(index)
    }

    fun removeActive(index: Int) {
        activeMap.remove(index)
        notifyItemChanged(index)
    }

    class ViewHolder(val binding: MediaListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MediaListItemBinding.inflate(inflater, parent, false)

        binding.root.setOnTouchListener { v, event ->
            lastEvent = event
            false
        }

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context
        val isActive = activeMap.getOrDefault(position, false)

        holder.binding.apply {
            val bgColor = if (isActive) R.color.white else R.color.trans
            val textColor = if (isActive) R.color.black else R.color.white
            val textColorC = ContextCompat.getColor(context, textColor)
            val bgColorC = ContextCompat.getColor(context, bgColor)

            name.text = item.name
            name.setTextColor(textColorC)

            subtitle.text = item.subtitle
            subtitle.setTextColor(textColorC)

            duration.text = DateUtils.formatElapsedTime(item.duration.toLong() / 1000L)
            duration.setTextColor(textColorC)

            if (isActive)
                root.setBackgroundColor(bgColorC)
            else
                root.setBackgroundResource(R.drawable.ripple_selector_2)

            root.setOnClickListener {
                onClick(position, item.id)
            }
            root.setOnLongClickListener {
                lastEvent?.let {
                    onLongPress(it, position, item.id)
                }

                true
            }
        }
    }
}

class SingleActiveMediaAdapter(
    activeIndex: Int,
    val onClick: (Int, Int) -> Unit,
    val onLongPress: (MotionEvent, Int, Int) -> Unit
) : RecyclerView.Adapter<SingleActiveMediaAdapter.ViewHolder>() {
    private var lastEvent: MotionEvent? = null
    var data: List<MediaData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var active: Int = activeIndex
        set(value) {
            val oldValue = active
            field = value

            notifyItemChanged(value)
            notifyItemChanged(oldValue)
        }

    class ViewHolder(val binding: MediaListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MediaListItemBinding.inflate(inflater, parent, false)

        binding.root.setOnTouchListener { v, event ->
            lastEvent = event
            false
        }

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context
        val isActive = position == active

        holder.binding.apply {
            val bgColor = if (isActive) R.color.white else R.color.trans
            val textColor = if (isActive) R.color.black else R.color.white
            val textColorC = ContextCompat.getColor(context, textColor)
            val bgColorC = ContextCompat.getColor(context, bgColor)

            name.text = item.name
            name.setTextColor(textColorC)

            subtitle.text = item.subtitle
            subtitle.setTextColor(textColorC)

            duration.text = DateUtils.formatElapsedTime(item.duration.toLong() / 1000L)
            duration.setTextColor(textColorC)

            if (isActive)
                root.setBackgroundColor(bgColorC)
            else
                root.setBackgroundResource(R.drawable.ripple_selector_2)

            root.setOnClickListener {
                onClick(position, item.id)
            }
            root.setOnLongClickListener {
                lastEvent?.let {
                    onLongPress(it, position, item.id)
                }

                true
            }
        }
    }
}