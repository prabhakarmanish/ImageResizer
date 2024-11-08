    package coded.toolbox.imageresizer


    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.net.Uri
    import android.provider.OpenableColumns
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.Button
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.asImageBitmap
    import androidx.compose.ui.graphics.painter.Painter
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.input.TextFieldValue
    import androidx.compose.ui.unit.dp
    import androidx.navigation.NavController
    import coil.compose.rememberAsyncImagePainter
    import coil.request.ImageRequest
    import coil.size.Scale
    import java.io.File
    import java.io.FileOutputStream
    import java.io.InputStream

    @Composable
    fun ImageDetailsScreen(navController: NavController) {
        val context = LocalContext.current
        val imageUri = navController.previousBackStackEntry?.savedStateHandle?.get<Uri>("imageUri")
        var imageResolution by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var imageSize by remember { mutableStateOf<Long?>(null) }
        var imageName by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var resizedImage by remember { mutableStateOf<Bitmap?>(null) }

        var width by remember { mutableStateOf(TextFieldValue("")) }
        var height by remember { mutableStateOf(TextFieldValue("")) }
        var showResizeDialog by remember { mutableStateOf(false) }

        LaunchedEffect(imageUri) {
            if (imageUri != null) {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                    inputStream?.let {
                        val bitmap = BitmapFactory.decodeStream(it)
                        imageResolution = Pair(bitmap.width, bitmap.height)

                        // Get file size
                        val fileSize = imageUri.toFileSize(context)
                        imageSize = fileSize

                        // Get file name
                        val cursor = context.contentResolver.query(imageUri, null, null, null, null)
                        cursor?.moveToFirst()
                        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        imageName = cursor?.getString(nameIndex!!)
                        cursor?.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {

                imageResolution?.let { resolution ->
                    Text(
                        text = "${resolution.first} x ${resolution.second} (${imageSize?.toFileSizeString()})",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                imageUri?.let {
                    val painter: Painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .scale(Scale.FILL)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Picked Image",
                        modifier = Modifier.size(200.dp)
                    )
                }

                imageName?.let {
                    Text(
                        text = it,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = { showResizeDialog = true },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Resize Image")
                }

                if (showResizeDialog) {
                    AlertDialog(
                        onDismissRequest = { showResizeDialog = false },
                        title = { Text("Enter new width and height") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = width,
                                    onValueChange = { width = it },
                                    label = { Text("Width") },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text("Height") },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    width.text.toIntOrNull()?.let { newWidth ->
                                        height.text.toIntOrNull()?.let { newHeight ->
                                            resizedImage = imageUri?.let {
                                                resizeImage(context, it, newWidth, newHeight)
                                            }
                                            resizedImage?.let {
                                                imageResolution = Pair(it.width, it.height)
                                                val resizedImageFile = File(context.cacheDir, "resized_image_${System.currentTimeMillis()}.jpg")
                                                imageSize = resizedImageFile.length()
                                                imageName = resizedImageFile.name
                                            }
                                            showResizeDialog = false
                                        }
                                    }
                                }
                            ) {
                                Text("Resize")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResizeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                resizedImage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Resized Image",
                        modifier = Modifier.size(200.dp)
                    )
                }

                imageResolution?.let { resolution ->
                    Text(
                        text = "${resolution.first} x ${resolution.second} (${imageSize?.toFileSizeString()})",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                imageName?.let {
                    Text(
                        text = it,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }


    fun resizeImage(context: android.content.Context, uri: Uri, width: Int, height: Int): Bitmap? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)

        // Save the resized image
        val outputFile = File(context.cacheDir, "resized_image_${System.currentTimeMillis()}.jpg") // Unique filename
        val outputStream = FileOutputStream(outputFile)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        // Return the resized bitmap
        return resizedBitmap
    }

    fun android.net.Uri.toFileSize(context: android.content.Context): Long {
        var fileSize: Long = 0
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                fileSize = it.getLong(sizeIndex)
            }
        }
        return fileSize
    }

    fun Long.toFileSizeString(): String {
        val kb = this / 1024
        return if (kb < 1024) "$kb KB" else "${kb / 1024} MB"
    }
