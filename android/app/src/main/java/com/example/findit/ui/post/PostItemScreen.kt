package com.example.findit.ui.post

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
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
import coil.compose.rememberAsyncImagePainter
import com.example.findit.api.RetrofitClient
import com.example.findit.ui.components.*
import com.example.findit.ui.theme.BlueContainer
import com.example.findit.ui.theme.BluePrimary
import com.example.findit.ui.theme.SurfaceGrey
import com.example.findit.utils.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private enum class ItemType(val label: String, val apiValue: String) {
    LOST("Lost", "LOST"),
    FOUND("Found", "FOUND")
}

@Composable
fun PostItemScreen(onPostSuccess: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(ItemType.LOST) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> imageUri = uri }

    fun submit() {
        if (title.isBlank() || category.isBlank() || location.isBlank()) {
            errorMessage = "Please fill in title, category and location"
            return
        }
        scope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                val token = tokenManager.getToken()
                if (token == null) {
                    errorMessage = "You're not logged in"
                    isSubmitting = false
                    return@launch
                }

                val imagePart = imageUri?.let { uriToMultipart(context, it) }

                val response = RetrofitClient.api.postItem(
                    token = "Bearer $token",
                    type = selectedType.apiValue.toRequestBody("text/plain".toMediaTypeOrNull()),
                    title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    category = category.toRequestBody("text/plain".toMediaTypeOrNull()),
                    location = location.toRequestBody("text/plain".toMediaTypeOrNull()),
                    image = imagePart
                )

                if (response.isSuccessful) {
                    onPostSuccess()
                } else {
                    errorMessage = "Failed to post item: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                errorMessage = "Cannot connect to server: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        AppHeader(
            title = "Post an Item",
            subtitle = "Help reunite lost items with their owners",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        SectionLabel("Type", modifier = Modifier.padding(bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ItemType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                        Text(
                            type.label,
                            fontWeight = if (selectedType == type) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = BluePrimary,
                        selectedContainerColor = BlueContainer,
                        selectedLabelColor = BluePrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedType == type,
                        borderColor = BlueContainer,
                        selectedBorderColor = BlueContainer,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppTextField(value = title, onValueChange = { title = it }, label = "Title")
        Spacer(modifier = Modifier.height(14.dp))

        AppTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            singleLine = false,
            minLines = 3
        )
        Spacer(modifier = Modifier.height(14.dp))

        AppTextField(value = category, onValueChange = { category = it }, label = "Category")
        Spacer(modifier = Modifier.height(14.dp))

        AppTextField(value = location, onValueChange = { location = it }, label = "Location")
        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("Photo (optional)", modifier = Modifier.padding(bottom = 10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(BlueContainer)
                .clickable {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap to add a photo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BluePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        errorMessage?.let { AppErrorText(it) }

        AppPrimaryButton(
            text = "Post Item",
            onClick = { submit() },
            loading = isSubmitting
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val inputStream = contentResolver.openInputStream(uri) ?: return null
    val bytes = inputStream.readBytes()
    inputStream.close()
    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    val fileName = "upload_${System.currentTimeMillis()}.jpg"
    return MultipartBody.Part.createFormData("image", fileName, requestBody)
}