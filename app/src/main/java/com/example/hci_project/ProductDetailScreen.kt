package com.example.hci_project

import android.util.Log
import androidx.compose.foundation.Image
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.hci_project.model.Rating
import com.example.hci_project.utils.toTitleCase
import com.example.hci_project.viewmodel.AuthViewModel
import com.example.hci_project.viewmodel.MessageViewModel
import com.example.hci_project.viewmodel.ProductViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
    onMessageClick: (String) -> Unit = {}, // New parameter for message navigation
    productViewModel: ProductViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    messageViewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val productState by productViewModel.productState.collectAsState()
    val selectedProduct by productViewModel.selectedProduct.collectAsState()
    val productRatings by productViewModel.productRatings.collectAsState()
    val userRating by productViewModel.userRating.collectAsState()
    val cartItemCount by productViewModel.cartItemCount.collectAsState()
    val sellerProfilePicture by productViewModel.sellerProfilePicture.collectAsState()
    val messageState by messageViewModel.messageState.collectAsState()
    val currentConversation by messageViewModel.currentConversation.collectAsState()

    var initialDataLoaded by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var userRatingValue by remember { mutableFloatStateOf(0f) }
    var userComment by remember { mutableStateOf("") }
    var showAddedToCartDialog by remember { mutableStateOf(false) }
    var showDeleteRatingDialog by remember { mutableStateOf(false) }
    var ratingToDelete by remember { mutableStateOf<Rating?>(null) }
    var showStartChatDialog by remember { mutableStateOf(false) }

    // Calculate average rating
    val averageRating = if (productRatings.isNotEmpty()) {
        productRatings.map { it.rating }.average().toFloat()
    } else {
        0f
    }

    // Observe lifecycle events to refresh data when the screen is resumed
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh data when screen is resumed
                Log.d("ProductDetailScreen", "Screen resumed, refreshing data")
                productViewModel.fetchRatingsForProduct(productId)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial data loading
    LaunchedEffect(productId) {
        Log.d("ProductDetailScreen", "Initial data loading for product: $productId")

        // Clear previous product ratings first
        productViewModel.clearProductRatings()

        // First fetch the product
        productViewModel.getProductById(productId)

        // Immediately fetch ratings - don't wait
        productViewModel.fetchRatingsForProduct(productId)

        // Fetch user's rating if logged in
        currentUser?.userId?.let { userId ->
            productViewModel.getUserRatingForProduct(productId, userId)
        }

        // Fetch cart items if logged in
        currentUser?.userId?.let { userId ->
            productViewModel.fetchCartItems(userId)
        }

        initialDataLoaded = true
    }

    // Add a debug log to track when ratings are updated
    LaunchedEffect(initialDataLoaded, productRatings) {
        if (initialDataLoaded) {
            Log.d("ProductDetailScreen", "Product ratings updated after initial load: ${productRatings.size} ratings")
        }
    }

    // Add another LaunchedEffect to refresh ratings when the screen is focused
    LaunchedEffect(Unit) {
        // This will run when the screen is first displayed
        Log.d("ProductDetailScreen", "Force refreshing ratings for product: $productId")
        delay(800) // Give a longer delay for initial load
        productViewModel.fetchRatingsForProduct(productId)
    }

    // Debug logging for productRatings changes
    LaunchedEffect(productRatings) {
        Log.d("ProductDetailScreen", "ProductRatings updated: ${productRatings.size} ratings")
        productRatings.forEachIndexed { index, rating ->
            Log.d("ProductDetailScreen", "Rating $index: ${rating.userName}, ${rating.rating}, ${rating.comment}")
        }
    }

    // Initialize user rating and comment if they already rated this product
    LaunchedEffect(userRating) {
        userRating?.let {
            userRatingValue = it.rating
            userComment = it.comment
            Log.d("ProductDetailScreen", "Loaded existing user rating: ${it.rating}, comment: ${it.comment}")
        }
    }

    // Handle message state changes
    LaunchedEffect(messageState) {
        when (messageState) {
            is MessageViewModel.MessageState.Success -> {
                // If we have a conversation, navigate to it
                currentConversation?.let { conversation ->
                    onMessageClick(conversation.id)
                    messageViewModel.resetMessageState()
                }
            }
            is MessageViewModel.MessageState.Error -> {
                Toast.makeText(
                    context,
                    (messageState as MessageViewModel.MessageState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                messageViewModel.resetMessageState()
            }
            else -> {}
        }
    }

    // Show added to cart dialog when product is added to cart
    LaunchedEffect(productState) {
        when (productState) {
            is ProductViewModel.ProductState.ProductAddedToCart -> {
                showAddedToCartDialog = true
                productViewModel.resetProductState()
            }
            is ProductViewModel.ProductState.RatingAdded -> {
                // Show success toast
                Toast.makeText(
                    context,
                    "Review submitted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("ProductDetailScreen", "Rating added, refreshing ratings")
                // Explicitly refresh ratings after a new rating is added
                productViewModel.fetchRatingsForProduct(productId)
                productViewModel.resetProductState()
            }
            is ProductViewModel.ProductState.RatingDeleted -> {
                // Show success toast
                Toast.makeText(
                    context,
                    "Review deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("ProductDetailScreen", "Rating deleted, refreshing ratings")
                // Explicitly refresh ratings after a rating is deleted
                productViewModel.fetchRatingsForProduct(productId)
                productViewModel.resetProductState()
            }
            is ProductViewModel.ProductState.Error -> {
                Toast.makeText(
                    context,
                    (productState as ProductViewModel.ProductState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                productViewModel.resetProductState()
            }
            else -> {}
        }
    }

    // Rating dialog
    if (showRatingDialog) {
        Dialog(onDismissRequest = { showRatingDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rate This Product",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Star rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= userRatingValue) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "Star $i",
                                tint = if (i <= userRatingValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { userRatingValue = i.toFloat() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment field
                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        label = { Text("Your Review (Optional)") },
                        placeholder = { Text("Share your thoughts about this product...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showRatingDialog = false }
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                // Submit rating
                                currentUser?.let { user ->
                                    Log.d("ProductDetailScreen", "Submitting rating: productId=$productId, userId=${user.userId}, rating=$userRatingValue, comment=$userComment")

                                    // Show loading toast
                                    Toast.makeText(
                                        context,
                                        "Submitting your review...",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    productViewModel.addRating(
                                        productId = productId,
                                        userId = user.userId,
                                        userName = user.fullname,
                                        rating = userRatingValue,
                                        comment = userComment
                                    )
                                }
                                showRatingDialog = false
                            },
                            enabled = userRatingValue > 0
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }

    // Delete rating confirmation dialog
    if (showDeleteRatingDialog && ratingToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteRatingDialog = false
                ratingToDelete = null
            },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete your review? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        ratingToDelete?.let { rating ->
                            productViewModel.deleteRating(rating.id, productId)
                        }
                        showDeleteRatingDialog = false
                        ratingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteRatingDialog = false
                        ratingToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Start chat confirmation dialog
    if (showStartChatDialog) {
        AlertDialog(
            onDismissRequest = { showStartChatDialog = false },
            title = { Text("Message Seller") },
            text = { Text("Would you like to start a conversation with ${selectedProduct?.sellerName?.toTitleCase() ?: "the seller"}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showStartChatDialog = false

                        // Start a conversation
                        currentUser?.let { user ->
                            selectedProduct?.let { product ->
                                messageViewModel.startOrGetConversation(
                                    currentUser = user,
                                    sellerId = product.sellerId,
                                    sellerName = product.sellerName,
                                    product = product
                                )
                            }
                        } ?: run {
                            Toast.makeText(
                                context,
                                "You need to be logged in to send messages",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Start Chat")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartChatDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Added to cart dialog
    if (showAddedToCartDialog) {
        AlertDialog(
            onDismissRequest = { showAddedToCartDialog = false },
            title = { Text("Added to Cart") },
            text = { Text("Product has been added to your cart.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAddedToCartDialog = false
                        onCartClick()
                    }
                ) {
                    Text("Go to Cart")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddedToCartDialog = false }
                ) {
                    Text("Continue Shopping")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Cart icon with badge
                    Box(
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        IconButton(onClick = onCartClick) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }

                        if (cartItemCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-8).dp, y = 8.dp)
                            ) {
                                Text(text = cartItemCount.toString())
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedProduct == null) {
                // Loading state
                if (productState is ProductViewModel.ProductState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Error state
                else if (productState is ProductViewModel.ProductState.Error) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = (productState as? ProductViewModel.ProductState.Error)?.message
                                    ?: "Failed to load product",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { productViewModel.getProductById(productId) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            } else {
                // Product details content
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Product images
                    item {
                        val product = selectedProduct!!

                        if (product.imageUrls.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { product.imageUrls.size })

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                // Image pager
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(product.imageUrls[page])
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Product image ${page + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Page indicator
                                if (product.imageUrls.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        repeat(product.imageUrls.size) { index ->
                                            val isSelected = pagerState.currentPage == index
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(if (isSelected) 10.dp else 8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Placeholder if no images
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Product info
                    item {
                        val product = selectedProduct!!

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Title and price
                            Text(
                                text = product.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "₱${product.price}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Rating summary
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Star rating
                                Row {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = if (i <= averageRating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                            contentDescription = null,
                                            tint = if (i <= averageRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Average rating text
                                Text(
                                    text = if (productRatings.isNotEmpty()) {
                                        String.format(Locale.US, "%.1f", averageRating)
                                    } else {
                                        "No ratings yet"
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Rating count
                                if (productRatings.isNotEmpty()) {
                                    Text(
                                        text = "(${productRatings.size} ${if (productRatings.size == 1) "review" else "reviews"})",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Seller info
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Seller avatar - use profile picture if available
                                    if (sellerProfilePicture.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                ImageRequest.Builder(context)
                                                    .data(sellerProfilePicture)
                                                    .crossfade(true)
                                                    .build()
                                            ),
                                            contentDescription = "Seller avatar",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = product.sellerName.toTitleCase().firstOrNull()?.toString() ?: "S",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Seller info
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = product.sellerName.toTitleCase(),
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = "Campus: ${product.campus}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Message button - now opens the chat dialog
                                    IconButton(
                                        onClick = {
                                            // Don't allow messaging your own products
                                            if (product.sellerId == currentUser?.userId) {
                                                Toast.makeText(
                                                    context,
                                                    "You cannot message yourself",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                showStartChatDialog = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "Message seller"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Description
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Product details
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Category
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Category",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Text(
                                    text = product.category,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Quantity available
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Available",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Text(
                                    text = "${product.quantity} ${if (product.quantity == 1) "item" else "items"}",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Status",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Text(
                                    text = product.status.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Medium,
                                    color = when (product.status) {
                                        "active" -> MaterialTheme.colorScheme.primary
                                        "sold" -> MaterialTheme.colorScheme.error
                                        "reserved" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Quantity selector and Add to Cart button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quantity selector
                                Row(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease quantity"
                                        )
                                    }

                                    Text(
                                        text = quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )

                                    IconButton(
                                        onClick = { if (quantity < product.quantity) quantity++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase quantity"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Add to Cart button
                                Button(
                                    onClick = {
                                        currentUser?.userId?.let { userId ->
                                            Log.d("ProductDetailScreen", "Add to Cart button clicked: productId=$productId, userId=$userId, quantity=$quantity")
                                            productViewModel.addToCart(userId, productId, quantity)
                                        } ?: run {
                                            Log.e("ProductDetailScreen", "Cannot add to cart: User is null")
                                            // Show a toast to inform the user they need to be logged in
                                            Toast.makeText(
                                                context,
                                                "You need to be logged in to add items to cart",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.weight(0.6f),
                                    enabled = product.status == "active" && product.quantity > 0 && currentUser != null
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("Add to Cart")
                                }
                            }

                            if (product.status != "active") {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (product.status) {
                                        "sold" -> "This product has been sold"
                                        "reserved" -> "This product is currently reserved"
                                        else -> "This product is not available"
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Reviews section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reviews",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                // Add review button
                                if (currentUser != null) {
                                    TextButton(
                                        onClick = { showRatingDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RateReview,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = if (userRating != null) "Edit Review" else "Write a Review"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Reviews
                    if (productRatings.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reviews yet. Be the first to review this product!",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(productRatings) { rating ->
                            ReviewItem(
                                rating = rating,
                                currentUserId = currentUser?.userId,
                                onDeleteClick = {
                                    ratingToDelete = rating
                                    showDeleteRatingDialog = true
                                }
                            )
                        }

                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Loading indicator for message operations
            if (messageState is MessageViewModel.MessageState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    rating: Rating,
    currentUserId: String?,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) } // Added year for clarity
    var showOptions by remember { mutableStateOf(false) }

    // Check if this review belongs to the current user
    val isCurrentUserReview = currentUserId != null && rating.userId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Reviewer info and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar - use profile picture if available, otherwise show initial
                        if (rating.userProfilePicture.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(rating.userProfilePicture)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "User avatar",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rating.userName.firstOrNull()?.toString()?.toTitleCase() ?: "U",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = dateFormat.format(rating.createdAt.toDate()),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = rating.userName.toTitleCase(),
                                fontWeight = FontWeight.Medium
                            )

                            // Show "You" badge if this is the current user's review
                            if (isCurrentUserReview) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "You",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Options menu
                if (isCurrentUserReview) {
                    Box {
                        IconButton(
                            onClick = { showOptions = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Review") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptions = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rating stars
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= rating.rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                        contentDescription = null,
                        tint = if (i <= rating.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Comment
            if (rating.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

