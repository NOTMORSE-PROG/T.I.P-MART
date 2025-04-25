package com.example.hci_project.repository

import android.util.Log
import com.example.hci_project.model.Conversation
import com.example.hci_project.model.Message
import com.example.hci_project.model.Product
import com.example.hci_project.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val conversationsCollection = firestore.collection("conversations")
    private val usersCollection = firestore.collection("users")

    // Start a new conversation or get existing one
    suspend fun getOrCreateConversation(
        currentUser: User,
        otherUserId: String,
        otherUserName: String,
        product: Product
    ): Result<Conversation> {
        return try {
            Log.d("MessageRepository", "Getting or creating conversation between ${currentUser.userId} and $otherUserId")

            // Check if a conversation already exists between these users about this product
            val existingConversations = conversationsCollection
                .whereArrayContains("participants", currentUser.userId)
                .whereEqualTo("productId", product.id)
                .get()
                .await()

            val conversation = existingConversations.documents.find { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(otherUserId) == true
            }

            if (conversation != null) {
                // Conversation exists, return it
                val conversationData = conversation.toObject(Conversation::class.java)
                    ?.copy(id = conversation.id)
                Log.d("MessageRepository", "Found existing conversation: ${conversation.id}")
                Result.success(conversationData!!)
            } else {
                // Create a new conversation
                Log.d("MessageRepository", "Creating new conversation")

                // Get other user's profile picture
                val otherUserProfile = getUserProfilePicture(otherUserId).getOrNull() ?: ""
                val currentUserProfile = getUserProfilePicture(currentUser.userId).getOrNull() ?: ""

                // Create participant maps
                val participantNames = mapOf(
                    currentUser.userId to currentUser.fullname,
                    otherUserId to otherUserName
                )

                val participantProfiles = mapOf(
                    currentUser.userId to currentUserProfile,
                    otherUserId to otherUserProfile
                )

                val unreadCount = mapOf(
                    currentUser.userId to 0,
                    otherUserId to 0
                )

                // Create the conversation object
                val newConversation = Conversation(
                    participants = listOf(currentUser.userId, otherUserId),
                    participantNames = participantNames,
                    participantProfiles = participantProfiles,
                    lastMessage = "",
                    lastMessageTimestamp = Timestamp.now(),
                    unreadCount = unreadCount,
                    productId = product.id,
                    productTitle = product.title,
                    productImage = if (product.imageUrls.isNotEmpty()) product.imageUrls[0] else ""
                )

                // Save to Firestore
                val docRef = conversationsCollection.add(newConversation).await()
                val createdConversation = newConversation.copy(id = docRef.id)

                Log.d("MessageRepository", "Created new conversation with ID: ${createdConversation.id}")
                Result.success(createdConversation)
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error getting or creating conversation: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Send a message
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        content: String
    ): Result<Message> {
        return try {
            Log.d("MessageRepository", "Sending message in conversation: $conversationId")

            // Get sender's profile picture
            val senderProfilePicture = getUserProfilePicture(senderId).getOrNull() ?: ""

            // Create the message
            val message = Message(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                timestamp = Timestamp.now(),
                read = false,
                senderProfilePicture = senderProfilePicture
            )

            // Add message to the messages subcollection
            val messageRef = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .add(message)
                .await()

            // Update the conversation with the last message
            val conversationRef = conversationsCollection.document(conversationId)

            // Get current unread counts
            val conversationDoc = conversationRef.get().await()
            val conversation = conversationDoc.toObject(Conversation::class.java)
            val currentUnreadCount = conversation?.unreadCount ?: mapOf()

            // Increment unread count for receiver
            val updatedUnreadCount = currentUnreadCount.toMutableMap()
            updatedUnreadCount[receiverId] = (updatedUnreadCount[receiverId] ?: 0) + 1

            // Update conversation
            conversationRef.update(
                mapOf(
                    "lastMessage" to content,
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageSenderId" to senderId,
                    "unreadCount" to updatedUnreadCount
                )
            ).await()

            val sentMessage = message.copy(id = messageRef.id)
            Log.d("MessageRepository", "Message sent successfully with ID: ${sentMessage.id}")
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get messages for a conversation
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> = callbackFlow {
        Log.d("MessageRepository", "Getting messages for conversation: $conversationId")

        val listenerRegistration = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageRepository", "Error getting messages: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val message = doc.toObject(Message::class.java)
                            message?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Error converting message: ${e.message}", e)
                            null
                        }
                    }
                    Log.d("MessageRepository", "Received ${messages.size} messages for conversation: $conversationId")
                    trySend(messages)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Get all conversations for a user
    fun getConversationsForUser(userId: String): Flow<List<Conversation>> = callbackFlow {
        Log.d("MessageRepository", "Getting conversations for user: $userId")

        val listenerRegistration = conversationsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageRepository", "Error getting conversations: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val conversation = doc.toObject(Conversation::class.java)
                            conversation?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Error converting conversation: ${e.message}", e)
                            null
                        }
                    }
                    Log.d("MessageRepository", "Received ${conversations.size} conversations for user: $userId")
                    trySend(conversations)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Mark messages as read
    suspend fun markConversationAsRead(conversationId: String, userId: String): Result<Unit> {
        return try {
            Log.d("MessageRepository", "Marking conversation as read: $conversationId for user: $userId")

            // Get the conversation
            val conversationRef = conversationsCollection.document(conversationId)
            val conversationDoc = conversationRef.get().await()
            val conversation = conversationDoc.toObject(Conversation::class.java)

            if (conversation != null) {
                // Update unread count for this user
                val unreadCount = conversation.unreadCount.toMutableMap()
                unreadCount[userId] = 0

                // Update the conversation
                conversationRef.update("unreadCount", unreadCount).await()

                // Mark all messages from other users as read
                val batch = firestore.batch()
                val messages = conversationsCollection
                    .document(conversationId)
                    .collection("messages")
                    .whereEqualTo("read", false)
                    .whereNotEqualTo("senderId", userId)
                    .get()
                    .await()

                messages.documents.forEach { doc ->
                    batch.update(doc.reference, "read", true)
                }

                batch.commit().await()

                Log.d("MessageRepository", "Marked conversation as read successfully")
                Result.success(Unit)
            } else {
                Log.e("MessageRepository", "Conversation not found: $conversationId")
                Result.failure(Exception("Conversation not found"))
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error marking conversation as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get total unread message count for a user
    fun getTotalUnreadCount(userId: String): Flow<Int> = callbackFlow {
        Log.d("MessageRepository", "Getting total unread count for user: $userId")

        val listenerRegistration = conversationsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageRepository", "Error getting unread count: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var totalUnread = 0
                    snapshot.documents.forEach { doc ->
                        val conversation = doc.toObject(Conversation::class.java)
                        val unreadForUser = conversation?.unreadCount?.get(userId) ?: 0
                        totalUnread += unreadForUser
                    }
                    Log.d("MessageRepository", "Total unread count for user $userId: $totalUnread")
                    trySend(totalUnread)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Helper function to get user profile picture
    private suspend fun getUserProfilePicture(userId: String): Result<String?> {
        return try {
            Log.d("MessageRepository", "Getting profile picture for user: $userId")

            // First try to get from users collection by userId field
            val userDocsByUserId = usersCollection.whereEqualTo("userId", userId).get().await()
            if (!userDocsByUserId.isEmpty) {
                val profilePic = userDocsByUserId.documents[0].getString("profilePictureUrl")
                Log.d("MessageRepository", "Found profile picture by userId: $profilePic")
                return Result.success(profilePic)
            }

            // If not found, try to get from the document ID
            val userDoc = usersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                val profilePic = userDoc.getString("profilePictureUrl")
                Log.d("MessageRepository", "Found profile picture by document ID: $profilePic")
                return Result.success(profilePic)
            }

            Log.d("MessageRepository", "No profile picture found for user: $userId")
            Result.success(null)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error getting user profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }
}
