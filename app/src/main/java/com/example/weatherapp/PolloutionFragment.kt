package com.example.weatherapp

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.weatherapp.databinding.FragmentPolloutionBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class PolloutionFragment : Fragment() {
    private lateinit var binding: FragmentPolloutionBinding
    val list = arrayListOf<BarEntry>()
    val txtBuilder = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_polloution, container, false)
        val data = arguments
        val pollutants = listOf(
            "co" to "CO",
            "nh3" to "NH3",
            "no" to "NO",
            "no2" to "NO2",
            "o3" to "O3",
            "pm10" to "PM10",
            "pm2_5" to "PM2_5",
            "so2" to "SO2"
        )

        pollutants.forEachIndexed { index, (key, label) ->
            val value = data?.getDouble(key)
            if (value != null) {
                list.add(BarEntry((index + 1).toFloat(), value.toFloat()))
            }
            txtBuilder.append("$label: ${value ?: "-"}\n")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barData = BarDataSet(list, "pollutants")
        barData.setColors(ColorTemplate.VORDIPLOM_COLORS, 255)
        barData.valueTextColor = Color.BLACK
        barData.barBorderColor = Color.BLACK
        barData.barBorderWidth = 1f
        val barDataSet = BarData(barData)
        val quarters= arrayOf("","CO","NH3","NO","NO2","O3","PM10","PM2_5","SO2")
        val formatter :ValueFormatter=object :ValueFormatter(){
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return quarters[value.toInt()]
            }
        }
        binding.apply {
            text1.text = txtBuilder
            barChart.apply {
              data=barDataSet
              description.text="Air Pollutants"
                animateY(1000)
                xAxis.valueFormatter=formatter
            }
        }
    }


}