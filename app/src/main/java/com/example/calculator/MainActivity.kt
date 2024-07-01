package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.api.CurrencyApi
import com.example.calculator.api.RetrofitInstance
import com.example.calculator.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resultTextView = binding.result

        currencyApi = RetrofitInstance.getInstance().create(CurrencyApi::class.java)

        initCalculatorButtons()

        initCurrencyViews()
    }

    private fun initCalculatorButtons() {
        val btnDecimal = binding.btnDecimal
        val btn0 = binding.btn0
        val btn1 = binding.btn1
        val btn2 = binding.btn2
        val btn3 = binding.btn3
        val btn4 = binding.btn4
        val btn5 = binding.btn5
        val btn6 = binding.btn6
        val btn7 = binding.btn7
        val btn8 = binding.btn8
        val btn9 = binding.btn9
        val btnPlus = binding.btnPlus
        val btnMinus = binding.btnMinus
        val btnMultiply = binding.btnMultiply
        val btnDivide = binding.btnDivide
        val btnPercent = binding.btnPercent
        val btnPower = binding.btnPower
        val btnRoot = binding.btnRoot
        val btnClear = binding.btnClear
        val btnReset = binding.btnReset
        val btnEquals = binding.btnEquals

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

    //ensure that numbers are displayed without unnecessary decimal places
    fun formatNumber(number: Double?): String {
        val df = DecimalFormat("#.##########")
        return if (number != null) df.format(number) else ""
    }

    private fun initCurrencyViews() {
        val spinnerFrom: Spinner = binding.spinnerFrom
        val spinnerTo: Spinner = binding.spinnerTo
        val calculateButton: Button = binding.buttonCalculate
        val resultCalculateTextView: TextView = binding.textviewCalculateResult

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
                    //this will be executed only after the rates are updated
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
        CoroutineScope(Dispatchers.IO).launch { //CoroutineScope to launch a coroutine in the IO context
            try {
                val response = currencyApi.getLatestRates(BuildConfig.API_KEY).execute()
                if (response.isSuccessful) {
                    val rates = response.body()?.rates

                    usdRate = rates?.get("USD")
                    eurRate = rates?.get("EUR")
                    gbpRate = rates?.get("GBP")
                    jpyRate = rates?.get("JPY")

                    withContext(Dispatchers.Main) { //switch to Main context to update UI
                        callback.onRatesUpdated()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: Response not successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: " + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}