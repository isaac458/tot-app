package com.empire.myapplication.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empire.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    val genders = listOf("ذكر", "أنثى")

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
                .padding(24.dp)
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "إكمال إعداد حسابك",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = GlassText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "نريد معرفة المزيد عنك لتقديم تجربة أفضل.",
                fontSize = 14.sp,
                color = GlassTextSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل", color = GlassTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicViolet,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = CosmicViolet,
                    unfocusedLabelColor = GlassTextSecondary,
                    focusedTextColor = GlassText,
                    unfocusedTextColor = GlassText,
                    cursorColor = CosmicViolet,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filter { c -> c.isDigit() } },
                label = { Text("العمر", color = GlassTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicViolet,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = CosmicViolet,
                    unfocusedLabelColor = GlassTextSecondary,
                    focusedTextColor = GlassText,
                    unfocusedTextColor = GlassText,
                    cursorColor = CosmicViolet,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الجنس:", fontWeight = FontWeight.Normal, color = GlassTextSecondary, modifier = Modifier.padding(end = 16.dp))
                genders.forEach { g ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        RadioButton(
                            selected = gender == g,
                            onClick = { gender = g },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CosmicViolet,
                                unselectedColor = GlassTextSecondary
                            )
                        )
                        Text(text = g, color = GlassText, fontWeight = FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Consent Checkbox — لا يمكن إنشاء الحساب إلا بعد الموافقة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle)
                    .clickable { acceptedTerms = !acceptedTerms }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = CosmicViolet)
                )
                Text(
                    "أوافق على سياسة الخصوصية وشروط الاستخدام",
                    color = GlassTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.saveOnboardingData(name, age, gender, acceptedTerms, onComplete)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(24.dp, CircleShape, ambientColor = CosmicViolet, spotColor = CosmicBlue),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                enabled = name.isNotBlank() && age.isNotBlank() && gender.isNotBlank() && acceptedTerms
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonGradient()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ابدأ المحادثة ✨", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }
        }
    }
}
