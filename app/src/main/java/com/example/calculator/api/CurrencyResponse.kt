package com.example.calculator.api

data class CurrencyResponse(
    val success: Boolean,
    val rates: Map<String, Double>
)
