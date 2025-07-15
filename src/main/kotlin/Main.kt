import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

const val BASE_URL = "http://localhost:9999"

@Serializable
data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var attachment: Attachment? = null,
)

@Serializable
data class Attachment(
    val url: String,
    val description: String,
    val type: AttachmentType,
)

@Serializable
enum class AttachmentType {
    IMAGE, VIDEO, AUDIO
}

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

suspend fun <T> makeRequest(url: String, deserializer: (String) -> T): T =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.inputStream.bufferedReader().use {
                val response = it.readText()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("HTTP error: ${connection.responseCode}")
                }
                deserializer(response)
            }
        } finally {
            connection.disconnect()
        }
    }

suspend fun getPosts(): List<Post> {
    val url = "$BASE_URL/api/posts"
    return makeRequest(url) { response ->
        json.decodeFromString(response)
    }
}

fun main() = runBlocking {
    try {
        println("Получаем посты...")
        val posts = getPosts()
        println("Получено ${posts.size} постов")

        if (posts.isNotEmpty()) {
            val firstPost = posts.first()
            println("\nПервый пост:")
            println("ID: ${firstPost.id}")
            println("Автор: ${firstPost.author}")
            println("Аватар: ${firstPost.authorAvatar}")
            println("Текст: ${firstPost.content}")
        }
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
        e.printStackTrace()
    }
}