package com.trishit.egloo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.trishit.egloo.ui.components.PingoAvatar
import com.trishit.egloo.ui.theme.EglooColors

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val totalPages = 4

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding",
        ) { currentPage ->
            when (currentPage) {
                0 -> OnboardingWelcome()
                1 -> OnboardingHowItWorks()
                2 -> OnboardingPrivacy()
                3 -> OnboardingReady()
                else -> OnboardingWelcome()
            }
        }

        // Bottom nav
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 40.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Page dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalPages) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i == page) EglooColors.TealPrimary
                                else EglooColors.TealPrimary.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // CTA button
            Button(
                onClick = { if (page < totalPages - 1) page++ else onComplete() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (page < totalPages - 1) "Continue" else "Let's go ❄",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (page == 0) {
                TextButton(onClick = onComplete) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    emoji: String,
    title: String,
    body: String,
    extraContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 100.dp, bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Pingo illustration placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(EglooColors.TealDarker.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(24.dp))
        extraContent()
    }
}

@Composable
private fun OnboardingWelcome() = OnboardingPage(
    emoji = "🐧",
    title = "Meet Pingo",
    body = "Your personal AI assistant who lives in an igloo and keeps your knowledge safe, organised, and always at hand.",
)

@Composable
private fun OnboardingHowItWorks() = OnboardingPage(
    emoji = "❄",
    title = "Your igloo of knowledge",
    body = "Connect Gmail, Slack, and Drive. Pingo reads everything and stores it safely in the igloo — then answers your questions in plain English.",
    extraContent = {
        // How it works steps
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HowItWorksStep("1", "Connect your tools")
            HowItWorksStep("2", "Pingo reads & understands")
            HowItWorksStep("3", "Ask anything, get answers")
        }
    }
)

@Composable
private fun OnboardingPrivacy() = OnboardingPage(
    emoji = "🔒",
    title = "Your data stays yours",
    body = "Everything lives in your igloo. Your data is encrypted and never used to train AI models. Pingo works for you, not for us.",
)

@Composable
private fun OnboardingReady() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp, bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(EglooColors.TealDarker.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            PingoAvatar(size = 80.dp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Pingo is ready!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "The igloo is built. Connect your first source and let Pingo get to work.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HowItWorksStep(number: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EglooColors.TealPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
