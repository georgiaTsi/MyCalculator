//package com.example.calculator
//
//import android.content.Context
//import android.widget.Toast
//import com.example.calculator.helpers.*
//import org.json.JSONObject
//import org.json.JSONTokener
//import java.math.BigDecimal
//
//interface Calculator {
//    fun showNewResult(value: String)
//
//    fun showNewFormula(value: String)
//}
//
//class Calculation(private val context: Context, private var decimalSeparator: String = DOT, private var groupingSeparator: String = COMMA) {
//    private var currentResult = "0"
//    private var previousCalculation = ""
//    private var baseValue = 0.0
//    private var secondValue = 0.0
//    private var inputDisplayedFormula = "0"
//    private var lastKey = ""
//    private var lastOperation = ""
//    private val operations = listOf("+", "-", "×", "÷", "^", "%", "√")
//    private val operationsRegex = "[-+×÷^%√]".toPattern()
//    private val numbersRegex = "[^0-9,.]".toRegex()
//    private val formatter = NumberFormatHelper(
//        decimalSeparator = decimalSeparator, groupingSeparator = groupingSeparator
//    )
//
//    init {
//        showNewResult(currentResult)
//        showNewFormula(previousCalculation)
//    }
//
//    private fun addDigit(number: Int) {
//        if (inputDisplayedFormula == "0") {
//            inputDisplayedFormula = ""
//        }
//
//        inputDisplayedFormula += number
//        addThousandsDelimiter()
//        showNewResult(inputDisplayedFormula)
//    }
//
//    private fun zeroClicked() {
//        val valueToCheck = inputDisplayedFormula.trimStart('-').removeGroupSeparator()
//        val value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
//        if (value != "0" || value.contains(decimalSeparator)) {
//            addDigit(0)
//        }
//    }
//
//    private fun decimalClicked() {
//        val valueToCheck = inputDisplayedFormula.trimStart('-').replace(groupingSeparator, "")
//        val value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
//        if (!value.contains(decimalSeparator)) {
//            when {
//                value == "0" && !valueToCheck.contains(operationsRegex.toRegex()) -> inputDisplayedFormula = "0$decimalSeparator"
//                value == "" -> inputDisplayedFormula += "0$decimalSeparator"
//                else -> inputDisplayedFormula += decimalSeparator
//            }
//        }
//
//        lastKey = DECIMAL
//        showNewResult(inputDisplayedFormula)
//    }
//
//    private fun addThousandsDelimiter() {
//        val valuesToCheck = numbersRegex.split(inputDisplayedFormula).filter { it.trim().isNotEmpty() }
//        valuesToCheck.forEach {
//            var newString = formatter.addGroupingSeparators(it)
//
//            // allow writing numbers like 0.003
//            if (it.contains(decimalSeparator)) {
//                val firstPart = newString.substringBefore(decimalSeparator)
//                val lastPart = it.substringAfter(decimalSeparator)
//                newString = "$firstPart$decimalSeparator$lastPart"
//            }
//
//            inputDisplayedFormula = inputDisplayedFormula.replace(it, newString)
//        }
//    }
//
//    fun handleOperation(operation: String) {
//        if (inputDisplayedFormula == Double.NaN.toString()) {
//            inputDisplayedFormula = "0"
//        }
//
//        if (inputDisplayedFormula == "") {
//            inputDisplayedFormula = "0"
//        }
//
//        if (operation == ROOT && inputDisplayedFormula == "0") {
//            if (lastKey != DIGIT) {
//                inputDisplayedFormula = "1√"
//            }
//        }
//
//        val lastChar = inputDisplayedFormula.last().toString()
//        if (lastChar == decimalSeparator) {
//            inputDisplayedFormula = inputDisplayedFormula.dropLast(1)
//        } else if (operations.contains(lastChar)) {
//            inputDisplayedFormula = inputDisplayedFormula.dropLast(1)
//            inputDisplayedFormula += getSign(operation)
//        } else if (!inputDisplayedFormula.trimStart('-').contains(operationsRegex.toRegex())) {
//            inputDisplayedFormula += getSign(operation)
//        }
//
//        if (lastKey == DIGIT || lastKey == DECIMAL) {
//            if (lastOperation != "" && operation == PERCENT) {
//                handlePercent()
//            } else {
//                // split to multiple lines just to see when does the crash happen
//                secondValue = when (operation) {
//                    PLUS -> getSecondValue()
//                    MINUS -> getSecondValue()
//                    MULTIPLY -> getSecondValue()
//                    DIVIDE -> getSecondValue()
//                    ROOT -> getSecondValue()
//                    POWER -> getSecondValue()
//                    PERCENT -> getSecondValue()
//                    else -> getSecondValue()
//                }
//
//                calculateResult()
//
//                if (!operations.contains(inputDisplayedFormula.last().toString())) {
//                    if (!inputDisplayedFormula.contains("÷")) {
//                        inputDisplayedFormula += getSign(operation)
//                    }
//                }
//            }
//        }
//
//        if (getSecondValue() == 0.0 && inputDisplayedFormula.contains("÷")) {
//            lastKey = DIVIDE
//            lastOperation = DIVIDE
//        } else {
//            lastKey = operation
//            lastOperation = operation
//        }
//
//        showNewResult(inputDisplayedFormula)
//    }
//
//    fun turnToNegative(): Boolean {
//        if (inputDisplayedFormula.isEmpty()) {
//            return false
//        }
//
//        if (!inputDisplayedFormula.trimStart('-').any { it.toString() in operations } && inputDisplayedFormula.removeGroupSeparator().toDouble() != 0.0) {
//            inputDisplayedFormula = if (inputDisplayedFormula.first() == '-') {
//                inputDisplayedFormula.substring(1)
//            } else {
//                "-$inputDisplayedFormula"
//            }
//
//            showNewResult(inputDisplayedFormula)
//            return true
//        }
//
//        return false
//    }
//
//    // handle percents manually, it doesn't seem to be possible via net.objecthunter:exp4j. "%" is used only for modulo there
//    // handle cases like 10+200% here
//    private fun handlePercent() {
//        var result = calculatePercentage(baseValue, getSecondValue(), lastOperation)
//        if (result.isInfinite() || result.isNaN()) {
//            result = 0.0
//        }
//
//        showNewFormula("${baseValue.format()}${getSign(lastOperation)}${getSecondValue().format()}%")
//        inputDisplayedFormula = result.format()
//        showNewResult(result.format())
//        baseValue = result
//    }
//
//    fun handleEquals() {
//        if (lastKey == EQUALS) {
//            calculateResult()
//        }
//
//        if (lastKey != DIGIT && lastKey != DECIMAL) {
//            return
//        }
//
//        secondValue = getSecondValue()
//        calculateResult()
//        if ((lastOperation == DIVIDE || lastOperation == PERCENT) && secondValue == 0.0) {
//            lastKey = DIGIT
//            return
//        }
//
//        lastKey = EQUALS
//    }
//
//    private fun getSecondValue(): Double {
//        val valueToCheck = inputDisplayedFormula.trimStart('-').removeGroupSeparator()
//
//        var value = valueToCheck.substring(valueToCheck.indexOfAny(operations) + 1)
//        if (value == "") {
//            value = "0"
//        }
//
//        return try {
//            value.toDouble()
//        } catch (e: NumberFormatException) {
//            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
//            0.0
//        }
//    }
//
//    private fun calculateResult() {
//        if (lastOperation == ROOT && inputDisplayedFormula.startsWith("√")) {
//            baseValue = 1.0
//        }
//
//        if (lastKey != EQUALS) {
//            val valueToCheck = inputDisplayedFormula.trimStart('-').removeGroupSeparator()
//            val parts = valueToCheck.split(operationsRegex).filter { it != "" }
//            if (parts.isEmpty()) {
//                return
//            }
//
//            try {
//                baseValue = parts.first().toDouble()
//            } catch (e: NumberFormatException) {
//                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
//            }
//
//            if (inputDisplayedFormula.startsWith("-")) {
//                baseValue *= -1
//            }
//
//            secondValue = parts.getOrNull(1)?.toDouble() ?: secondValue
//        }
//
//        if (lastOperation != "") {
//            val sign = getSign(lastOperation)
//            val formattedBaseValue = baseValue.format().removeGroupSeparator()
//            val formatterSecondValue = secondValue.format().removeGroupSeparator()
//            val expression = "$formattedBaseValue$sign$formatterSecondValue"
//                .replace("√", "sqrt")
//                .replace("×", "*")
//                .replace("÷", "/")
//
//            try {
//                if (sign == "÷" && secondValue == 0.0) {
//                    Toast.makeText(context, "Error: division by zero", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//                // handle percents manually, it doesn't seem to be possible via net.objecthunter:exp4j. "%" is used only for modulo there
//                // handle cases like 10%200 here
//                var result = 0.0
//                if (sign == "%") {
//                    val second = (secondValue / 100f).format().removeGroupSeparator()
//                    result = second.toDouble()//TODO
////                    ExpressionBuilder("$formattedBaseValue*$second").build().evaluate() TODO
//                } else {
//                    // avoid Double rounding errors at expressions like 5250,74 + 14,98
//                    if (sign == "+" || sign == "-") {
//                        val first = BigDecimal.valueOf(baseValue)
//                        val second = BigDecimal.valueOf(secondValue)
//                        val bigDecimalResult = when (sign) {
//                            "-" -> first.minus(second)
//                            else -> first.plus(second)
//                        }
//                        result = bigDecimalResult.toDouble()
//                    } else {
////                        ExpressionBuilder(expression).build().evaluate() TODO
//                        result = expression.toDouble()//TODO
//                    }
//                }
//
//                if (result.isInfinite() || result.isNaN()) {
//                    Toast.makeText(context, "Unknown error occured", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//                showNewResult(result.format())
//                val newFormula = "${baseValue.format()}$sign${secondValue.format()}"
//
//                showNewFormula(newFormula)
//
//                inputDisplayedFormula = result.format()
//                baseValue = result as Double
//            } catch (e: Exception) {
//                Toast.makeText(context, "Error: Unknown error occured", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun calculatePercentage(baseValue: Double, secondValue: Double, sign: String): Double {
//        return when (sign) {
//            MULTIPLY -> {
//                val partial = 100 / secondValue
//                baseValue / partial
//            }
//            DIVIDE -> {
//                val partial = 100 / secondValue
//                baseValue * partial
//            }
//            PLUS -> {
//                val partial = baseValue / (100 / secondValue)
//                baseValue.plus(partial)
//            }
//            MINUS -> {
//                val partial = baseValue / (100 / secondValue)
//                baseValue.minus(partial)
//            }
//            PERCENT -> {
//                val partial = (baseValue % secondValue) / 100
//                partial
//            }
//            else -> baseValue / (100 * secondValue)
//        }
//    }
//
//    private fun showNewResult(value: String) {
//        currentResult = value
//        (context as MainActivity).showNewResult(value)
//    }
//
//    fun handleClear() {
//        val lastDeletedValue = inputDisplayedFormula.lastOrNull().toString()
//
//        var newValue = inputDisplayedFormula.dropLast(1)
//        if (newValue == "" || newValue == "0") {
//            newValue = "0"
//            lastKey = CLEAR
//        } else {
//            if (operations.contains(lastDeletedValue) || lastKey == EQUALS) {
//                lastOperation = ""
//            }
//            val lastValue = newValue.last().toString()
//            lastKey = when {
//                operations.contains(lastValue) -> CLEAR
//                lastValue == decimalSeparator -> DECIMAL
//                else -> DIGIT
//            }
//        }
//
//        newValue = newValue.trimEnd(groupingSeparator.single())
//        inputDisplayedFormula = newValue
//        addThousandsDelimiter()
//        showNewResult(inputDisplayedFormula)
//    }
//
//    fun handleReset() {
//        resetValues()
//        showNewResult("0")
//        showNewFormula("")
//        inputDisplayedFormula = ""
//    }
//
//    private fun resetValues() {
//        baseValue = 0.0
//        secondValue = 0.0
//        lastKey = ""
//        lastOperation = ""
//    }
//
//    private fun getSign(lastOperation: String) = when (lastOperation) {
//        MINUS -> "-"
//        MULTIPLY -> "×"
//        DIVIDE -> "÷"
//        PERCENT -> "%"
//        POWER -> "^"
//        ROOT -> "√"
//        else -> "+"
//    }
//
//    fun addNumberToFormula(number: String) {
//        handleReset()
//        inputDisplayedFormula = number
//        addThousandsDelimiter()
//        showNewResult(inputDisplayedFormula)
//    }
//
//    fun updateSeparators(decimalSeparator: String, groupingSeparator: String) {
//        if (this.decimalSeparator != decimalSeparator || this.groupingSeparator != groupingSeparator) {
//            this.decimalSeparator = decimalSeparator
//            this.groupingSeparator = groupingSeparator
//            formatter.decimalSeparator = decimalSeparator
//            formatter.groupingSeparator = groupingSeparator
//            // future: maybe update the formulas with new separators instead of resetting the whole thing
//            handleReset()
//        }
//    }
//
//    private fun Double.format() = formatter.doubleToString(this)
//
//    private fun String.removeGroupSeparator() = formatter.removeGroupingSeparator(this)
//
//    fun getCalculatorStateJson(): JSONObject {
//        val jsonObj = JSONObject()
//        jsonObj.put(RES, currentResult)
//        jsonObj.put(PREVIOUS_CALCULATION, previousCalculation)
//        jsonObj.put(LAST_KEY, lastKey)
//        jsonObj.put(LAST_OPERATION, lastOperation)
//        jsonObj.put(BASE_VALUE, baseValue)
//        jsonObj.put(SECOND_VALUE, secondValue)
//        jsonObj.put(INPUT_DISPLAYED_FORMULA, inputDisplayedFormula)
//        return jsonObj
//    }
//}
