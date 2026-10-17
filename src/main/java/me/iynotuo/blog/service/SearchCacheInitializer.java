package me.iynotuo.blog.service;

import me.iynotuo.blog.dao.PostDao;
import me.iynotuo.blog.dao.SearchCacheDao;
import me.iynotuo.blog.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 启动时若带 --force-cache 参数，则强制为每篇文章重建 raw_text 与 bigram 缓存
 */
@Component
public class SearchCacheInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchCacheInitializer.class);
    private static final String FORCE_CACHE = "--force-cache";

    private final PostDao postDao;
    private final SearchCacheDao searchCacheDao;
    private final MarkdownTextExtractor extractor;

    public SearchCacheInitializer(PostDao postDao, SearchCacheDao searchCacheDao, MarkdownTextExtractor extractor) {
        this.postDao = postDao;
        this.searchCacheDao = searchCacheDao;
        this.extractor = extractor;
    }

    @Override
    public void run(String... args) {
        if (!Arrays.asList(args).contains(FORCE_CACHE)) {
            return;
        }
        log.info("开始重建全文搜索缓存...");

        long _t0 = System.currentTimeMillis();

        searchCacheDao.clear();
        List<Post> posts = postDao.findAll();
        for (Post post : posts) {
            String rawText = extractor.toPlainText(post.getContent());
            postDao.updateRawText(post.getId(), rawText);
            searchCacheDao.rebuild(post.getId(), rawText);
        }

        long _t1 = System.currentTimeMillis();

        log.info("全文搜索缓存重建完成, 共 {} 篇文章. 用时: {}ms", posts.size(), _t1 - _t0);
    }
}
