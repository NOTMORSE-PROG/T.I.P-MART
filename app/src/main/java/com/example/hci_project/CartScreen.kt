package com.example.hci_project

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.hci_project.model.Product
import com.example.hci_project.viewmodel.AuthViewModel
import com.example.hci_project.viewmodel.ProductViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    onProductClick: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val cartItems by productViewModel.cartItems.collectAsState()

    // State to track loaded products
    var cartProducts by remember { mutableStateOf<Map<String, Product?>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Format currency
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    // Calculate total
    val subtotal = cartProducts.entries.sumOf { (productId, product) ->
        val quantity = cartItems[productId] ?: 0
        val price = product?.price ?: 0.0
        quantity * price
    }

    // Shipping fee (fixed for now)
    val shippingFee = 50.0

    // Total
    val total = subtotal + shippingFee

    // Load cart products
    LaunchedEffect(cartItems) {
        isLoading = true

        val productMap = mutableMapOf<String, Product?>()

        for (productId in cartItems.keys) {
            try {
                productViewModel.getProductById(productId)
                // We'll handle the result in the ProductViewModel
            } catch (e: Exception) {
                Log.e("CartScreen", "Error loading product $productId: ${e.message}")
                productMap[productId] = null
            }
        }

        isLoading = false
    }

    // Update cart products when selected product changes
    val selectedProduct by productViewModel.selectedProduct.collectAsState()

    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { product ->
            cartProducts = cartProducts.toMutableMap().apply {
                this[product.id] = product
            }
        }
    }

    // Fetch cart items when the screen is first displayed
    LaunchedEffect(Unit) {
        currentUser?.userId?.let { userId ->
            productViewModel.fetchCartItems(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Cart") },
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
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (cartItems.isEmpty()) {
                // Empty cart
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Browse products and add items to your cart",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Browse Products")
                    }
                }
            } else {
                // Cart with items
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Cart items list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Your Items",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }

                        items(cartItems.size) { index ->
                            val productId = cartItems.keys.elementAt(index)
                            val quantity = cartItems[productId] ?: 0
                            val product = cartProducts[productId]

                            CartItem(
                                product = product,
                                quantity = quantity,
                                onProductClick = { onProductClick(productId) },
                                onQuantityIncrease = {
                                    currentUser?.userId?.let { userId ->
                                        productViewModel.addToCart(userId, productId, quantity + 1)
                                    }
                                },
                                onQuantityDecrease = {
                                    currentUser?.userId?.let { userId ->
                                        if (quantity > 1) {
                                            productViewModel.addToCart(userId, productId, quantity - 1)
                                        } else {
                                            productViewModel.removeFromCart(userId, productId)
                                        }
                                    }
                                },
                                onRemove = {
                                    currentUser?.userId?.let { userId ->
                                        productViewModel.removeFromCart(userId, productId)
                                    }
                                }
                            )

                            if (index < cartItems.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }

                        // Add some space at the bottom
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Order summary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Order Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Subtotal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = currencyFormat.format(subtotal),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Shipping
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Shipping",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = currencyFormat.format(shippingFee),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currencyFormat.format(total),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Checkout button
                            Button(
                                onClick = onCheckoutClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC0CB), // Light pink color
                                    contentColor = Color(0xFF3A2E2E) // Dark brown color
                                )
                            ) {
                                Text(
                                    text = "Proceed to Checkout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItem(
    product: Product?,
    quantity: Int,
    onProductClick: () -> Unit,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    // Format currency
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onProductClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (product != null && product.imageUrls.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(product.imageUrls.first())
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Product details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product?.title ?: "Loading...",
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currencyFormat.format(product?.price ?: 0.0),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Quantity selector
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrease button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onQuantityDecrease),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease quantity",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity
                Text(
                    text = quantity.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontWeight = FontWeight.Medium
                )

                // Increase button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onQuantityIncrease),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase quantity",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Remove button
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
