package com.subhajitrajak.durare.data.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.subhajitrajak.durare.data.models.DailyPushStats
import com.subhajitrajak.durare.data.models.DailyWorkoutStats
import com.subhajitrajak.durare.utils.Constants

class StatsRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun saveOrAccumulateDaily(uid: String, date: String, stats: DailyPushStats): Task<Void> {
        val userDoc = db.collection(Constants.USERS).document(uid)
        val docRef = userDoc.collection(Constants.DAILY_PUSHUP_STATS).document(date)

        return db.runTransaction { txn ->
            // Read everything first
            val dailySnap = txn.get(docRef)
            val userSnap = txn.get(userDoc)
            var pushupIncrement: Int

            // Handle daily stats
            if (dailySnap.exists()) {
                val existing = dailySnap.toObject(DailyPushStats::class.java)
                val merged = existing?.copy(
                    totalReps = existing.totalReps + stats.totalReps,
                    totalPushups = existing.totalPushups + stats.totalPushups,
                    totalActiveTimeMs = existing.totalActiveTimeMs + stats.totalActiveTimeMs,
                    averagePushDurationMs = if (existing.totalPushups + stats.totalPushups > 0)
                        ((existing.averagePushDurationMs * existing.totalPushups) + (stats.averagePushDurationMs * stats.totalPushups)) / (existing.totalPushups + stats.totalPushups)
                    else 0L,
                    totalRestTimeMs = existing.totalRestTimeMs + stats.totalRestTimeMs
                ) ?: stats
                txn.set(docRef, merged)
                pushupIncrement = merged.totalPushups - existing!!.totalPushups
            } else {
                txn.set(docRef, stats)
                pushupIncrement = stats.totalPushups
            }

            // Handle lifetime stats
            if (userSnap.exists()) {
                txn.update(userDoc, Constants.LIFETIME_TOTAL_PUSHUPS, FieldValue.increment(pushupIncrement.toLong()))
            } else {
                txn.set(userDoc, mapOf(Constants.LIFETIME_TOTAL_PUSHUPS to pushupIncrement))
            }

            null
        }
    }

    fun saveDailyChinUpStats(uid: String, date: String, stats: DailyWorkoutStats): Task<Void> {
        val userDoc = db.collection(Constants.USERS).document(uid)
        val docRef = userDoc.collection(Constants.DAILY_CHIN_UP_STATS).document(date)

        return db.runTransaction { txn ->
            // Read everything first
            val dailySnap = txn.get(docRef)
            val userSnap = txn.get(userDoc)
            var repIncrement: Int

            // Handle daily stats
            if (dailySnap.exists()) {
                val existing = dailySnap.toObject(DailyWorkoutStats::class.java)
                val merged = existing?.copy(
                    sets = existing.sets + stats.sets,
                    reps = existing.reps + stats.reps,
                    activeTimeMs = existing.activeTimeMs + stats.activeTimeMs,
                    averageRepDurationMs = if (existing.reps + stats.reps > 0)
                        ((existing.averageRepDurationMs * existing.reps) + (stats.averageRepDurationMs * stats.reps)) / (existing.reps + stats.reps)
                    else 0L,
                    restTimeMs = existing.restTimeMs + stats.restTimeMs
                ) ?: stats
                txn.set(docRef, merged)
                repIncrement = merged.reps - existing!!.reps
            } else {
                txn.set(docRef, stats)
                repIncrement = stats.reps
            }

            // Handle lifetime stats
            if (userSnap.exists()) {
                txn.update(userDoc, Constants.LIFETIME_TOTAL_CHIN_UPS, FieldValue.increment(repIncrement.toLong()))
            } else {
                txn.set(userDoc, mapOf(Constants.LIFETIME_TOTAL_CHIN_UPS to repIncrement))
            }

            null
        }
    }
}