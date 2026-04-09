package springai.util;

import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.ai.chat.client.ChatClient;

import java.io.ByteArrayOutputStream;

/**
 * UML工具类
 */
public class UmlUtil {

    private final static String plantUmlUrl = "@startuml\r\n" +
            "actor 用户\r\n" +
            "participant 系统\r\n" +
            "用户 -> 系统 : 请求\r\n" +
            "@enduml";

    /**
     * 将文本内容转换为PlantUML代码
     * 
     * @param chatClient ChatClient实例
     * @param text       输入文本
     * @return PlantUML代码
     */
    public static String convertTextToPlantUml(ChatClient chatClient, String text) {
        return chatClient
                .prompt("请根据以下内容生成plantuml时序图代码，要求：\n" +
                        "1. 只返回代码不要其他说明\n" +
                        "2. 使用简洁的中文描述\n" +
                        "3. 总体不超过300个字符\n" +
                        "4. 参考简单模板：" + plantUmlUrl)
                .user(text).call().content();
    }

    /**
     * 将PlantUML代码转换为图片字节数组
     * 
     * @param plantUmlCode PlantUML代码
     * @return 图片字节数组
     */
    public static byte[] convertPlantUmlToImage(String plantUmlCode) {
        try {
            // 清理PlantUML代码，移除可能的markdown标记
            String cleanCode = plantUmlCode.replaceAll("```plantuml", "")
                    .replaceAll("```", "")
                    .trim();

            // System.out.println("清理后的PlantUML代码：" + cleanCode);

            // 使用PlantUML生成图片
            SourceStringReader reader = new SourceStringReader(cleanCode);

            ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

            // 生成PNG格式图片
            net.sourceforge.plantuml.FileFormatOption formatOption = new net.sourceforge.plantuml.FileFormatOption(
                    net.sourceforge.plantuml.FileFormat.PNG);

            reader.outputImage(imageStream, formatOption);

            return imageStream.toByteArray();

        } catch (Exception e) {
            System.err.println("生成PlantUML图片失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}