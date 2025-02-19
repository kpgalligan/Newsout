package com.inspiredandroid.newsout

import com.squareup.sqldelight.Query

/*
 * Copyright 2019 Simon Schubert Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
object Database {
    const val SORT_UNREADCOUNT = 1L
    const val SORT_TITLE = 3L

    fun setup() {
        setupDatabase()
    }

    /**
     * Gets the feed queries
     */
    fun getFeedQueries(): FeedQueries? {
        sqlDriver?.let { driver ->
            val database = SqlDelightDatabase(driver)
            return database.feedQueries
        }
        return null
    }

    /**
     * Gets the item queries
     */
    fun getItemQueries(): ItemQueries? {
        sqlDriver?.let { driver ->
            val database = SqlDelightDatabase(driver)
            return database.itemQueries
        }
        return null
    }

    /**
     * Get the user queries
     */
    fun getUserQueries(): UserQueries? {
        sqlDriver?.let { driver ->
            val database = SqlDelightDatabase(driver)
            return database.userQueries
        }
        return null
    }

    /**
     * Get all feeds sorted by unread count or title based on the the user settings
     */
    fun getFeeds(): MutableList<Feed> {
        val feedQueries = getFeedQueries()
        feedQueries?.let {
            val user = getUser()
            user?.let { u ->
                return when (u.sorting) {
                    SORT_UNREADCOUNT ->
                        if (u.isFolderTop.toBoolean()) {
                            it.selectAllByUnreadCountAndFolder().executeAsList().toMutableList()
                        } else {
                            it.selectAllByUnreadCount().executeAsList().toMutableList()
                        }
                    SORT_TITLE ->
                        if (u.isFolderTop.toBoolean()) {
                            it.selectAllByTitleAndFolder().executeAsList().toMutableList()
                        } else {
                            it.selectAllByTitle().executeAsList().toMutableList()
                        }
                    else -> arrayListOf()
                }
            }
        }
        return arrayListOf()
    }

    /**
     * Get items by feed or folder
     */
    fun getItems(feedId: Long, type: Long): MutableList<Item> {
        val itemQueries = getItemQueries()
        itemQueries?.let {
            return when (type) {
                -2L -> it.selectUnread().executeAsList().toMutableList()
                -1L -> it.selectStarred().executeAsList().toMutableList()
                else -> it.selectAllByFeedIdAndType(feedId, type).executeAsList().toMutableList()
            }
        }
        return arrayListOf()
    }

    /**
     * Get items by feed or folder and search by query
     */
    fun getItemsByQuery(feedId: Long, type: Long, query: String): MutableList<Item> {
        val itemQueries = getItemQueries()
        itemQueries?.let {
            return it.selectAllByFeedIdAndTypeAndQuery(feedId, type, query).executeAsList().toMutableList()
        }
        return arrayListOf()
    }

    /**
     * Get the only and one user from the database
     */
    fun getUser(): User? {
        val userQueries = getUserQueries()
        userQueries?.let {
            return it.selectAll().executeAsOne()
        }
        return null
    }

    /**
     * Get number of unread articles
     */
    fun getTotalUnreadCount(): Long {
        var result = 0L
        try {
            val feedQueries = getFeedQueries()
            feedQueries?.let {
                result = it.countUnread().executeAsOne()
            }
        } catch (ignore: Throwable) {}
        return result
    }

    /**
     * Get number of starred articles
     */
    fun getTotalStarredCount(): Long {
        val itemQueries = getItemQueries()
        itemQueries?.let {
            return it.countStarred().executeAsOne()
        }
        return 0L
    }

    /**
     * Clear feeds and items
     */
    fun clear() {
        getFeedQueries()?.clear()
        getItemQueries()?.clear()
    }
}