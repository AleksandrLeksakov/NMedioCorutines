import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

const val BASE_URL = "http://localhost:9999"

// Основной класс поста с опциональными полями
@Serializable
data class Post(
    val id: Long,
    @SerialName("authorId") val authorId: Long? = null,
    @SerialName("author") val authorName: String? = null,
    @SerialName("authorAvatar") val authorAvatar: String? = null,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var attachment: Attachment? = null,
) {
    // Вычисляемое свойство для удобного доступа к имени автора
    val authorDisplayName: String
        get() = authorName ?: "Unknown Author"
}

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

// Гибкий JSON парсер
val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

suspend fun <T> makeRequest(url: String, deserializer: (String) -> T): T =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.inputStream.bufferedReader().use {
                val response = it.readText()
                println("Response from $url: $response") // Логирование ответа

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("HTTP error ${connection.responseCode}: ${connection.responseMessage}")
                }

                try {
                    deserializer(response)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to parse response: ${e.message}", e)
                }
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

suspend fun getAuthor(authorId: Long): Author? {
    return try {
        val url = "$BASE_URL/api/authors/$authorId"
        makeRequest(url) { response ->
            json.decodeFromString<Author>(response)
        }
    } catch (e: Exception) {
        println("Failed to fetch author $authorId: ${e.message}")
        null
    }
}

@Serializable
data class Author(
    val id: Long,
    val name: String,
    val avatar: String,
)

suspend fun enrichPostsWithAuthors(posts: List<Post>): List<Post> = coroutineScope {
    posts.map { post ->
        if (post.authorId != null && post.authorName == null) {
            val author = getAuthor(post.authorId)
            post.copy(
                authorName = author?.name,
                authorAvatar = author?.avatar
            )
        } else {
            post
        }
    }
}

fun main() = runBlocking {
    try {
        println("Fetching posts...")
        val posts = getPosts()

        println("Enriching posts with author info...")
        val enrichedPosts = enrichPostsWithAuthors(posts)

        println("\nFound ${enrichedPosts.size} posts:")
        enrichedPosts.forEachIndexed { index, post ->
            println("${index + 1}. [ID: ${post.id}] ${post.authorDisplayName}: ${post.content.take(30)}...")
            println("   Likes: ${post.likes}, Published: ${post.published}")
            if (post.attachment != null) {
                println("   Attachment: ${post.attachment!!.type} - ${post.attachment!!.url}")
            }
            println()
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}