package com.vardiya.notlar.gemini

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardiya.notlar.ui.theme.NotlarTheme

/**
 * Front door of the Gemini chat exporter: a launcher entry and a target in the share sheet.
 * A link shared from Gemini is fetched at once; nothing else in the intent is used.
 */
class GeminiExportActivity : ComponentActivity() {

    private val viewModel: ParserViewModel by viewModels()

    private val pickJson = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val success = viewModel.state.value as? ParserUiState.Success ?: return@registerForActivityResult
        if (uri != null) {
            val ok = GeminiExporter.write(this, uri, GeminiChat.toJson(success.turns, success.source))
            toast(if (ok) "JSON kaydedildi" else "Dosya yazılamadı")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A rotation recreates the activity with the same intent; the share is handled only once.
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            NotlarTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val link by viewModel.link.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    link = link,
                    actions = HomeActions(
                        onLinkChange = viewModel::onLinkChange,
                        onFetch = viewModel::fetch,
                        onCancel = viewModel::cancel,
                        onReset = viewModel::reset,
                        onCopyMarkdown = { withResult { copyMarkdown(it) } },
                        onShareMarkdown = { withResult { shareMarkdown(it) } },
                        onSaveJson = { pickJson.launch(GeminiExporter.JSON_NAME) },
                    ),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    override fun onDestroy() {
        // The chat text must not stay in the cache once the user is done.
        if (isFinishing) GeminiExporter.clear(this)
        super.onDestroy()
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::onSharedText)
    }

    private fun withResult(block: (ParserUiState.Success) -> Unit) {
        val result = viewModel.state.value as? ParserUiState.Success
        if (result == null) toast("Önce bir sohbet çekin") else block(result)
    }

    private fun copyMarkdown(result: ParserUiState.Success) {
        GeminiExporter.copy(this, GeminiChat.toMarkdown(result.turns, result.source))
        toast("Markdown panoya kopyalandı")
    }

    private fun shareMarkdown(result: ParserUiState.Success) {
        val text = GeminiChat.toMarkdown(result.turns, result.source)
        startActivity(GeminiExporter.shareIntent(this, text, GeminiExporter.MARKDOWN_NAME, "text/markdown"))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
