package com.appetiser.appetiserdatepicker

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom date picker view. Uses [SingleDateAndTimePickerDialog] as the date picker.
 *
 * Link to library: https://github.com/florent37/SingleDateAndTimePicker
 */
class AppetiserDatePicker(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet) {

    companion object {
        private const val TAG = "AppetiserDatePicker"
    }

    private var dpLabelText: String? = null
    private var dpLabelTextColor: ColorStateList? = null
    private var dpUnderLineBackground: Drawable? = null

    private var dpTextHint: String? = null
    @ColorInt
    private var dpTextColorHint: Int? = null
    @ColorInt
    private var dpTextColor: Int? = null

    private lateinit var container: LinearLayout
    private lateinit var txtViewLabel: TextView
    private lateinit var txtViewMain: TextView
    private lateinit var viewUnderLine: View

    private lateinit var dateFormat: SimpleDateFormat
    private var defaultPickerDate: Date? = null
    private var minDateRange: Date? = null
    private var maxDateRange: Date? = null // Necessary for library usage.
    private var selectedDate: Date? = null

    private var dateSelectedListener: ((Date) -> Unit)? = null

    init {
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.AppetiserDatePicker)

        try {
            getLabelAttributes(array)
            getMainTextAttributes(array)
            getUnderlineAttributes(array)
        } finally {
            array.recycle()
        }
    }

    private fun getLabelAttributes(array: TypedArray) {
        with(array) {
            getString(R.styleable.AppetiserDatePicker_dp_label_text)
                ?.let { text ->
                    dpLabelText = text
                }

            getColorStateList(R.styleable.AppetiserDatePicker_dp_label_text_color)
                ?.let { color ->
                    dpLabelTextColor = color
                }
        }
    }

    private fun getMainTextAttributes(array: TypedArray) {
        with(array) {
            getString(R.styleable.AppetiserDatePicker_dp_text_hint)
                ?.let { text ->
                    dpTextHint = text
                }

            getColor(R.styleable.AppetiserDatePicker_dp_text_color_hint, -1)
                .let { color ->
                    if (color != -1) {
                        dpTextColorHint = color
                    }
                }

            getColor(R.styleable.AppetiserDatePicker_dp_text_color, -1)
                .let { color ->
                    if (color != -1) {
                        dpTextColor = color
                    }
                }
        }
    }

    private fun getUnderlineAttributes(array: TypedArray) {
        with(array) {
            getDrawable(R.styleable.AppetiserDatePicker_dp_line_background)
                ?.let { drawable ->
                    dpUnderLineBackground = drawable
                }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initViews()
        initDate()
    }

    override fun onDetachedFromWindow() {
        removeAllViewsInLayout()
        super.onDetachedFromWindow()
    }

    private fun initDate() {
        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

    /**
     * Sets default date in date picker. Must be inside in min and max date ranges.
     */
    fun setDefaultPickerDate(defaultDate: Date) {
        this.defaultPickerDate = defaultDate
    }

    /**
     * Sets minimum and maximum date allowed in date picker.
     */
    fun setMinAndMaxDateRange(minDateRange: Date, maxDateRange: Date) {
        this.minDateRange = minDateRange
        this.maxDateRange = maxDateRange
    }

    /**
     * Sets date format in displaying date in the view. Defaults to dd/MM/yyyy.
     */
    fun setDateFormat(dateFormat: SimpleDateFormat) {
        this.dateFormat = dateFormat
    }

    fun setOnDateSelectedListener(listener: (Date) -> Unit) {
        dateSelectedListener = listener
    }

    private fun showDobSelector() {
        val hasMinDate = minDateRange != null
        val hasMaxDate = maxDateRange != null

        val builder = SingleDateAndTimePickerDialog
            .Builder(context)
            .displayHours(false)
            .displayMinutes(false)
            .displayDays(false)
            .displayYears(true)
            .displayMonth(true)
            .displayDaysOfMonth(true)
            .mustBeOnFuture()
            // TODO 2019-08-15 Improve these. Add picker fields as attribute in custom view.
            .title(dpLabelText)
            .titleTextColor(ContextCompat.getColor(context, R.color.pink))
            .mainColor(ContextCompat.getColor(context, R.color.pink))
            .listener { dateSelected ->
                Log.d(TAG, dateSelected.toString())
                selectedDate = dateSelected

                setDobText(dateSelected)

                dateSelectedListener?.invoke(dateSelected)
            }
            .displayListener { picker ->
                Log.d(TAG, "display listener")
                setDobActive()
                picker
                    .viewTreeObserver
                    .addOnWindowAttachListener(object : ViewTreeObserver.OnWindowAttachListener {
                        override fun onWindowDetached() {
                            Log.d(TAG, "window detached")
                            picker.viewTreeObserver.removeOnWindowAttachListener(this)
                            setDobInactive()
                        }

                        override fun onWindowAttached() = Unit
                    })
            }
            .bottomSheet()

        if (defaultPickerDate != null) {
            builder.defaultDate(defaultPickerDate)
        }

        if (hasMinDate && hasMaxDate) {
            builder.minDateRange(minDateRange)
            builder.maxDateRange(maxDateRange)
        }

        builder.display()
    }

    /**
     * Set date of birth field active state.
     */
    private fun setDobActive() {
        txtViewLabel.isActivated = true
        viewUnderLine.isActivated = true
    }

    /**
     * Set date of birth field inactive state.
     */
    private fun setDobInactive() {
        txtViewLabel.isActivated = false
        viewUnderLine.isActivated = false
    }

    private fun setDobText(dateSelected: Date) {
        txtViewMain.text = dateFormat.format(dateSelected)
    }

    fun setDobText(date: String) {
        txtViewMain.text = date
    }

    fun emptyDobText() {
        txtViewMain.text = ""
    }

    private fun initViews() {
        View.inflate(context, R.layout.view_date_picker, this)

        container = findViewById(R.id.container)
        txtViewLabel = findViewById(R.id.txtDobLabel)
        txtViewMain = findViewById(R.id.txtDob)
        viewUnderLine = findViewById(R.id.viewBottomLine)

        setupLabel()
        setupTxtViewMain()
        setupUnderline()

        setListeners()
    }

    private fun setListeners() {
        container
            .setOnClickListener {
                showDobSelector()
            }
    }

    fun setLabelText(value: String) {
        txtViewLabel.text = value
    }

    /**
     * Sets label text color. ColorStateList must have `state_activated` state.
     */
    fun setLabelTextColor(value: ColorStateList) {
        txtViewLabel.setTextColor(value)
    }

    /**
     * Sets text hint.
     */
    fun setHintText(value: String) {
        txtViewMain.hint = value
    }

    /**
     * Set hint text color.
     */
    fun setHintTextColor(@ColorInt value: Int) {
        txtViewMain.setHintTextColor(value)
    }

    /**
     * Set text color.
     */
    fun setTextColor(@ColorInt value: Int) {
        txtViewMain.setTextColor(value)
    }

    /**
     * Sets underline background drawable. StateDrawableList must have `state_activated` state.
     */
    fun setUnderLineBackground(value: Drawable) {
        viewUnderLine.background = value
    }

    private fun setupLabel() {
        dpLabelText?.let {
            setLabelText(it)
        }

        dpLabelTextColor?.let {
            setLabelTextColor(it)
        }
    }

    private fun setupTxtViewMain() {
        dpTextHint?.let {
            setHintText(it)
        }

        dpTextColorHint?.let {
            setHintTextColor(it)
        }

        dpTextColor?.let {
            setTextColor(it)
        }
    }

    private fun setupUnderline() {
        dpUnderLineBackground
            ?.let { drawable ->
                setUnderLineBackground(drawable)
            }
    }
}
