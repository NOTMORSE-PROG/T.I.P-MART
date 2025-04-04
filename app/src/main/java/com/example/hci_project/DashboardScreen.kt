package com.example.hci_project

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hci_project.ui.theme.HCI_PROJECYTheme
import com.example.hci_project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Sample data
    val categories = listOf(
        "All", "Electronics", "Books", "Clothing", "Food", "Services", "Accessories", "School Supplies"
    )

    val products = listOf(
        Product(
            id = 1,
            name = "TIP Engineering Calculator",
            price = "₱450",
            seller = "John Doe",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Electronics"
        ),
        Product(
            id = 2,
            name = "Data Structures Textbook",
            price = "₱350",
            seller = "Jane Smith",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Books"
        ),
        Product(
            id = 3,
            name = "TIP Uniform (Medium)",
            price = "₱800",
            seller = "Mike Johnson",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Clothing"
        ),
        Product(
            id = 4,
            name = "Homemade Cookies (12 pcs)",
            price = "₱120",
            seller = "Sarah Lee",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Food"
        ),
        Product(
            id = 5,
            name = "Programming Tutoring (per hour)",
            price = "₱250",
            seller = "Alex Chen",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Services"
        ),
        Product(
            id = 6,
            name = "Scientific Calculator",
            price = "₱350",
            seller = "David Wilson",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Electronics"
        ),
        Product(
            id = 7,
            name = "TIP Lanyard",
            price = "₱75",
            seller = "Emma Garcia",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Accessories"
        ),
        Product(
            id = 8,
            name = "Engineering Notebook",
            price = "₱120",
            seller = "Carlos Rodriguez",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "School Supplies"
        )
    )

    var selectedCategory by remember { mutableStateOf("All") }
    val filteredProducts = if (selectedCategory == "All") {
        products
    } else {
        products.filter { it.category == selectedCategory }
    }

    if (showAboutScreen) {
        AboutScreen(onBackClick = { showAboutScreen = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "T.I.P MART",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Search functionality */ }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { /* TODO: Cart functionality */ }) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Sell") },
                        label = { Text("Sell") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Messages") },
                        label = { Text("Messages") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Notice") },
                        label = { Text("Notice") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { paddingValues ->
            when (selectedTab) {
                0 -> HomeTab(
                    categories = categories,
                    products = filteredProducts,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier.padding(paddingValues)
                )
                1 -> SellTab(modifier = Modifier.padding(paddingValues))
                2 -> MessagesTab(modifier = Modifier.padding(paddingValues))
                3 -> NoticeTab(modifier = Modifier.padding(paddingValues))
                4 -> SettingsTab(
                    onLogout = onLogout,
                    onAboutClick = { showAboutScreen = true },
                    modifier = Modifier.padding(paddingValues),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun HomeTab(
    categories: List<String>,
    products: List<Product>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Categories
        Text(
            text = "Categories",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    selected = category == selectedCategory,
                    onSelected = { onCategorySelected(category) }
                )
            }
        }

        // Products
        Text(
            text = "Products",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found in this category",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    ProductCard(product = product)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable { onSelected() }
    ) {
        Text(
            text = category,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { /* TODO: Product detail */ },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Product Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Image(
                    painter = painterResource(id = product.imageRes),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.price,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Seller: ${product.seller}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SellTab(modifier: Modifier = Modifier) {
    // Placeholder for Sell tab
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sell Your Products",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a listing for items you want to sell",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { /* TODO: Create listing */ },
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Listing")
            }
        }
    }
}

@Composable
fun MessagesTab(modifier: Modifier = Modifier) {
    // Sample messages
    val messages = listOf(
        Message(
            id = 1,
            senderName = "John Doe",
            lastMessage = "Hi, I'm interested in your calculator. Is it still available?",
            time = "10:30 AM",
            unread = true,
            avatarRes = R.drawable.ic_launcher_foreground
        ),
        Message(
            id = 2,
            senderName = "Jane Smith",
            lastMessage = "Thanks for the quick delivery! The textbook is in great condition.",
            time = "Yesterday",
            unread = false,
            avatarRes = R.drawable.ic_launcher_foreground
        ),
        Message(
            id = 3,
            senderName = "Alex Chen",
            lastMessage = "When are you available for the tutoring session?",
            time = "Yesterday",
            unread = true,
            avatarRes = R.drawable.ic_launcher_foreground
        ),
        Message(
            id = 4,
            senderName = "Sarah Lee",
            lastMessage = "I'll have a new batch of cookies next week if you're interested.",
            time = "Monday",
            unread = false,
            avatarRes = R.drawable.ic_launcher_foreground
        ),
        Message(
            id = 5,
            senderName = "Mike Johnson",
            lastMessage = "Do you have this uniform in size Large?",
            time = "Sunday",
            unread = false,
            avatarRes = R.drawable.ic_launcher_foreground
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Messages",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No messages yet",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages) { message ->
                    MessageItem(message = message)
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* TODO: Open conversation */ },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.unread)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Image(
                    painter = painterResource(id = message.avatarRes),
                    contentDescription = "Avatar for ${message.senderName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.senderName,
                        fontWeight = if (message.unread) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = message.time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.lastMessage,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (message.unread) 0.8f else 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
            }

            if (message.unread) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun NoticeTab(modifier: Modifier = Modifier) {
    // Sample notifications
    val notifications = listOf(
        Notification(
            id = 1,
            title = "New message from John",
            description = "Hi, I'm interested in your calculator",
            time = "2 hours ago",
            read = false
        ),
        Notification(
            id = 2,
            title = "Order update",
            description = "Your order #1234 has been shipped",
            time = "Yesterday",
            read = true
        ),
        Notification(
            id = 3,
            title = "Price drop alert",
            description = "A product on your wishlist is now on sale",
            time = "2 days ago",
            read = true
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notice",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notices yet",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications) { notification ->
                    NotificationItem(notification = notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* TODO: Handle notification click */ },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification indicator
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsTab(
    onLogout: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // Debug log to check what user data we have
    LaunchedEffect(currentUser) {
        Log.d("SettingsTab", "Current user data: ${currentUser?.toString()}")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Wrap the scrollable content in a Box with weight
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile picture
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Display user data with fallbacks
                            val displayName = currentUser?.fullname.takeIf { !it.isNullOrBlank() } ?: "User Name"
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                text = currentUser?.email ?: "user@tip.edu.ph",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            val displayStudentId = currentUser?.studentId.takeIf { !it.isNullOrBlank() } ?: "0000000"
                            Text(
                                text = "Student ID: $displayStudentId",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { /* TODO: Edit profile */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }

                // Settings options
                item {
                    SettingsItem(
                        icon = Icons.Default.ShoppingBag,
                        title = "My Orders",
                        onClick = { /* TODO: Navigate to orders */ }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.Favorite,
                        title = "Wishlist",
                        onClick = { /* TODO: Navigate to wishlist */ }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Methods",
                        onClick = { /* TODO: Navigate to payment methods */ }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.LocationOn,
                        title = "Address",
                        onClick = { /* TODO: Navigate to address */ }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "Verify Account",
                        onClick = { /* TODO: Navigate to verification */ }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        onClick = onAboutClick
                    )
                }
            }
        }

        // Logout button - outside of LazyColumn to ensure it's always visible
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}


// Data classes
data class Product(
    val id: Int,
    val name: String,
    val price: String,
    val seller: String,
    val imageRes: Int,
    val category: String
)

data class Notification(
    val id: Int,
    val title: String,
    val description: String,
    val time: String,
    val read: Boolean
)

data class Message(
    val id: Int,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val unread: Boolean,
    val avatarRes: Int
)

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    HCI_PROJECYTheme {
        DashboardScreen(
            onLogout = {}
        )
    }
}

