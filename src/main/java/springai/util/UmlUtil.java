package springai.util;

import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.ai.chat.client.ChatClient;

import java.io.ByteArrayOutputStream;

/**
 * UML工具类
 */
public class UmlUtil {


    private final static String plantUmlUrl = "@startuml\r\n" + //
                "actor 用户\r\n" + //
                "participant \"前端页面\" as Web\r\n" + //
                "participant \"后端服务\" as API\r\n" + //
                "database 数据库\r\n" + //
                "\r\n" + //
                "用户 -> Web : 点击登录按钮\r\n" + //
                "activate Web\r\n" + //
                "Web -> API : POST /api/login\r\n" + //
                "activate API\r\n" + //
                "API -> 数据库 : 查询用户信息\r\n" + //
                "activate 数据库\r\n" + //
                "数据库 --> API : 返回用户信息\r\n" + //
                "deactivate 数据库\r\n" + //
                "API --> Web : 返回 token\r\n" + //
                "deactivate API\r\n" + //
                "Web --> 用户 : 跳转首页\r\n" + //
                "deactivate Web\r\n" + //
                "@enduml";

    /**
     * 构造plantuml流图
     * @param chatClient ChatClient实例
     * @param text 输入文本
     * @return PlantUML代码
     */
    public static String umlwithimage(ChatClient chatClient, String text) {
        return chatClient.prompt("请根据以下内容生成plantuml代码，只返回代码不要其他说明，注意文字使用中文行楷,参考模板："+plantUmlUrl)
                .user(text).call().content();
    }

    /**
     * 生成PlantUML图片
     * @param plantUmlCode PlantUML代码
     * @return 图片字节数组
     */
    public static byte[] generatePlantUmlImage(String plantUmlCode) {
        try {
            // 清理PlantUML代码，移除可能的markdown标记
            String cleanCode = plantUmlCode.replaceAll("```plantuml", "")
                                          .replaceAll("```", "")
                                          .trim();

            System.out.println("清理后的PlantUML代码：" + cleanCode);

            // 使用PlantUML生成图片
            SourceStringReader reader = new SourceStringReader(cleanCode);

            ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

            // 生成PNG格式图片
            net.sourceforge.plantuml.FileFormatOption formatOption =
                new net.sourceforge.plantuml.FileFormatOption(net.sourceforge.plantuml.FileFormat.PNG);

            reader.outputImage(imageStream, formatOption);

            return imageStream.toByteArray();

        } catch (Exception e) {
            System.err.println("生成PlantUML图片失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}