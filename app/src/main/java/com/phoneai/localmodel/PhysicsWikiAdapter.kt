package com.phoneai.localmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhysicsWikiAdapter(
    private var topics: MutableList<PhysicsWikiTopic>,
    private val onDiscussClick: (PhysicsWikiTopic) -> Unit
) : RecyclerView.Adapter<PhysicsWikiAdapter.WikiViewHolder>() {

    class WikiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvWikiTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvWikiCategory)
        val tvFormula: TextView = itemView.findViewById(R.id.tvWikiFormula)
        val tvSummary: TextView = itemView.findViewById(R.id.tvWikiSummary)
        val tvPrinciples: TextView = itemView.findViewById(R.id.tvWikiPrinciples)
        val btnDiscuss: LinearLayout = itemView.findViewById(R.id.btnDiscussWithBuddy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WikiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_physics_wiki, parent, false)
        return WikiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WikiViewHolder, position: Int) {
        val topic = topics[position]
        holder.tvTitle.text = topic.title
        holder.tvCategory.text = topic.category
        holder.tvFormula.text = topic.formula
        holder.tvSummary.text = topic.summary
        holder.tvPrinciples.text = "Principles: ${topic.keyPrinciples}"

        holder.btnDiscuss.setOnClickListener {
            onDiscussClick(topic)
        }
    }

    override fun getItemCount(): Int = topics.size

    fun updateList(newTopics: List<PhysicsWikiTopic>) {
        topics.clear()
        topics.addAll(newTopics)
        notifyDataSetChanged()
    }
}

