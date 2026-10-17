package me.iynotuo.blog.service;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用 flexmark 将 Markdown 解析成可供全文搜索的纯文本：
 * 表格替换为 [表格]、代码块替换为 [代码块]、公式替换为 [公式]，其余部分只保留文字内容。
 */
@Component
public class MarkdownTextExtractor {

    private static final String TABLE = "[表格]";
    private static final String CODE_BLOCK = "[代码块]";
    private static final String FORMULA = "[公式]";

    private final Parser parser;

    public MarkdownTextExtractor() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    /**
     * 把 markdown 文本替换成纯文本。
     * */
    public String toPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String text = markdown;

        // flexmark 没有内建公式支持，迫不得已用正则了
        text = text.replaceAll("(?s)\\$\\$.*?\\$\\$", " " + FORMULA + " ");
        text = text.replaceAll("\\$[^$\\n]+\\$", " " + FORMULA + " ");

        Node document = parser.parse(text);
        StringBuilder sb = new StringBuilder();
        collect(document, sb);

        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * 从 raw_text 开头截取摘要，最多 {@code max} 个字符；
     * 若截断点落在 {@code [...]} 标记内部（如 [表格]、[代码块]），回退到标记起始处之前。
     */
    public static String summarize(String rawText, int max) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        String trimmed = rawText.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        String head = trimmed.substring(0, max);
        int open = head.lastIndexOf('[');
        if (open >= 0 && head.indexOf(']', open) < 0) {
            head = head.substring(0, open);
        }
        String result = head.trim();
        return result.isEmpty() ? null : result + "…";
    }

    private void collect(Node node, StringBuilder sb) {
        while (node != null) {
            if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
                append(sb, CODE_BLOCK);
            } else if (node instanceof TableBlock) {
                append(sb, TABLE);
            } else if (node instanceof Text) {
                sb.append(node.getChars());
            } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
                sb.append(' ');
            } else if (node instanceof HtmlInline || node instanceof HtmlBlock || node instanceof HtmlEntity) {
                sb.append(' ');
            } else {
                collect(node.getFirstChild(), sb);
                // 块级节点之间用空格分隔，避免跨段落文本粘连
                if (node instanceof Block) {
                    sb.append(' ');
                }
            }
            node = node.getNext();
        }
    }

    private void append(StringBuilder sb, String marker) {
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != ' ') {
            sb.append(' ');
        }
        sb.append(marker).append(' ');
    }
}
