package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.vasilis.investmentcalculator.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.math.roundToInt

@Composable
fun AdBanner(
    modifier: Modifier = Modifier
) {
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // Google Test Banner Ad Unit ID
    } else {
        "ca-app-pub-3707930885555910/1093246857" // Your Real Live Banner Ad Unit ID
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        (context.resources.displayMetrics.widthPixels /
                                context.resources.displayMetrics.density).roundToInt()
                    )
                )
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView -> adView.destroy() }
    )
}
