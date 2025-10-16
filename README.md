
The goal of our project was to create a basic forum site with some unique features. We accomplished this by implementing a relatively normal forum along with a novel map based interface to explore the posts with. Reflecting the name Bazaar News, we created a forum experience where you "Enter the Bazaar". 

We chose the language of Kotlin to build the project in because we thought it would be interesting and found it had good libraries and support for web application development.

Two of the main features of Kotlin we utilized for our project were its concurrency and coroutines and null safety features.

One example of built in concurrency features are suspend functions: 

```kotlin
suspend fun getPosts(sort: String = "new"): List<Post> {
    return httpClient.get("$BASE_URL/posts?sort=$sort").body()
} kotlin
```
Suspend functions are non-blocking functions that do not stop threads from running when they are paused. This was particularly useful for HTTP requests so threads would not block while calls were wating for responses.

The null safety of kotlin was useful for database indexing and had many other use cases.

One example of this is was the upvote counting function

```kotlin
suspend fun getUpvoteCount(postId: Int): Int {
    return try {
        val response: Map<String, Int> = httpClient.get("$BASE_URL/posts/$postId/upvotes").body()
        response["count"] ?: 0
    } catch (e: Exception) {
        console.error("Error fetching upvote count for post $postId: $e")
        0
    }
}
```
Since sql transactions may return null for the count, Kotlin infers the type Int? for response["count"], which represents a nullable integer in Kotlin. The ?: operator guarantees that this null case has a fall back, ensuring that the function will always return a non-null Int so there is never a NullPointerException during database transactions.
