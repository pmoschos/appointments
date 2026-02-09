package com.ai.appointments.calender


import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.ai.appointments.R

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GridAdapter(
    context: Context?,
    private val monthlyDates: List<Date>,
    private val currentDate: Calendar,
    var allEvents: MutableList<EventObjects>,
    private var color_date: Date?,
    var colorFulDates: MutableList<ColoredDate>
) : ArrayAdapter<Any?>(context!!, R.layout.calendarview_cell) {

    companion object {
        private val TAG = GridAdapter::class.java.simpleName
    }

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val sdfDmy = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    //customizations
    private var todayIndicator: Drawable? = null
    private var selectedIndicator: Drawable? = null
    private var eventIndicator: Drawable? = null

    private var dateColor = 0
    private var nonMonthDateColor = 0
    private var todayDateColor = 0
    private var selectedDateColor = 0

    private var dateFontFace: Typeface? = null
    private var dateTextStyle = 0
    private var calendarBackgroundColor = 0

    fun setDrawables(todayIndicator: Drawable?, selectedIndicator: Drawable?, eventIndicator: Drawable?) {
        this.todayIndicator = todayIndicator
        this.selectedIndicator = selectedIndicator
        this.eventIndicator = eventIndicator
    }

    fun setTextColors(dateColor: Int, nonMonthDateColor: Int, todayDateColor: Int, selectedDateColor: Int) {
        this.dateColor = dateColor
        this.nonMonthDateColor = nonMonthDateColor
        this.todayDateColor = todayDateColor
        this.selectedDateColor = selectedDateColor
    }

    fun setFontProperties(fontFace: Typeface?, textStyle: Int) {
        dateFontFace = fontFace
        dateTextStyle = textStyle
    }

    fun setCalendarBackgroundColor(backgroundColor: Int) {
        calendarBackgroundColor = backgroundColor
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val mDate = monthlyDates[position]

        val dateCal = Calendar.getInstance()
        dateCal.time = mDate

        val dayValue = dateCal.get(Calendar.DAY_OF_MONTH)
        val displayMonth = dateCal.get(Calendar.MONTH) + 1
        val displayYear = dateCal.get(Calendar.YEAR)

        val currentMonth = currentDate.get(Calendar.MONTH) + 1
        val currentYear = currentDate.get(Calendar.YEAR)

        val today_date = Calendar.getInstance()
        val toYear = today_date.get(Calendar.YEAR)
        val toMonth = today_date.get(Calendar.MONTH) + 1
        val toDay = today_date.get(Calendar.DATE)

        val colorday = color_date?.date
        val colorMonth = color_date?.month?.plus(1)
        val coloryear = color_date?.year

        var view = convertView

        if (view == null) {
            view = mInflater.inflate(R.layout.calendarview_cell, parent, false)
            val llParent = view.findViewById<LinearLayout>(R.id.ll_parent)
            val clRoot = view.findViewById<ConstraintLayout>(R.id.cl_root)
            llParent.setBackgroundColor(calendarBackgroundColor)
            clRoot.setBackgroundColor(calendarBackgroundColor)
        }

        val llParent = view!!.findViewById<LinearLayout>(R.id.ll_parent)
        val clRoot = view.findViewById<ConstraintLayout>(R.id.cl_root)
        val cellNumber = view.findViewById<TextView>(R.id.calendar_date_id)

        //set font family
        if (dateTextStyle != 0) cellNumber.setTextAppearance(context, dateTextStyle)
        if (dateFontFace != null) cellNumber.typeface = dateFontFace

        //set background
        llParent.setBackgroundColor(calendarBackgroundColor)
        clRoot.setBackgroundColor(calendarBackgroundColor)

        if (displayMonth == currentMonth && displayYear == currentYear) {
            cellNumber.setTextColor(if (dateColor != 0) dateColor else Color.BLACK)
            cellNumber.tag = 0
        } else {
            if (displayMonth > currentMonth && displayYear == currentYear ||
                (displayMonth < currentMonth && displayYear > currentYear)
            ) {
                cellNumber.tag = 1
            } else {
                cellNumber.tag = 2
            }
            cellNumber.setTextColor(if (nonMonthDateColor != 0) nonMonthDateColor else Color.LTGRAY)
        }

        //Add day to calendar
        cellNumber.text = dayValue.toString()

        if (displayMonth == toMonth && displayYear == toYear && dayValue == toDay) {
            if (todayIndicator != null) {
                llParent.background = todayIndicator
            } else {
                llParent.setBackgroundResource(R.drawable.calendarview_today)
            }

            cellNumber.tag = -1
            if (todayDateColor != 0) cellNumber.setTextColor(todayDateColor)
        }

        //set custom date color before set the selected color
        val customDateColor = getDateColor(mDate)
        if (customDateColor != 0) {
            cellNumber.setTextColor(customDateColor)
            // llParent.setBackgroundResource(R.drawable.round_bg_gray)
        }

        if (displayMonth == colorMonth && colorday == dayValue) {
            if (selectedIndicator != null) {
                llParent.setBackgroundResource(R.drawable.round_bg_blue)
            } else {
                llParent.setBackgroundResource(R.drawable.round_bg_blue)
            }
            cellNumber.setTextColor(if (selectedDateColor != 0) selectedDateColor else Color.WHITE)
        }

        view.tag = position

        //Add events to the calendar
        val tvEventIndicator = view.findViewById<TextView>(R.id.event_id)
        val eventCalendar = Calendar.getInstance()

        for (i in allEvents.indices) {
            eventCalendar.time = allEvents[i].date
            if (dayValue == eventCalendar.get(Calendar.DAY_OF_MONTH) &&
                displayMonth == eventCalendar.get(Calendar.MONTH) + 1 &&
                displayYear == eventCalendar.get(Calendar.YEAR)
            ) {
                if (eventIndicator != null) {
                    tvEventIndicator.background = eventIndicator
                } else {
                    tvEventIndicator.setBackgroundResource(R.drawable.calendarview_event)
                }
            }
        }

        return view
    }

    override fun getCount(): Int {
        return monthlyDates.size
    }

    override fun getItem(position: Int): Any? {
        return monthlyDates[position]
    }

    override fun getPosition(item: Any?): Int {
        return monthlyDates.indexOf(item)
    }

    fun getDateColor(mDate: Date): Int {
        for (i in colorFulDates.indices) {
            if (sdfDmy.format(mDate) == sdfDmy.format(colorFulDates[i].date)) {
                return colorFulDates[i].color
            }
        }
        return 0
    }
}
