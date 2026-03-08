package com.sadtaz.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogWorkoutActivity : AppCompatActivity() {

    private lateinit var repo: WorkoutRepository
    private lateinit var exercise: String
    private lateinit var muscle: String
    private lateinit var dayCode: String
    private lateinit var todayDate: String
    private lateinit var exType: ExerciseType
    private val setInputs = mutableListOf<SetInput>()
    private lateinit var setsAdapter: SetsInputAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_workout)

        exercise  = intent.getStringExtra("exercise") ?: run { finish(); return }
        muscle    = intent.getStringExtra("muscle") ?: ""
        dayCode   = intent.getStringExtra("dayCode") ?: ""
        exType    = getExerciseType(exercise)
        todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        repo      = WorkoutRepository(WorkoutDatabase.getInstance(this).workoutDao())

        findViewById<TextView>(R.id.tvExerciseTitle).text = exercise
        findViewById<TextView>(R.id.tvMuscleLabel).text   = muscle
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val tvH1 = findViewById<TextView>(R.id.tvColA)
        val tvH2 = findViewById<TextView>(R.id.tvColB)
        val tvH3 = findViewById<TextView>(R.id.tvColC)
        when (exType) {
            ExerciseType.PLANK  -> { tvH1.text = "SET"; tvH2.text = "MINUTES"; tvH3.visibility = View.GONE }
            ExerciseType.CARDIO -> { tvH1.text = "SET"; tvH2.text = "MINUTES"; tvH3.text = "SPEED (km/h)"; tvH3.visibility = View.VISIBLE }
            else                -> { tvH1.text = "SET"; tvH2.text = "REPS";    tvH3.text = "WEIGHT (kg)";  tvH3.visibility = View.VISIBLE }
        }

        val rv = findViewById<RecyclerView>(R.id.rvSets)
        setsAdapter = SetsInputAdapter(setInputs, exType)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = setsAdapter

        lifecycleScope.launch {
            val existing = repo.getSetsForExerciseAndDate(exercise, todayDate)
            setInputs.clear()
            if (existing.isNotEmpty()) {
                existing.forEach { setInputs.add(SetInput(it.setNumber, it.reps, it.weightKg, it.minutes, it.speedKmh)) }
            } else {
                setInputs.add(SetInput(1))
            }
            setsAdapter.notifyDataSetChanged()
        }

        findViewById<Button>(R.id.btnAddSet).setOnClickListener {
            setInputs.add(SetInput(setInputs.size + 1))
            setsAdapter.notifyItemInserted(setInputs.size - 1)
            rv.smoothScrollToPosition(setInputs.size - 1)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            lifecycleScope.launch {
                val toSave = setInputs.filterIndexed { _, s ->
                    when (exType) {
                        ExerciseType.PLANK, ExerciseType.CARDIO -> s.minutes > 0f
                        else -> s.reps > 0 || s.weightKg > 0f
                    }
                }.mapIndexed { i, s -> s.copy(setNumber = i + 1) }

                if (toSave.isEmpty()) {
                    Toast.makeText(this@LogWorkoutActivity, "Kuch values enter karo pehle!", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                repo.saveSets(exercise, muscle, dayCode, todayDate, toSave)
                Toast.makeText(this@LogWorkoutActivity, "Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

class SetsInputAdapter(
    private val sets: MutableList<SetInput>,
    private val type: ExerciseType
) : RecyclerView.Adapter<SetsInputAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum:  TextView  = v.findViewById(R.id.tvSetNum)
        val etA:    EditText  = v.findViewById(R.id.etFieldA)
        val etB:    EditText  = v.findViewById(R.id.etFieldB)
        val etC:    EditText  = v.findViewById(R.id.etFieldC)
        val btnDel: ImageView = v.findViewById(R.id.btnDelSet)
        var watcherA: TextWatcher? = null
        var watcherB: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_set_row, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = sets[position]
        holder.tvNum.text = "${position + 1}"

        holder.etA.removeTextChangedListener(holder.watcherA)
        holder.etB.removeTextChangedListener(holder.watcherB)

        when (type) {
            ExerciseType.PLANK -> {
                holder.etA.hint = "Minutes"
                holder.etA.setText(if (s.minutes > 0f) s.minutes.toString() else "")
                holder.etB.visibility = View.GONE
                holder.etC.visibility = View.GONE
            }
            ExerciseType.CARDIO -> {
                holder.etA.hint = "Minutes"
                holder.etB.hint = "Speed (km/h)"
                holder.etA.setText(if (s.minutes > 0f) s.minutes.toString() else "")
                holder.etB.setText(if (s.speedKmh > 0f) s.speedKmh.toString() else "")
                holder.etB.visibility = View.VISIBLE
                holder.etC.visibility = View.GONE
            }
            else -> {
                holder.etA.hint = "Reps"
                holder.etB.hint = "Weight (kg)"
                holder.etA.setText(if (s.reps > 0) s.reps.toString() else "")
                holder.etB.setText(if (s.weightKg > 0f) s.weightKg.toString() else "")
                holder.etB.visibility = View.VISIBLE
                holder.etC.visibility = View.GONE
            }
        }

        holder.watcherA = watcher { text ->
            val idx = holder.adapterPosition; if (idx < 0) return@watcher
            when (type) {
                ExerciseType.PLANK, ExerciseType.CARDIO -> sets[idx].minutes  = text.toFloatOrNull() ?: 0f
                else                                    -> sets[idx].reps     = text.toIntOrNull()   ?: 0
            }
        }
        holder.watcherB = watcher { text ->
            val idx = holder.adapterPosition; if (idx < 0) return@watcher
            when (type) {
                ExerciseType.CARDIO -> sets[idx].speedKmh = text.toFloatOrNull() ?: 0f
                else                -> sets[idx].weightKg = text.toFloatOrNull() ?: 0f
            }
        }
        holder.etA.addTextChangedListener(holder.watcherA)
        holder.etB.addTextChangedListener(holder.watcherB)

        holder.btnDel.setOnClickListener {
            val idx = holder.adapterPosition
            if (idx >= 0 && sets.size > 1) { sets.removeAt(idx); notifyItemRemoved(idx); notifyItemRangeChanged(idx, sets.size) }
        }
    }

    override fun getItemCount() = sets.size

    private fun watcher(cb: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) { cb(s?.toString().orEmpty()) }
    }
}
