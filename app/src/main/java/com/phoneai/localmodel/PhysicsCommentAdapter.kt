package com.phoneai.localmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhysicsCommentAdapter(
    private var comments: MutableList<PhysicsComment>
) : RecyclerView.Adapter<PhysicsCommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        val tvAiBadge: TextView = itemView.findViewById(R.id.tvAiBadge)
        val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_physics_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvAuthor.text = comment.author
        holder.tvTime.text = comment.timestamp
        holder.tvContent.text = comment.content
        holder.tvAiBadge.visibility = if (comment.isAi) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = comments.size

    fun updateList(newComments: List<PhysicsComment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    fun addComment(comment: PhysicsComment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }
}

