package com.sadtaz.workout

// ── EXERCISE TYPES ───────────────────────────────────────────
enum class ExerciseType { NORMAL, PLANK, CARDIO }

fun getExerciseType(name: String): ExerciseType = when (name) {
    "Plank"  -> ExerciseType.PLANK
    "Cardio" -> ExerciseType.CARDIO
    else     -> ExerciseType.NORMAL
}

// ── WORKOUT PLAN ─────────────────────────────────────────────
data class WorkoutDay(
    val dayCode: String,
    val dayLabel: String,
    val muscleGroup: String,
    val exercises: List<String>
)

object WorkoutPlan {
    val days = listOf(
        WorkoutDay("MON", "Monday", "Chest", listOf(
            "Bench Press", "Incline Bench Press", "Pec Fly", "Cable Crossover"
        )),
        WorkoutDay("TUE", "Tuesday", "Back", listOf(
            "Lat Pulldown", "T-Bar Row", "Single Hand Dumbbell Row"
        )),
        WorkoutDay("WED", "Wednesday", "Legs", listOf(
            "Squat", "Leg Press", "Leg Extension", "Leg Curl", "Calf Raises"
        )),
        WorkoutDay("THU", "Thursday", "Shoulders", listOf(
            "Dumbbell Press", "Cable Side Lateral", "Cable Rear Delt Pull"
        )),
        WorkoutDay("FRI", "Friday", "Biceps & Triceps", listOf(
            "Dumbbell Curl", "Preacher Curl", "Hammer Curl",
            "Overhead Tricep Extension", "Cable Pushdown"
        )),
        WorkoutDay("SAT", "Saturday", "Cardio & Abs", listOf(
            "Abs Crunches", "Plank", "Cardio"
        ))
    )

    fun forDayCode(code: String) = days.find { it.dayCode == code }
}

// ── SET INPUT DATA CLASS ─────────────────────────────────────
data class SetInput(
    val setNumber: Int,
    var reps: Int = 0,
    var weightKg: Float = 0f,
    var minutes: Float = 0f,
    var speedKmh: Float = 0f
)

// ── REPOSITORY ───────────────────────────────────────────────
class WorkoutRepository(private val dao: WorkoutDao) {

    fun getAllSets()               = dao.getAllSets()
    fun getSetsForDate(date: String) = dao.getSetsForDate(date)
    fun getAllWorkoutDates()       = dao.getAllWorkoutDates()
    fun getTotalWorkoutDays()      = dao.getTotalWorkoutDays()
    fun getTotalSets()             = dao.getTotalSets()
    fun getHistoryForExercise(ex: String) = dao.getHistoryForExercise(ex)
    fun getMaxWeight(ex: String)   = dao.getMaxWeight(ex)
    fun getMaxMinutes(ex: String)  = dao.getMaxMinutes(ex)

    suspend fun getSetsForExerciseAndDate(exercise: String, date: String) =
        dao.getSetsForExerciseAndDate(exercise, date)

    suspend fun saveSets(
        exercise: String,
        muscle: String,
        dayCode: String,
        date: String,
        sets: List<SetInput>
    ) {
        dao.deleteExerciseForDate(exercise, date)
        val entities = sets.map { s ->
            WorkoutSet(
                exerciseName = exercise,
                muscleGroup  = muscle,
                dayOfWeek    = dayCode,
                setNumber    = s.setNumber,
                loggedDate   = date,
                reps         = s.reps,
                weightKg     = s.weightKg,
                minutes      = s.minutes,
                speedKmh     = s.speedKmh
            )
        }
        dao.insertSets(entities)
    }
}
