package com.vasilis.investmentcalculator

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "InvestmentCalculatorScreen")
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logCalculateClicked(modeName: String) {
        val bundle = Bundle().apply {
            putString("calculation_mode", modeName)
        }
        firebaseAnalytics?.logEvent("calculate_clicked", bundle)
    }

    fun logResetClicked() {
        firebaseAnalytics?.logEvent("reset_clicked", Bundle.EMPTY)
    }
}
