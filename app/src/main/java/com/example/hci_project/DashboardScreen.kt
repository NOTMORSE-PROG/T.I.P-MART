package com.example.hci_project

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.hci_project.model.Product
import com.example.hci_project.ui.theme.HCI_PROJECYTheme
import com.example.hci_project.utils.toTitleCase
import com.example.hci_project.viewmodel.AuthViewModel
import com.example.hci_project.viewmodel.ProductViewModel
import com.example.hci_project.viewmodel.RatingSortOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showEditProfileScreen by remember { mutableStateOf(false) }
    var showCreateListingScreen by remember { mutableStateOf(false) }
    var showProductDetailScreen by remember { mutableStateOf(false) }
    var showCartScreen by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Get cart item count for badge
    val cartItemCount by productViewModel.cartItemCount.collectAsState()

    // Fetch cart items when the screen is first displayed
    val currentUser by viewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) {
        currentUser?.userId?.let { userId ->
            Log.d("DashboardScreen", "Fetching cart items for user: $userId")
            productViewModel.fetchCartItems(userId)
        }
    }

    // Sample data
    val categories = listOf(
        "All", "Electronics", "Books", "Clothing", "Food", "Services", "Accessories", "School Supplies"
    )

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCampus by remember { mutableStateOf("All") }
    var ratingSortOption by remember { mutableStateOf(RatingSortOption.NONE) }

    if (showProductDetailScreen) {
        ProductDetailScreen(
            productId = selectedProductId,
            onBackClick = {
                showProductDetailScreen = false
                selectedProductId = ""
            },
            onCartClick = {
                showProductDetailScreen = false
                showCartScreen = true
            }
        )
    } else if (showCartScreen) {
        CartScreen(
            onBackClick = {
                showCartScreen = false
            },
            onCheckoutClick = {
                // TODO: Implement checkout flow
                showCartScreen = false
            },
            onProductClick = { productId ->
                selectedProductId = productId
                showCartScreen = false
                showProductDetailScreen = true
            }
        )
    } else if (showAboutScreen) {
        AboutScreen(onBackClick = { showAboutScreen = false })
    } else if (showEditProfileScreen) {
        EditProfileScreen(onBackClick = { showEditProfileScreen = false })
    } else if (showCreateListingScreen) {
        CreateListingScreen(
            onBackClick = { showCreateListingScreen = false },
            productViewModel = productViewModel
        )
    } else {
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

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                    // Categories Filter
                    Text(
                        text = "Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                    // Apply Filters Button (FIXED PADDING)
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (isSearchActive) {
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
                        TopAppBar(
                            title = {
                                Text(
                                    text = "T.I.P MART",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
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

                                Box(modifier = Modifier.padding(end = 12.dp)) {
                                    IconButton(onClick = { showCartScreen = true }) {
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
                        selectedCategory = selectedCategory,
                        selectedCampus = selectedCampus,
                        ratingSortOption = ratingSortOption,
                        searchQuery = searchQuery,
                        onFilterClick = { scope.launch { drawerState.open() } },
                        onCategoryCleared = { selectedCategory = "All" },
                        onCampusCleared = { selectedCampus = "All" },
                        onRatingSortCleared = { ratingSortOption = RatingSortOption.NONE },
                        onProductClick = { productId ->
                            selectedProductId = productId
                            showProductDetailScreen = true
                        },
                        modifier = Modifier.padding(paddingValues),
                        productViewModel = productViewModel
                    )
                    1 -> SellTab(
                        onCreateListingClick = { showCreateListingScreen = true },
                        modifier = Modifier.padding(paddingValues)
                    )
                    2 -> MessagesTab(modifier = Modifier.padding(paddingValues))
                    3 -> NoticeTab(modifier = Modifier.padding(paddingValues))
                    4 -> SettingsTab(
                        onLogout = onLogout,
                        onAboutClick = { showAboutScreen = true },
                        onEditProfileClick = { showEditProfileScreen = true },
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


// Modify the HomeTab to force refresh ratings when needed
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeTab(
    selectedCategory: String,
    selectedCampus: String,
    ratingSortOption: RatingSortOption,
    searchQuery: String,
    onFilterClick: () -> Unit,
    onCategoryCleared: () -> Unit,
    onCampusCleared: () -> Unit,
    onRatingSortCleared: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    productViewModel: ProductViewModel = hiltViewModel()
) {
    // State for real products from Firebase
    val productState by productViewModel.productState.collectAsState()
    val allProducts by productViewModel.allProducts.collectAsState()
    val currentRatingSortOption by productViewModel.ratingSortOption.collectAsState()

    // Observe the allRatings state to ensure UI updates when ratings change
    val allRatings by productViewModel.allRatings.collectAsState()

    // State for sort dropdown
    var showSortDropdown by remember { mutableStateOf(false) }

    // Fetch products and ratings immediately when the component is first composed
    LaunchedEffect(Unit) {
        Log.d("HomeTab", "Initial load - fetching all products and ratings")
        productViewModel.fetchAllProducts() // This will also trigger loadAllRatingsAtOnce()
    }

    // Force refresh ratings periodically to ensure they're loaded
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Refresh every 5 seconds
            productViewModel.loadAllRatingsAtOnce()
        }
    }


    LocalContext.current

    // Filter real products based on category, campus, and search query
    var filteredProducts = allProducts.filter { product ->
        val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
        val matchesCampus = selectedCampus == "All" || product.campus == selectedCampus
        val matchesSearch = searchQuery.isEmpty() ||
                product.title.contains(searchQuery, ignoreCase = true) ||
                product.sellerName.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesCampus && matchesSearch
    }

    filteredProducts = when (currentRatingSortOption) {
        RatingSortOption.HIGH_TO_LOW -> {
            filteredProducts.sortedByDescending { product ->
                val ratings = allRatings[product.id] ?: emptyList()
                if (ratings.isEmpty()) 0f else ratings.map { it.rating }.average().toFloat()
            }
        }

        RatingSortOption.LOW_TO_HIGH -> {
            filteredProducts.sortedWith(compareBy { product ->
                val ratings = allRatings[product.id] ?: emptyList()
                if (ratings.isEmpty()) Float.MIN_VALUE else ratings.map { it.rating }.average().toFloat()
            })
        }

        else -> filteredProducts
    }



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
            if (selectedCategory != "All" || selectedCampus != "All" || ratingSortOption != RatingSortOption.NONE) {
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
                                        onCampusCleared()
                                    }
                                )
                            }
                        )
                    }

                    if (ratingSortOption != RatingSortOption.NONE) {
                        FilterChip(
                            selected = true,
                            onClick = { /* Do nothing on chip body click */ },
                            label = {
                                Text(
                                    when (ratingSortOption) {
                                        RatingSortOption.HIGH_TO_LOW -> "Rating: High to Low"
                                        RatingSortOption.LOW_TO_HIGH -> "Rating: Low to High"
                                        else -> ""
                                    }
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear rating sort",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onRatingSortCleared()
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

        // Products header with sort button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Products",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // Sort button with dropdown
            Box {
                IconButton(onClick = { showSortDropdown = !showSortDropdown }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort"
                    )
                }

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No Rating Sort") },
                        onClick = {
                            productViewModel.setRatingSortOption(RatingSortOption.NONE)
                            onRatingSortCleared()
                            showSortDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rating: High to Low")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        onClick = {
                            productViewModel.setRatingSortOption(RatingSortOption.HIGH_TO_LOW)
                            showSortDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rating: Low to High")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        onClick = {
                            productViewModel.setRatingSortOption(RatingSortOption.LOW_TO_HIGH)
                            showSortDropdown = false
                        }
                    )
                }
            }
        }

        // Loading state
        if (productState is ProductViewModel.ProductState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // Error state
        else if (productState is ProductViewModel.ProductState.Error) {
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
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = (productState as ProductViewModel.ProductState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { productViewModel.fetchAllProducts() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        // Empty state
        else if (filteredProducts.isEmpty()) {
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
        }
        // Products grid
        else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->
                    RealProductCard(
                        product = product,
                        onProductClick = onProductClick,
                        productViewModel = productViewModel
                    )
                }
            }
        }
    }
}
// Modify the RealProductCard to ensure it always shows ratings
@Composable
fun RealProductCard(
    product: Product,
    onProductClick: (String) -> Unit,
    productViewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Get ratings from the global allRatings state
    val allRatings by productViewModel.allRatings.collectAsState()
    val ratings = allRatings[product.id] ?: emptyList()

    val averageRating = if (ratings.isNotEmpty()) {
        ratings.map { it.rating }.average().toFloat()
    } else {
        0f
    }

    // Debug log to check if ratings are available
    LaunchedEffect(ratings) {
        Log.d("RealProductCard", "Product ${product.id} has ${ratings.size} ratings, avg: $averageRating")
    }

    // If no ratings found in global state, try to fetch them specifically for this product
    LaunchedEffect(product.id, ratings.isEmpty()) {
        if (ratings.isEmpty()) {
            Log.d("RealProductCard", "No ratings found for ${product.id}, fetching now")
            productViewModel.fetchRatingsForProduct(product.id)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp) // Increased height to accommodate rating
            .clickable { onProductClick(product.id) },
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
                if (product.imageUrls.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(product.imageUrls.first())
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "₱${product.price}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Rating display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    // Star rating
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= averageRating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            tint = if (i <= averageRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Rating text
                    Text(
                        text = if (ratings.isNotEmpty()) {
                            String.format(Locale.US, "%.1f", averageRating)
                        } else {
                            "No ratings"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "Seller: ${product.sellerName.toTitleCase()}",
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
fun SellTab(
    onCreateListingClick: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val productState by productViewModel.productState.collectAsState()
    val userProducts by productViewModel.userProducts.collectAsState()

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var productToUpdateStatus by remember { mutableStateOf<Product?>(null) }

    // Edit state
    var showEditScreen by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // Fetch user products when the component is first composed
    LaunchedEffect(currentUser) {
        currentUser?.userId?.let { userId ->
            productViewModel.fetchUserProducts(userId)
        }
    }

    // Show edit screen if a product is selected for editing
    if (showEditScreen && productToEdit != null) {
        EditListingScreen(
            product = productToEdit!!,
            onBackClick = {
                showEditScreen = false
                productToEdit = null
            }
        )
        return
    }

    // Delete confirmation dialog
    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                productToDelete = null
            },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.id?.let { productId ->
                            productViewModel.deleteProduct(productId)
                        }
                        showDeleteDialog = false
                        productToDelete = null
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
                        showDeleteDialog = false
                        productToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Status update dialog
    if (showStatusDialog && productToUpdateStatus != null) {
        val statusOptions = listOf("active", "sold", "reserved")

        AlertDialog(
            onDismissRequest = {
                showStatusDialog = false
                productToUpdateStatus = null
            },
            title = { Text("Update Status") },
            text = {
                Column {
                    Text("Select the new status for this listing:")
                    Spacer(modifier = Modifier.height(16.dp))

                    statusOptions.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    productToUpdateStatus?.id?.let { productId ->
                                        productViewModel.updateProductStatus(productId, status)
                                    }
                                    showStatusDialog = false
                                    productToUpdateStatus = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = productToUpdateStatus?.status == status,
                                onClick = {
                                    productToUpdateStatus?.id?.let { productId ->
                                        productViewModel.updateProductStatus(productId, status)
                                    }
                                    showStatusDialog = false
                                    productToUpdateStatus = null
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = status.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStatusDialog = false
                        productToUpdateStatus = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            dismissButton = null
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "My Listings",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Create listing button
        Button(
            onClick = onCreateListingClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Listing")
        }

        // Loading state
        if (productState is ProductViewModel.ProductState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // Error state
        else if (productState is ProductViewModel.ProductState.Error) {
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
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = (productState as ProductViewModel.ProductState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            currentUser?.userId?.let { userId ->
                                productViewModel.fetchUserProducts(userId)
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        // Empty state
        else if (userProducts.isEmpty()) {
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
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You don't have any listings yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCreateListingClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Your First Listing")
                    }
                }
            }
        }
        // User products list
        else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(userProducts) { product ->
                    UserProductCard(
                        product = product,
                        onDeleteClick = {
                            productToDelete = product
                            showDeleteDialog = true
                        },
                        onStatusClick = {
                            productToUpdateStatus = product
                            showStatusDialog = true
                        },
                        onEditClick = {
                            productToEdit = product
                            showEditScreen = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserProductCard(
    product: Product,
    onDeleteClick: () -> Unit,
    onStatusClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Product Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(product.imageUrls.first())
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = when (product.status) {
                        "active" -> MaterialTheme.colorScheme.primary
                        "sold" -> MaterialTheme.colorScheme.error
                        "reserved" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.secondary
                    }.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "₱${product.price}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Category: ${product.category}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Campus: ${product.campus}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Quantity: ${product.quantity}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit Button
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    // Update Status Button
                    OutlinedButton(
                        onClick = onStatusClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Status")
                    }

                    // Delete Button
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notices yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
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
    onEditProfileClick: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

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
                            // Profile picture - now using Coil to load from URL if available
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onEditProfileClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentUser?.profilePictureUrl.isNullOrEmpty()) {
                                    // Load profile picture from URL
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(currentUser?.profilePictureUrl)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Default icon if no profile picture
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Use title-cased full name
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
                                onClick = { onEditProfileClick() },
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    HCI_PROJECYTheme {
        DashboardScreen(
            onLogout = {}
        )
    }
}
