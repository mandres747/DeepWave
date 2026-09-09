package de.binauralbeats.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
)

private val onboardingPages = listOf(
    OnboardingPage(Icons.Default.Headphones, R.string.onboarding_1_title, R.string.onboarding_1_body),
    OnboardingPage(Icons.Default.Tune, R.string.onboarding_2_title, R.string.onboarding_2_body),
    OnboardingPage(Icons.Default.Bedtime, R.string.onboarding_3_title, R.string.onboarding_3_body),
    OnboardingPage(Icons.Default.Book, R.string.onboarding_4_title, R.string.onboarding_4_body)
)

/**
 * First-launch walkthrough asked for in the closed-test feedback (2026-08-29,
 * "Dynamic Walkthrough for New Users"). Skippable from every page - the report
 * explicitly called out that a forced tutorial works against the user.
 */
@Composable
fun OnboardingOverlay(onFinish: () -> Unit) {
    val colors = LocalBinauralColors.current
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surfaceDark,
                            colors.primaryDark,
                            colors.primaryMid,
                            colors.surfaceVariant
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        stringResource(R.string.skip),
                        color = colors.onSurfaceMuted,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.indices.forEach { index ->
                    val isActive = index == pagerState.currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isActive) colors.accentPrimary
                        else colors.overlay.copy(alpha = 0.25f),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLastPage) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val colors = LocalBinauralColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = colors.accentPrimary.copy(alpha = 0.12f),
            shape = CircleShape
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = colors.accentPrimary,
                modifier = Modifier
                    .padding(28.dp)
                    .size(48.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(page.titleRes),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(page.bodyRes),
            fontSize = 15.sp,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
