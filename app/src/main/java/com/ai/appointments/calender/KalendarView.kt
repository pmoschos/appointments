package com.ai.appointments.calender


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.GridLayoutAnimationController
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.ai.appointments.R

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KalendarView : LinearLayout {

    companion object {
        private val TAG = KalendarView::class.java.simpleName
        private const val MAX_CALENDAR_COLUMN = 42
    }

    private var previousButton: ImageView? = null
    private var nextButton: ImageView? = null
    private var currentDate: TextView? = null
    var calendarGridView: GridView? = null
    private var addEventButton: Button? = null
    private var mYearChangeListener: YearChangeListener? = null

    private var month = 0
    private var year = 0
    private var formatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private var cal = Calendar.getInstance(Locale.ENGLISH)
    private var context: Context? = null
    private var mAdapter: GridAdapter? = null

    private var prev = -1
    private var pos = 0
    private var cr_pos = -2

    private var dayValueInCells: MutableList<Date> = ArrayList()
    private var mEvents: MutableList<EventObjects> = ArrayList()
    private var selected: Date? = null

    private var today_date = Calendar.getInstance()
    private var color_date: Date? = null

    private var mDateSelector: DateSelector? = null
    private var mMonthChanger: MonthChanger? = null
    private var colorFulDates: MutableList<ColoredDate> = ArrayList()

    //customizations
    private var todayIndicator: Drawable? = null
    private var selectedIndicator: Drawable? = null
    private var eventIndicator: Drawable? = null

    private var dateColor = 0
    private var nonMonthDateColor = 0
    private var todayDateColor = 0
    private var selectedDateColor = 0

    private var monthFontFace: Typeface? = null
    private var weekFontFace: Typeface? = null
    private var dateFontFace: Typeface? = null

    private var monthTextStyle = 0
    private var weekTextStyle = 0
    private var dateTextStyle = 0

    private var nextIcon: Drawable? = null
    private var prevIcon: Drawable? = null

    private var calendarBackgroundColor = 0
    private var animatingMonths = true
    private var animationController: GridLayoutAnimationController? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.context = context

        cal = getZeroTime(cal)
        today_date = getZeroTime(today_date)

        color_date = today_date.time
        val tempTomorrow = Calendar.getInstance()
        tempTomorrow.time = today_date.time
        tempTomorrow.add(Calendar.DATE, 1)
        color_date = tempTomorrow.time

        todayIndicator = AppCompatResources.getDrawable(context, R.drawable.calendarview_today)
        selectedIndicator = AppCompatResources.getDrawable(context, R.drawable.calendarview_select_date)
        eventIndicator = AppCompatResources.getDrawable(context, R.drawable.calendarview_event)

        dateColor = Color.BLACK
        nonMonthDateColor = Color.LTGRAY
        todayDateColor = Color.BLACK
        selectedDateColor = Color.WHITE
        calendarBackgroundColor = Color.WHITE

        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.KalendarView, 0, 0
        )

        val drwTodayId = typedArray.getResourceId(R.styleable.KalendarView_todayIndicator, 0)
        if (drwTodayId != 0) todayIndicator = AppCompatResources.getDrawable(context, drwTodayId)

        val drwSelectedId = typedArray.getResourceId(R.styleable.KalendarView_selectedIndicator, 0)
        if (drwSelectedId != 0) selectedIndicator = AppCompatResources.getDrawable(context, drwSelectedId)

        val drwEventId = typedArray.getResourceId(R.styleable.KalendarView_eventIndicator, 0)
        if (drwEventId != 0) eventIndicator = AppCompatResources.getDrawable(context, drwEventId)

        val colorDate = typedArray.getColor(R.styleable.KalendarView_dateColor, 0)
        if (colorDate != 0) dateColor = colorDate

        val colorNonMonth = typedArray.getColor(R.styleable.KalendarView_nonMonthDateColor, 0)
        if (colorNonMonth != 0) nonMonthDateColor = colorNonMonth

        val colorToday = typedArray.getColor(R.styleable.KalendarView_todayDateColor, 0)
        if (colorToday != 0) todayDateColor = colorToday

        val colorSelected = typedArray.getColor(R.styleable.KalendarView_selectedDateColor, 0)
        if (colorSelected != 0) selectedDateColor = colorSelected

        val monthFontId = typedArray.getResourceId(R.styleable.KalendarView_monthFontFamily, 0)
        if (monthFontId != 0) monthFontFace = ResourcesCompat.getFont(context, monthFontId)

        val weekFontId = typedArray.getResourceId(R.styleable.KalendarView_weekFontFamily, 0)
        if (weekFontId != 0) weekFontFace = ResourcesCompat.getFont(context, weekFontId)

        val dateFontId = typedArray.getResourceId(R.styleable.KalendarView_dateFontFamily, 0)
        if (dateFontId != 0) dateFontFace = ResourcesCompat.getFont(context, dateFontId)

        monthTextStyle = typedArray.getResourceId(R.styleable.KalendarView_monthTextStyle, 0)
        weekTextStyle = typedArray.getResourceId(R.styleable.KalendarView_weekTextStyle, 0)
        dateTextStyle = typedArray.getResourceId(R.styleable.KalendarView_dateTextStyle, 0)

        val tempNextIcon = typedArray.getResourceId(R.styleable.KalendarView_nextIcon, 0)
        if (tempNextIcon != 0) nextIcon = AppCompatResources.getDrawable(context, tempNextIcon)

        val tempPrevIcon = typedArray.getResourceId(R.styleable.KalendarView_prevIcon, 0)
        if (tempPrevIcon != 0) prevIcon = AppCompatResources.getDrawable(context, tempPrevIcon)

        val colorBg = typedArray.getColor(R.styleable.KalendarView_calendarBackground, 0)
        if (colorBg != 0) calendarBackgroundColor = colorBg

        animatingMonths = typedArray.getBoolean(R.styleable.KalendarView_animatingMonths, true)

        typedArray.recycle()

        initializeUILayout()
        setUpCalendarAdapter()
        setPreviousButtonClickEvent()
        setNextButtonClickEvent()
        setGridCellClickEvents()

        Log.d(TAG, "I need to call this method")
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initializeUILayout() {
        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.calendarview, this)

        previousButton = view.findViewById(R.id.previous_month)
        nextButton = view.findViewById(R.id.next_month)
        currentDate = view.findViewById(R.id.display_current_date)
        calendarGridView = view.findViewById(R.id.calendar_grid)

        val llRoot = view.findViewById<LinearLayout>(R.id.ll_root)
        val llCalendarHead = view.findViewById<LinearLayout>(R.id.ll_calendar_head)
        val llCalendarWeek = view.findViewById<LinearLayout>(R.id.ll_calendar_week)

        if (monthTextStyle != 0) currentDate?.setTextAppearance(context, monthTextStyle)

        if (weekTextStyle != 0) {
            view.findViewById<TextView>(R.id.sun).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.mon).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.tue).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.wed).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.thu).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.fri).setTextAppearance(context, weekTextStyle)
            view.findViewById<TextView>(R.id.sat).setTextAppearance(context, weekTextStyle)
        }

        if (monthFontFace != null) currentDate?.typeface = monthFontFace

        if (weekFontFace != null) {
            view.findViewById<TextView>(R.id.sun).typeface = weekFontFace
            view.findViewById<TextView>(R.id.mon).typeface = weekFontFace
            view.findViewById<TextView>(R.id.tue).typeface = weekFontFace
            view.findViewById<TextView>(R.id.wed).typeface = weekFontFace
            view.findViewById<TextView>(R.id.thu).typeface = weekFontFace
            view.findViewById<TextView>(R.id.fri).typeface = weekFontFace
            view.findViewById<TextView>(R.id.sat).typeface = weekFontFace
        }

        if (nextIcon != null) nextButton?.setImageDrawable(nextIcon)
        if (prevIcon != null) previousButton?.setImageDrawable(prevIcon)

        llRoot.setBackgroundColor(calendarBackgroundColor)
        llCalendarHead.setBackgroundColor(calendarBackgroundColor)
        llCalendarWeek.setBackgroundColor(calendarBackgroundColor)
        calendarGridView?.setBackgroundColor(calendarBackgroundColor)

        val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.grid_anim)
        animationController = GridLayoutAnimationController(animation, 0f, .1f)
    }

    private fun setPreviousButtonClickEvent() {
        previousButton?.setOnClickListener {
            val oldYear = cal.get(Calendar.YEAR)
            cal.add(Calendar.MONTH, -1)
            val newYear = cal.get(Calendar.YEAR)
            setUpCalendarAdapter()

            mMonthChanger?.onMonthChanged(cal.time)

            if (mYearChangeListener != null && oldYear != newYear) {
                mYearChangeListener?.onYearChanged(newYear)
            }
        }
    }

    private fun setNextButtonClickEvent() {
        nextButton?.setOnClickListener {
            val oldYear = cal.get(Calendar.YEAR)
            cal.add(Calendar.MONTH, 1)
            val newYear = cal.get(Calendar.YEAR)
            setUpCalendarAdapter()

            mMonthChanger?.onMonthChanged(cal.time)

            if (mYearChangeListener != null && oldYear != newYear) {
                mYearChangeListener?.onYearChanged(newYear)
            }
        }
    }

    fun setGridCellClickEvents() {
        calendarGridView?.setOnItemClickListener { parent, view, _, _ ->
            pos = view.tag as Int

            val llParent = view.findViewById<LinearLayout>(R.id.ll_parent)
            llParent.background = selectedIndicator
            llParent.setBackgroundResource(R.drawable.round_bg_blue)

            val txt = view.findViewById<TextView>(R.id.calendar_date_id)
            txt.setTextColor(selectedDateColor)

            color_date = dayValueInCells[pos]

            if (prev != -1) {
                val prevView = parent.getChildAt(prev)
                if (prevView != null && prev != pos) {
                    val prevParent = prevView.findViewById<LinearLayout>(R.id.ll_parent)
                    prevParent.setBackgroundColor(calendarBackgroundColor)

                    val txtd = prevView.findViewById<TextView>(R.id.calendar_date_id)
                    txtd.setTextColor(if (txtd.tag as Int == 0) dateColor else nonMonthDateColor)

                    val customDateColor = mAdapter?.getDateColor(dayValueInCells[prev]) ?: 0
                    if (customDateColor != 0 && txtd.tag as Int == 0) {
                        txtd.setTextColor(customDateColor)
                    }
                }
            }

            if (prev == pos) {
                llParent.setBackgroundResource(R.drawable.round_bg_blue)
                txt.setTextColor(selectedDateColor)
            }

            if (prev == cr_pos) {
                val childView = parent.getChildAt(prev)
                if (childView != null) {
                    val todayParent = childView.findViewById<LinearLayout>(R.id.ll_parent)
                    todayParent.background = todayIndicator

                    val txtd = childView.findViewById<TextView>(R.id.calendar_date_id)
                    txtd.setTextColor(todayDateColor)

                    val customDateColor = mAdapter?.getDateColor(dayValueInCells[prev]) ?: 0
                    if (customDateColor != 0) {
                        txtd.setTextColor(customDateColor)
                    }
                }
            }

            prev = pos

            val month_id = txt.tag as Int
            if (month_id == -1) {
                cr_pos = pos
            }
            if (month_id == 1) {
                cal.add(Calendar.MONTH, 1)
                setUpCalendarAdapter()
                cr_pos = -2
                mMonthChanger?.onMonthChanged(cal.time)
            }
            if (month_id == 2) {
                cal.add(Calendar.MONTH, -1)
                setUpCalendarAdapter()
                cr_pos = -2
                mMonthChanger?.onMonthChanged(cal.time)
            }

            mDateSelector?.onDateClicked(color_date!!)
        }
    }

    private fun setUpCalendarAdapter() {
        dayValueInCells = ArrayList()

        val sDate1 = "27/02/2020"
        try {
            val date1 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(sDate1)
            val date2 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse("08/09/2023")

            val evd = EventObjects(10, "hello", date1)
            val evde = EventObjects(11, "hi", date2)

            // mEvents.add(evd)
            // mEvents.add(evde)
        } catch (e: ParseException) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }

        val mCal = cal.clone() as Calendar
        mCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfTheMonth = mCal.get(Calendar.DAY_OF_WEEK) - 1
        mCal.add(Calendar.DAY_OF_MONTH, -firstDayOfTheMonth)

        while (dayValueInCells.size < MAX_CALENDAR_COLUMN) {
            dayValueInCells.add(mCal.time)
            mCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d(TAG, "Number of date " + dayValueInCells.size)

        val englishMonths = arrayOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )

        val monthIndex = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val sDate = englishMonths[monthIndex] + " " + year

        Log.d(TAG, "Number of datesDate $sDate")
        currentDate?.text = sDate

        prev = dayValueInCells.indexOf(color_date)
        cr_pos = dayValueInCells.indexOf(today_date.time)

        mAdapter = GridAdapter(context, dayValueInCells, cal, mEvents, color_date, colorFulDates)
        mAdapter?.setDrawables(todayIndicator, selectedIndicator, eventIndicator)
        mAdapter?.setTextColors(dateColor, nonMonthDateColor, todayDateColor, selectedDateColor)
        mAdapter?.setFontProperties(dateFontFace, dateTextStyle)
        mAdapter?.setCalendarBackgroundColor(calendarBackgroundColor)

        calendarGridView?.adapter = mAdapter

        if (animatingMonths && !isInEditMode) {
            calendarGridView?.layoutAnimation = animationController
        }
    }

    interface DateSelector {
        fun onDateClicked(selectedDate: Date)
    }

    interface MonthChanger {
        fun onMonthChanged(changedMonth: Date)
    }

    interface YearChangeListener {
        fun onYearChanged(newYear: Int)
    }

    fun setEvents(mEvents: MutableList<EventObjects>) {
        if (mAdapter != null) {
            mAdapter!!.allEvents
            this.mEvents.clear()
            mAdapter!!.allEvents = mEvents
            this.mEvents = mEvents
            mAdapter!!.notifyDataSetChanged()
        }
    }

    fun addEvents(mEvents: MutableList<EventObjects>) {
        if (mAdapter != null) {

            this.mEvents.addAll(mEvents)
            mAdapter!!.notifyDataSetChanged()
        }
    }

   public fun setColoredDates(colorDates: MutableList<ColoredDate>) {
        if (mAdapter != null) {
            mAdapter!!.colorFulDates = colorDates
            colorFulDates = colorDates
            mAdapter!!.notifyDataSetChanged()
        }
    }

    fun addColoredDates(colorDates: MutableList<ColoredDate>) {
        if (mAdapter != null) {

            colorFulDates.addAll(colorDates)
            mAdapter!!.notifyDataSetChanged()
        }
    }

    fun setDateSelector(mSelector: DateSelector?) {
        mDateSelector = mSelector
    }

    fun setMonthSelector(mSelector: MonthChanger?) {
        mMonthChanger = mSelector
    }

    fun setYearChangeListener(listener: YearChangeListener?) {
        mYearChangeListener = listener
    }

    fun setMonthChanger(mChanger: MonthChanger?) {
        mMonthChanger = mChanger
    }

    fun setInitialSelectedDate(initialDate: Date) {
        color_date = getZeroTime(initialDate)
        cal.time = color_date!!
        setUpCalendarAdapter()
    }

    fun getSelectedDate(): Date? {
        return color_date
    }

    fun getShowingMonth(): Date {
        return cal.time
    }

    private fun getZeroTime(date: Date): Date {
        val tempCal = Calendar.getInstance()
        tempCal.time = date
        tempCal.set(Calendar.HOUR, 0)
        tempCal.set(Calendar.MINUTE, 0)
        tempCal.set(Calendar.SECOND, 0)
        tempCal.set(Calendar.MILLISECOND, 0)
        return tempCal.time
    }

    private fun getZeroTime(mCalendar: Calendar): Calendar {
        val tempCal = mCalendar.clone() as Calendar
        tempCal.set(Calendar.HOUR, 0)
        tempCal.set(Calendar.MINUTE, 0)
        tempCal.set(Calendar.SECOND, 0)
        tempCal.set(Calendar.MILLISECOND, 0)
        return tempCal
    }
}
