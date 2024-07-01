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

        //show a Toast message when the user tries to type beyond the maximum length
        val maxLength = 10
        resultTextView.filters = arrayOf(MaxLengthInputFilter(maxLength, this))

        currencyApi = RetrofitInstance.getInstance().create(CurrencyApi::class.java)

        initCalculatorButtons()

        initCurrencyViews()
    }

    private fun initCalculatorButtons() {
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        val numberListener = View.OnClickListener { v ->
            val textView = v as TextView
            if (isUserTyping) {
                resultTextView.append(textView.text)
            } else {
                resultTextView.setText(textView.text)
                isUserTyping = true
            }
        }

        numberButtons.forEach { it.setOnClickListener(numberListener) }

        binding.btnDecimal.setOnClickListener {
            if (!binding.result.text.contains('.')) {
                binding.result.append(".")
            }
        }

        val operationButtons = listOf(
            binding.btnPlus, binding.btnMinus, binding.btnMultiply,
            binding.btnDivide, binding.btnPercent, binding.btnPower,
            binding.btnRoot, binding.btnReset, binding.btnEquals
        )

        val operationListener = View.OnClickListener { v ->
            val operation = (v as TextView).text.toString()
            if (isUserTyping) {
                performOperation(binding.result.text.toString().toDoubleOrNull() ?: 0.0, pendingOperation)
                isUserTyping = false
            }
            pendingOperation = operation
        }

        operationButtons.forEach { it.setOnClickListener(operationListener) }

        binding.btnClear.setOnClickListener {
            binding.result.text = ""
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

        val valueToRate = operand ?: binding.result.text.toString().toDoubleOrNull()

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