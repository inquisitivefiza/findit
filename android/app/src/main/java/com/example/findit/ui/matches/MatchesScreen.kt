package com.example.findit.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.findit.api.RetrofitClient
import com.example.findit.model.Match
import com.example.findit.ui.components.AppHeader
import com.example.findit.ui.theme.BlueContainer
import com.example.findit.ui.theme.BluePrimary
import com.example.findit.ui.theme.SurfaceGrey
import com.example.findit.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun MatchesScreen(itemId: Int) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadMatches() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = tokenManager.getToken()
                if (token == null) {
                    errorMessage = "You're not logged in"
                    isLoading = false
                    return@launch
                }
                val response = RetrofitClient.api.getMatches("Bearer $token", itemId)
                if (response.isSuccessful) {
                    matches = response.body()?.matches ?: emptyList()
                } else {
                    errorMessage = "Failed to load matches: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Cannot connect to server: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(itemId) { loadMatches() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        AppHeader(
            title = "Possible Matches",
            subtitle = "Items that might be related",
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadMatches() },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) { Text("Retry") }
                    }
                }
            }
            matches.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No matches found yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(matches) { match -> MatchCard(match = match) }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: Match) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = BlueContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${(match.similarity_score * 100).toInt()}% match",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MatchItemPreview(
                    label = "LOST",
                    title = match.lost_title,
                    location = match.lost_location,
                    imageUrl = match.lost_image,
                    labelColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.weight(1f)
                )
                MatchItemPreview(
                    label = "FOUND",
                    title = match.found_title,
                    location = match.found_location,
                    imageUrl = match.found_image,
                    labelColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MatchItemPreview(
    label: String,
    title: String,
    location: String,
    imageUrl: String?,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(color = labelColor, shape = RoundedCornerShape(6.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ── Image / placeholder — same light blue as the FAB plus icon ──
        if (imageUrl != null) {
            AsyncImage(
                model = RetrofitClient.BASE_URL.trimEnd('/') + imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BlueContainer)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BlueContainer)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            location,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1
        )
    }
}