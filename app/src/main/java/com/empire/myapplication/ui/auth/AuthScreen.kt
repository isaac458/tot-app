package com.empire.myapplication.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empire.myapplication.R
import com.empire.myapplication.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    onGuestLogin: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    
    val webClientId = "764601305581-sh36efhgg918hagaqbi113h1q78p0l9r.apps.googleusercontent.com"

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account?.idToken?.let { idToken ->
                    viewModel.firebaseAuthWithGoogle(idToken, onLoginSuccess)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    val launchGoogleSignIn = {
        googleSignInClient.signOut().addOnCompleteListener {
            launcher.launch(googleSignInClient.signInIntent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GeminiBg)
    ) {
        // Gradient Glow Effect in Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(modernGradientBackground())
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sparkle Icon
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Sparkle",
                modifier = Modifier
                    .size(80.dp)
                    .glowEffect(color = CosmicBlue, radius = 50.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "مرحباً بك في توت للذكاء الاصطناعي",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "مساعدك الذكي دائمًا هنا ✨",
                fontSize = 14.sp,
                color = GlassTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isLoading) {
                CircularProgressIndicator(color = CosmicViolet)
            } else {
                // Google Button (Glassmorphism)
                Button(
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("المتابعة باستخدام Google", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("أو", color = GlassTextHint, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(24.dp))

                // Guest Button (Gradient Glow)
                Button(
                    onClick = { 
                        viewModel.setGuestMode {
                            onGuestLogin()
                        } 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(24.dp, CircleShape, ambientColor = CosmicViolet, spotColor = CosmicBlue),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(buttonGradient()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("الاستمرار كضيف", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = error!!,
                    color = GeminiError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
