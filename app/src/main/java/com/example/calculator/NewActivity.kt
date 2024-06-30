package com.example.calculator

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.api.CurrencyApi
import com.example.calculator.api.CurrencyResponse
import com.example.calculator.api.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat

class NewActivity : AppCompatActivity() {

    private lateinit var resultText: EditText
    private var operand1: Double? = null
    private var pendingOperation = "="
    private var userIsInTheMiddleOfTyping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new)

        resultText = findViewById(R.id.resultText)

        val button0: Button = findViewById(R.id.button0)
        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        val button3: Button = findViewById(R.id.button3)
        val button4: Button = findViewById(R.id.button4)
        val button5: Button = findViewById(R.id.button5)
        val button6: Button = findViewById(R.id.button6)
        val button7: Button = findViewById(R.id.button7)
        val button8: Button = findViewById(R.id.button8)
        val button9: Button = findViewById(R.id.button9)
        val buttonDot: Button = findViewById(R.id.buttonDot)

        val buttonEquals: Button = findViewById(R.id.buttonEquals)
        val buttonDivide: Button = findViewById(R.id.buttonDivide)
        val buttonMultiply: Button = findViewById(R.id.buttonMultiply)
        val buttonSubtract: Button = findViewById(R.id.buttonSubtract)
        val buttonAdd: Button = findViewById(R.id.buttonAdd)
        val buttonClear: Button = findViewById(R.id.buttonClear)
        val buttonCurrency: Button = findViewById(R.id.buttonCurrency)

        val spinnerFrom: Spinner = findViewById(R.id.spinner_from)
        val spinnerTo: Spinner = findViewById(R.id.spinner_to)

        val numberClickListener = View.OnClickListener { v ->
            val b = v as Button
            if (userIsInTheMiddleOfTyping) {
                resultText.append(b.text)
            } else {
                resultText.setText(b.text)
                userIsInTheMiddleOfTyping = true
            }
        }

        button0.setOnClickListener(numberClickListener)
        button1.setOnClickListener(numberClickListener)
        button2.setOnClickListener(numberClickListener)
        button3.setOnClickListener(numberClickListener)
        button4.setOnClickListener(numberClickListener)
        button5.setOnClickListener(numberClickListener)
        button6.setOnClickListener(numberClickListener)
        button7.setOnClickListener(numberClickListener)
        button8.setOnClickListener(numberClickListener)
        button9.setOnClickListener(numberClickListener)
        buttonDot.setOnClickListener {
            if (!resultText.text.contains('.')) {
                resultText.append(".")
            }
        }

        val opClickListener = View.OnClickListener { v ->
            val op = (v as Button).text.toString()
            if (userIsInTheMiddleOfTyping) {
                performOperation(resultText.text.toString().toDouble(), pendingOperation)
                userIsInTheMiddleOfTyping = false
            }
            pendingOperation = op
        }

        buttonEquals.setOnClickListener(opClickListener)
        buttonDivide.setOnClickListener(opClickListener)
        buttonMultiply.setOnClickListener(opClickListener)
        buttonSubtract.setOnClickListener(opClickListener)
        buttonAdd.setOnClickListener(opClickListener)

        buttonClear.setOnClickListener {
            resultText.setText("")
            operand1 = null
            pendingOperation = "="
            userIsInTheMiddleOfTyping = false
        }

        buttonCurrency.setOnClickListener {
            convertCurrency()
        }

//        val currencies = resources.getStringArray(R.array.currency_array)
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerFrom.adapter = adapter
//        spinnerTo.adapter = adapter
    }

    private fun performOperation(value: Double, operation: String) {
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

        resultText.setText(formatNumber(operand1))
    }

    private fun formatNumber(number: Double?): String {
        val df = DecimalFormat("#.##########")
        return if (number != null) df.format(number) else ""
    }

    private fun convertCurrency() {
        val retrofitInstance = RetrofitInstance.getInstance()
        val currencyApi = retrofitInstance.create(CurrencyApi::class.java)

        val call = currencyApi.getLatestRates("731748b10a6307de75913292a0ddf7d5")
        call.enqueue(object : Callback<CurrencyResponse> {
            override fun onResponse(call: Call<CurrencyResponse>, response: Response<CurrencyResponse>) {
                if (response.isSuccessful) {
                    val rates = response.body()?.rates
                    val usdRate = rates?.get("USD")
                    val eurRate = rates?.get("EUR")

                    if (usdRate != null && eurRate != null && operand1 != null) {
                        val convertedValue = operand1!! * usdRate / eurRate
                        resultText.setText(formatNumber(convertedValue))
                    }
                }
            }

            override fun onFailure(call: Call<CurrencyResponse>, t: Throwable) {
                resultText.setText("Error: ${t.message}")
            }
        })
    }
}
