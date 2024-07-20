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
    val onClick: (Int) -> Unit,
    val onLongPress: (MotionEvent, Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    private var lastEvent: MotionEvent? = null
    var data: List<MediaData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var active: Int = -1
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

        holder.binding.apply {
            val bgColor = if (position == active) R.color.white else R.color.trans
            val textColor = if (position == active) R.color.black else R.color.white
            val textColorC = ContextCompat.getColor(context, textColor)
            val bgColorC = ContextCompat.getColor(context, bgColor)

            name.text = item.name
            name.setTextColor(textColorC)

            subtitle.text = item.subtitle
            subtitle.setTextColor(textColorC)

            duration.text = DateUtils.formatElapsedTime(item.duration.toLong() / 1000L)
            duration.setTextColor(textColorC)

            if (position == active)
                root.setBackgroundColor(bgColorC)
            else
                root.setBackgroundResource(R.drawable.ripple_selector_2)

            root.setOnClickListener {
                onClick(position)
            }
            root.setOnLongClickListener {
                lastEvent?.let {
                    onLongPress(it, position)
                }

                true
            }
        }
    }
}