package com.sadtaz.workout

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val repo = WorkoutRepository(WorkoutDatabase.getInstance(this).workoutDao())
        val rv   = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            repo.getAllSets().collectLatest { allSets ->
                val items = mutableListOf<HistoryItem>()

                if (allSets.isEmpty()) {
                    items.add(HistoryItem.Header("Abhi koi workout log nahi hai"))
                    rv.adapter = HistoryAdapter(items)
                    return@collectLatest
                }

                allSets
                    .groupBy { it.loggedDate }
                    .entries
                    .sortedByDescending { it.key }
                    .forEach { (date, daySets) ->
                        items.add(HistoryItem.Header(date))
                        daySets.groupBy { it.exerciseName }.forEach { (ex, exSets) ->
                            val type = getExerciseType(ex)
                            val detail = when (type) {
                                ExerciseType.PLANK  -> "${exSets.size} sets • ${exSets.maxOf { it.minutes }} min max"
                                ExerciseType.CARDIO -> "${exSets.size} sets • ${exSets.maxOf { it.minutes }} min • ${exSets.maxOf { it.speedKmh }} km/h"
                                else                -> "${exSets.size} sets • ${exSets.maxOf { it.weightKg }} kg max"
                            }
                            items.add(HistoryItem.Entry(ex, exSets.first().muscleGroup, detail))
                        }
                    }

                rv.adapter = HistoryAdapter(items)
            }
        }
    }
}

sealed class HistoryItem {
    data class Header(val date: String) : HistoryItem()
    data class Entry(val exercise: String, val muscle: String, val detail: String) : HistoryItem()
}

class HistoryAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(pos: Int) = if (items[pos] is HistoryItem.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == 0) R.layout.item_history_date else R.layout.item_history_entry
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return object : RecyclerView.ViewHolder(v) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = items[pos]) {
            is HistoryItem.Header -> holder.itemView.findViewById<TextView>(R.id.tvDate).text   = item.date
            is HistoryItem.Entry  -> {
                holder.itemView.findViewById<TextView>(R.id.tvExName).text  = item.exercise
                holder.itemView.findViewById<TextView>(R.id.tvDetail).text  = "${item.muscle} • ${item.detail}"
            }
        }
    }

    override fun getItemCount() = items.size
}
