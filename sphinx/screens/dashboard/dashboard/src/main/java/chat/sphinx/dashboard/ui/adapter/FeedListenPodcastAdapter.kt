package chat.sphinx.dashboard.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.dashboard.databinding.LayoutFeedListenPodcastHolderBinding
import chat.sphinx.dashboard.ui.placeholder.PlaceholderContent.PlaceholderItem

class FeedListenPodcastAdapter(
    private val values: List<PlaceholderItem>
) : RecyclerView.Adapter<FeedListenPodcastAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            LayoutFeedListenPodcastHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.idView.text = item.content
        holder.contentView.text = item.details
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: LayoutFeedListenPodcastHolderBinding) : RecyclerView.ViewHolder(binding.root) {
        val idView: TextView = binding.textViewListenPodcastName
        val contentView: TextView = binding.textViewListenPodcastDescription

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}