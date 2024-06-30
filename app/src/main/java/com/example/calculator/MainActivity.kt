package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.api.CurrencyApi
import com.example.calculator.api.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    interface FixerApi {
        @GET("latest")
        fun getRates(@Query("access_key") accessKey: String, @Query("symbols") symbols: String): Call<CurrencyResponse>
    }

    lateinit var resultTextView: TextView

    var operand1: Double? = null
    var pendingOperation = "="
    var userIsInTheMiddleOfTyping = false

    data class CurrencyResponse(val rates: Map<String, Double>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.result)

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
            val b = v as TextView
            if (userIsInTheMiddleOfTyping) {
                resultTextView.append(b.text)
            } else {
                resultTextView.setText(b.text)
                userIsInTheMiddleOfTyping = true
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
            val op = (v as TextView).text.toString()
            if (userIsInTheMiddleOfTyping) {
                performOperation(resultTextView.text.toString().toDouble(), pendingOperation)
                userIsInTheMiddleOfTyping = false
            }
            pendingOperation = op
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
            operand1 = null
            pendingOperation = "="
            userIsInTheMiddleOfTyping = false
        }
    }

    fun performOperation(value: Double, operation: String) {
        if (operand1 == null) {
            operand1 = value
        } else {
            if (pendingOperation == "=") {
                pendingOperation = operation
            }

            when (pendingOperation) {
                "=" -> operand1 = value
                "/" -> operand1 = if (value == 0.0) Double.NaN else operand1!! / value
                "*" -> operand1 = operand1!! * value
                "-" -> operand1 = operand1!! - value
                "+" -> operand1 = operand1!! + value
            }
        }

        resultTextView.setText(formatNumber(operand1))
    }

    //ensure that numbers are displayed without unnecessary decimal places
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
            val fromValue = spinnerFrom.selectedItem.toString()
            val toValue = spinnerTo.selectedItem.toString()

            convertCurrency(resultCalculateTextView, fromValue, toValue)
        }
    }

    fun convertCurrency(resultCalculateTextView: TextView, fromValue: String, toValue: String) {
        val retrofitInstance = RetrofitInstance.getInstance()
        val currencyApi = retrofitInstance.create(CurrencyApi::class.java)

        val call = currencyApi.getLatestRates("731748b10a6307de75913292a0ddf7d5")
        call.enqueue(object : Callback<com.example.calculator.api.CurrencyResponse> {
            override fun onResponse(call: Call<com.example.calculator.api.CurrencyResponse>, response: Response<com.example.calculator.api.CurrencyResponse>) {
                if (response.isSuccessful) {
                    val rates = response.body()?.rates

                    val fromRate: Double? = rates?.get(fromValue)
                    val toRate: Double? = rates?.get(toValue)

                    if (fromRate != null && toRate != null && operand1 != null) {
                        val convertedValue = operand1!! * (fromRate / toRate)
                        resultCalculateTextView.setText(formatNumber(convertedValue))
                    }
                }
            }

            override fun onFailure(call: Call<com.example.calculator.api.CurrencyResponse>, t: Throwable) {
                resultCalculateTextView.setText("Error: ${t.message}")
            }
        })
    }
}