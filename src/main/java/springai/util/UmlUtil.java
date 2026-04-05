package springai.util;

import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.ai.chat.client.ChatClient;

import java.io.ByteArrayOutputStream;

/**
 * UML工具类
 */
public class UmlUtil {

    /**
     * 构造plantuml流图
     * @param chatClient ChatClient实例
     * @param text 输入文本
     * @return PlantUML代码
     */
    public static String umlwithimage(ChatClient chatClient, String text) {
        return chatClient.prompt("请根据以下内容生成plantuml代码，只返回代码不要其他说明，注意文字都使用中文：")
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