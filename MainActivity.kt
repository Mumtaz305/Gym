package com.sadtaz.workout

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var selectedDayIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val tvTitle   = findViewById<TextView>(R.id.tvDayTitle)
        val tvMuscle  = findViewById<TextView>(R.id.tvMuscle)
        val tvDate    = findViewById<TextView>(R.id.tvDate)
        val recycler  = findViewById<RecyclerView>(R.id.recyclerView)

        tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        // Auto-select today's tab
        val todayCode = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> "MON"
            Calendar.TUESDAY   -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY  -> "THU"
            Calendar.FRIDAY    -> "FRI"
            Calendar.SATURDAY  -> "SAT"
            else               -> "MON"
        }
        selectedDayIndex = WorkoutPlan.days.indexOfFirst { it.dayCode == todayCode }.coerceAtLeast(0)
        WorkoutPlan.days.forEach { tabLayout.addTab(tabLayout.newTab().setText(it.dayCode)) }
        tabLayout.getTabAt(selectedDayIndex)?.select()

        fun loadDay(index: Int) {
            val day = WorkoutPlan.days[index]
            tvTitle.text  = day.dayLabel
            tvMuscle.text = day.muscleGroup
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = ExerciseAdapter(day) { ex ->
                startActivity(Intent(this, LogWorkoutActivity::class.java).apply {
                    putExtra("exercise", ex)
                    putExtra("muscle",   day.muscleGroup)
                    putExtra("dayCode",  day.dayCode)
                })
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab) { selectedDayIndex = t.position; loadDay(selectedDayIndex) }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })
        loadDay(selectedDayIndex)

        findViewById<View>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.btnGraph).setOnClickListener {
            startActivity(Intent(this, GraphActivity::class.java))
        }
    }
}

class ExerciseAdapter(
    private val day: WorkoutDay,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvExerciseName)
        val tvSub:  TextView = v.findViewById(R.id.tvTapToLog)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_exercise, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val ex = day.exercises[pos]
        h.tvName.text = ex
        h.tvSub.text  = when (getExerciseType(ex)) {
            ExerciseType.PLANK  -> "Sets + Minutes"
            ExerciseType.CARDIO -> "Sets + Minutes + Speed"
            else                -> "Reps + Weight (kg)"
        }
        h.itemView.setOnClickListener { onClick(ex) }
    }

    override fun getItemCount() = day.exercises.size
}
