package de.binauralbeats.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.billing.Access
import de.binauralbeats.app.billing.Entitlements
import de.binauralbeats.app.billing.PurchaseResult
import de.binauralbeats.app.ui.theme.LocalBinauralColors

/**
 * The one place Premium and the bundle are sold, opened by every locked
 * element (decided 2026-09-24: locked things stay visible with a lock).
 * The bundle is shown only while none of its parts is owned - Play cannot
 * lower a bundle price by what someone already bought.
 */
@Composable
fun PremiumSheet(
    access: Access,
    prices: Map<String, String>,
    onPurchase: (Activity, String, (PurchaseResult) -> Unit) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalBinauralColors.current
    val context = LocalContext.current

    fun buy(productId: String) {
        val activity = context as? Activity
        if (activity == null) {
            Toast.makeText(context, R.string.rhythm_purchase_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        onPurchase(activity, productId) { result ->
            val message = when (result) {
                PurchaseResult.OWNED -> R.string.premium_thanks
                PurchaseResult.CANCELLED -> R.string.rhythm_purchase_cancelled
                PurchaseResult.UNAVAILABLE -> R.string.rhythm_purchase_unavailable
                PurchaseResult.ERROR -> R.string.rhythm_purchase_error
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (result == PurchaseResult.OWNED) onClose()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.premium_header),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close), tint = colors.onSurface)
                }
            }
            HorizontalDivider(color = colors.overlay.copy(0.06f))
            Spacer(Modifier.height(16.dp))

            if (access.premium) {
                FeatureLine(stringResource(R.string.premium_active))
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            Text(
                stringResource(R.string.premium_intro),
                fontSize = 13.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            listOf(
                R.string.premium_feature_presets,
                R.string.premium_feature_mixer,
                R.string.premium_feature_export,
                R.string.premium_feature_stats,
                R.string.premium_feature_custom
            ).forEach { FeatureLine(stringResource(it)) }

            Spacer(Modifier.height(16.dp))
            val premiumPrice = prices[Entitlements.PRODUCT_PREMIUM]
            Button(
                onClick = { buy(Entitlements.PRODUCT_PREMIUM) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentPrimary, contentColor = colors.onAccent)
            ) {
                Text(
                    if (premiumPrice != null) stringResource(R.string.premium_unlock_price, premiumPrice)
                    else stringResource(R.string.premium_unlock),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (access.offerBundle) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = colors.accentPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(R.string.bundle_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            stringResource(R.string.bundle_body),
                            fontSize = 12.sp,
                            color = colors.onSurfaceMuted,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                        val bundlePrice = prices[Entitlements.PRODUCT_COMPLETE]
                        OutlinedButton(onClick = { buy(Entitlements.PRODUCT_COMPLETE) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (bundlePrice != null) stringResource(R.string.bundle_buy_price, bundlePrice)
                                else stringResource(R.string.bundle_buy),
                                color = colors.accentPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wake_restore), fontSize = 12.sp, color = colors.onSurfaceMuted)
            }
            Text(
                stringResource(R.string.premium_promise),
                fontSize = 11.sp,
                color = colors.onSurfaceMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    val colors = LocalBinauralColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(Icons.Default.Check, null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = colors.onSurface)
    }
}

/** Small lock drawn over a locked toolbar icon or button. */
@Composable
fun LockBadge(size: Dp = 12.dp, tint: Color = LocalBinauralColors.current.onSurfaceMuted) {
    Icon(Icons.Default.Lock, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

/**
 * Pointer from an add-on's locked card to the bundle, shown only while the
 * bundle is on offer (nothing of it owned yet).
 */
@Composable
fun BundleHint(onClick: () -> Unit) {
    val colors = LocalBinauralColors.current
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.bundle_hint), fontSize = 12.sp, color = colors.accentPrimary)
    }
}
