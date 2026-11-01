package com.mabsSD.toolbox.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mabsSD.toolbox.ToolboxApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * History for the Home and Files screens, held by each screen's back-stack
 * entry.
 *
 * The screens used to collect the Room flow directly with an empty-list
 * initial value. Every return from a tool recomposed them from scratch: one
 * frame of the "no files yet" empty state, a fresh query, then the list
 * snapping in with every item running its appear animation — the hitch felt
 * on going back to Home. A StateFlow here keeps the last list while the user
 * is in a tool, so coming back shows it immediately.
 *
 * Values are null until the first query returns, so screens can show nothing
 * rather than briefly claiming there are no files.
 */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as ToolboxApplication).container.historyDb.historyDao()

    val recent: StateFlow<List<HistoryEntry>?> =
        dao.observeRecent(5).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val all: StateFlow<List<HistoryEntry>?> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
