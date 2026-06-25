package com.doomhamsters.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.R
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.DoomHamstersTheme
import com.doomhamsters.ui.theme.SoftWhite

/** Displays the app welcome screen and entry actions. */
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    viewModel: LobbyViewModel? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardDarkMaroon)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.welcome_message),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel?.currentStep = 2 },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = SoftWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                stringResource(R.string.start_button_text),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel?.currentStep = 5 },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = SoftWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.rules_button_text),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }
    }
}

/** Displays the game rules screen. */
@Composable
fun RulesScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardDarkMaroon)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.rules_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BackgroundCream,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = AnnotatedString.fromHtml(stringResource(R.string.rules_content)),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = CardDarkMaroon
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = SoftWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.rules_back),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }
    }
}

/** Previews the start screen in Android Studio. */
@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    DoomHamstersTheme {
        StartScreen()
    }
}

/** Previews the rules screen in Android Studio. */
@Preview(showBackground = true)
@Composable
fun RulesPreview() {
    DoomHamstersTheme {
        RulesScreen(onBackClick = {})
    }
}
