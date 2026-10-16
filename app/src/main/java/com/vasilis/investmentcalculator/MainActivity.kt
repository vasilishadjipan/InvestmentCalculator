package com.vasilis.investmentcalculator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vasilis.investmentcalculator.ui.theme.InvestmentCalculatorTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AppOpenAd"

        // Google's official App Open test ad unit ID.
        private const val TEST_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/9257395921"

        // Your real App Open ad unit ID.
        private const val RELEASE_AD_UNIT_ID =
            "ca-app-pub-3707930885555910/2061274208"

        // App Open ads expire after 4 hours.
        private const val AD_EXPIRY_MS = 4 * 60 * 60 * 1000L
    }

    private var appOpenAd: AppOpenAd? = null
    private var adLoadTime = 0L

    private var isLoadingAd = false
    private var isShowingAd = false
    private var isMobileAdsInitialized = false

    private var isAppForeground = false

    // Prevents another ad from appearing during the same foreground session.
    private var hasShownAdThisForeground = false

    var showAds by mutableStateOf(true)
    var hasChosenAdsPreference by mutableStateOf(false)

    private val appOpenAdUnitId: String
        get() = if (BuildConfig.DEBUG) {
            TEST_AD_UNIT_ID
        } else {
            RELEASE_AD_UNIT_ID
        }

    /**
     * Observes the whole app's lifecycle rather than Activity onResume().
     * Showing or dismissing an ad must not trigger another ad immediately.
     */
    private val appLifecycleObserver = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            // The app has entered the foreground.
            isAppForeground = true
            hasShownAdThisForeground = false

            if (isMobileAdsInitialized && showAds) {
                if (isAdAvailable()) {
                    showAppOpenAd()
                } else {
                    loadAppOpenAd()
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            // The app has gone into the background.
            isAppForeground = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AnalyticsHelper.init(applicationContext)

        // Register once to observe app-level foreground/background changes.
        ProcessLifecycleOwner.get().lifecycle
            .addObserver(appLifecycleObserver)

        // Your existing Compose UI.
        setContent {
            InvestmentCalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (BuildConfig.DEBUG && !hasChosenAdsPreference) {
                        DebugAdChoiceScreen(
                            showAds = showAds,
                            onShowAdsChanged = { showAds = it },
                            onContinueClicked = {
                                hasChosenAdsPreference = true
                                if (showAds) {
                                    setupAds()
                                }
                            }
                        )
                    } else {
                        if (showAds && !isMobileAdsInitialized) {
                            setupAds()
                        }
                        InvestmentCalculatorScreen(showAds = showAds)
                    }
                }
            }
        }
    }

    private fun setupAds() {
        if (isMobileAdsInitialized) return
        MobileAds.initialize(applicationContext) {
            runOnUiThread {
                isMobileAdsInitialized = true
                if (showAds) {
                    loadAppOpenAd()
                }
            }
        }
    }

    /**
     * Loads an ad without displaying it automatically unless this is
     * the first available ad for the current foreground session.
     */
    private fun loadAppOpenAd() {
        if (!showAds) return
        if (!isMobileAdsInitialized) return
        if (isLoadingAd) return
        if (isAdAvailable()) return

        isLoadingAd = true

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            applicationContext,
            appOpenAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open ad loaded")

                    appOpenAd = ad
                    adLoadTime = System.currentTimeMillis()
                    isLoadingAd = false

                    // Show only if the app is foregrounded and no ad
                    // has already been shown in this foreground session.
                    if (isAppForeground && !hasShownAdThisForeground && showAds) {
                        showAppOpenAd()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load App Open ad: ${error.message}")

                    isLoadingAd = false
                    appOpenAd = null
                }
            }
        )
    }

    /**
     * Shows at most one App Open ad per foreground session.
     */
    private fun showAppOpenAd() {
        if (!showAds) return
        if (!isAppForeground) return
        if (isShowingAd) return
        if (hasShownAdThisForeground) return
        if (isFinishing || isDestroyed) return

        val ad = appOpenAd

        if (ad == null || !isAdAvailable()) {
            loadAppOpenAd()
            return
        }

        // Mark it as shown BEFORE displaying it, preventing lifecycle
        // callbacks from causing another ad to appear.
        hasShownAdThisForeground = true
        isShowingAd = true
        appOpenAd = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad displayed")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open ad dismissed")

                isShowingAd = false

                // Prepare the next ad, but do not display it immediately.
                // It can be displayed on the next genuine app foreground.
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Failed to show App Open ad: ${error.message}")

                isShowingAd = false
                loadAppOpenAd()
            }
        }

        ad.show(this)
    }

    /**
     * Checks that the loaded ad exists and has not expired.
     */
    private fun isAdAvailable(): Boolean {
        val ad = appOpenAd ?: return false

        if (System.currentTimeMillis() - adLoadTime >= AD_EXPIRY_MS) {
            appOpenAd = null
            return false
        }

        return true
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle
            .removeObserver(appLifecycleObserver)

        super.onDestroy()
    }
}
