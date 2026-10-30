package com.phoneai.localmodel

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phoneai.localmodel.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: PhysicsDatabaseHelper
    private lateinit var aiEngine: LocalAIEngine

    // Adapters
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var feedAdapter: PhysicsPostAdapter
    private lateinit var savedAdapter: PhysicsPostAdapter
    private lateinit var wikiAdapter: PhysicsWikiAdapter

    private val messagesList = mutableListOf<ChatMessage>()
    private var currentInferenceJob: Job? = null
    private var isGenerating = false

    // State
    private var currentTab = Tab.FEED
    private var currentTagFilter: String = "All"
    private var selectedModelFile: File? = null

    enum class Tab { FEED, BUDDY, WIKI, SAVED }

    // File picker contract for importing GGUF models
    private val modelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importModelFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = PhysicsDatabaseHelper(this)
        aiEngine = LocalAIEngine(applicationContext)

        setupNavigation()
        setupFeed()
        setupBuddyChat()
        setupWiki()
        setupSaved()
        setupModelControls()

        checkAndInitializeModel()
    }

    // ==========================================
    // NAVIGATION & TAB SWITCHING (Boxy & Royal)
    // ==========================================
    private fun setupNavigation() {
        binding.tabFeed.setOnClickListener { switchTab(Tab.FEED) }
        binding.tabBuddy.setOnClickListener { switchTab(Tab.BUDDY) }
        binding.tabWiki.setOnClickListener { switchTab(Tab.WIKI) }
        binding.tabSaved.setOnClickListener { switchTab(Tab.SAVED) }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab

        // Toggle Views
        binding.viewFeed.visibility = if (tab == Tab.FEED) View.VISIBLE else View.GONE
        binding.viewBuddy.visibility = if (tab == Tab.BUDDY) View.VISIBLE else View.GONE
        binding.viewWiki.visibility = if (tab == Tab.WIKI) View.VISIBLE else View.GONE
        binding.viewSaved.visibility = if (tab == Tab.SAVED) View.VISIBLE else View.GONE

        // Update Tab Colors
        val gold = ContextCompat.getColor(this, R.color.royal_gold)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        // Feed
        binding.ivTabFeed.setColorFilter(if (tab == Tab.FEED) gold else muted)
        binding.tvTabFeed.setTextColor(if (tab == Tab.FEED) gold else muted)

        // Buddy
        binding.ivTabBuddy.setColorFilter(if (tab == Tab.BUDDY) gold else muted)
        binding.tvTabBuddy.setTextColor(if (tab == Tab.BUDDY) gold else muted)

        // Wiki
        binding.ivTabWiki.setColorFilter(if (tab == Tab.WIKI) gold else muted)
        binding.tvTabWiki.setTextColor(if (tab == Tab.WIKI) gold else muted)

        // Saved
        binding.ivTabSaved.setColorFilter(if (tab == Tab.SAVED) gold else muted)
        binding.tvTabSaved.setTextColor(if (tab == Tab.SAVED) gold else muted)

        // Refresh data on tab switch
        when (tab) {
            Tab.FEED -> loadFeedPosts()
            Tab.SAVED -> loadSavedPosts()
            Tab.WIKI -> loadWikiTopics()
            Tab.BUDDY -> { /* Keep chat as is */ }
        }
    }

    // ==========================================
    // 1. FEED: LOCAL PHYSICS SOCIAL MEDIA
    // ==========================================
    private fun setupFeed() {
        feedAdapter = PhysicsPostAdapter(
            posts = mutableListOf(),
            onUpvoteClick = { post, pos ->
                val (newUpvoted, newCount) = dbHelper.toggleUpvote(post.id)
                val updated = post.copy(isUpvoted = newUpvoted, upvotes = newCount)
                feedAdapter.updateItem(pos, updated)
            },
            onCommentsClick = { post ->
                showPostDetailsDialog(post)
            },
            onAskBuddyClick = { post, pos ->
                generateHypothesisForPost(post, pos)
            },
            onSaveClick = { post, pos ->
                val newSaved = dbHelper.toggleSave(post.id)
                val updated = post.copy(isSaved = newSaved)
                feedAdapter.updateItem(pos, updated)
                val msg = if (newSaved) "Saved to bookmarks" else "Removed from bookmarks"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(this)
        binding.rvPosts.adapter = feedAdapter

        // Tag Filter Chips
        val tags = listOf(
            Pair(binding.filterAll, "All"),
            Pair(binding.filterQuantum, "Quantum"),
            Pair(binding.filterRelativity, "Relativity"),
            Pair(binding.filterThermodynamics, "Thermodynamics"),
            Pair(binding.filterAstrophysics, "Astrophysics"),
            Pair(binding.filterSolidState, "SolidState")
        )

        for ((view, tag) in tags) {
            view.setOnClickListener {
                currentTagFilter = tag
                for ((v, t) in tags) {
                    if (t == tag) {
                        v.setBackgroundResource(R.drawable.bg_card_boxy_gold)
                        v.setTextColor(ContextCompat.getColor(this, R.color.royal_gold))
                    } else {
                        v.setBackgroundResource(R.drawable.bg_tag_boxy)
                        v.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    }
                }
                loadFeedPosts()
            }
        }

        // New Post FAB
        binding.fabNewPost.setOnClickListener {
            showNewPostDialog()
        }

        loadFeedPosts()
    }

    private fun loadFeedPosts() {
        val posts = dbHelper.getAllPosts(tagFilter = currentTagFilter)
        feedAdapter.updateList(posts)
    }

    private fun generateHypothesisForPost(post: PhysicsPost, pos: Int) {
        if (!aiEngine.isInitialized) {
            Toast.makeText(this, "Please load an AI model first.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Physics Buddy is generating theoretical hypothesis...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val (hypothesis, _) = aiEngine.generatePhysicsHypothesis(post.title, post.content)
            dbHelper.updateAiHypothesis(post.id, hypothesis)
            val updated = post.copy(aiHypothesis = hypothesis)
            feedAdapter.updateItem(pos, updated)
            Toast.makeText(this@MainActivity, "Hypothesis generated and saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNewPostDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_new_post)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etTitle = dialog.findViewById<EditText>(R.id.etNewPostTitle)
        val etContent = dialog.findViewById<EditText>(R.id.etNewPostContent)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnDialogClose)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitPost)

        var selectedTag = "Quantum"
        val tagViews = listOf(
            Pair(dialog.findViewById<TextView>(R.id.tagOptQuantum), "Quantum"),
            Pair(dialog.findViewById<TextView>(R.id.tagOptRelativity), "Relativity"),
            Pair(dialog.findViewById<TextView>(R.id.tagOptThermodynamics), "Thermodynamics"),
            Pair(dialog.findViewById<TextView>(R.id.tagOptAstrophysics), "Astrophysics"),
            Pair(dialog.findViewById<TextView>(R.id.tagOptSolidState), "SolidState")
        )

        for ((v, tag) in tagViews) {
            v.setOnClickListener {
                selectedTag = tag
                for ((otherV, otherTag) in tagViews) {
                    if (otherTag == tag) {
                        otherV.setBackgroundResource(R.drawable.bg_card_boxy_gold)
                        otherV.setTextColor(ContextCompat.getColor(this, R.color.royal_gold))
                    } else {
                        otherV.setBackgroundResource(R.drawable.bg_tag_boxy)
                        otherV.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    }
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please enter both title and content.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newPost = PhysicsPost(
                title = title,
                content = content,
                author = "You",
                role = "Theoretical Explorer",
                tag = selectedTag,
                timestamp = "Just now"
            )
            dbHelper.insertPost(newPost)
            loadFeedPosts()
            dialog.dismiss()
            Toast.makeText(this, "Discussion published to local network!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showPostDetailsDialog(post: PhysicsPost) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_post_comments)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDetailsTitle)
        val tvAuthor = dialog.findViewById<TextView>(R.id.tvDetailsAuthor)
        val tvTag = dialog.findViewById<TextView>(R.id.tvDetailsTag)
        val tvContent = dialog.findViewById<TextView>(R.id.tvDetailsContent)
        val layoutAi = dialog.findViewById<LinearLayout>(R.id.layoutDetailsAiHypothesis)
        val tvAiText = dialog.findViewById<TextView>(R.id.tvDetailsAiText)
        val btnAskAi = dialog.findViewById<Button>(R.id.btnDetailsAskAi)
        val pbAi = dialog.findViewById<ProgressBar>(R.id.pbAiGenerating)
        val rvComments = dialog.findViewById<RecyclerView>(R.id.rvComments)
        val etComment = dialog.findViewById<EditText>(R.id.etNewComment)
        val btnSendComment = dialog.findViewById<ImageView>(R.id.btnSendComment)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnDetailsClose)

        tvTitle.text = post.title
        tvAuthor.text = "By ${post.author} (${post.role}) • ${post.timestamp}"
        tvTag.text = "#${post.tag}"
        tvContent.text = post.content

        // AI hypothesis
        fun updateAiView(hyp: String?) {
            if (!hyp.isNullOrBlank()) {
                layoutAi.visibility = View.VISIBLE
                tvAiText.text = hyp
                btnAskAi.text = "✨ Re-generate AI Hypothesis"
            } else {
                layoutAi.visibility = View.GONE
                btnAskAi.text = "✨ Ask Physics Buddy for Theoretical Hypothesis"
            }
        }
        updateAiView(post.aiHypothesis)

        // Comments
        val commentsList = dbHelper.getComments(post.id).toMutableList()
        val commentAdapter = PhysicsCommentAdapter(commentsList)
        rvComments.layoutManager = LinearLayoutManager(this)
        rvComments.adapter = commentAdapter

        btnClose.setOnClickListener { dialog.dismiss() }

        btnAskAi.setOnClickListener {
            if (!aiEngine.isInitialized) {
                Toast.makeText(this, "Please load an AI model first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pbAi.visibility = View.VISIBLE
            btnAskAi.isEnabled = false

            lifecycleScope.launch {
                val (hyp, _) = aiEngine.generatePhysicsHypothesis(post.title, post.content)
                dbHelper.updateAiHypothesis(post.id, hyp)
                updateAiView(hyp)
                pbAi.visibility = View.GONE
                btnAskAi.isEnabled = true
                loadFeedPosts()
            }
        }

        btnSendComment.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val comment = PhysicsComment(
                postId = post.id,
                author = "You",
                content = text,
                timestamp = "Just now",
                isAi = false
            )
            dbHelper.insertComment(comment)
            commentAdapter.addComment(comment)
            etComment.setText("")
            rvComments.smoothScrollToPosition(commentAdapter.itemCount - 1)
            loadFeedPosts()
        }

        dialog.show()
    }

    // ==========================================
    // 2. BUDDY: AI COMPANION CHAT
    // ==========================================
    private fun setupBuddyChat() {
        // Load chat history from DB
        val history = dbHelper.getChatHistory()
        messagesList.clear()
        messagesList.addAll(history)

        chatAdapter = ChatAdapter(messagesList)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvBuddyMessages.layoutManager = layoutManager
        binding.rvBuddyMessages.adapter = chatAdapter

        binding.fabSendBuddy.setOnClickListener {
            if (isGenerating) stopGeneration() else sendBuddyMessage()
        }

        binding.etBuddyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendBuddyMessage()
                true
            } else {
                false
            }
        }

        binding.btnClearChatHistory.setOnClickListener {
            dbHelper.clearChatHistory()
            chatAdapter.clearMessages()
            Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show()
        }

        // Quick prompt chips
        val promptChips = listOf(
            Pair(binding.chipPrompt1, "Explain Bell's Theorem and how it rules out local hidden variables."),
            Pair(binding.chipPrompt2, "Derive mass-energy equivalence E=mc² step-by-step from relativistic momentum."),
            Pair(binding.chipPrompt3, "Why does entropy strictly increase according to statistical mechanics? Explain the Arrow of Time."),
            Pair(binding.chipPrompt4, "How does Hawking radiation lead to black hole evaporation?")
        )

        for ((chip, promptText) in promptChips) {
            chip.setOnClickListener {
                binding.etBuddyInput.setText(promptText)
                binding.etBuddyInput.setSelection(promptText.length)
                sendBuddyMessage()
            }
        }
    }

    private fun sendBuddyMessage() {
        val prompt = binding.etBuddyInput.text?.toString()?.trim() ?: ""
        if (prompt.isEmpty()) return

        if (!aiEngine.isInitialized) {
            Toast.makeText(this, "Please select or load a model first.", Toast.LENGTH_SHORT).show()
            binding.modelSetupCard.visibility = View.VISIBLE
            return
        }

        val userMsg = ChatMessage(text = prompt, isUser = true)
        chatAdapter.addMessage(userMsg)
        dbHelper.insertChatMessage(userMsg)
        binding.etBuddyInput.setText("")
        binding.rvBuddyMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)

        val botMsg = ChatMessage(text = "Physics Buddy is thinking...", isUser = false, isGenerating = true)
        chatAdapter.addMessage(botMsg)
        binding.rvBuddyMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)

        setGeneratingState(true)

        currentInferenceJob = lifecycleScope.launch {
            // High token limit (1024) ensures no cut-offs or truncated code/formulas
            val (responseText, stats) = aiEngine.generateTextWithStats(prompt, maxTokens = 1024)
            chatAdapter.updateLastBotMessage(newText = responseText, isGenerating = false, stats = stats)
            val completedBotMsg = ChatMessage(text = responseText, isUser = false, isGenerating = false, statsInfo = stats)
            dbHelper.insertChatMessage(completedBotMsg)
            setGeneratingState(false)
            binding.rvBuddyMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun stopGeneration() {
        currentInferenceJob?.cancel()
        currentInferenceJob = null
        setGeneratingState(false)
        chatAdapter.updateLastBotMessage(
            newText = "[Generation stopped by user]",
            isGenerating = false,
            stats = "Cancelled"
        )
    }

    private fun setGeneratingState(generating: Boolean) {
        isGenerating = generating
        if (generating) {
            binding.fabSendBuddy.setImageResource(R.drawable.ic_stop)
            updateEngineStatus("Thinking…", R.color.status_loading)
        } else {
            binding.fabSendBuddy.setImageResource(R.drawable.ic_send)
            updateEngineStatus("Ready", R.color.status_ready)
        }
    }

    // ==========================================
    // 3. WIKI: PHYSICS KNOWLEDGE EXPLORER
    // ==========================================
    private fun setupWiki() {
        wikiAdapter = PhysicsWikiAdapter(
            topics = mutableListOf(),
            onDiscussClick = { topic ->
                // Switch to Buddy tab with prompt preloaded
                switchTab(Tab.BUDDY)
                val prompt = "Explain the ${topic.title} (${topic.formula}) in detail and its key physical principles: ${topic.keyPrinciples}"
                binding.etBuddyInput.setText(prompt)
                binding.etBuddyInput.setSelection(prompt.length)
                sendBuddyMessage()
            }
        )

        binding.rvWikiTopics.layoutManager = LinearLayoutManager(this)
        binding.rvWikiTopics.adapter = wikiAdapter

        binding.etWikiSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadWikiTopics(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Check if ZIM file exists
        val zimFile = ModelFileManager.findZimFile(this)
        if (zimFile != null) {
            val sizeMb = zimFile.length() / (1024 * 1024)
            binding.tvZimStatusBadge.text = "Offline Wikipedia Physics Archive (${sizeMb}MB Active)"
            binding.tvZimStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.royal_gold))
        } else {
            binding.tvZimStatusBadge.text = "Offline Physics Knowledge Base"
        }

        loadWikiTopics()
    }

    private fun loadWikiTopics(query: String? = null) {
        val topics = dbHelper.getAllWikiTopics(query)
        wikiAdapter.updateList(topics)
        binding.tvWikiCountBadge.text = "${topics.size} Core Theories"
    }

    // ==========================================
    // 4. SAVED: BOOKMARKED POSTS
    // ==========================================
    private fun setupSaved() {
        savedAdapter = PhysicsPostAdapter(
            posts = mutableListOf(),
            onUpvoteClick = { post, pos ->
                val (newUpvoted, newCount) = dbHelper.toggleUpvote(post.id)
                val updated = post.copy(isUpvoted = newUpvoted, upvotes = newCount)
                savedAdapter.updateItem(pos, updated)
            },
            onCommentsClick = { post -> showPostDetailsDialog(post) },
            onAskBuddyClick = { post, pos -> generateHypothesisForPost(post, pos) },
            onSaveClick = { post, pos ->
                dbHelper.toggleSave(post.id)
                loadSavedPosts()
            }
        )
        binding.rvSavedPosts.layoutManager = LinearLayoutManager(this)
        binding.rvSavedPosts.adapter = savedAdapter
        loadSavedPosts()
    }

    private fun loadSavedPosts() {
        val saved = dbHelper.getAllPosts(onlySaved = true)
        savedAdapter.updateList(saved)
        binding.tvEmptySaved.visibility = if (saved.isEmpty()) View.VISIBLE else View.GONE
    }

    // ==========================================
    // MODEL CONTROLS & SWITCHING
    // ==========================================
    private fun setupModelControls() {
        binding.btnSwitchModel.setOnClickListener {
            showModelSelectionDialog()
        }

        binding.tvModelSubtitle.setOnClickListener {
            showModelSelectionDialog()
        }

        binding.btnImportModel.setOnClickListener {
            modelPickerLauncher.launch("*/*")
        }

        binding.btnScanTmp.setOnClickListener {
            scanLocalTmp()
        }
    }

    private fun checkAndInitializeModel() {
        val existingModel = ModelFileManager.findExistingModel(this)
        if (existingModel != null) {
            initializeEngineWithModel(existingModel)
        } else {
            showModelMissingState()
        }
    }

    private fun initializeEngineWithModel(modelFile: File) {
        selectedModelFile = modelFile
        val isGguf = modelFile.name.lowercase().endsWith(".gguf")
        val isGemma = modelFile.name.contains("gemma", ignoreCase = true)
        val engineLabel = if (isGguf) (if (isGemma) "Gemma-2 GGUF" else "SmolLM2 GGUF") else "MediaPipe"

        updateEngineStatus("Loading...", R.color.status_loading)
        binding.modelSetupCard.visibility = View.GONE

        lifecycleScope.launch {
            val result = aiEngine.initializeEngine(
                modelPath = modelFile.absolutePath,
                maxTokens = 1024,
                temperature = 0.7f
            )

            result.onSuccess { engineDesc ->
                val sizeMb = modelFile.length() / (1024 * 1024)
                binding.tvModelSubtitle.text = "$engineLabel • ${sizeMb}MB"
                updateEngineStatus("Ready", R.color.status_ready)
                binding.modelSetupCard.visibility = View.GONE
                Toast.makeText(this@MainActivity, "$engineDesc ready for inference!", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                updateEngineStatus("Init Failed", R.color.status_error)
                binding.modelSetupCard.visibility = View.VISIBLE
                binding.tvModelPromptInstructions.text = "Initialization error: ${error.localizedMessage}"
            }
        }
    }

    private fun showModelSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_model_select)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val layoutList = dialog.findViewById<LinearLayout>(R.id.layoutModelsList)
        val btnImport = dialog.findViewById<Button>(R.id.btnImportNewModel)

        val installedModels = ModelFileManager.getInstalledModels(this)

        if (installedModels.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "No models currently in app private storage.\nTap '+ Import' below or run push_model.ps1."
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                textSize = 12f
                setPadding(0, 16, 0, 16)
            }
            layoutList.addView(tvEmpty)
        } else {
            for (file in installedModels) {
                val sizeMb = file.length() / (1024 * 1024)
                val isSelected = selectedModelFile?.absolutePath == file.absolutePath

                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 8, 0, 8) }
                    setBackgroundResource(if (isSelected) R.drawable.bg_card_boxy_gold else R.drawable.bg_card_boxy)
                    setPadding(16, 14, 16, 14)
                    isClickable = true
                    isFocusable = true
                }

                val infoLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvName = TextView(this).apply {
                    text = file.name
                    setTextColor(ContextCompat.getColor(context, if (isSelected) R.color.royal_gold else R.color.text_primary))
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val tvInfo = TextView(this).apply {
                    text = "${sizeMb}MB • ${if (file.name.endsWith(".gguf")) "llama.cpp CPU" else "MediaPipe"}"
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    textSize = 11f
                }

                infoLayout.addView(tvName)
                infoLayout.addView(tvInfo)
                itemLayout.addView(infoLayout)

                if (isSelected) {
                    val tvActive = TextView(this).apply {
                        text = "ACTIVE"
                        setTextColor(ContextCompat.getColor(context, R.color.royal_gold))
                        textSize = 10f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setBackgroundResource(R.drawable.bg_tag_boxy)
                        setPadding(8, 4, 8, 4)
                    }
                    itemLayout.addView(tvActive)
                }

                itemLayout.setOnClickListener {
                    dialog.dismiss()
                    initializeEngineWithModel(file)
                }

                layoutList.addView(itemLayout)
            }
        }

        btnImport.setOnClickListener {
            dialog.dismiss()
            modelPickerLauncher.launch("*/*")
        }

        dialog.show()
    }

    private fun showModelMissingState() {
        updateEngineStatus("No Model", R.color.status_error)
        binding.modelSetupCard.visibility = View.VISIBLE
    }

    private fun scanLocalTmp() {
        binding.pbModelCopy.visibility = View.VISIBLE
        binding.tvCopyProgress.visibility = View.VISIBLE
        binding.tvCopyProgress.text = "Checking /data/local/tmp/..."

        lifecycleScope.launch {
            val result = ModelFileManager.copyFromLocalTmp(this@MainActivity) { percent, copied, total ->
                binding.pbModelCopy.isIndeterminate = percent < 0
                if (percent >= 0) {
                    binding.pbModelCopy.progress = percent
                    val mbCopied = copied / (1024 * 1024)
                    val mbTotal = total / (1024 * 1024)
                    binding.tvCopyProgress.text = "Importing: $mbCopied / $mbTotal MB ($percent%)"
                }
            }

            binding.pbModelCopy.visibility = View.GONE
            binding.tvCopyProgress.visibility = View.GONE

            result.onSuccess { file ->
                Toast.makeText(this@MainActivity, "Model imported into app storage!", Toast.LENGTH_SHORT).show()
                initializeEngineWithModel(file)
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
                binding.tvModelPromptInstructions.text = error.message
            }
        }
    }

    private fun importModelFromUri(uri: Uri) {
        binding.pbModelCopy.visibility = View.VISIBLE
        binding.tvCopyProgress.visibility = View.VISIBLE
        binding.tvCopyProgress.text = "Importing model file into app storage..."

        lifecycleScope.launch {
            val result = ModelFileManager.copyFromUri(this@MainActivity, uri) { percent, copied, total ->
                binding.pbModelCopy.isIndeterminate = percent < 0
                if (percent >= 0) {
                    binding.pbModelCopy.progress = percent
                    val mbCopied = copied / (1024 * 1024)
                    val mbTotal = total / (1024 * 1024)
                    binding.tvCopyProgress.text = "Importing: $mbCopied / $mbTotal MB ($percent%)"
                }
            }

            binding.pbModelCopy.visibility = View.GONE
            binding.tvCopyProgress.visibility = View.GONE

            result.onSuccess { file ->
                Toast.makeText(this@MainActivity, "Model imported into app storage!", Toast.LENGTH_SHORT).show()
                initializeEngineWithModel(file)
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Import failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEngineStatus(statusText: String, indicatorColorRes: Int) {
        binding.tvEngineStatus.text = statusText
        binding.statusIndicatorDot.backgroundTintList = ContextCompat.getColorStateList(this, indicatorColorRes)
    }

    override fun onDestroy() {
        super.onDestroy()
        aiEngine.close()
    }
}
