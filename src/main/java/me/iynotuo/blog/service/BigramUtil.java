package me.iynotuo.blog.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * bigram 工具：把文本切成相邻两字符（bigram），用于全文搜索的倒排索引。
 * 统一使用小写进行匹配，跳过包含空白字符的 bigram。
 */
public final class BigramUtil {

    private BigramUtil() {
    }

    /**
     * 建立文本的 bigram 倒排索引：bigram -> 其在文本中出现的位置列表（0 基下标）。
     */
    public static Map<String, List<Integer>> index(String text) {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        if (text == null || text.length() < 2) {
            return map;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 1 < lower.length(); i++) {
            if (Character.isWhitespace(lower.charAt(i)) || Character.isWhitespace(lower.charAt(i + 1))) {
                continue;
            }
            String bigram = lower.substring(i, i + 2);
            map.computeIfAbsent(bigram, k -> new ArrayList<>()).add(i);
        }
        return map;
    }

    /**
     * 生成查询串的去重 bigram 列表（小写、跳过空白）。
     */
    public static List<String> bigrams(String text) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (text == null) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 1 < lower.length(); i++) {
            if (Character.isWhitespace(lower.charAt(i)) || Character.isWhitespace(lower.charAt(i + 1))) {
                continue;
            }
            set.add(lower.substring(i, i + 2));
        }
        return new ArrayList<>(set);
    }
}
