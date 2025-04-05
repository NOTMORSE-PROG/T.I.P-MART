package com.example.hci_project

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import com.example.hci_project.utils.toTitleCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
            category = "Electronics",
            campus = "Manila"
        ),
        Product(
            id = 2,
            name = "Data Structures Textbook",
            price = "₱350",
            seller = "Jane Smith",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Books",
            campus = "Quezon City"
        ),
        Product(
            id = 3,
            name = "TIP Uniform (Medium)",
            price = "₱800",
            seller = "Mike Johnson",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Clothing",
            campus = "Manila"
        ),
        Product(
            id = 4,
            name = "Homemade Cookies (12 pcs)",
            price = "₱120",
            seller = "Sarah Lee",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Food",
            campus = "Quezon City"
        ),
        Product(
            id = 5,
            name = "Programming Tutoring (per hour)",
            price = "₱250",
            seller = "Alex Chen",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Services",
            campus = "Manila"
        ),
        Product(
            id = 6,
            name = "Scientific Calculator",
            price = "₱350",
            seller = "David Wilson",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Electronics",
            campus = "Quezon City"
        ),
        Product(
            id = 7,
            name = "TIP Lanyard",
            price = "₱75",
            seller = "Emma Garcia",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "Accessories",
            campus = "Manila"
        ),
        Product(
            id = 8,
            name = "Engineering Notebook",
            price = "₱120",
            seller = "Carlos Rodriguez",
            imageRes = R.drawable.ic_launcher_foreground,
            category = "School Supplies",
            campus = "Quezon City"
        )
    )

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCampus by remember { mutableStateOf("All") }

    // Filter products based on category, campus, and search query
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
        val matchesCampus = selectedCampus == "All" || product.campus == selectedCampus
        val matchesSearch = searchQuery.isEmpty() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.seller.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesCampus && matchesSearch
    }

    if (showAboutScreen) {
        AboutScreen(onBackClick = { showAboutScreen = false })
    } else {
        // Drawer layout
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp)
                ) {
                    // Drawer header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Filter Products",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campus Filter
                    Text(
                        text = "Campus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        listOf("All", "Manila", "Quezon City").forEach { campus ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCampus = campus
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = campus == selectedCampus,
                                    onClick = {
                                        selectedCampus = campus
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = campus)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Categories
                    Text(
                        text = "Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        items(categories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = category
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = category == selectedCategory,
                                    onClick = {
                                        selectedCategory = category
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = category)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Apply button
                    Button(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (isSearchActive) {
                        // Search TopBar
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search products...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
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
                    } else {
                        // Regular TopBar
                        TopAppBar(
                            title = {
                                Text(
                                    text = "T.I.P MART",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearchActive = true }) {
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
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
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
                        products = filteredProducts,
                        selectedCategory = selectedCategory,
                        selectedCampus = selectedCampus,
                        searchQuery = searchQuery,
                        onFilterClick = { scope.launch { drawerState.open() } },
                        onCategoryCleared = { selectedCategory = "All" },
                        onCampusCleared = { selectedCampus = "All" },
                        modifier = Modifier.padding(paddingValues)
                    )
                    1 -> SellTab(modifier = Modifier.padding(paddingValues))
                    2 -> MessagesTab(modifier = Modifier.padding(paddingValues))
                    3 -> NoticeTab(modifier = Modifier.padding(paddingValues))
                    4 -> SettingsTab(
                        onLogout = onLogout,
                        onAboutClick = { showAboutScreen = true },
                        onDeleteAccount = {
                            viewModel.deleteAccount {
                                onLogout()
                            }
                        },
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTab(
    products: List<Product>,
    selectedCategory: String,
    selectedCampus: String,
    searchQuery: String,
    onFilterClick: () -> Unit,
    onCategoryCleared: () -> Unit,
    onCampusCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Active filters section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Filters:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Show active filters as chips
            if (selectedCategory != "All" || selectedCampus != "All") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (selectedCategory != "All") {
                        FilterChip(
                            selected = true,
                            onClick = { /* Do nothing on chip body click */ },
                            label = { Text("Category: $selectedCategory") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear category filter",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // Clear category filter without opening sidebar
                                        onCategoryCleared()
                                    }
                                )
                            }
                        )
                    }

                    if (selectedCampus != "All") {
                        FilterChip(
                            selected = true,
                            onClick = { /* Do nothing on chip body click */ },
                            label = { Text("Campus: $selectedCampus") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear campus filter",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // Clear campus filter without opening sidebar
                                        onCampusCleared()
                                    }
                                )
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = "None",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Search Results Label (only show when searching)
        if (searchQuery.isNotEmpty()) {
            Text(
                text = "Search Results for \"$searchQuery\"",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "No products found matching \"$searchQuery\""
                        else
                            "No products found with the selected filters",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onFilterClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Filters")
                    }
                }
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
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)  // Increased height to accommodate campus info
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

                // Campus Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.campus,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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
                Text(
                    text = "Category: ${product.category}",
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
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    // Debug log to check what user data we have
    LaunchedEffect(currentUser) {
        Log.d("SettingsTab", "Current user data: ${currentUser?.toString()}")
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
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

        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
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

                            // ✅ Use title-cased full name
                            val displayName = currentUser?.fullname
                                ?.toTitleCase()
                                .takeIf { !it.isNullOrBlank() } ?: "User Name"

                            Text(
                                text = displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                text = currentUser?.email ?: "user@tip.edu.ph",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            val displayStudentId =
                                currentUser?.studentId.takeIf { !it.isNullOrBlank() } ?: "0000000"
                            Text(
                                text = "Student ID: $displayStudentId",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            val displayCampus = currentUser?.campus.takeIf { !it.isNullOrBlank() }
                                ?: "Not specified"
                            Text(
                                text = "Campus: $displayCampus",
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

                // Delete Account Option
                item {
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        onClick = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }

        // Loading indicator during account deletion
        if (authState is AuthViewModel.AuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error message
        if (authState is AuthViewModel.AuthState.Error) {
            Text(
                text = (authState as AuthViewModel.AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
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
    val category: String,
    val campus: String  // Added campus field
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

