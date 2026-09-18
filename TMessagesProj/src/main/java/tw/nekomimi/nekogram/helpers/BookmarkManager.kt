package tw.nekomimi.nekogram.helpers

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


object BookmarkManager {

    private val fileName = "bookmarks.json"

    fun bookmarkMessage(context: Context, chatId: Long, messageId: Long): Boolean {
        val file = File(context.filesDir, fileName)
        val json = if (file.exists()) JSONObject(file.readText()) else JSONObject()
        val key = chatId.toString()
        val array = if (json.has(key)) json.getJSONArray(key) else JSONArray()

        // Convert JSONArray → MutableList<Long> for easy handling
        val list = mutableListOf<Long>()
        for (i in 0 until array.length()) list.add(array.getLong(i))

        return if (list.contains(messageId)) {
            // Remove bookmark
            list.remove(messageId)
            json.put(key, JSONArray(list))
           file.writeText(json.toString())
            Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
            false
        } else {
            // Add bookmark (limit 20 per chat)
            if (list.size >= 20) {
                Toast.makeText(context, "Max 20 bookmarks per chat", Toast.LENGTH_SHORT).show()
                false
            } else {
                list.add(messageId)
                json.put(key, JSONArray(list))
                file.writeText(json.toString())
                Toast.makeText(context, "Message bookmarked", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    // Returns list of message IDs that are bookmarked for a chat
    fun getBookmarks(context: Context, chatId: Long): List<Long> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        val json = JSONObject(file.readText())
        if (!json.has(chatId.toString())) return emptyList()

        val array = json.getJSONArray(chatId.toString())
        val list = mutableListOf<Long>()
        for (i in 0 until array.length()) list.add(array.getLong(i))
       return list
    }

    // Helper to check if a message is bookmarked (for decoration)
    fun isBookmarked(context: Context, chatId: Long, messageId: Long): Boolean {
        return getBookmarks(context, chatId).contains(messageId)
    }

    // Debug/log view of bookmarks per chat
    fun logBookmarks(context: Context, chatId: Long) {
        val bookmarks = getBookmarks(context, chatId)
        if (bookmarks.isEmpty()) {
            Log.d("BookmarkManager", "No bookmarks for chat $chatId")
        } else {
            Log.d("BookmarkManager", "Bookmarks for chat $chatId: $bookmarks")
        }
    }
   fun showBookmarksDialog(context: Context, chatId: Long) {
        val bookmarks = getBookmarks(context, chatId)

        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Bookmarks")
                .setMessage("No bookmarks yet")
                .setPositiveButton("OK", null)
                .show()
            return
        }

       val items = bookmarks.map { "Message ID: $it" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Bookmarked Messages")
            .setItems(items) { _, index ->
                val messageId = bookmarks[index]
                GoToMessage(context, chatId, messageId)
            }
            .setPositiveButton("OK", null)
            .show()
    }
    fun GoToMessage(context: Context, chatId: Long, messageId: Long){
        try {
            val uri = android.net.Uri.parse("https://t.me/c/$chatId/$messageId")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to open message link", Toast.LENGTH_SHORT).show()
        }
    }

}