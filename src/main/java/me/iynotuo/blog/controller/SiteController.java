package me.iynotuo.blog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点级配置接口，备案信息通过启动参数注入，未配置时对应字段为空。
 */
@RestController
@RequestMapping("/api/site")
public class SiteController {

    private final String icp;
    private final String icpUrl;
    private final String gongan;
    private final String gonganUrl;
    private final String avatar;
    private final String highlightJs;
    private final String highlightCss;
    private final String katexJs;
    private final String katexCss;

    public SiteController(
            @Value("${blog.beian.icp:}") String icp,
            @Value("${blog.beian.icp-url:}") String icpUrl,
            @Value("${blog.beian.gongan:}") String gongan,
            @Value("${blog.beian.gongan-url:}") String gonganUrl,
            @Value("${blog.about.avatar:}") String avatar,
            @Value("${blog.cdn.highlight-js:}") String highlightJs,
            @Value("${blog.cdn.highlight-css:}") String highlightCss,
            @Value("${blog.cdn.katex-js:}") String katexJs,
            @Value("${blog.cdn.katex-css:}") String katexCss) {
        this.icp = icp;
        this.icpUrl = icpUrl;
        this.gongan = gongan;
        this.gonganUrl = gonganUrl;
        this.avatar = avatar;
        this.highlightJs = highlightJs;
        this.highlightCss = highlightCss;
        this.katexJs = katexJs;
        this.katexCss = katexCss;
    }

    @GetMapping("/beian")
    public BeianInfo beian() {
        return new BeianInfo(icp, icpUrl, gongan, gonganUrl);
    }

    @GetMapping("/about")
    public AboutInfo about() {
        return new AboutInfo(avatar);
    }

    @GetMapping("/cdn")
    public CdnInfo cdn() {
        return new CdnInfo(highlightJs, highlightCss, katexJs, katexCss);
    }

    public record BeianInfo(String icp, String icpUrl, String gongan, String gonganUrl) {}

    public record AboutInfo(String avatar) {}

    public record CdnInfo(String highlightJs, String highlightCss, String katexJs, String katexCss) {}
}
