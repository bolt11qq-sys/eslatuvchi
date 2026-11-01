package uz.tanish.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tanish.BuildConfig
import uz.tanish.R
import uz.tanish.log.ErrorLog

private fun loadJournal(fallback: String): String = try {
    ErrorLog.read()
} catch (e: Exception) {
    ErrorLog.error("Jurnal", "Jurnalni o'qishda xato", e)
    fallback
}

/** Qarovchi uchun xatolar jurnali: ko'rish, nusxa olish, tozalash. */
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val readError = stringResource(R.string.journal_read_error)
    val copiedMsg = stringResource(R.string.journal_copied)
    val clearedMsg = stringResource(R.string.journal_cleared)

    var text by remember { mutableStateOf(loadJournal(readError)) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.headlineLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                },
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
            ) { Text(stringResource(R.string.journal_copy)) }

            OutlinedButton(
                onClick = {
                    try {
                        ErrorLog.clear()
                    } catch (e: Exception) {
                        ErrorLog.error("Jurnal", "Jurnalni tozalashda xato", e)
                    }
                    text = loadJournal(readError)
                    Toast.makeText(context, clearedMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
            ) { Text(stringResource(R.string.journal_clear)) }
        }

        if (BuildConfig.DEBUG) {
            // Faqat debug yig'ilmada: jurnal ishlashini telefonda sinash uchun.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        ErrorLog.error("Sinov", "Bu sinov yozuvi", IllegalStateException("Sinov istisnosi"))
                        text = loadJournal(readError)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp),
                ) { Text(stringResource(R.string.journal_test_entry)) }

                OutlinedButton(
                    onClick = { throw RuntimeException("Sinov: ataylab yopilish") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp),
                ) { Text(stringResource(R.string.journal_test_crash)) }
            }
        }

        SelectionContainer(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = text.ifBlank { stringResource(R.string.journal_empty) },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
        ) { Text(stringResource(R.string.back)) }
    }
}
