package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
import com.example.calculator.api.CurrencyApi
import com.example.calculator.api.RetrofitInstance
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    // Interface for notifying when rates are updated
    interface OnRatesUpdatedCallback {
        fun onRatesUpdated()
    }

    lateinit var resultTextView: TextView

    var operand: Double? = null
    var pendingOperation = "="
    var isUserTyping = false

    var usdRate: Double? = null
    var eurRate: Double? = null
    var gbpRate: Double? = null
    var jpyRate: Double? = null

    private lateinit var currencyApi: CurrencyApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.result)

        currencyApi = RetrofitInstance.getInstance().create(CurrencyApi::class.java)

        initCalculatorButtons()

        initCurrencyViews()
    }

    private fun initCalculatorButtons() {
        val btnDecimal = findViewById<TextView>(R.id.btn_decimal)
        val btn0 = findViewById<TextView>(R.id.btn_0)
        val btn1 = findViewById<TextView>(R.id.btn_1)
        val btn2 = findViewById<TextView>(R.id.btn_2)
        val btn3 = findViewById<TextView>(R.id.btn_3)
        val btn4 = findViewById<TextView>(R.id.btn_4)
        val btn5 = findViewById<TextView>(R.id.btn_5)
        val btn6 = findViewById<TextView>(R.id.btn_6)
        val btn7 = findViewById<TextView>(R.id.btn_7)
        val btn8 = findViewById<TextView>(R.id.btn_8)
        val btn9 = findViewById<TextView>(R.id.btn_9)
        val btnPlus = findViewById<TextView>(R.id.btn_plus)
        val btnMinus = findViewById<TextView>(R.id.btn_minus)
        val btnMultiply = findViewById<TextView>(R.id.btn_multiply)
        val btnDivide = findViewById<TextView>(R.id.btn_divide)
        val btnPercent = findViewById<TextView>(R.id.btn_percent)
        val btnPower = findViewById<TextView>(R.id.btn_power)
        val btnRoot = findViewById<TextView>(R.id.btn_root)
        val btnClear = findViewById<TextView>(R.id.btn_clear)
        val btnReset = findViewById<TextView>(R.id.btn_reset)
        val btnEquals = findViewById<TextView>(R.id.btn_equals)

        val numberListener = View.OnClickListener { v ->
            val textView = v as TextView
            if (isUserTyping) {
                resultTextView.append(textView.text)
            } else {
                resultTextView.setText(textView.text)
                isUserTyping = true
            }
        }

        btn0.setOnClickListener(numberListener)
        btn1.setOnClickListener(numberListener)
        btn2.setOnClickListener(numberListener)
        btn3.setOnClickListener(numberListener)
        btn4.setOnClickListener(numberListener)
        btn5.setOnClickListener(numberListener)
        btn6.setOnClickListener(numberListener)
        btn7.setOnClickListener(numberListener)
        btn8.setOnClickListener(numberListener)
        btn9.setOnClickListener(numberListener)

        btnDecimal.setOnClickListener {
            if (!resultTextView.text.contains('.')) {
                resultTextView.append(".")
            }
        }

        val operationListener = View.OnClickListener { v ->
            val textViewText = (v as TextView).text.toString()
            if (isUserTyping) {
                performOperation(resultTextView.text.toString().toDouble(), pendingOperation)
                isUserTyping = false
            }
            pendingOperation = textViewText
        }

        btnPlus.setOnClickListener(operationListener)
        btnMinus.setOnClickListener(operationListener)
        btnMultiply.setOnClickListener(operationListener)
        btnDivide.setOnClickListener(operationListener)
        btnPercent.setOnClickListener(operationListener)
        btnPower.setOnClickListener(operationListener)
        btnRoot.setOnClickListener(operationListener)
        btnReset.setOnClickListener(operationListener)
        btnEquals.setOnClickListener(operationListener)

        btnClear.setOnClickListener {
            resultTextView.setText("")
            operand = null
            pendingOperation = "="
            isUserTyping = false
        }
    }

    fun performOperation(value: Double, operation: String) {
        try {
            if (operand == null)
                operand = value
            else {
                if (pendingOperation.equals("="))
                    pendingOperation = operation

                when (pendingOperation) {
                    "=" -> operand = value
                    "÷" -> operand = if (value == 0.0) Double.NaN else operand!! / value
                    "×" -> operand = operand!! * value
                    "-" -> operand = operand!! - value
                    "+" -> operand = operand!! + value
                    "^" -> operand = Math.pow(operand!!, value)
                    "%" -> operand = operand!! / 100
                }
            }

            if (pendingOperation.equals("√"))
                operand = Math.sqrt(operand!!)

            resultTextView.setText(formatNumber(operand))
        }
        catch(e: Exception){
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    // Ensure that numbers are displayed without unnecessary decimal places
    fun formatNumber(number: Double?): String {
        val df = DecimalFormat("#.##########")
        return if (number != null) df.format(number) else ""
    }

    private fun initCurrencyViews() {
        val spinnerFrom: Spinner = findViewById(R.id.spinner_from)
        val spinnerTo: Spinner = findViewById(R.id.spinner_to)
        val calculateButton: Button = findViewById(R.id.button_calculate)
        val resultCalculateTextView: TextView = findViewById(R.id.textview_calculate_result)

        calculateButton.setOnClickListener { v ->
            try {
                val fromValue = spinnerFrom.selectedItem.toString()
                val toValue = spinnerTo.selectedItem.toString()

                convertCurrency(resultCalculateTextView, fromValue, toValue)
            }
            catch(e: Exception){
                Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertCurrency(resultCalculateTextView: TextView, fromValue: String, toValue: String) {
        if (usdRate == null && eurRate == null) {
            fetchRates(object : OnRatesUpdatedCallback {
                override fun onRatesUpdated() {
                    // This block will be executed only after the rates are updated
                    performConversion(resultCalculateTextView, fromValue, toValue)
                }
            })
        } else {
            performConversion(resultCalculateTextView, fromValue, toValue)
        }
    }

    private fun performConversion(resultCalculateTextView: TextView, fromValue: String, toValue: String) {
        val fromRate: Double?
        val toRate: Double?

        fromRate = when (fromValue) {
            "USD" -> usdRate
            "EUR" -> eurRate
            "GBP" -> gbpRate
            else -> jpyRate
        }

        toRate = when (toValue) {
            "USD" -> usdRate
            "EUR" -> eurRate
            "GBP" -> gbpRate
            else -> jpyRate
        }

        var valueToRate = operand
        if(operand == null)
            valueToRate = resultTextView.text.toString().toDoubleOrNull()

        if (fromRate != null && toRate != null && valueToRate != null) {
            val convertedValue = valueToRate * (fromRate / toRate)
            resultCalculateTextView.text = formatNumber(convertedValue)
        }
    }

    private fun fetchRates(callback: OnRatesUpdatedCallback) {
        lifecycleScope.launch(Dispatchers.IO) { // Launch coroutine on IO dispatcher
            try {
                val response = currencyApi.getLatestRates(BuildConfig.API_KEY)
                if (response.isSuccessful) {
                    val rates = response.body()?.rates
                    // Update rates on the main thread
                    withContext(Dispatchers.Main) {
                        usdRate = rates?.get("USD")
                        eurRate = rates?.get("EUR")
                        gbpRate = rates?.get("GBP")
                        jpyRate = rates?.get("JPY")

                        callback.onRatesUpdated()
                    }
                } else {
                    withContext(Dispatchers.Main) { // Handle unsuccessful responseon the main thread
                        Toast.makeText(
                            this@MainActivity,
                            "Error fetching rates",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


//        val call = currencyApi.getLatestRates(BuildConfig.API_KEY)
//        call.enqueue(object : Callback<com.example.calculator.api.CurrencyResponse> {
//            override fun onResponse(
//                call: Call<com.example.calculator.api.CurrencyResponse>,
//                response: Response<com.example.calculator.api.CurrencyResponse>) {
//                if (response.isSuccessful) {
//                    try {
//                        val rates = response.body()?.rates
//
//                        usdRate = rates?.get("USD")
//                        eurRate = rates?.get("EUR")
//                        gbpRate = rates?.get("GBP")
//                        jpyRate = rates?.get("JPY")
//
//                        callback.onRatesUpdated()
//                    } catch (e: Exception) {
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Error: " + e.message,
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//
//            override fun onFailure(call: Call<com.example.calculator.api.CurrencyResponse>, t: Throwable) {
//                Toast.makeText(this@MainActivity, "Error: " + t.message, Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
}