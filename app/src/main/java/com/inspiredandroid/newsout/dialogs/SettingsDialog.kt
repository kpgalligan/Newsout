package com.inspiredandroid.newsout.dialogs

import android.accounts.AccountManager
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.inspiredandroid.newsout.Database
import com.inspiredandroid.newsout.R
import com.inspiredandroid.newsout.activities.LoginActivity
import com.inspiredandroid.newsout.callbacks.OnSortingChangeInterface
import com.inspiredandroid.newsout.toBoolean
import com.inspiredandroid.newsout.toLong
import io.ktor.util.InternalAPI
import kotlinx.android.synthetic.main.dialog_settings.view.*

/*
 * Copyright 2019 Simon Schubert Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
class SettingsDialog : DialogFragment(), AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    var spinner: Spinner? = null
    private var callback: OnSortingChangeInterface? = null
    private val spinnerArray = ArrayList<Pair<String, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callback = activity as? OnSortingChangeInterface
    }

    @InternalAPI
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_settings, null)

        val user = Database.getUser()
        spinner = view.sortingSp
        view.folderSw.isChecked = user?.isFolderTop?.toBoolean() ?: false
        view.folderSw.setOnCheckedChangeListener(this)

        view.logoutBtn.setOnClickListener {
            val accountManager = AccountManager.get(context)
            accountManager.getAccountsByType("com.inspiredandroid.newsout").forEach {
                accountManager.removeAccountExplicitly(it)
            }

            Database.clear()

            startActivity(Intent(context, LoginActivity::class.java))
            activity?.finish()
        }

        spinnerArray.add(Pair("Unread count", Database.SORT_UNREADCOUNT))
        spinnerArray.add(Pair("Alphabetically", Database.SORT_TITLE))

        val adapter = ArrayAdapter<String>(
            requireContext(), R.layout.dialog_settings_spinner_item, spinnerArray.map { it.first }
        )
        adapter.setDropDownViewResource(R.layout.dialog_settings_spinner_dropdown_item)

        spinner?.adapter = adapter
        spinner?.onItemSelectedListener = this

        val currentSorting = user?.sorting ?: Database.SORT_UNREADCOUNT
        spinner?.setSelection(spinnerArray.indexOfFirst { it.second == currentSorting })

        return AlertDialog.Builder(requireContext())
            .setTitle("Settings")
            .setView(view)
            .create()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        spinnerArray.getOrNull(position)?.let {
            Database.getUserQueries()?.updateSorting(it.second)
            callback?.onSortingChange()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        Database.getUserQueries()?.updateFolderTop(isChecked.toLong())
        callback?.onSortingChange()
    }

    companion object {

        internal fun getInstance(): SettingsDialog {
            return SettingsDialog()
        }
    }
}