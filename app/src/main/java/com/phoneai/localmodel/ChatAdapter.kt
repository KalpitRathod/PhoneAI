package com.phoneai.localmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) {
            holder.tvMessage.text = message.text
            holder.tvTime.text = timeFormatter.format(Date(message.timestamp))
        } else if (holder is BotViewHolder) {
            holder.tvMessage.text = message.text
            holder.tvTime.text = timeFormatter.format(Date(message.timestamp))
            holder.pbGenerating.visibility = if (message.isGenerating) View.VISIBLE else View.GONE
            
            if (message.statsInfo != null) {
                holder.tvStats.visibility = View.VISIBLE
                holder.tvStats.text = message.statsInfo
            } else {
                holder.tvStats.visibility = View.GONE
            }

            holder.btnCopy.setOnClickListener {
                copyToClipboard(it.context, message.text)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastBotMessage(newText: String, isGenerating: Boolean, stats: String? = null) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            val last = messages[lastIndex]
            if (!last.isUser) {
                last.text = newText
                last.isGenerating = isGenerating
                if (stats != null) last.statsInfo = stats
                notifyItemChanged(lastIndex)
            }
        }
    }

    fun clearMessages() {
        val count = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, count)
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Gemma AI Response", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvUserTime)
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvBotMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvBotTime)
        val pbGenerating: ProgressBar = itemView.findViewById(R.id.pbGenerating)
        val tvStats: TextView = itemView.findViewById(R.id.tvInferenceStats)
        val btnCopy: ImageView = itemView.findViewById(R.id.btnCopyMessage)
    }
}
