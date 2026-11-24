package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        userId: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    snap.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.GET, path)
        )
    }

    fun observeCustomMaterials(userId: String): Flow<List<StudyMaterial>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/studyMaterials"
        emitAll(
            db.collection("users").document(userId).collection("studyMaterials")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        StudyMaterial::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeMaterialProgress(userId: String): Flow<List<MaterialProgress>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/materialProgress"
        emitAll(
            db.collection("users").document(userId).collection("materialProgress")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MaterialProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeStudyPlans(userId: String): Flow<List<StudyPlanItem>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/studyPlans"
        emitAll(
            db.collection("users").document(userId).collection("studyPlans")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        StudyPlanItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/quizAttempts"
        emitAll(
            db.collection("users").document(userId).collection("quizAttempts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        QuizAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeFlashcards(userId: String): Flow<List<Flashcard>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/flashcards"
        emitAll(
            db.collection("users").document(userId).collection("flashcards")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Flashcard::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "Student" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "studyGoal" to "Master Core Drive Modules & Practice MCQs",
                    "dailyTargetMinutes" to 45L,
                    "totalStudyMinutes" to 0L,
                    "completedSessions" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudyGoal(
        userId: String,
        studyGoal: String,
        dailyTargetMinutes: Long
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeGoal = studyGoal.trim().take(200)
            val safeTarget = dailyTargetMinutes.coerceIn(5L, 720L)
            db.collection("users").document(uid).update(
                mapOf(
                    "studyGoal" to safeGoal,
                    "dailyTargetMinutes" to safeTarget,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logStudySession(userId: String, addedMinutes: Long): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeMinutes = addedMinutes.coerceIn(1L, 600L)
            db.collection("users").document(uid).update(
                mapOf(
                    "totalStudyMinutes" to FieldValue.increment(safeMinutes),
                    "completedSessions" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomMaterial(
        userId: String,
        title: String,
        description: String,
        materialType: MaterialType,
        sourceUrl: String,
        contentMarkdown: String,
        category: String,
        estimatedMinutes: Long
    ): Result<Unit> {
        val id = "mat_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/studyMaterials/$id"
        return try {
            val uid = requireUserId(userId)
            val safeTitle = title.trim().take(140)
            require(safeTitle.isNotEmpty()) { "Title cannot be empty." }
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "description" to description.trim().take(600),
                "materialType" to materialType.name,
                "sourceUrl" to sourceUrl.trim().take(1000),
                "contentMarkdown" to contentMarkdown.trim().take(12000),
                "category" to category.trim().ifBlank { "Custom" }.take(60),
                "estimatedMinutes" to estimatedMinutes.coerceIn(1L, 600L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomMaterial(userId: String, materialId: String): Result<Unit> {
        val path = "users/$userId/studyMaterials/$materialId"
        return try {
            requireUserId(userId)
            require(materialId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMaterialCompletion(
        userId: String,
        materialId: String,
        completed: Boolean,
        existingProgress: MaterialProgress?,
        additionalMinutes: Long = 0L
    ): Result<Unit> {
        val safeMaterialId = materialId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "users/$userId/materialProgress/$safeMaterialId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.document(path)
            if (existingProgress == null && !docRef.get().await().exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "materialId" to safeMaterialId,
                    "completed" to completed,
                    "personalNotes" to "",
                    "minutesSpent" to additionalMinutes.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (additionalMinutes > 0L) {
                    val current = existingProgress?.minutesSpent ?: 0L
                    updates["minutesSpent"] = (current + additionalMinutes).coerceIn(0L, 10000L)
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMaterialNotes(
        userId: String,
        materialId: String,
        notes: String,
        existingProgress: MaterialProgress?
    ): Result<Unit> {
        val safeMaterialId = materialId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "users/$userId/materialProgress/$safeMaterialId"
        return try {
            val uid = requireUserId(userId)
            val safeNotes = notes.take(4000)
            val docRef = db.document(path)
            if (existingProgress == null && !docRef.get().await().exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "materialId" to safeMaterialId,
                    "completed" to false,
                    "personalNotes" to safeNotes,
                    "minutesSpent" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "personalNotes" to safeNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addStudyPlanItem(
        userId: String,
        title: String,
        topic: String,
        dayLabel: String,
        targetMinutes: Long,
        linkedMaterialId: String = ""
    ): Result<Unit> {
        val id = "plan_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/studyPlans/$id"
        return try {
            val uid = requireUserId(userId)
            val safeTitle = title.trim().take(140)
            require(safeTitle.isNotEmpty()) { "Plan title cannot be empty." }
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "topic" to topic.trim().take(120),
                "dayLabel" to dayLabel.trim().ifBlank { "Today" }.take(40),
                "targetMinutes" to targetMinutes.coerceIn(5L, 480L),
                "completed" to false,
                "linkedMaterialId" to linkedMaterialId.take(128),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleStudyPlanItem(
        userId: String,
        planId: String,
        completed: Boolean
    ): Result<Unit> {
        val path = "users/$userId/studyPlans/$planId"
        return try {
            requireUserId(userId)
            require(planId.isNotBlank())
            db.document(path).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyPlanItem(userId: String, planId: String): Result<Unit> {
        val path = "users/$userId/studyPlans/$planId"
        return try {
            requireUserId(userId)
            require(planId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizAttempt(
        userId: String,
        topic: String,
        totalQuestions: Long,
        correctCount: Long,
        durationSeconds: Long,
        fullScreenUsed: Boolean
    ): Result<Unit> {
        val id = "quiz_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/quizAttempts/$id"
        return try {
            val uid = requireUserId(userId)
            val safeTotal = totalQuestions.coerceIn(1L, 100L)
            val safeCorrect = correctCount.coerceIn(0L, safeTotal)
            val safeDuration = durationSeconds.coerceIn(0L, 36000L)
            val payload = mapOf(
                "userId" to uid,
                "topic" to topic.trim().ifBlank { "Practice Quiz" }.take(140),
                "totalQuestions" to safeTotal,
                "correctCount" to safeCorrect,
                "durationSeconds" to safeDuration,
                "fullScreenUsed" to fullScreenUsed,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            val earnedMinutes = (safeDuration / 60L).coerceAtLeast(1L)
            logStudySession(userId, earnedMinutes)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedFlashcards(
        userId: String,
        materialId: String,
        deckTitle: String,
        cards: List<GeneratedFlashcard>
    ): Result<Int> {
        val basePath = "users/$userId/flashcards"
        return try {
            val uid = requireUserId(userId)
            val safeMaterialId = materialId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
            val safeDeckTitle = deckTitle.trim().ifBlank { "Study Flashcards" }.take(140)
            val batch = db.batch()
            var count = 0
            for (card in cards) {
                val safeFront = card.front.trim().take(500)
                val safeBack = card.back.trim().take(1500)
                if (safeFront.isEmpty() || safeBack.isEmpty()) continue
                val id = "fc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val docRef = db.document("$basePath/$id")
                val payload = mapOf(
                    "userId" to uid,
                    "materialId" to safeMaterialId,
                    "deckTitle" to safeDeckTitle,
                    "front" to safeFront,
                    "back" to safeBack,
                    "hint" to card.hint.trim().take(300),
                    "mastered" to false,
                    "reviewCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
                count++
            }
            require(count > 0) { "No valid flashcards to save." }
            batch.commit().await()
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    suspend fun addCustomFlashcard(
        userId: String,
        materialId: String,
        deckTitle: String,
        front: String,
        back: String,
        hint: String
    ): Result<Unit> {
        val id = "fc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/flashcards/$id"
        return try {
            val uid = requireUserId(userId)
            val safeFront = front.trim().take(500)
            val safeBack = back.trim().take(1500)
            require(safeFront.isNotEmpty() && safeBack.isNotEmpty()) {
                "Front prompt and back answer cannot be empty."
            }
            val safeMaterialId = materialId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
            val safeDeckTitle = deckTitle.trim().ifBlank { "Custom Flashcards" }.take(140)
            val payload = mapOf(
                "userId" to uid,
                "materialId" to safeMaterialId,
                "deckTitle" to safeDeckTitle,
                "front" to safeFront,
                "back" to safeBack,
                "hint" to hint.trim().take(300),
                "mastered" to false,
                "reviewCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFlashcardMastery(
        userId: String,
        cardId: String,
        mastered: Boolean,
        currentReviewCount: Long
    ): Result<Unit> {
        val path = "users/$userId/flashcards/$cardId"
        return try {
            requireUserId(userId)
            require(cardId.isNotBlank())
            val nextReviews = (currentReviewCount + 1L).coerceIn(0L, 100000L)
            db.document(path).update(
                mapOf(
                    "mastered" to mastered,
                    "reviewCount" to nextReviews,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFlashcard(userId: String, cardId: String): Result<Unit> {
        val path = "users/$userId/flashcards/$cardId"
        return try {
            requireUserId(userId)
            require(cardId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
