package com.example.hci_project.repository

import android.net.Uri
import android.util.Log
import com.example.hci_project.model.Product
import com.example.hci_project.model.Rating
import com.example.hci_project.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val productsCollection = firestore.collection("products")
    private val reviewsCollection = firestore.collection("reviews")
    private val cartsCollection = firestore.collection("carts")
    private val usersCollection = firestore.collection("users")

    suspend fun createProduct(
        title: String,
        description: String,
        price: Double,
        quantity: Int,
        category: String,
        campus: String,
        imageUris: List<Uri>,
        currentUser: User
    ): Result<Product> {
        return try {
            Log.d("ProductRepository", "Creating product: $title")

            // 1. Upload images to Firebase Storage
            val imageUrls = uploadProductImages(imageUris, currentUser.email)

            // 2. Create product object
            val product = Product(
                title = title,
                description = description,
                price = price,
                quantity = quantity,
                category = category,
                campus = campus,
                sellerId = currentUser.userId,
                sellerName = currentUser.fullname,
                sellerEmail = currentUser.email,
                imageUrls = imageUrls,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 3. Save to Firestore
            val documentRef = productsCollection.add(product).await()

            // 4. Get the product with the assigned ID
            val createdProduct = product.copy(id = documentRef.id)

            Log.d("ProductRepository", "Product created successfully with ID: ${createdProduct.id}")
            Result.success(createdProduct)

        } catch (e: Exception) {
            Log.e("ProductRepository", "Error creating product: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(
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
    ): Result<Product> {
        return try {
            Log.d("ProductRepository", "Updating product: $productId")

            // 1. Get the current product
            val productDoc = productsCollection.document(productId).get().await()
            val currentProduct = productDoc.toObject(Product::class.java)
                ?: return Result.failure(Exception("Product not found"))

            // 2. Delete images that are not in imagesToKeep
            val imagesToDelete = currentProduct.imageUrls.filter { !imagesToKeep.contains(it) }
            for (imageUrl in imagesToDelete) {
                try {
                    val storageRef = storage.getReferenceFromUrl(imageUrl)
                    storageRef.delete().await()
                    Log.d("ProductRepository", "Deleted image: $imageUrl")
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error deleting image: ${e.message}", e)
                    // Continue with other images even if one fails
                }
            }

            // 3. Upload new images
            val newImageUrls = uploadProductImages(newImageUris, currentUser.email)

            // 4. Combine kept images and new images
            val finalImageUrls = imagesToKeep + newImageUrls

            // 5. Update product object
            val updatedProduct = Product(
                id = productId,
                title = title,
                description = description,
                price = price,
                quantity = quantity,
                category = category,
                campus = campus,
                sellerId = currentUser.userId,
                sellerName = currentUser.fullname,
                sellerEmail = currentUser.email,
                imageUrls = finalImageUrls,
                createdAt = currentProduct.createdAt,
                updatedAt = Timestamp.now(),
                status = currentProduct.status
            )

            // 6. Save to Firestore
            productsCollection.document(productId).set(updatedProduct).await()

            Log.d("ProductRepository", "Product updated successfully: $productId")
            Result.success(updatedProduct)

        } catch (e: Exception) {
            Log.e("ProductRepository", "Error updating product: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadProductImages(imageUris: List<Uri>, userEmail: String): List<String> {
        val imageUrls = mutableListOf<String>()

        for (uri in imageUris) {
            try {
                // Create a unique filename for each image
                val filename = "product_${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference
                    .child("product_images")
                    .child(userEmail)
                    .child(filename)

                // Upload the file
                storageRef.putFile(uri).await()

                // Get the download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()
                imageUrls.add(downloadUrl)

                Log.d("ProductRepository", "Image uploaded successfully: $downloadUrl")
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error uploading image: ${e.message}", e)
                // Continue with other images even if one fails
            }
        }

        return imageUrls
    }

    suspend fun getProductsByUser(userId: String): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereEqualTo("sellerId", userId)
                .get()
                .await()

            // Sort in memory instead of in the query
            val products = snapshot.toObjects(Product::class.java)
                .sortedByDescending { it.createdAt }

            Result.success(products)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting user products: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            // Simplified query that doesn't require a composite index
            val snapshot = productsCollection
                .get()
                .await()

            // Filter active products in memory instead of in the query
            val products = snapshot.toObjects(Product::class.java)
                .filter { it.status == "active" }
                .sortedByDescending { it.createdAt }

            Result.success(products)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting all products: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getProductById(productId: String): Result<Product> {
        return try {
            Log.d("ProductRepository", "Getting product by ID: $productId")

            val documentSnapshot = productsCollection.document(productId).get().await()
            val product = documentSnapshot.toObject(Product::class.java)
                ?: return Result.failure(Exception("Product not found"))

            Result.success(product)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting product by ID: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            Log.d("ProductRepository", "Deleting product: $productId")

            // Get the product to check if it has images to delete
            val productDoc = productsCollection.document(productId).get().await()
            val product = productDoc.toObject(Product::class.java)

            // Delete images from storage if they exist
            product?.imageUrls?.forEach { imageUrl ->
                try {
                    val storageRef = storage.getReferenceFromUrl(imageUrl)
                    storageRef.delete().await()
                    Log.d("ProductRepository", "Deleted image: $imageUrl")
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error deleting image: ${e.message}", e)
                    // Continue with other images even if one fails
                }
            }

            // Delete the product document
            productsCollection.document(productId).delete().await()
            Log.d("ProductRepository", "Product deleted successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error deleting product: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProductStatus(productId: String, status: String): Result<Unit> {
        return try {
            Log.d("ProductRepository", "Updating product status: $productId to $status")

            productsCollection.document(productId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()

            Log.d("ProductRepository", "Product status updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error updating product status: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user profile picture - now public so it can be used for sellers too
    suspend fun getUserProfilePicture(userId: String): Result<String?> {
        return try {
            Log.d("ProductRepository", "Getting profile picture for user: $userId")

            // First try to get from users collection by userId field
            val userDocsByUserId = usersCollection.whereEqualTo("userId", userId).get().await()
            if (!userDocsByUserId.isEmpty) {
                val profilePic = userDocsByUserId.documents[0].getString("profilePictureUrl")
                Log.d("ProductRepository", "Found profile picture by userId: $profilePic")
                return Result.success(profilePic)
            }

            // If not found, try to get from the document ID
            val userDoc = usersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                val profilePic = userDoc.getString("profilePictureUrl")
                Log.d("ProductRepository", "Found profile picture by document ID: $profilePic")
                return Result.success(profilePic)
            }

            Log.d("ProductRepository", "No profile picture found for user: $userId")
            Result.success(null)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting user profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Rating and Review functions
    suspend fun addRating(
        productId: String,
        userId: String,
        userName: String,
        rating: Float,
        comment: String
    ): Result<Rating> {
        return try {
            Log.d("ProductRepository", "Adding rating for product: $productId, user: $userId, rating: $rating")

            // Get user profile picture
            val profilePictureResult = getUserProfilePicture(userId)
            val profilePicture = profilePictureResult.getOrNull() ?: ""
            Log.d("ProductRepository", "User profile picture: $profilePicture")

            // Create the review document data with explicit field names
            val reviewData = mapOf(
                "productId" to productId,
                "userId" to userId,
                "userName" to userName,
                "rating" to rating,
                "comment" to comment,
                "createdAt" to Timestamp.now(),
                "userProfilePicture" to profilePicture
            )

            Log.d("ProductRepository", "Review data to save: $reviewData")

            // Check if user already rated this product
            val existingReviews = reviewsCollection
                .whereEqualTo("productId", productId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d("ProductRepository", "Found ${existingReviews.size()} existing reviews")

            val reviewId: String

            if (!existingReviews.isEmpty) {
                // Update existing review
                val existingReviewId = existingReviews.documents[0].id
                Log.d("ProductRepository", "Updating existing review: $existingReviewId")
                reviewsCollection.document(existingReviewId).set(reviewData).await()
                reviewId = existingReviewId
            } else {
                // Create new review
                Log.d("ProductRepository", "Creating new review")
                val newReviewRef = reviewsCollection.add(reviewData).await()
                Log.d("ProductRepository", "Created new review with ID: ${newReviewRef.id}")
                reviewId = newReviewRef.id
            }

            // Verify the review was saved
            val savedReview = reviewsCollection.document(reviewId).get().await()
            if (savedReview.exists()) {
                Log.d("ProductRepository", "Review saved successfully: ${savedReview.data}")
            } else {
                Log.e("ProductRepository", "Review was not saved properly!")
            }

            // Create the Rating object to return
            val resultRating = Rating(
                id = reviewId,
                productId = productId,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment,
                createdAt = Timestamp.now(),
                userProfilePicture = profilePicture
            )

            Log.d("ProductRepository", "Rating operation completed successfully: $resultRating")
            Result.success(resultRating)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding rating: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get ratings for a product from the reviews collection
    suspend fun getRatingsForProduct(productId: String): Result<List<Rating>> {
        return try {
            Log.d("ProductRepository", "Getting ratings for product: $productId")

            // Use get() with source set to SERVER to force a fresh fetch from Firestore
            val snapshot = reviewsCollection
                .whereEqualTo("productId", productId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get(Source.SERVER)  // Force server fetch
                .await()

            Log.d("ProductRepository", "Raw reviews data count: ${snapshot.size()}")

            // Log each document for debugging
            snapshot.documents.forEachIndexed { index, doc ->
                Log.d("ProductRepository", "Review document $index: ${doc.id}, data: ${doc.data}")
            }

            val ratings = snapshot.documents.mapNotNull { doc ->
                try {
                    // Manually construct Rating object from document fields
                    val id = doc.id
                    val docProductId = doc.getString("productId") ?: ""
                    val userId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: ""

                    // Handle different possible types for rating
                    val ratingValue = when (val ratingField = doc.get("rating")) {
                        is Number -> ratingField.toFloat()
                        is String -> ratingField.toFloatOrNull() ?: 0f
                        else -> 0f
                    }

                    val comment = doc.getString("comment") ?: ""
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    val userProfilePicture = doc.getString("userProfilePicture") ?: ""

                    val rating = Rating(
                        id = id,
                        productId = docProductId,
                        userId = userId,
                        userName = userName,
                        rating = ratingValue,
                        comment = comment,
                        createdAt = createdAt,
                        userProfilePicture = userProfilePicture
                    )

                    Log.d("ProductRepository", "Converted review to Rating: $rating")
                    rating
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error converting document to Rating: ${e.message}", e)
                    null
                }
            }

            Log.d("ProductRepository", "Retrieved ${ratings.size} ratings for product: $productId")
            Result.success(ratings)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting ratings: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Alternative method to force fetch ratings with a different approach
    suspend fun getRatingsForProductForced(productId: String): Result<List<Rating>> {
        return try {
            Log.d("ProductRepository", "Force getting ratings for product: $productId")

            // Get all reviews and filter manually
            val snapshot = reviewsCollection
                .get(Source.SERVER)
                .await()

            Log.d("ProductRepository", "Force raw reviews total count: ${snapshot.size()}")

            // Filter and map the reviews
            val ratings = snapshot.documents
                .filter { it.getString("productId") == productId }
                .mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: ""

                        val ratingValue = when (val ratingField = doc.get("rating")) {
                            is Number -> ratingField.toFloat()
                            is String -> ratingField.toFloatOrNull() ?: 0f
                            else -> 0f
                        }

                        val comment = doc.getString("comment") ?: ""
                        val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        val userProfilePicture = doc.getString("userProfilePicture") ?: ""

                        Rating(
                            id = id,
                            productId = productId,
                            userId = userId,
                            userName = userName,
                            rating = ratingValue,
                            comment = comment,
                            createdAt = createdAt,
                            userProfilePicture = userProfilePicture
                        )
                    } catch (e: Exception) {
                        Log.e("ProductRepository", "Error converting document in forced fetch: ${e.message}", e)
                        null
                    }
                }
                .sortedByDescending { it.createdAt }

            Log.d("ProductRepository", "Force retrieved ${ratings.size} ratings for product: $productId")
            Result.success(ratings)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error in forced getting ratings: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user's rating for a product from the reviews collection
    suspend fun getUserRatingForProduct(productId: String, userId: String): Result<Rating?> {
        return try {
            Log.d("ProductRepository", "Getting user rating for product: $productId, user: $userId")

            val snapshot = reviewsCollection
                .whereEqualTo("productId", productId)
                .whereEqualTo("userId", userId)
                .get(Source.SERVER)  // Force server fetch
                .await()

            if (snapshot.isEmpty) {
                Log.d("ProductRepository", "No rating found for user $userId on product $productId")
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]

                try {
                    // Manually construct Rating object from document fields
                    val id = doc.id
                    val docProductId = doc.getString("productId") ?: ""
                    val docUserId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: ""
                    // Handle different possible types for rating
                    val ratingValue = when (val ratingField = doc.get("rating")) {
                        is Number -> ratingField.toFloat()
                        else -> 0f
                    }
                    val comment = doc.getString("comment") ?: ""
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    val userProfilePicture = doc.getString("userProfilePicture") ?: ""

                    val rating = Rating(
                        id = id,
                        productId = docProductId,
                        userId = docUserId,
                        userName = userName,
                        rating = ratingValue,
                        comment = comment,
                        createdAt = createdAt,
                        userProfilePicture = userProfilePicture
                    )

                    Log.d("ProductRepository", "Found rating for user: $rating")
                    Result.success(rating)
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error converting document to Rating: ${e.message}", e)
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting user rating: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Add this function to directly get all ratings for all products in one call
    suspend fun getAllRatingsAtOnce(): Result<Map<String, List<Rating>>> {
        return try {
            Log.d("ProductRepository", "Getting all ratings at once")

            // Get all reviews directly from Firestore
            val snapshot = reviewsCollection
                .get(Source.SERVER)
                .await()

            Log.d("ProductRepository", "Retrieved ${snapshot.size()} total reviews")

            // Group reviews by product ID
            val ratingsMap = mutableMapOf<String, MutableList<Rating>>()

            snapshot.documents.forEach { doc ->
                try {
                    val productId = doc.getString("productId")
                    if (productId != null) {
                        val id = doc.id
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: ""

                        val ratingValue = when (val ratingField = doc.get("rating")) {
                            is Number -> ratingField.toFloat()
                            is String -> ratingField.toFloatOrNull() ?: 0f
                            else -> 0f
                        }

                        val comment = doc.getString("comment") ?: ""
                        val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        val userProfilePicture = doc.getString("userProfilePicture") ?: ""

                        val rating = Rating(
                            id = id,
                            productId = productId,
                            userId = userId,
                            userName = userName,
                            rating = ratingValue,
                            comment = comment,
                            createdAt = createdAt,
                            userProfilePicture = userProfilePicture
                        )

                        // Add to the map, creating a new list if needed
                        if (!ratingsMap.containsKey(productId)) {
                            ratingsMap[productId] = mutableListOf()
                        }
                        ratingsMap[productId]?.add(rating)
                    }
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error processing review document: ${e.message}")
                    // Continue with other documents
                }
            }

            // Sort each product's ratings by createdAt (newest first)
            ratingsMap.forEach { (productId, ratings) ->
                ratings.sortByDescending { it.createdAt }
                Log.d("ProductRepository", "Product $productId has ${ratings.size} ratings")
            }

            Result.success(ratingsMap)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting all ratings: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Cart functions
    suspend fun addToCart(userId: String, productId: String, quantity: Int): Result<Unit> {
        return try {
            Log.d("ProductRepository", "Adding product to cart: $productId for user: $userId, quantity: $quantity")

            // Reference to the user's cart document
            val cartDocRef = cartsCollection.document(userId)

            // Transaction to ensure atomic updates
            firestore.runTransaction { transaction ->
                val cartDoc = transaction.get(cartDocRef)

                if (cartDoc.exists()) {
                    // Get current items or create empty map
                    @Suppress("UNCHECKED_CAST")
                    val currentItems = cartDoc.get("items") as? Map<String, Any> ?: mapOf()

                    // Create updated items map
                    val updatedItems = currentItems.toMutableMap()
                    updatedItems[productId] = mapOf(
                        "quantity" to quantity,
                        "updatedAt" to Timestamp.now()
                    )

                    // Update the cart document
                    transaction.update(cartDocRef,
                        "items", updatedItems,
                        "updatedAt", Timestamp.now()
                    )
                } else {
                    // Create new cart document
                    val newCart = mapOf(
                        "userId" to userId,
                        "items" to mapOf(productId to mapOf(
                            "quantity" to quantity,
                            "addedAt" to Timestamp.now()
                        )),
                        "createdAt" to Timestamp.now(),
                        "updatedAt" to Timestamp.now()
                    )

                    transaction.set(cartDocRef, newCart)
                }
            }.await()

            Log.d("ProductRepository", "Product added to cart successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding to cart: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fix the getCartItems method to properly fetch cart items from Firestore
    suspend fun getCartItems(userId: String): Result<Map<String, Int>> {
        return try {
            Log.d("ProductRepository", "Getting cart items for user: $userId")

            val cartDoc = cartsCollection.document(userId).get().await()

            val cartItems = if (cartDoc.exists()) {
                val items = cartDoc.get("items") as? Map<*, *>

                items?.mapNotNull { (key, value) ->
                    val productId = key as? String ?: return@mapNotNull null

                    // Handle both formats: direct integer or nested map
                    val quantity = when (value) {
                        is Number -> value.toInt()
                        is Map<*, *> -> (value["quantity"] as? Number)?.toInt() ?: 0
                        else -> 0
                    }

                    productId to quantity
                }?.toMap()
                    ?: mapOf()
            } else {
                mapOf()
            }

            Log.d("ProductRepository", "Retrieved ${cartItems.size} cart items for user: $userId")
            Result.success(cartItems)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting cart items: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(userId: String, productId: String): Result<Unit> {
        return try {
            Log.d("ProductRepository", "Removing product from cart: $productId for user: $userId")

            val cartDocRef = cartsCollection.document(userId)
            val cartDoc = cartDocRef.get().await()

            if (cartDoc.exists()) {
                val itemsAny = cartDoc.get("items")

                if (itemsAny is Map<*, *>) {
                    // Safely convert to the correct type
                    @Suppress("UNCHECKED_CAST")
                    val cartItems = itemsAny as Map<String, Any>

                    val updatedItems = cartItems.toMutableMap()

                    // Remove the product from the cart
                    updatedItems.remove(productId)

                    cartDocRef.update(
                        "items", updatedItems,
                        "updatedAt", Timestamp.now()
                    ).await()

                    Log.d("ProductRepository", "Product removed from cart successfully")
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("ProductRepository", "Error removing from cart: ${e.message}", e)
            Result.failure(e)
        }
    }
}
