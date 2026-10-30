package com.phoneai.localmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PhysicsPostAdapter(
    private var posts: MutableList<PhysicsPost>,
    private val onUpvoteClick: (PhysicsPost, Int) -> Unit,
    private val onCommentsClick: (PhysicsPost) -> Unit,
    private val onAskBuddyClick: (PhysicsPost, Int) -> Unit,
    private val onSaveClick: (PhysicsPost, Int) -> Unit
) : RecyclerView.Adapter<PhysicsPostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView = itemView.findViewById(R.id.tvPostAuthor)
        val tvRole: TextView = itemView.findViewById(R.id.tvPostRole)
        val tvTag: TextView = itemView.findViewById(R.id.tvPostTag)
        val tvTime: TextView = itemView.findViewById(R.id.tvPostTime)
        val tvTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvPostContent)

        val layoutAiHypothesis: LinearLayout = itemView.findViewById(R.id.layoutAiHypothesis)
        val tvAiHypothesisText: TextView = itemView.findViewById(R.id.tvAiHypothesisText)

        val btnUpvote: LinearLayout = itemView.findViewById(R.id.btnUpvote)
        val ivUpvoteIcon: ImageView = itemView.findViewById(R.id.ivUpvoteIcon)
        val tvUpvoteCount: TextView = itemView.findViewById(R.id.tvUpvoteCount)

        val btnComments: LinearLayout = itemView.findViewById(R.id.btnComments)
        val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)

        val btnAskBuddy: LinearLayout = itemView.findViewById(R.id.btnAskBuddy)
        val tvAskBuddyLabel: TextView = itemView.findViewById(R.id.tvAskBuddyLabel)

        val btnSave: ImageView = itemView.findViewById(R.id.btnSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_physics_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.tvAuthor.text = post.author
        holder.tvRole.text = post.role
        holder.tvTag.text = "#${post.tag}"
        holder.tvTime.text = post.timestamp
        holder.tvTitle.text = post.title
        holder.tvContent.text = post.content

        // AI hypothesis
        if (!post.aiHypothesis.isNullOrBlank()) {
            holder.layoutAiHypothesis.visibility = View.VISIBLE
            holder.tvAiHypothesisText.text = post.aiHypothesis
            holder.tvAskBuddyLabel.text = "Refine AI"
        } else {
            holder.layoutAiHypothesis.visibility = View.GONE
            holder.tvAskBuddyLabel.text = "Ask Buddy"
        }

        // Upvotes
        holder.tvUpvoteCount.text = post.upvotes.toString()
        if (post.isUpvoted) {
            holder.ivUpvoteIcon.setImageResource(R.drawable.ic_upvote_filled)
            holder.tvUpvoteCount.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.royal_gold))
        } else {
            holder.ivUpvoteIcon.setImageResource(R.drawable.ic_upvote)
            holder.tvUpvoteCount.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_secondary))
        }

        // Comments
        holder.tvCommentCount.text = post.commentCount.toString()

        // Saved
        if (post.isSaved) {
            holder.btnSave.setImageResource(R.drawable.ic_bookmark_filled)
        } else {
            holder.btnSave.setImageResource(R.drawable.ic_bookmark)
        }

        // Click listeners
        holder.btnUpvote.setOnClickListener {
            onUpvoteClick(post, holder.bindingAdapterPosition)
        }

        holder.btnComments.setOnClickListener {
            onCommentsClick(post)
        }

        holder.itemView.setOnClickListener {
            onCommentsClick(post)
        }

        holder.btnAskBuddy.setOnClickListener {
            onAskBuddyClick(post, holder.bindingAdapterPosition)
        }

        holder.btnSave.setOnClickListener {
            onSaveClick(post, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updateList(newPosts: List<PhysicsPost>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, updatedPost: PhysicsPost) {
        if (position in posts.indices) {
            posts[position] = updatedPost
            notifyItemChanged(position)
        }
    }
}

