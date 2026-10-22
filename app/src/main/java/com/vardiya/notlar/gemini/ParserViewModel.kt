package com.vardiya.notlar.gemini

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ParserUiState {
    data object Idle : ParserUiState
    data class Loading(val attempt: Int, val maxAttempts: Int) : ParserUiState
    data class Success(val source: String, val turns: List<ChatTurn>) : ParserUiState
    data class Error(val reason: ParseFailure) : ParserUiState
}

/** Holds the link field and the state of one scrape; the WebView work itself lives in [GeminiParserEngine]. */
class ParserViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = GeminiParserEngine(application)
    private var job: Job? = null

    private val _link = MutableStateFlow("")
    val link: StateFlow<String> = _link.asStateFlow()

    private val _state = MutableStateFlow<ParserUiState>(ParserUiState.Idle)
    val state: StateFlow<ParserUiState> = _state.asStateFlow()

    fun onLinkChange(text: String) {
        _link.value = text.take(MAX_INPUT)
        // Typing after a bad link clears the complaint.
        if (_state.value is ParserUiState.Error) _state.value = ParserUiState.Idle
    }

    /** Text shared from another app: the link in it is filled in and fetched at once. */
    fun onSharedText(text: String) {
        val found = GeminiChat.linkIn(text)
        if (found == null) {
            _link.value = text.take(MAX_INPUT)
            _state.value = ParserUiState.Error(ParseFailure.InvalidLink)
        } else {
            _link.value = found
            fetch()
        }
    }

    fun fetch() {
        val url = GeminiChat.validLink(_link.value)
        if (url == null) {
            _state.value = ParserUiState.Error(ParseFailure.InvalidLink)
            return
        }
        job?.cancel()
        _state.value = ParserUiState.Loading(1, GeminiParserEngine.MAX_ATTEMPTS)
        job = viewModelScope.launch {
            val result = engine.parse(url) { attempt ->
                _state.update { if (it is ParserUiState.Loading) it.copy(attempt = attempt) else it }
            }
            _state.value = when (result) {
                is ParseResult.Success -> ParserUiState.Success(url, result.turns)
                is ParseResult.Failure -> ParserUiState.Error(result.reason)
            }
        }
    }

    /** Stops a scrape in progress; cancelling the job destroys its WebView. */
    fun cancel() {
        job?.cancel()
        _state.value = ParserUiState.Idle
    }

    /** Back to an empty field. */
    fun reset() {
        job?.cancel()
        _link.value = ""
        _state.value = ParserUiState.Idle
    }

    private companion object {
        const val MAX_INPUT = 2_000
    }
}
