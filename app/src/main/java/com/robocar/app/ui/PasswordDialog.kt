package com.robocar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.robocar.app.MainViewModel

private val Blue  = Color(0xFF3B82F6)
private val Muted = Color(0xFF64748B)
private val Card  = Color(0xFF1E293B)

@Composable
fun PasswordDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var pass by remember { mutableStateOf("") }

    fun send() {
        if (pass.isNotEmpty()) {
            viewModel.sendPassword(pass)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Темний scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable(onClick = onDismiss)
            )

            // Картка по центру
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Іконка замка вгорі
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Blue.copy(alpha = 0.15f))
                        .border(1.dp, Blue.copy(0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = 22.sp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "ДОСТУП",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Введіть пароль пристрою",
                    fontSize = 12.sp,
                    color = Muted
                )

                Spacer(Modifier.height(20.dp))

                // Поле пароля
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    placeholder = {
                        Text(
                            "••••",
                            color = Muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 20.sp
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { send() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = Blue,
                        focusedContainerColor = Card,
                        unfocusedContainerColor = Card
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Скасувати
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Card)
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Скасувати", color = Muted, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }

                    // OK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Blue)
                            .clickable(onClick = ::send),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
