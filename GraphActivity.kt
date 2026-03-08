package com.sadtaz.workout

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GraphActivity : AppCompatActivity() {

    private val repo by lazy { WorkoutRepository(WorkoutDatabase.getInstance(this).workoutDao()) }
    private val allExercises = WorkoutPlan.days.flatMap { it.exercises }
    private var selectedExercise = ""
    private var chart: LineChart? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        selectedExercise = allExercises.first()
        chart = findViewById(R.id.lineChart)
        styleChart()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val spinner = findViewById<Spinner>(R.id.spinnerExercise)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allExercises)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                selectedExercise = allExercises[pos]
                loadGraph()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        loadPersonalRecords()
    }

    private fun styleChart() {
        chart?.apply {
            setBackgroundColor(Color.parseColor("#0D0D0D"))
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#666666")
                gridColor = Color.parseColor("#1E1E1E")
                axisLineColor = Color.parseColor("#1E1E1E")
                granularity = 1f
                setDrawAxisLine(true)
            }
            axisLeft.apply {
                textColor = Color.parseColor("#666666")
                gridColor = Color.parseColor("#1E1E1E")
                axisLineColor = Color.parseColor("#1E1E1E")
            }
            axisRight.isEnabled = false
        }
    }

    private fun loadGraph() {
        val tvEmpty = findViewById<TextView>(R.id.tvChartEmpty)
        val type = getExerciseType(selectedExercise)

        lifecycleScope.launch {
            repo.getHistoryForExercise(selectedExercise).collectLatest { sets ->
                if (sets.isEmpty()) {
                    chart?.visibility = View.GONE
                    tvEmpty?.visibility = View.VISIBLE
                    return@collectLatest
                }

                val byDate = sets.groupBy { it.loggedDate }.entries.sortedBy { it.key }
                val labels = byDate.map { it.key.substring(5).replace("-", "/") }
                val entries = byDate.mapIndexed { i, (_, daySets) ->
                    val v = when (type) {
                        ExerciseType.PLANK, ExerciseType.CARDIO -> daySets.maxOf { it.minutes }
                        else -> daySets.maxOf { it.weightKg }
                    }
                    Entry(i.toFloat(), v)
                }

                val accentColor = Color.parseColor("#E8FF00")
                val dataSet = LineDataSet(entries, "").apply {
                    color = accentColor
                    setCircleColor(accentColor)
                    circleRadius = 4f
                    circleHoleRadius = 2f
                    lineWidth = 2.5f
                    valueTextColor = Color.parseColor("#888888")
                    valueTextSize = 9f
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#E8FF00")
                    fillAlpha = 20
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawHighlightIndicators(true)
                    highLightColor = Color.parseColor("#E8FF00")
                }

                chart?.apply {
                    visibility = View.VISIBLE
                    tvEmpty?.visibility = View.GONE
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    data = LineData(dataSet)
                    animateX(600)
                    invalidate()
                }
            }
        }
    }

    private fun loadPersonalRecords() {
        val container = findViewById<LinearLayout>(R.id.prContainer)

        WorkoutPlan.days.flatMap { it.exercises }.forEach { ex ->
            val type = getExerciseType(ex)
            lifecycleScope.launch {
                val flow = if (type == ExerciseType.NORMAL) repo.getMaxWeight(ex) else repo.getMaxMinutes(ex)
                flow.collectLatest { value ->
                    if (value != null && value > 0f) {
                        runOnUiThread {
                            if (container.findViewWithTag<View>(ex) == null) {
                                val row = layoutInflater.inflate(R.layout.item_pr_row, container, false)
                                row.tag = ex
                                val unit = if (type == ExerciseType.NORMAL) "kg" else "min"
                                row.findViewById<TextView>(R.id.tvPrName).text  = ex
                                row.findViewById<TextView>(R.id.tvPrValue).text = "$value $unit"
                                container.addView(row)
                            } else {
                                val row = container.findViewWithTag<View>(ex)
                                val unit = if (type == ExerciseType.NORMAL) "kg" else "min"
                                row?.findViewById<TextView>(R.id.tvPrValue)?.text = "$value $unit"
                            }
                        }
                    }
                }
            }
        }
    }
}
