package io.iskopasi.player_test.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.iskopasi.player_test.R
import io.iskopasi.player_test.databinding.RecommendItemBinding
import jp.wasabeef.glide.transformations.gpu.KuwaharaFilterTransformation


data class RecommendItemData(
    val text: String,
    val text2: String = "",
    val imageResource: Int
)

class CardsAdapter(
    val onClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<CardsAdapter.ViewHolder>() {
    private var lastPosition = -1
    var data: List<RecommendItemData> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: RecommendItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecommendItemBinding.inflate(inflater, parent, false)

        binding.root.setOnClickListener {
            Toast.makeText(binding.root.context, R.string.to_be_done, Toast.LENGTH_SHORT).show()
        }

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context

        val animation: Animation = AnimationUtils.loadAnimation(
            context,
            if ((position > lastPosition)) R.anim.left_from_right
            else R.anim.right_from_left
        )
        holder.itemView.startAnimation(animation)
        lastPosition = position

        holder.binding.tv.text = item.text
        holder.binding.tv2.text = item.text2

        if (item.text2.isEmpty()) holder.binding.tv2.layoutParams.height = 0

        Glide
            .with(context.applicationContext)
            .load(item.imageResource)
            .centerCrop()
//            .placeholder(spinner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .downsample(DownsampleStrategy.CENTER_OUTSIDE)
            .override(200, 200)
//            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.none)
            .apply(RequestOptions.bitmapTransform(KuwaharaFilterTransformation()))
            .into(holder.binding.iv)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }
}