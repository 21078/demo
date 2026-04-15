package springai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 视图控制器 - 处理页面重定向
 */
@Controller
public class ViewController {

    /**
     * 根路径重定向到index.html
     */
    @GetMapping("/")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }
}