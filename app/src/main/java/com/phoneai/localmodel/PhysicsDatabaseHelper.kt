package com.phoneai.localmodel

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PhysicsPost(
    val id: Long = 0,
    val title: String,
    val content: String,
    val author: String,
    val role: String,
    val tag: String,
    val upvotes: Int = 0,
    val commentCount: Int = 0,
    val isUpvoted: Boolean = false,
    val isSaved: Boolean = false,
    val timestamp: String,
    val aiHypothesis: String? = null
)

data class PhysicsComment(
    val id: Long = 0,
    val postId: Long,
    val author: String,
    val content: String,
    val timestamp: String,
    val isAi: Boolean = false
)

data class PhysicsWikiTopic(
    val id: Long = 0,
    val title: String,
    val category: String,
    val summary: String,
    val formula: String,
    val keyPrinciples: String
)

class PhysicsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "phoneai_physics.db"
        private const val DATABASE_VERSION = 1

        // Tables
        private const val TABLE_POSTS = "posts"
        private const val TABLE_COMMENTS = "comments"
        private const val TABLE_WIKI = "wiki_topics"
        private const val TABLE_CHAT = "chat_history"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_POSTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                author TEXT NOT NULL,
                role TEXT NOT NULL,
                tag TEXT NOT NULL,
                upvotes INTEGER DEFAULT 0,
                comment_count INTEGER DEFAULT 0,
                is_upvoted INTEGER DEFAULT 0,
                is_saved INTEGER DEFAULT 0,
                timestamp TEXT NOT NULL,
                ai_hypothesis TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_COMMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                post_id INTEGER NOT NULL,
                author TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                is_ai INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_WIKI (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                summary TEXT NOT NULL,
                formula TEXT NOT NULL,
                key_principles TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CHAT (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                is_user INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                stats TEXT
            )
            """.trimIndent()
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WIKI")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Seed initial social media posts
        val seedPosts = listOf(
            PhysicsPost(
                title = "Wave-Particle Duality in Macroscopic Matter Waves",
                content = "Does the de Broglie wavelength constraint mean macroscopic biological structures could ever exhibit quantum interference under extreme vacuum cooling, or does environmental decoherence make it mathematically impossible?",
                author = "Dr. Elena Rostova",
                role = "Quantum Coherence Fellow",
                tag = "Quantum",
                upvotes = 34,
                commentCount = 3,
                timestamp = "2h ago",
                aiHypothesis = "Decoherence time scale scales exponentially as τ ~ ħ / (m * γ * Δx²). For macro-objects, even single cosmic microwave photons induce instantaneous phase randomization, rendering interference unobservable despite non-zero theoretical amplitude."
            ),
            PhysicsPost(
                title = "Why Entropy strictly increases: The Arrow of Time",
                content = "Boltzmann's H-theorem explains entropy statistically through microstate multiplicity Ω (S = k * ln Ω). But if microscopic physical laws (Hamiltonian dynamics) are strictly time-reversible, where does macroscopic irreversibility fundamentally originate?",
                author = "Julian Thorne",
                role = "Statistical Mechanics",
                tag = "Thermodynamics",
                upvotes = 52,
                commentCount = 4,
                timestamp = "4h ago",
                aiHypothesis = "The asymmetry emerges from boundary conditions rather than dynamical equations. The Past Hypothesis posits the universe began in an anomalously low-entropy state (the Big Bang), defining the geometric arrow of thermodynamic progression."
            ),
            PhysicsPost(
                title = "Frame-Dragging (Lense-Thirring Effect) around Kerr Black Holes",
                content = "In General Relativity, rotating mass literally drags spacetime along with its spin. Inside the ergosphere (between the static limit and the event horizon), can an observer ever remain stationary relative to asymptotic infinity?",
                author = "Prof. Marcus Thorne",
                role = "Relativistic Astrophysics",
                tag = "Relativity",
                upvotes = 41,
                commentCount = 2,
                timestamp = "7h ago",
                aiHypothesis = "No. Inside the ergosphere, the metric coefficient g_tt becomes positive, meaning the timelike Killing vector becomes spacelike. Spacetime rotation exceeds the speed of light relative to distant observers; co-rotation is mandatory."
            ),
            PhysicsPost(
                title = "Primordial Black Holes as Dark Matter Candidates",
                content = "With James Webb Space Telescope observing supermassive black hole seeds at z > 10, the hypothesis that dark matter comprises asteroid-mass primordial black holes (10¹⁷ to 10²² g) from inflation fluctuations is receiving renewed attention.",
                author = "AstroObserver_99",
                role = "Cosmology Explorer",
                tag = "Astrophysics",
                upvotes = 29,
                commentCount = 1,
                timestamp = "1d ago",
                aiHypothesis = "Asteroid-mass PBHs escape both Hawking evaporation limits (which destroy PBHs < 10¹⁵ g) and gravitational microlensing exclusion constraints (MACHO/EROS surveys), leaving an open window for non-baryonic dark matter."
            ),
            PhysicsPost(
                title = "Phonon-Mediated Superconductivity vs Magnetic Pairing",
                content = "BCS theory flawlessly predicts Cooper pairing via electron-phonon coupling in conventional low-Tc metals. In cuprate and nickelate high-Tc superconductors, why do electron-electron repulsive d-wave interactions sustain higher pairing temperatures?",
                author = "K. Ramanathan",
                role = "Condensed Matter Physics",
                tag = "SolidState",
                upvotes = 38,
                commentCount = 2,
                timestamp = "2d ago",
                aiHypothesis = "Spin-fluctuation exchange replaces phonon interaction. The sign change of the d(x²-y²) superconducting order parameter across the Fermi surface turns local Coulomb repulsion into an effective attractive pairing channel."
            )
        )

        for (post in seedPosts) {
            val cv = ContentValues().apply {
                put("title", post.title)
                put("content", post.content)
                put("author", post.author)
                put("role", post.role)
                put("tag", post.tag)
                put("upvotes", post.upvotes)
                put("comment_count", post.commentCount)
                put("is_upvoted", if (post.isUpvoted) 1 else 0)
                put("is_saved", if (post.isSaved) 1 else 0)
                put("timestamp", post.timestamp)
                put("ai_hypothesis", post.aiHypothesis)
            }
            val postId = db.insert(TABLE_POSTS, null, cv)

            // Seed initial comments
            if (postId == 1L) {
                db.insert(TABLE_COMMENTS, null, ContentValues().apply {
                    put("post_id", 1L)
                    put("author", "Zeilinger_Fan")
                    put("content", "Vienna groups tested fullerenes (C60, C70) and even gramicidin proteins! Environmental vacuum is the real bottleneck.")
                    put("timestamp", "1h ago")
                    put("is_ai", 0)
                })
            } else if (postId == 2L) {
                db.insert(TABLE_COMMENTS, null, ContentValues().apply {
                    put("post_id", 2L)
                    put("author", "EntropyWalker")
                    put("content", "Loschmidt's paradox! The Past Hypothesis is still one of physics' greatest cosmological mysteries.")
                    put("timestamp", "3h ago")
                    put("is_ai", 0)
                })
            }
        }

        // Seed initial Wikipedia physics knowledge database
        val wikiTopics = listOf(
            PhysicsWikiTopic(
                title = "Schrödinger Equation",
                category = "Quantum Mechanics",
                summary = "Fundamental differential equation governing the quantum state evolution of an isolated physical system over time.",
                formula = "iℏ ∂/∂t |Ψ(t)⟩ = Ĥ |Ψ(t)⟩",
                keyPrinciples = "Unitary time evolution • Hamiltonian energy operator • Complex probability amplitude • Linear superposition"
            ),
            PhysicsWikiTopic(
                title = "Mass-Energy Equivalence",
                category = "Special Relativity",
                summary = "Principle stating that mass and energy are equivalent and can be converted into each other via the speed of light squared.",
                formula = "E² = (mc²)² + (pc)², rest: E = mc²",
                keyPrinciples = "Invariant mass • Lorentz four-momentum • Nuclear fission/fusion binding energy deficits"
            ),
            PhysicsWikiTopic(
                title = "Maxwell's Equations",
                category = "Electrodynamics",
                summary = "Set of coupled partial differential equations forming the classical foundation of electromagnetism, optics, and electric circuits.",
                formula = "∇·E = ρ/ε₀, ∇·B = 0, ∇×E = -∂B/∂t, ∇×B = μ₀J + μ₀ε₀∂E/∂t",
                keyPrinciples = "Displacement current • Speed of light derivation c = 1/√(μ₀ε₀) • Electromagnetic radiation waves"
            ),
            PhysicsWikiTopic(
                title = "Black Hole Thermodynamics (Bekenstein-Hawking)",
                category = "General Relativity & Quantum",
                summary = "Relates black hole event horizon area to thermodynamic entropy and predicts quantum thermal emission (Hawking radiation).",
                formula = "S_BH = (k_B * c³ * A) / (4 * G * ℏ)",
                keyPrinciples = "Area law of entropy • Event horizon surface gravity as temperature • Information paradox • Holographic principle"
            ),
            PhysicsWikiTopic(
                title = "Second Law of Thermodynamics",
                category = "Statistical Mechanics",
                summary = "Total entropy of an isolated system always increases over time, dictating the irreversibility of natural physical processes.",
                formula = "ΔS_universe ≥ 0, S = k_B ln(Ω)",
                keyPrinciples = "Microstate multiplicity • Carnot thermodynamic efficiency limit • Arrow of time • Maximum entropy equilibrium"
            ),
            PhysicsWikiTopic(
                title = "Einstein Field Equations",
                category = "General Relativity",
                summary = "Relates the geometry of four-dimensional spacetime curvature directly to the stress-energy tensor of matter and radiation.",
                formula = "G_μν + Λ g_μν = (8πG / c⁴) T_μν",
                keyPrinciples = "Equivalence principle • Geodesic motion • Cosmological constant Λ • Gravitational waves and lensing"
            ),
            PhysicsWikiTopic(
                title = "Quantum Entanglement & Bell's Theorem",
                category = "Quantum Information",
                summary = "Demonstrates that no physical theory of local hidden variables can ever reproduce all predictions of quantum mechanics.",
                formula = "|Ψ⟩ = 1/√2 (|00⟩ + |11⟩), CHSH: |S| ≤ 2 (classical) vs 2√2 (quantum)",
                keyPrinciples = "Non-locality • Quantum teleportation • EPR paradox • Density matrix correlations"
            )
        )

        for (wiki in wikiTopics) {
            val cv = ContentValues().apply {
                put("title", wiki.title)
                put("category", wiki.category)
                put("summary", wiki.summary)
                put("formula", wiki.formula)
                put("key_principles", wiki.keyPrinciples)
            }
            db.insert(TABLE_WIKI, null, cv)
        }
    }

    // Post queries
    fun getAllPosts(tagFilter: String? = null, onlySaved: Boolean = false): List<PhysicsPost> {
        val list = mutableListOf<PhysicsPost>()
        val db = readableDatabase
        val selection = StringBuilder()
        val args = mutableListOf<String>()

        if (!tagFilter.isNullOrEmpty() && tagFilter != "All") {
            selection.append("tag = ?")
            args.add(tagFilter)
        }

        if (onlySaved) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("is_saved = 1")
        }

        val cursor: Cursor = db.query(
            TABLE_POSTS,
            null,
            if (selection.isEmpty()) null else selection.toString(),
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            "id DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    PhysicsPost(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        author = it.getString(it.getColumnIndexOrThrow("author")),
                        role = it.getString(it.getColumnIndexOrThrow("role")),
                        tag = it.getString(it.getColumnIndexOrThrow("tag")),
                        upvotes = it.getInt(it.getColumnIndexOrThrow("upvotes")),
                        commentCount = it.getInt(it.getColumnIndexOrThrow("comment_count")),
                        isUpvoted = it.getInt(it.getColumnIndexOrThrow("is_upvoted")) == 1,
                        isSaved = it.getInt(it.getColumnIndexOrThrow("is_saved")) == 1,
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")),
                        aiHypothesis = it.getString(it.getColumnIndexOrThrow("ai_hypothesis"))
                    )
                )
            }
        }
        return list
    }

    fun getPostById(id: Long): PhysicsPost? {
        val db = readableDatabase
        val cursor = db.query(TABLE_POSTS, null, "id = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return PhysicsPost(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    content = it.getString(it.getColumnIndexOrThrow("content")),
                    author = it.getString(it.getColumnIndexOrThrow("author")),
                    role = it.getString(it.getColumnIndexOrThrow("role")),
                    tag = it.getString(it.getColumnIndexOrThrow("tag")),
                    upvotes = it.getInt(it.getColumnIndexOrThrow("upvotes")),
                    commentCount = it.getInt(it.getColumnIndexOrThrow("comment_count")),
                    isUpvoted = it.getInt(it.getColumnIndexOrThrow("is_upvoted")) == 1,
                    isSaved = it.getInt(it.getColumnIndexOrThrow("is_saved")) == 1,
                    timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")),
                    aiHypothesis = it.getString(it.getColumnIndexOrThrow("ai_hypothesis"))
                )
            }
        }
        return null
    }

    fun insertPost(post: PhysicsPost): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", post.title)
            put("content", post.content)
            put("author", post.author)
            put("role", post.role)
            put("tag", post.tag)
            put("upvotes", post.upvotes)
            put("comment_count", post.commentCount)
            put("is_upvoted", if (post.isUpvoted) 1 else 0)
            put("is_saved", if (post.isSaved) 1 else 0)
            put("timestamp", post.timestamp)
            put("ai_hypothesis", post.aiHypothesis)
        }
        return db.insert(TABLE_POSTS, null, cv)
    }

    fun toggleUpvote(postId: Long): Pair<Boolean, Int> {
        val db = writableDatabase
        val post = getPostById(postId) ?: return Pair(false, 0)
        val newUpvoted = !post.isUpvoted
        val newCount = if (newUpvoted) post.upvotes + 1 else (post.upvotes - 1).coerceAtLeast(0)

        val cv = ContentValues().apply {
            put("is_upvoted", if (newUpvoted) 1 else 0)
            put("upvotes", newCount)
        }
        db.update(TABLE_POSTS, cv, "id = ?", arrayOf(postId.toString()))
        return Pair(newUpvoted, newCount)
    }

    fun toggleSave(postId: Long): Boolean {
        val db = writableDatabase
        val post = getPostById(postId) ?: return false
        val newSaved = !post.isSaved
        val cv = ContentValues().apply {
            put("is_saved", if (newSaved) 1 else 0)
        }
        db.update(TABLE_POSTS, cv, "id = ?", arrayOf(postId.toString()))
        return newSaved
    }

    fun updateAiHypothesis(postId: Long, hypothesis: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("ai_hypothesis", hypothesis)
        }
        db.update(TABLE_POSTS, cv, "id = ?", arrayOf(postId.toString()))
    }

    // Comment queries
    fun getComments(postId: Long): List<PhysicsComment> {
        val list = mutableListOf<PhysicsComment>()
        val db = readableDatabase
        val cursor = db.query(TABLE_COMMENTS, null, "post_id = ?", arrayOf(postId.toString()), null, null, "id ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    PhysicsComment(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        postId = it.getLong(it.getColumnIndexOrThrow("post_id")),
                        author = it.getString(it.getColumnIndexOrThrow("author")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        timestamp = it.getString(it.getColumnIndexOrThrow("timestamp")),
                        isAi = it.getInt(it.getColumnIndexOrThrow("is_ai")) == 1
                    )
                )
            }
        }
        return list
    }

    fun insertComment(comment: PhysicsComment): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("post_id", comment.postId)
            put("author", comment.author)
            put("content", comment.content)
            put("timestamp", comment.timestamp)
            put("is_ai", if (comment.isAi) 1 else 0)
        }
        val id = db.insert(TABLE_COMMENTS, null, cv)
        // Increment comment count on post
        db.execSQL("UPDATE $TABLE_POSTS SET comment_count = comment_count + 1 WHERE id = ?", arrayOf(comment.postId))
        return id
    }

    // Wiki queries
    fun getAllWikiTopics(query: String? = null): List<PhysicsWikiTopic> {
        val list = mutableListOf<PhysicsWikiTopic>()
        val db = readableDatabase
        val selection = if (query.isNullOrBlank()) null else "title LIKE ? OR summary LIKE ? OR category LIKE ?"
        val args = if (query.isNullOrBlank()) null else arrayOf("%$query%", "%$query%", "%$query%")

        val cursor = db.query(TABLE_WIKI, null, selection, args, null, null, "title ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    PhysicsWikiTopic(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        summary = it.getString(it.getColumnIndexOrThrow("summary")),
                        formula = it.getString(it.getColumnIndexOrThrow("formula")),
                        keyPrinciples = it.getString(it.getColumnIndexOrThrow("key_principles"))
                    )
                )
            }
        }
        return list
    }

    // Chat History
    fun getChatHistory(): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CHAT, null, null, null, null, null, "id ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ChatMessage(
                        text = it.getString(it.getColumnIndexOrThrow("text")),
                        isUser = it.getInt(it.getColumnIndexOrThrow("is_user")) == 1,
                        statsInfo = it.getString(it.getColumnIndexOrThrow("stats"))
                    )
                )
            }
        }
        return list
    }

    fun insertChatMessage(msg: ChatMessage) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("text", msg.text)
            put("is_user", if (msg.isUser) 1 else 0)
            put("timestamp", System.currentTimeMillis())
            put("stats", msg.statsInfo)
        }
        db.insert(TABLE_CHAT, null, cv)
    }

    fun clearChatHistory() {
        val db = writableDatabase
        db.delete(TABLE_CHAT, null, null)
    }
}
