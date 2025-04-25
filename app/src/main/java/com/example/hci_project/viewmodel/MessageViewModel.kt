package com.example.hci_project.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hci_project.model.Conversation
import com.example.hci_project.model.Message
import com.example.hci_project.model.Product
import com.example.hci_project.model.User
import com.example.hci_project.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    // Message state
    private val _messageState = MutableStateFlow<MessageState>(MessageState.Initial)
    val messageState: StateFlow<MessageState> = _messageState.asStateFlow()


    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    // Messages for current conversation
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // User conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Total unread count
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    // Message input
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    // Start or get a conversation
    fun startOrGetConversation(currentUser: User, sellerId: String, sellerName: String, product: Product) {
        viewModelScope.launch {
            _messageState.value = MessageState.Loading

            try {
                val result = messageRepository.getOrCreateConversation(
                    currentUser = currentUser,
                    otherUserId = sellerId,
                    otherUserName = sellerName,
                    product = product
                )

                result.fold(
                    onSuccess = { conversation ->
                        _currentConversation.value = conversation
                        _messageState.value = MessageState.Success

                        // Start listening for messages
                        getMessagesForConversation(conversation.id)

                        // Mark as read
                        markConversationAsRead(conversation.id, currentUser.userId)
                    },
                    onFailure = { e ->
                        _messageState.value = MessageState.Error(e.message ?: "Failed to start conversation")
                    }
                )
            } catch (e: Exception) {
                _messageState.value = MessageState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Send a message
    fun sendMessage(senderId: String, senderName: String, receiverId: String) {
        viewModelScope.launch {
            val conversationId = _currentConversation.value?.id ?: return@launch
            val content = _messageInput.value.trim()

            if (content.isEmpty()) {
                return@launch
            }

            _messageState.value = MessageState.Loading

            try {
                val result = messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    receiverId = receiverId,
                    content = content
                )

                result.fold(
                    onSuccess = { message ->
                        _messageInput.value = ""
                        _messageState.value = MessageState.MessageSent(message)
                    },
                    onFailure = { e ->
                        _messageState.value = MessageState.Error(e.message ?: "Failed to send message")
                    }
                )
            } catch (e: Exception) {
                _messageState.value = MessageState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Get messages for a conversation
    fun getMessagesForConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                messageRepository.getMessagesForConversation(conversationId).collectLatest { messages ->
                    _messages.value = messages
                }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error getting messages: ${e.message}", e)
            }
        }
    }

    // Get conversations for a user
    fun getConversationsForUser(userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.getConversationsForUser(userId).collectLatest { conversations ->
                    _conversations.value = conversations
                }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error getting conversations: ${e.message}", e)
            }
        }
    }

    // Mark conversation as read
    fun markConversationAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markConversationAsRead(conversationId, userId)
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error marking conversation as read: ${e.message}", e)
            }
        }
    }

    // Get total unread count
    fun getTotalUnreadCount(userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.getTotalUnreadCount(userId).collectLatest { count ->
                    _totalUnreadCount.value = count
                }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error getting total unread count: ${e.message}", e)
            }
        }
    }

    // Update message input
    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    // Set current conversation
    fun setCurrentConversation(conversation: Conversation) {
        _currentConversation.value = conversation
    }

    // Clear current conversation
    fun clearCurrentConversation() {
        _currentConversation.value = null
        _messages.value = emptyList()
    }

    // Reset message state
    fun resetMessageState() {
        _messageState.value = MessageState.Initial
    }

    // Message state sealed class
    sealed class MessageState {
        data object Initial : MessageState()
        data object Loading : MessageState()
        data object Success : MessageState()
        data class MessageSent(val message: Message) : MessageState()
        data class Error(val message: String) : MessageState()
    }
}
