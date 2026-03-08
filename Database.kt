package com.sadtaz.workout

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── ENTITY ───────────────────────────────────────────────────
@Entity(tableName = "workout_sets")
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val muscleGroup: String,
    val dayOfWeek: String,
    val setNumber: Int,
    val loggedDate: String,          // "2026-03-07"
    val timestamp: Long = System.currentTimeMillis(),

    // Regular exercises
    val reps: Int = 0,
    val weightKg: Float = 0f,

    // Plank
    val minutes: Float = 0f,

    // Cardio extra
    val speedKmh: Float = 0f
)

// ── DAO ──────────────────────────────────────────────────────
@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Query("DELETE FROM workout_sets WHERE exerciseName = :exercise AND loggedDate = :date")
    suspend fun deleteExerciseForDate(exercise: String, date: String)

    @Query("SELECT * FROM workout_sets WHERE loggedDate = :date ORDER BY exerciseName, setNumber")
    fun getSetsForDate(date: String): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE exerciseName = :exercise ORDER BY loggedDate ASC")
    fun getHistoryForExercise(exercise: String): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets ORDER BY timestamp DESC")
    fun getAllSets(): Flow<List<WorkoutSet>>

    @Query("SELECT DISTINCT loggedDate FROM workout_sets ORDER BY loggedDate DESC")
    fun getAllWorkoutDates(): Flow<List<String>>

    @Query("SELECT * FROM workout_sets WHERE loggedDate = :date AND exerciseName = :exercise ORDER BY setNumber")
    suspend fun getSetsForExerciseAndDate(exercise: String, date: String): List<WorkoutSet>

    @Query("SELECT COUNT(DISTINCT loggedDate) FROM workout_sets")
    fun getTotalWorkoutDays(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_sets")
    fun getTotalSets(): Flow<Int>

    @Query("SELECT MAX(weightKg) FROM workout_sets WHERE exerciseName = :exercise AND weightKg > 0")
    fun getMaxWeight(exercise: String): Flow<Float?>

    @Query("SELECT MAX(minutes) FROM workout_sets WHERE exerciseName = :exercise AND minutes > 0")
    fun getMaxMinutes(exercise: String): Flow<Float?>
}

// ── DATABASE ─────────────────────────────────────────────────
@Database(entities = [WorkoutSet::class], version = 1, exportSchema = false)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var INSTANCE: WorkoutDatabase? = null
        fun getInstance(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "sadtaz_workout_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
