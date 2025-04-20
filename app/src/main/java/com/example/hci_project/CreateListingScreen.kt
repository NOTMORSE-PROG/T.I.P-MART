package com.example.hci_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.hci_project.model.Product
import com.example.hci_project.viewmodel.AuthViewModel
import com.example.hci_project.viewmodel.ProductViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBackClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val productState by productViewModel.productState.collectAsState()

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedCampus by remember { mutableStateOf("") }

    // Validation state
    var titleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var campusError by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    // Dropdown state
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isCampusDropdownExpanded by remember { mutableStateOf(false) }

    // Image state
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Permission dialog state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDialogText by remember { mutableStateOf("") }

    // Success dialog state - controlled separately from productState
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdProduct by remember { mutableStateOf<Product?>(null) }

    // Navigation state
    var shouldNavigateHome by remember { mutableStateOf(false) }

    // Available options
    val categories = listOf(
        "Electronics", "Books", "Clothing", "Food", "Services",
        "Accessories", "School Supplies"
    )

    val campuses = listOf("Manila", "Quezon City")

    // Add a variable to track which action is pending permission approval
    var pendingAction by remember { mutableStateOf("") }

    // Check permissions on initial load
    val cameraPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val storagePermissionState = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            // Add the camera image to the list
            selectedImages = selectedImages + cameraUri!!
            imageError = false
            Log.d("CreateListingScreen", "Camera capture successful: ${cameraUri.toString()}")
        } else {
            Log.e("CreateListingScreen", "Camera capture failed. Success: $success, URI: $cameraUri")
        }
    }

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Add new images to the existing list
            selectedImages = selectedImages + uris
            imageError = false
            Log.d("CreateListingScreen", "Gallery selection successful: ${uris.size} images")
        } else {
            Log.d("CreateListingScreen", "Gallery selection returned no images")
        }
    }

    // Permission launcher with improved handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Update permission states
        permissions[Manifest.permission.CAMERA]?.let { granted ->
            cameraPermissionState.value = granted
            Log.d("CreateListingScreen", "Camera permission granted: $granted")
        }

        // Check for the appropriate storage permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES]?.let { granted ->
                storagePermissionState.value = granted
                Log.d("CreateListingScreen", "READ_MEDIA_IMAGES permission granted: $granted")
            }
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE]?.let { granted ->
                storagePermissionState.value = granted &&
                        (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED)
                Log.d("CreateListingScreen", "READ_EXTERNAL_STORAGE permission granted: $granted")
            }
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]?.let { granted ->
                storagePermissionState.value = granted &&
                        (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED)
                Log.d("CreateListingScreen", "WRITE_EXTERNAL_STORAGE permission granted: $granted")
            }
        }

        // Check if we have all the permissions we need for the pending action
        when (pendingAction) {
            "camera" -> {
                if (cameraPermissionState.value && storagePermissionState.value) {
                    // Both permissions granted, proceed with camera
                    launchCamera(context) { uri, error ->
                        if (uri != null) {
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Show dialog if permissions were denied
                    if (!cameraPermissionState.value || !storagePermissionState.value) {
                        permissionDialogText = "Camera and storage permissions are required to take photos. Please enable them in app settings."
                        showPermissionDialog = true
                    }
                }
            }
            "gallery" -> {
                if (storagePermissionState.value) {
                    // Permission granted, proceed with gallery
                    galleryLauncher.launch("image/*")
                } else {
                    // Show dialog if permission was denied
                    permissionDialogText = "Storage permission is required to select images. Please enable it in app settings."
                    showPermissionDialog = true
                }
            }
        }

        // Reset pending action after handling
        if (pendingAction.isNotEmpty()) {
            pendingAction = ""
        }
    }

    // Request permissions on initial load if needed
    LaunchedEffect(Unit) {
        // Check if we need to request permissions on startup
        val permissionsToRequest = mutableListOf<String>()

        if (!cameraPermissionState.value) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (!storagePermissionState.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // Log initial permission states
        Log.d("CreateListingScreen", "Initial camera permission: ${cameraPermissionState.value}")
        Log.d("CreateListingScreen", "Initial storage permission: ${storagePermissionState.value}")
    }

    // Handle navigation to home when requested
    LaunchedEffect(shouldNavigateHome) {
        if (shouldNavigateHome) {
            Log.d("CreateListingScreen", "Navigating to home screen")
            onBackClick()
        }
    }

    // Handle product creation state
    LaunchedEffect(productState) {
        Log.d("CreateListingScreen", "Product state changed: $productState")

        when (productState) {
            is ProductViewModel.ProductState.ProductCreated -> {
                val product = (productState as ProductViewModel.ProductState.ProductCreated).product
                Log.d("CreateListingScreen", "Product created successfully: ${product.id}")

                // Store the created product and show success dialog
                createdProduct = product
                showSuccessDialog = true
            }
            is ProductViewModel.ProductState.Error -> {
                val errorMessage = (productState as ProductViewModel.ProductState.Error).message
                Toast.makeText(
                    context,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // No action needed for other states
            }
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text(permissionDialogText) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success dialog - using Dialog instead of AlertDialog for better visibility
    if (showSuccessDialog) {
        Log.d("CreateListingScreen", "Showing success dialog for product: ${createdProduct?.id}")
        Dialog(
            onDismissRequest = { /* Prevent dismissal by tapping outside */ }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Success!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your listing has been created successfully!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Go to Home button
                        Button(
                            onClick = {
                                Log.d("CreateListingScreen", "Go to Home clicked")
                                showSuccessDialog = false
                                productViewModel.resetProductState()
                                shouldNavigateHome = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go to Home")
                        }

                        // Create Another button
                        OutlinedButton(
                            onClick = {
                                Log.d("CreateListingScreen", "Create Another clicked")
                                showSuccessDialog = false
                                productViewModel.resetProductState()

                                // Reset form fields
                                title = ""
                                description = ""
                                price = ""
                                quantity = "1"
                                selectedCategory = ""
                                selectedCampus = ""
                                selectedImages = emptyList()
                                createdProduct = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Another")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Product Images Section
                Text(
                    text = "Product Images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Image selection row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add image button
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                // Show image source options dialog
                                val options = arrayOf("Take Photo", "Choose from Gallery")

                                android.app.AlertDialog.Builder(context)
                                    .setTitle("Add Product Image")
                                    .setItems(options) { _, which ->
                                        when (which) {
                                            0 -> { // Camera option
                                                pendingAction = "camera"
                                                if (cameraPermissionState.value && storagePermissionState.value) {
                                                    // Already have permissions, proceed
                                                    launchCamera(context) { uri, error ->
                                                        if (uri != null) {
                                                            cameraUri = uri
                                                            cameraLauncher.launch(uri)
                                                        } else {
                                                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                } else {
                                                    // Request permissions
                                                    val permissionsToRequest = mutableListOf<String>()

                                                    if (!cameraPermissionState.value) {
                                                        permissionsToRequest.add(Manifest.permission.CAMERA)
                                                    }

                                                    if (!storagePermissionState.value) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                                                        } else {
                                                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                        }
                                                    }

                                                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                                }
                                            }
                                            1 -> { // Gallery option
                                                pendingAction = "gallery"
                                                if (storagePermissionState.value) {
                                                    // Already have permission, proceed
                                                    galleryLauncher.launch("image/*")
                                                } else {
                                                    // Request permission
                                                    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                                    } else {
                                                        arrayOf(
                                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                        )
                                                    }

                                                    permissionLauncher.launch(permissionToRequest)
                                                }
                                            }
                                        }
                                    }
                                    .show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add Image",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Selected images
                    selectedImages.forEachIndexed { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(uri)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Product Image $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Delete button
                            IconButton(
                                onClick = {
                                    selectedImages = selectedImages.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (imageError) {
                    Text(
                        text = "Please add at least one image",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Product Title") },
                    placeholder = { Text("Enter product title") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title is required") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = false
                    },
                    label = { Text("Description") },
                    placeholder = { Text("Enter product description") },
                    isError = descriptionError,
                    supportingText = if (descriptionError) {
                        { Text("Description is required") }
                    } else null,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Price and Quantity Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Price Field
                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            // Only allow numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                price = it
                                priceError = false
                            }
                        },
                        label = { Text("Price (₱)") },
                        placeholder = { Text("0.00") },
                        isError = priceError,
                        supportingText = if (priceError) {
                            { Text("Valid price required") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Text(
                                text = "₱",
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Quantity Field
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            // Only allow positive integers
                            if (it.isEmpty() || it.matches(Regex("^[1-9]\\d*$"))) {
                                quantity = it
                                quantityError = false
                            }
                        },
                        label = { Text("Quantity") },
                        placeholder = { Text("1") },
                        isError = quantityError,
                        supportingText = if (quantityError) {
                            { Text("Valid quantity required") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        placeholder = { Text("Select a category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                        },
                        isError = categoryError,
                        supportingText = if (categoryError) {
                            { Text("Category is required") }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryError = false
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campus Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCampusDropdownExpanded,
                    onExpandedChange = { isCampusDropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCampus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Campus") },
                        placeholder = { Text("Select a campus") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCampusDropdownExpanded)
                        },
                        isError = campusError,
                        supportingText = if (campusError) {
                            { Text("Campus is required") }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isCampusDropdownExpanded,
                        onDismissRequest = { isCampusDropdownExpanded = false }
                    ) {
                        campuses.forEach { campus ->
                            DropdownMenuItem(
                                text = { Text(campus) },
                                onClick = {
                                    selectedCampus = campus
                                    campusError = false
                                    isCampusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        // Validate form
                        var isValid = true

                        if (title.isBlank()) {
                            titleError = true
                            isValid = false
                        }

                        if (description.isBlank()) {
                            descriptionError = true
                            isValid = false
                        }

                        if (price.isBlank() || price.toDoubleOrNull() == null || price.toDouble() <= 0) {
                            priceError = true
                            isValid = false
                        }

                        if (quantity.isBlank() || quantity.toIntOrNull() == null || quantity.toInt() <= 0) {
                            quantityError = true
                            isValid = false
                        }

                        if (selectedCategory.isBlank()) {
                            categoryError = true
                            isValid = false
                        }

                        if (selectedCampus.isBlank()) {
                            campusError = true
                            isValid = false
                        }

                        if (selectedImages.isEmpty()) {
                            imageError = true
                            isValid = false
                        }

                        if (isValid && currentUser != null) {
                            // Submit the product
                            productViewModel.createProduct(
                                title = title,
                                description = description,
                                price = price.toDouble(),
                                quantity = quantity.toInt(),
                                category = selectedCategory,
                                campus = selectedCampus,
                                imageUris = selectedImages,
                                currentUser = currentUser!!
                            )
                        } else if (currentUser == null) {
                            Toast.makeText(
                                context,
                                "You must be logged in to create a listing",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = productState !is ProductViewModel.ProductState.Loading
                ) {
                    if (productState is ProductViewModel.ProductState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Listing")
                    }
                }
            }

            // Loading overlay
            if (productState is ProductViewModel.ProductState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Creating listing...")
                        }
                    }
                }
            }
        }
    }
}

// Helper function to create a temporary file for camera photos and handle FileProvider
private fun launchCamera(context: android.content.Context, callback: (Uri?, String?) -> Unit) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

        if (storageDir == null) {
            callback(null, "Storage directory not available")
            return
        }

        // Ensure the directory exists
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                callback(null, "Failed to create directory")
                return
            }
        }

        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        // Log file path for debugging
        Log.d("CreateListingScreen", "Created image file: ${imageFile.absolutePath}")

        // Create URI using FileProvider
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        // Log URI for debugging
        Log.d("CreateListingScreen", "Created image URI: $imageUri")

        callback(imageUri, null)
    } catch (e: Exception) {
        Log.e("CreateListingScreen", "Error creating image file: ${e.message}", e)
        callback(null, e.message ?: "Unknown error")
    }
}
