package com.example.angularprimer

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/* -------------------------------------------------------------------------- */
/*                                   ACTIVITY                                 */
/* -------------------------------------------------------------------------- */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AngularPrimerTheme {
                MarkdownReaderApp()
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                APP ENTRY                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun MarkdownReaderApp() {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<BookPage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        pages = withContext(Dispatchers.IO) {
            loadMarkdownPagesFromAssets(context, "primer")
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ReaderScaffold(pages)
    }
}

/* -------------------------------------------------------------------------- */
/*                                  READER                                    */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScaffold(pages: List<BookPage>) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(pages[pagerState.currentPage].title) }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage - 1).coerceAtLeast(0)
                            )
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1)
                                    .coerceAtMost(pages.lastIndex)
                            )
                        }
                    },
                    enabled = pagerState.currentPage < pages.lastIndex
                ) {
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) { index ->
            ReaderPage(pages[index])
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                   PAGE                                     */
/* -------------------------------------------------------------------------- */

@Composable
fun ReaderPage(page: BookPage) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        page.blocks.forEach {
            when (it) {
                is Block.Subheading -> {
                    when (it.level) {
                        2 -> Text(
                            text = it.text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.onSurface
                        )

                        3 -> Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.primary
                        )

                        else -> Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface
                        )
                    }
                }

                is Block.Paragraph ->
                    MarkdownText(it.text, MaterialTheme.typography.bodyLarge)

                is Block.BulletList ->
                    it.items.forEach { item ->
                        Row {
                            Text(
                                "• ",
                                color = colors.onSurface
                            )
                            MarkdownText(item, MaterialTheme.typography.bodyLarge)
                        }
                    }

                is Block.CodeBlock ->
                    CodeBlock(it)

                is Block.TableBlock -> {}
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                           CODE PALETTES                                    */
/* -------------------------------------------------------------------------- */

data class CodePalette(
    val base: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val tag: Color
)

val DarkCodePalette = CodePalette(
    base = Color(0xFFE0E0E0),
    keyword = Color(0xFF82AAFF),
    string = Color(0xFFC3E88D),
    number = Color(0xFFF78C6C),
    comment = Color(0xFF546E7A),
    type = Color(0xFF4EC9B0),
    tag = Color(0xFF569CD6)
)

val LightCodePalette = CodePalette(
    base = Color(0xFF1E1E1E),
    keyword = Color(0xFF0000FF),
    string = Color(0xFF008000),
    number = Color(0xFFB00020),
    comment = Color(0xFF6A737D),
    type = Color(0xFF267F99),
    tag = Color(0xFF800000)
)

/* -------------------------------------------------------------------------- */
/*                           REGEX SYNTAX HIGHLIGHTING                         */
/* -------------------------------------------------------------------------- */

fun highlightCodeRegex(
    code: String,
    language: String?,
    isDark: Boolean
): AnnotatedString {

    val palette = if (isDark) DarkCodePalette else LightCodePalette
    val builder = AnnotatedString.Builder(code)

    // Base color
    builder.addStyle(
        SpanStyle(color = palette.base),
        0,
        code.length
    )

    fun apply(regex: Regex, color: Color) {
        regex.findAll(code).forEach {
            builder.addStyle(
                SpanStyle(color = color),
                it.range.first,
                it.range.last + 1
            )
        }
    }

    val lang = language?.lowercase()

    // Shared patterns
    val stringRegex = Regex("""(['"`])(?:\\.|(?!\1).)*\1""")
    val numberRegex = Regex("""\b\d+(\.\d+)?\b""")
    val typeRegex = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")

    // CSV rainbow palette
    val csvPalette = listOf(
        Color(0xFFEF5350),
        Color(0xFFAB47BC),
        Color(0xFF5C6BC0),
        Color(0xFF29B6F6),
        Color(0xFF26A69A),
        Color(0xFF66BB6A),
        Color(0xFFFFCA28),
        Color(0xFFFF7043)
    )

    when (lang) {

        /* ---------------- JS / TS ---------------- */
        "js", "javascript", "ts", "typescript" -> {
            apply(
                Regex("""\b(class|interface|enum|function|return|const|let|var|if|else|for|while|new|this|extends|import|from|export|async|await|switch|case|break|continue)\b"""),
                palette.keyword
            )
            apply(Regex("""//.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""/\*[\s\S]*?\*/"""), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
            apply(typeRegex, palette.type)
        }

        /* ---------------- BASH / SHELL ---------------- */
        "bash", "sh", "shell" -> {
            apply(
                Regex("""\b(if|then|else|elif|fi|for|while|do|done|case|esac|in|function|select|until|break|continue|return|export|local|readonly)\b"""),
                palette.keyword
            )
            apply(Regex("""#.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(stringRegex, palette.string)
            apply(Regex("""\$\{?[A-Za-z_][A-Za-z0-9_]*\}?"""), palette.type)
        }

        /* ---------------- PHP ---------------- */
        "php" -> {
            apply(
                Regex("""\b(class|function|public|private|protected|static|return|if|else|elseif|foreach|for|while|new|try|catch|throw|namespace|use)\b"""),
                palette.keyword
            )
            apply(Regex("""//.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""#.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""/\*[\s\S]*?\*/"""), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
            apply(Regex("""\$[A-Za-z_][A-Za-z0-9_]*"""), palette.type)
        }

        /* ---------------- PYTHON ---------------- */
        "py", "python" -> {
            apply(
                Regex("""\b(def|class|return|if|elif|else|for|while|break|continue|import|from|as|try|except|finally|with|lambda|yield|pass|raise)\b"""),
                palette.keyword
            )
            apply(Regex("""#.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
            apply(typeRegex, palette.type)
        }

        /* ---------------- JAVA ---------------- */
        "java" -> {
            apply(
                Regex("""\b(class|interface|enum|public|private|protected|static|final|void|int|long|double|boolean|new|return|if|else|for|while|switch|case|break|continue|try|catch|throw|package|import)\b"""),
                palette.keyword
            )
            apply(Regex("""//.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""/\*[\s\S]*?\*/"""), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
            apply(typeRegex, palette.type)
        }

        /* ---------------- C# ---------------- */
        "c#", "csharp" -> {
            apply(
                Regex("""\b(class|interface|enum|public|private|protected|static|readonly|void|int|string|bool|new|return|if|else|for|foreach|while|switch|case|break|continue|try|catch|throw|using|namespace)\b"""),
                palette.keyword
            )
            apply(Regex("""//.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""/\*[\s\S]*?\*/"""), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
            apply(typeRegex, palette.type)
        }

        /* ---------------- SQL / PL/SQL ---------------- */
        "sql", "plsql", "psql" -> {
            apply(
                Regex(
                    """\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|BY|ORDER|HAVING|AS|DISTINCT|CREATE|ALTER|DROP|TABLE|VIEW|FUNCTION|PROCEDURE|BEGIN|END|DECLARE|IF|ELSE|ELSIF|LOOP|FOR|WHILE|RETURN)\b""",
                    RegexOption.IGNORE_CASE
                ),
                palette.keyword
            )
            apply(Regex("""--.*?$""", RegexOption.MULTILINE), palette.comment)
            apply(Regex("""/\*[\s\S]*?\*/"""), palette.comment)
            apply(stringRegex, palette.string)
            apply(numberRegex, palette.number)
        }

        /* ---------------- XML ---------------- */
        "xml" -> {
            apply(Regex("""</?[A-Za-z_:][A-Za-z0-9_.:-]*"""), palette.tag)
            apply(Regex("""\s+[A-Za-z_:][A-Za-z0-9_.:-]*(?=\s*=)"""), palette.keyword)
            apply(Regex("""=\s*"[^"]*"|=\s*'[^']*'"""), palette.string)
            apply(Regex("""<!--[\s\S]*?-->"""), palette.comment)
        }

        /* ---------------- JSON ---------------- */
        "json" -> {
            // Keys
            apply(
                Regex(""""(\\.|[^"])*"(?=\s*:)"""),
                palette.keyword
            )

            // String values
            Regex(""":\s*"([^"]*)"""").findAll(code).forEach {
                val start = it.range.first + it.value.indexOf('"')
                val end = it.range.last + 1
                builder.addStyle(
                    SpanStyle(color = palette.string),
                    start,
                    end
                )
            }

            apply(numberRegex, palette.number)
            apply(Regex("""\b(true|false|null)\b"""), palette.keyword)
        }

        /* ---------------- YAML ---------------- */
        "yaml", "yml" -> {
            // Keys
            apply(
                Regex("""^[\s\-]*[A-Za-z0-9_-]+(?=:)""", RegexOption.MULTILINE),
                palette.keyword
            )

            // String values
            Regex(""":\s*(".*?"|'.*?')""").findAll(code).forEach {
                val value = it.groupValues[1]
                val start = it.range.first + it.value.indexOf(value)
                val end = start + value.length
                builder.addStyle(
                    SpanStyle(color = palette.string),
                    start,
                    end
                )
            }

            apply(numberRegex, palette.number)
            apply(Regex("""\b(true|false|null)\b"""), palette.keyword)
            apply(Regex("""#.*?$""", RegexOption.MULTILINE), palette.comment)
        }

        /* ---------------- CSV (RAINBOW) ---------------- */
        "csv" -> {
            var index = 0
            var cellStart = 0
            var colorIndex = 0

            while (index <= code.length) {
                val isEnd = index == code.length
                val isComma = !isEnd && code[index] == ','

                if (isComma || isEnd) {
                    val color = csvPalette[colorIndex % csvPalette.size]

                    builder.addStyle(
                        SpanStyle(color = color),
                        cellStart,
                        index
                    )

                    if (isComma) {
                        builder.addStyle(
                            SpanStyle(color = palette.comment),
                            index,
                            index + 1
                        )
                    }

                    cellStart = index + 1
                    colorIndex++
                }
                index++
            }
        }
    }

    return builder.toAnnotatedString()
}

/* -------------------------------------------------------------------------- */
/*                                 CODE BLOCK                                 */
/* -------------------------------------------------------------------------- */

@Composable
fun CodeBlock(block: Block.CodeBlock) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val highlighted = remember(block.code, block.language, isDark) {
        highlightCodeRegex(block.code, block.language, isDark)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f))
    ) {
        Text(
            text = highlighted,
            modifier = Modifier.padding(16.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = colors.onSurfaceVariant
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                              MARKDOWN TEXT                                 */
/* -------------------------------------------------------------------------- */

@Composable
fun MarkdownText(text: String, style: TextStyle) {
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val codeBg = colors.surfaceVariant

    val parsed = remember(text, codeBg) {
        parseInlineMarkdown(text, codeBg)
    }

    ClickableText(
        text = parsed,
        style = style.copy(color = colors.onSurface),
        onClick = { offset ->
            parsed.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}

/* -------------------------------------------------------------------------- */
/*                          INLINE MARKDOWN PARSER                             */
/* -------------------------------------------------------------------------- */

fun parseInlineMarkdown(
    text: String,
    codeBackground: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0

    val isDark = codeBackground.luminance() < 0.5f
    val boldAccent = if (isDark)
        Color(0xFFFF79C6) // neon magenta
    else
        Color(0xFFB4005A) // rich raspberry

    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    builder.withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = boldAccent
                        )
                    ) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            text.startsWith("[", i) -> {
                val close = text.indexOf("]", i)
                val openParen = text.indexOf("(", close)
                val closeParen = text.indexOf(")", openParen)
                if (close != -1 && openParen == close + 1 && closeParen != -1) {
                    val label = text.substring(i + 1, close)
                    val url = text.substring(openParen + 1, closeParen)
                    val start = builder.length
                    builder.append(label)
                    builder.addStyle(
                        SpanStyle(
                            color = Color(0xFF2962FF),
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        builder.length
                    )
                    builder.addStringAnnotation("URL", url, start, builder.length)
                    i = closeParen + 1
                    continue
                }
            }
        }

        builder.append(text[i])
        i++
    }

    return builder.toAnnotatedString()
}

/* -------------------------------------------------------------------------- */
/*                              DATA MODELS                                   */
/* -------------------------------------------------------------------------- */

data class BookPage(val title: String, val blocks: List<Block>)

sealed class Block {
    data class Paragraph(val text: String) : Block()
    data class Subheading(val text: String, val level: Int) : Block()
    data class BulletList(val items: List<String>) : Block()
    data class CodeBlock(val code: String, val language: String?) : Block()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : Block()
}

/* -------------------------------------------------------------------------- */
/*                           MARKDOWN PARSER                                   */
/* -------------------------------------------------------------------------- */

fun loadMarkdownPagesFromAssets(context: Context, dir: String): List<BookPage> {
    val files = context.assets.list(dir) ?: emptyArray()

    return files
        .filter { it.endsWith(".md") }
        .sorted()
        .map { file ->
            val content = context.assets.open("$dir/$file").use {
                BufferedReader(InputStreamReader(it)).readText()
            }
            parseMarkdownFile(file, content)
        }
}

fun parseMarkdownFile(fileName: String, markdown: String): BookPage {
    val lines = markdown.lines()
    val title = lines.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")
        ?: fileName.removeSuffix(".md")

    val blocks = parseMarkdownBlocks(lines.drop(1))
    return BookPage(title, blocks)
}

fun parseMarkdownBlocks(lines: List<String>): List<Block> {
    val blocks = mutableListOf<Block>()
    val buffer = mutableListOf<String>()
    var inCode = false
    var language: String? = null

    fun flushParagraph() {
        if (buffer.isNotEmpty()) {
            blocks.add(Block.Paragraph(buffer.joinToString("\n")))
            buffer.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        when {
            trimmed.startsWith("```") -> {
                if (inCode) {
                    blocks.add(Block.CodeBlock(buffer.joinToString("\n"), language))
                    buffer.clear()
                    inCode = false
                } else {
                    flushParagraph()
                    language = trimmed.removePrefix("```").trim().ifEmpty { null }
                    inCode = true
                }
            }

            inCode -> buffer.add(line)

            trimmed.startsWith("### ") ->
                blocks.add(Block.Subheading(trimmed.removePrefix("### ").trim(), 3))

            trimmed.startsWith("## ") ->
                blocks.add(Block.Subheading(trimmed.removePrefix("## ").trim(), 2))

            trimmed.startsWith("- ") -> {
                flushParagraph()
                blocks.add(Block.BulletList(listOf(trimmed.removePrefix("- "))))
            }

            trimmed.isBlank() ->
                flushParagraph()

            else ->
                buffer.add(line)
        }
    }

    flushParagraph()
    return blocks
}

/* -------------------------------------------------------------------------- */
/*                                   THEME                                    */
/* -------------------------------------------------------------------------- */

@Composable
fun AngularPrimerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()

    val colorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            if (dark) darkColorScheme()
            else lightColorScheme()
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}