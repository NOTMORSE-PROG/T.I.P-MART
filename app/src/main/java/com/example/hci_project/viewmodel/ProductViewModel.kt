package com.example.hci_project.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hci_project.model.Product
import com.example.hci_project.model.Rating
import com.example.hci_project.model.User
import com.example.hci_project.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


enum class RatingSortOption {
    NONE,
    HIGH_TO_LOW,
    LOW_TO_HIGH
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _productState = MutableStateFlow<ProductState>(ProductState.Initial)
    val productState: StateFlow<ProductState> = _productState.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _userProducts = MutableStateFlow<List<Product>>(emptyList())
    val userProducts: StateFlow<List<Product>> = _userProducts.asStateFlow()

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    private val _productRatings = MutableStateFlow<List<Rating>>(emptyList())
    val productRatings: StateFlow<List<Rating>> = _productRatings.asStateFlow()

    private val _allRatings = MutableStateFlow<Map<String, List<Rating>>>(emptyMap())
    val allRatings: StateFlow<Map<String, List<Rating>>> = _allRatings.asStateFlow()

    private val _userRating = MutableStateFlow<Rating?>(null)
    val userRating: StateFlow<Rating?> = _userRating.asStateFlow()

    private val _cartItems = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cartItems: StateFlow<Map<String, Int>> = _cartItems.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _sellerProfilePicture = MutableStateFlow("")
    val sellerProfilePicture: StateFlow<String> = _sellerProfilePicture.asStateFlow()

    private val _ratingSortOption = MutableStateFlow(RatingSortOption.NONE)
    val ratingSortOption: StateFlow<RatingSortOption> = _ratingSortOption.asStateFlow()

    private val productRatingsCache = mutableMapOf<String, List<Rating>>()

    private val _wishlistProducts = MutableStateFlow<List<Product>>(emptyList())
    val wishlistProducts: StateFlow<List<Product>> = _wishlistProducts.asStateFlow()

    private val _wishlistProductIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistProductIds: StateFlow<Set<String>> = _wishlistProductIds.asStateFlow()

    // Create a new product
    fun createProduct(
        title: String,
        description: String,
        price: Double,
        quantity: Int,
        category: String,
        campus: String,
        imageUris: List<Uri>,
        currentUser: User
    ) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.createProduct(
                    title = title,
                    description = description,
                    price = price,
                    quantity = quantity,
                    category = category,
                    campus = campus,
                    imageUris = imageUris,
                    currentUser = currentUser
                )

                result.fold(
                    onSuccess = { product ->
                        _productState.value = ProductState.ProductCreated(product)
                        fetchUserProducts(currentUser.userId)
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to create product")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Update an existing product
    fun updateProduct(
        productId: String,
        title: String,
        description: String,
        price: Double,
        quantity: Int,
        category: String,
        campus: String,
        newImageUris: List<Uri>,
        imagesToKeep: List<String>,
        currentUser: User
    ) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.updateProduct(
                    productId = productId,
                    title = title,
                    description = description,
                    price = price,
                    quantity = quantity,
                    category = category,
                    campus = campus,
                    newImageUris = newImageUris,
                    imagesToKeep = imagesToKeep,
                    currentUser = currentUser
                )

                result.fold(
                    onSuccess = { product ->
                        _productState.value = ProductState.ProductUpdated(product)
                        // Refresh user products
                        fetchUserProducts(currentUser.userId)
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to update product")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get products by user
    fun fetchUserProducts(userId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.getProductsByUser(userId)

                result.fold(
                    onSuccess = { products ->
                        _userProducts.value = products
                        _productState.value = ProductState.Success
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to fetch user products")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Load all ratings at once for immediate display
    fun loadAllRatingsAtOnce() {
        viewModelScope.launch {
            Log.d("ProductViewModel", "Loading all ratings at once")

            try {
                val result = productRepository.getAllRatingsAtOnce()

                result.fold(
                    onSuccess = { ratingsMap ->
                        Log.d("ProductViewModel", "Successfully loaded ratings for ${ratingsMap.size} products")

                        // Update the cache with all ratings
                        productRatingsCache.putAll(ratingsMap)

                        // Update the state flow for UI updates
                        _allRatings.value = ratingsMap

                        // Log the ratings for each product
                        ratingsMap.forEach { (productId, ratings) ->
                            Log.d("ProductViewModel", "Product $productId has ${ratings.size} ratings")
                        }
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Failed to load all ratings: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception loading all ratings: ${e.message}")
            }
        }
    }

    fun fetchAllProducts() {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.getAllProducts()

                result.fold(
                    onSuccess = { products ->
                        _allProducts.value = products
                        _productState.value = ProductState.Success

                        // Immediately load all ratings after products are loaded
                        loadAllRatingsAtOnce()
                    },
                    onFailure = { e ->
                        _productState.value =
                            ProductState.Error(e.message ?: "Failed to fetch products")
                    }
                )
            } catch (e: Exception) {
                _productState.value =
                    ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get product by ID
    fun getProductById(productId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.getProductById(productId)

                result.fold(
                    onSuccess = { product ->
                        _selectedProduct.value = product
                        _productState.value = ProductState.Success
                        fetchSellerProfilePicture(product.sellerId)
                        fetchRatingsForProduct(productId)
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to fetch product")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Fetch seller profile picture
    private fun fetchSellerProfilePicture(sellerId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.getUserProfilePicture(sellerId)
                result.fold(
                    onSuccess = { profilePictureUrl ->
                        _sellerProfilePicture.value = profilePictureUrl ?: ""
                        Log.d("ProductViewModel", "Fetched seller profile picture: $profilePictureUrl")
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error fetching seller profile picture: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception fetching seller profile picture: ${e.message}")
            }
        }
    }

    // Delete a product
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.deleteProduct(productId)

                result.fold(
                    onSuccess = {
                        _productState.value = ProductState.ProductDeleted
                        // Remove from user products list
                        _userProducts.value = _userProducts.value.filter { it.id != productId }
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to delete product")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Update product status
    fun updateProductStatus(productId: String, status: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.updateProductStatus(productId, status)

                result.fold(
                    onSuccess = {
                        // Update the status in the local list
                        _userProducts.value = _userProducts.value.map { product ->
                            if (product.id == productId) {
                                product.copy(status = status)
                            } else {
                                product
                            }
                        }
                        _productState.value = ProductState.Success
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to update product status")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Add a rating
    fun addRating(
        productId: String,
        userId: String,
        userName: String,
        rating: Float,
        comment: String
    ) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                Log.d("ProductViewModel", "Adding rating: productId=$productId, userId=$userId, rating=$rating, comment=$comment")
                val result = productRepository.addRating(
                    productId = productId,
                    userId = userId,
                    userName = userName,
                    rating = rating,
                    comment = comment
                )

                result.fold(
                    onSuccess = { newRating ->
                        Log.d("ProductViewModel", "Rating added successfully: ${newRating.id}")

                        // Update the user's rating immediately for better UX
                        _userRating.value = newRating

                        // Update the ratings list if it contains an old version of this rating
                        val currentRatings = _productRatings.value.toMutableList()
                        val existingIndex = currentRatings.indexOfFirst { it.userId == userId }

                        if (existingIndex >= 0) {
                            currentRatings[existingIndex] = newRating
                        } else {
                            currentRatings.add(0, newRating) // Add to beginning of list
                        }

                        _productRatings.value = currentRatings

                        // Update the cache
                        productRatingsCache[productId] = currentRatings

                        // Update the all ratings map
                        val updatedAllRatings = _allRatings.value.toMutableMap()
                        updatedAllRatings[productId] = currentRatings
                        _allRatings.value = updatedAllRatings

                        // Give Firebase a moment to update before fetching
                        delay(500)

                        // Also refresh all ratings from server to ensure consistency
                        fetchRatingsForProduct(productId)

                        _productState.value = ProductState.RatingAdded(newRating)
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error adding rating: ${e.message}", e)
                        _productState.value = ProductState.Error(e.message ?: "Failed to add rating")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception adding rating: ${e.message}", e)
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Delete a rating
    fun deleteRating(ratingId: String, productId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                Log.d("ProductViewModel", "Deleting rating: $ratingId for product: $productId")
                val result = productRepository.deleteRating(ratingId)

                result.fold(
                    onSuccess = {
                        Log.d("ProductViewModel", "Rating deleted successfully")

                        // Remove the rating from the current list
                        _productRatings.value = _productRatings.value.filter { it.id != ratingId }

                        // If this was the user's rating, clear it
                        if (_userRating.value?.id == ratingId) {
                            _userRating.value = null
                        }

                        // Update the all ratings map
                        val updatedAllRatings = _allRatings.value.toMutableMap()
                        val productRatings = updatedAllRatings[productId]?.filter { it.id != ratingId }
                        if (productRatings != null) {
                            updatedAllRatings[productId] = productRatings
                        }
                        _allRatings.value = updatedAllRatings

                        // Update the cache
                        productRatingsCache[productId] = productRatingsCache[productId]?.filter { it.id != ratingId } ?: emptyList()

                        _productState.value = ProductState.RatingDeleted

                        // Refresh ratings after a short delay
                        delay(500)
                        fetchRatingsForProduct(productId)
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error deleting rating: ${e.message}", e)
                        _productState.value = ProductState.Error(e.message ?: "Failed to delete rating")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception deleting rating: ${e.message}", e)
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get ratings for a product
    fun fetchRatingsForProduct(productId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "Fetching ratings for product: $productId")

                val result = productRepository.getRatingsForProduct(productId)

                result.fold(
                    onSuccess = { ratings ->
                        Log.d("ProductViewModel", "Fetched ${ratings.size} ratings for product: $productId")

                        // Always update the ratings, even if empty
                        if (productId == _selectedProduct.value?.id) {
                            _productRatings.value = ratings
                        }

                        // Update the cache
                        productRatingsCache[productId] = ratings

                        // Update the all ratings map
                        val updatedAllRatings = _allRatings.value.toMutableMap()
                        updatedAllRatings[productId] = ratings
                        _allRatings.value = updatedAllRatings
                        ratings.forEachIndexed { index, rating ->
                            Log.d("ProductViewModel", "Rating $index: id=${rating.id}, userId=${rating.userId}, rating=${rating.rating}, comment=${rating.comment}")
                        }
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error fetching ratings: ${e.message}")
                        retryFetchRatings(productId)
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception fetching ratings: ${e.message}")
                retryFetchRatings(productId)
            }
        }
    }

    // Retry fetching ratings with a different approach
    private fun retryFetchRatings(productId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "Retrying fetch ratings for product: $productId")

                val result = productRepository.getRatingsForProductForced(productId)

                result.fold(
                    onSuccess = { ratings ->
                        Log.d("ProductViewModel", "Retry fetched ${ratings.size} ratings for product: $productId")

                        if (ratings.isNotEmpty()) {
                            if (productId == _selectedProduct.value?.id) {
                                _productRatings.value = ratings
                            }

                            // Update the cache
                            productRatingsCache[productId] = ratings

                            // Update the all ratings map
                            val updatedAllRatings = _allRatings.value.toMutableMap()
                            updatedAllRatings[productId] = ratings
                            _allRatings.value = updatedAllRatings

                            // Log each rating for debugging
                            ratings.forEachIndexed { index, rating ->
                                Log.d("ProductViewModel", "Retry Rating $index: id=${rating.id}, userId=${rating.userId}, rating=${rating.rating}, comment=${rating.comment}")
                            }
                        }
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error in retry fetching ratings: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception in retry fetching ratings: ${e.message}")
            }
        }
    }

    // Set rating sort option
    fun setRatingSortOption(option: RatingSortOption) {
        _ratingSortOption.value = option
    }

    // Get user's rating for a product
    fun getUserRatingForProduct(productId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.getUserRatingForProduct(productId, userId)

                result.fold(
                    onSuccess = { rating ->
                        _userRating.value = rating
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error fetching user rating: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception fetching user rating: ${e.message}")
            }
        }
    }

    // Add to cart
    fun addToCart(userId: String, productId: String, quantity: Int) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                Log.d("ProductViewModel", "Adding to cart: userId=$userId, productId=$productId, quantity=$quantity")
                val result = productRepository.addToCart(userId, productId, quantity)

                result.fold(
                    onSuccess = {
                        Log.d("ProductViewModel", "Product added to cart successfully")
                        // Update local cart state immediately for better UX
                        val currentCart = _cartItems.value.toMutableMap()
                        currentCart[productId] = quantity
                        _cartItems.value = currentCart
                        _cartItemCount.value = currentCart.values.sum()

                        _productState.value = ProductState.ProductAddedToCart

                        fetchCartItems(userId)
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error adding to cart: ${e.message}", e)
                        _productState.value = ProductState.Error(e.message ?: "Failed to add to cart")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception adding to cart: ${e.message}", e)
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get cart items
    fun fetchCartItems(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "Fetching cart items for user: $userId")
                val result = productRepository.getCartItems(userId)

                result.fold(
                    onSuccess = { cartItems ->
                        _cartItems.value = cartItems
                        val totalItems = cartItems.values.sum()
                        _cartItemCount.value = totalItems
                        Log.d("ProductViewModel", "Fetched ${cartItems.size} cart items, total quantity: $totalItems")
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error fetching cart items: ${e.message}")
                        // Even on failure, ensure we have a valid cart state
                        if (_cartItems.value.isEmpty()) {
                            _cartItems.value = emptyMap()
                            _cartItemCount.value = 0
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception fetching cart items: ${e.message}")
                // Even on exception, ensure we have a valid cart state
                if (_cartItems.value.isEmpty()) {
                    _cartItems.value = emptyMap()
                    _cartItemCount.value = 0
                }
            }
        }
    }

    // Remove from cart
    fun removeFromCart(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.removeFromCart(userId, productId)

                result.fold(
                    onSuccess = {
                        // Refresh cart items
                        fetchCartItems(userId)
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error removing from cart: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception removing from cart: ${e.message}")
            }
        }
    }

    // Reset product state
    fun resetProductState() {
        _productState.value = ProductState.Initial
    }

    // Add this function to clear product ratings when navigating to a new product
    fun clearProductRatings() {
        _productRatings.value = emptyList()
        _userRating.value = null
    }

    // Check if a product is in the wishlist
    fun checkIfInWishlist(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.isProductInWishlist(userId, productId)

                result.fold(
                    onSuccess = { isInWishlist ->
                        if (isInWishlist) {
                            _wishlistProductIds.value += productId
                        } else {
                            _wishlistProductIds.value -= productId
                        }
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error checking wishlist: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception checking wishlist: ${e.message}")
            }
        }
    }

    // Fetch all wishlist product IDs
    fun fetchWishlistProductIds(userId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.getWishlistItems(userId)

                result.fold(
                    onSuccess = { productIds ->
                        _wishlistProductIds.value = productIds.toSet()
                        Log.d("ProductViewModel", "Fetched ${productIds.size} wishlist product IDs")
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error fetching wishlist IDs: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception fetching wishlist IDs: ${e.message}")
            }
        }
    }

    // Fetch all wishlist products
    fun fetchWishlistProducts(userId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            try {
                val result = productRepository.getWishlistProducts(userId)

                result.fold(
                    onSuccess = { products ->
                        _wishlistProducts.value = products
                        _productState.value = ProductState.Success
                        Log.d("ProductViewModel", "Fetched ${products.size} wishlist products")

                        // Also update the IDs set
                        _wishlistProductIds.value = products.map { it.id }.toSet()
                    },
                    onFailure = { e ->
                        _productState.value = ProductState.Error(e.message ?: "Failed to fetch wishlist products")
                    }
                )
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Replace the toggleWishlist function with this optimistic update version
    // Toggle wishlist status
    fun toggleWishlist(userId: String, productId: String) {
        viewModelScope.launch {
            // Get current wishlist status
            val isInWishlist = _wishlistProductIds.value.contains(productId)

            // Optimistically update UI immediately
            if (isInWishlist) {
                // Remove from wishlist UI immediately
                _wishlistProductIds.value -= productId
            } else {
                // Add to wishlist UI immediately
                _wishlistProductIds.value += productId
            }

            try {
                // Perform the actual backend operation without setting loading state
                if (isInWishlist) {
                    removeFromWishlistQuietly(userId, productId)
                } else {
                    addToWishlistQuietly(userId, productId)
                }
            } catch (e: Exception) {
                // If exception occurred, revert the UI change
                Log.e("ProductViewModel", "Exception toggling wishlist: ${e.message}")
                if (isInWishlist) {
                    // Revert back to being in wishlist
                    _wishlistProductIds.value += productId
                } else {
                    // Revert back to not being in wishlist
                    _wishlistProductIds.value -= productId
                }
            }
        }
    }

    // Add these new quiet versions of the wishlist functions that don't set loading state
    private fun addToWishlistQuietly(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.addToWishlist(userId, productId)

                result.fold(
                    onSuccess = {
                        // Update local wishlist state
                        _wishlistProductIds.value += productId

                        // Refresh wishlist products without showing loading state
                        refreshWishlistProductsQuietly(userId)
                    },
                    onFailure = { e ->
                        // Revert the optimistic update
                        _wishlistProductIds.value -= productId
                        Log.e("ProductViewModel", "Error adding to wishlist: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                // Revert the optimistic update
                _wishlistProductIds.value -= productId
                Log.e("ProductViewModel", "Exception adding to wishlist: ${e.message}")
            }
        }
    }

    private fun removeFromWishlistQuietly(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.removeFromWishlist(userId, productId)

                result.fold(
                    onSuccess = {
                        // Update local wishlist state
                        _wishlistProductIds.value -= productId

                        // Refresh wishlist products without showing loading state
                        refreshWishlistProductsQuietly(userId)
                    },
                    onFailure = { e ->
                        // Revert the optimistic update
                        _wishlistProductIds.value += productId
                        Log.e("ProductViewModel", "Error removing from wishlist: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                // Revert the optimistic update
                _wishlistProductIds.value += productId
                Log.e("ProductViewModel", "Exception removing from wishlist: ${e.message}")
            }
        }
    }

    // Add this helper function to refresh wishlist products without showing loading state
    private fun refreshWishlistProductsQuietly(userId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.getWishlistProducts(userId)

                result.fold(
                    onSuccess = { products ->
                        _wishlistProducts.value = products
                        // Also update the IDs set
                        _wishlistProductIds.value = products.map { it.id }.toSet()
                        Log.d("ProductViewModel", "Quietly refreshed ${products.size} wishlist products")
                    },
                    onFailure = { e ->
                        Log.e("ProductViewModel", "Error refreshing wishlist products: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Exception refreshing wishlist products: ${e.message}")
            }
        }
    }

    // Product state sealed class
    sealed class ProductState {
        data object Initial : ProductState()
        data object Loading : ProductState()
        data object Success : ProductState()
        data class ProductCreated(val product: Product) : ProductState()
        data class ProductUpdated(val product: Product) : ProductState()
        data object ProductDeleted : ProductState()
        data object ProductAddedToCart : ProductState()
        data class Error(val message: String) : ProductState()
        data class RatingAdded(val rating: Rating) : ProductState()
        data object RatingDeleted : ProductState()
    }
}
