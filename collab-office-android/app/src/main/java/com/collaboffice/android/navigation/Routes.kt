package com.collaboffice.android.navigation

object Routes {
    // Auth flow
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main flow
    const val MAIN = "main"
    const val HOME = "home"
    const val CHAT = "chat"
    const val DOCUMENT = "document"
    const val TASK = "task"
    const val FILE = "file"
    const val PROFILE = "profile"

    // Detail screens
    const val CHAT_DETAIL = "chat/{sessionId}"
    const val DOC_EDIT = "doc/edit/{docId}"
    const val TASK_DETAIL = "task/{taskId}"
    const val MEETING = "meeting/{roomId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SPACE_LIST = "space/list"
    const val SPACE_DETAIL = "space/{spaceId}"
}
