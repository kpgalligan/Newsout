package com.inspiredandroid.newsout.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.inspiredandroid.newsout.*
import com.inspiredandroid.newsout.adapters.ItemsAdapter
import com.inspiredandroid.newsout.callbacks.OnAddFeedInterface
import com.inspiredandroid.newsout.callbacks.OnItemClickInterface
import com.inspiredandroid.newsout.dialogs.AddFeedDialog
import com.inspiredandroid.newsout.views.SpacingItemDecoration
import kotlinx.android.synthetic.main.activity_items.*
import kotlinx.android.synthetic.main.content_feeds.*

/*
 * Copyright 2019 Simon Schubert Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
class ItemsActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, OnAddFeedInterface,
    OnItemClickInterface {

    private val adapter by lazy {
        ItemsAdapter(Database.getItems(id, type), this)
    }
    private val layoutManager by lazy {
        StaggeredGridLayoutManager(calculateNumberOfColumns(), StaggeredGridLayoutManager.VERTICAL)
    }
    var id: Long = 0
    var type: Long = 0
    var searchMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_items)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        id = intent.getLongExtra(KEY_ID, 0)
        type = intent.getLongExtra(KEY_TYPE, 0L)
        title = intent.getStringExtra(KEY_TITLE)

        fab.setOnClickListener { _ ->
            if (type == 0L) {
                Api.markFeedAsRead(id) {
                    adapter.updateItems(it)
                    updateFab()
                }
            } else if (type == 1L) {
                Api.markFolderAsRead(id) {
                    adapter.updateItems(it)
                    updateFab()
                }
            }
        }

        swiperefresh.setOnRefreshListener(this)
        swiperefresh.isRefreshing = true

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(SpacingItemDecoration(4.px))
        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val positions = layoutManager.findFirstVisibleItemPositions(null)
                positions.forEach { position ->
                    val viewHolder =
                        recyclerView.findViewHolderForAdapterPosition(position) as? ItemsAdapter.ItemViewHolder
                    viewHolder?.let {
                        if (it.isUnread) {
                            Handler().post {
                                it.markAsRead()
                                updateFab()
                            }
                        }
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && (type == 1L || type == 0L)) {
                    showLoading()
                    fetchItems(true)
                }
            }
        }
        recyclerView.addOnScrollListener(listener)

        fetchItems(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_items, menu)
        menu?.findItem(R.id.action_add)?.isVisible = type != 1L

        searchMenuItem = menu?.findItem(R.id.action_search)
        searchMenuItem?.isVisible = type == 1L || type == 0L
        val searchView = searchMenuItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (!searchView.isIconified) {
                    searchView.isIconified = true
                }
                searchMenuItem?.collapseActionView()
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                adapter.query = s.toLowerCase()
                if(s.isEmpty()) {
                    adapter.updateItems(Database.getItems(id, type))
                } else {
                    adapter.updateItems(Database.getItemsByQuery(id, type, "%$s%"))
                }
                return false
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
            R.id.action_add -> {
                val dialog = AddFeedDialog.getInstance()
                dialog.show(supportFragmentManager, "TAG")
            }
        }
        return true
    }

    override fun onRefresh() {
        fetchItems(false)
    }

    override fun onAddFeed(url: String) {
        showLoading()
        Api.createFeed(url, id, {
            fetchItems(false)
        }, {
            if (isThere()) {
                hideLoading()
            }
        })
    }

    override fun onClickItem(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun fetchItems(offset: Boolean) {
        if (type == 1L || type == 0L) {
            Api.getItems(id, type, offset, {
                if (isThere()) {
                    updateAdapterAndHideLoading(it)
                    updateFab()
                }
            }, {
                if (isThere()) {
                    hideLoading()
                }
            })
        } else if (type == -2L) {
            Api.getUnreadItems({
                if (isThere()) {
                    updateAdapterAndHideLoading(it)
                    updateFab()
                }
            }, {
                if (isThere()) {
                    hideLoading()
                }
            })
        } else if (type == -1L) {
            Api.getStarredItems({
                if (isThere()) {
                    updateAdapterAndHideLoading(it)
                    updateFab()
                }
            }, {
                if (isThere()) {
                    hideLoading()
                }
            })
        }
    }

    private fun updateFab() {
        if (adapter.unreadMap.values.any { it }) {
            fab?.show()
        } else {
            fab?.hide()
        }
    }

    private fun updateAdapterAndHideLoading(items: List<Item>) {
        searchMenuItem?.collapseActionView()
        adapter.query = ""
        adapter.updateItems(items)
        swiperefresh?.isRefreshing = false
    }

    private fun hideLoading() {
        swiperefresh?.isRefreshing = false
    }

    private fun showLoading() {
        swiperefresh?.isRefreshing = true
    }

    private fun calculateNumberOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        var columns = (dpWidth / 300).toInt()
        if (columns < 1) {
            columns = 1
        }
        return columns
    }

    companion object {
        const val KEY_ID = "KEY_ID"
        const val KEY_TYPE = "KEY_TYPE"
        const val KEY_TITLE = "KEY_TITLE"
    }
}
