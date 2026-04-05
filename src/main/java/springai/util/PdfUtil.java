package springai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF工具类
 */
public class PdfUtil {

    /**
     * 方法1：按页读取PDF文件并转为字符串列表
     * @param pdfFile PDF文件
     * @return 按页分割的字符串列表，图片会被跳过
     */
    public static ArrayList<String> readPdfByPage(File pdfFile) {
        ArrayList<String> pageTexts = new ArrayList<>();
        PDDocument document = null;

        try {
            document = PDDocument.load(pdfFile);
            int totalPages = document.getNumberOfPages();

            for (int i = 0; i < totalPages; i++) {
                // 创建一个PDFTextStripper来处理每一页
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);

                // 设置编码为UTF-8以正确处理中文
                stripper.setSortByPosition(true);

                String pageText = stripper.getText(document);
                pageTexts.add(pageText);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取PDF文件失败: " + e.getMessage(), e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }

        return pageTexts;
    }

    /**
     * 方法2：将图片列表生成PDF文件
     * @param images 图片数据列表（支持BufferedImage、byte[]、File等类型）
     * @param outputFile 输出的PDF文件
     */
    public static void createPdfFromImages(List<?> images, File outputFile) {
        try (PDDocument document = new PDDocument()) {

            for (Object imageObj : images) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                PDImageXObject pdImage;

                if (imageObj instanceof byte[]) {
                    // 处理byte[]类型
                    pdImage = PDImageXObject.createFromByteArray(document, (byte[]) imageObj, "image");
                } else if (imageObj instanceof File) {
                    // 处理File类型
                    pdImage = PDImageXObject.createFromFileByExtension((File) imageObj, document);
                } else {
                    throw new IllegalArgumentException("不支持的图片类型: " + imageObj.getClass().getName());
                }

                // 获取页面尺寸
                PDRectangle pageSize = page.getMediaBox();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                // 计算图片缩放比例，使其适应页面
                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();

                float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;

                // 居中显示图片
                float x = (pageWidth - scaledWidth) / 2;
                float y = (pageHeight - scaledHeight) / 2;

                // 将图片添加到页面
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
                }
            }

            // 保存PDF文件
            document.save(outputFile);

        } catch (IOException e) {
            throw new RuntimeException("生成PDF文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法2重载：将图片列表生成PDF文件并返回byte[]
     * @param images 图片数据列表
     * @return PDF文件的字节数组
     */
    public static byte[] createPdfFromImagesToBytes(List<?> images) {
        try (PDDocument document = new PDDocument()) {

            for (Object imageObj : images) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                PDImageXObject pdImage;

                if (imageObj instanceof byte[]) {
                    // 处理byte[]类型
                    pdImage = PDImageXObject.createFromByteArray(document, (byte[]) imageObj, "image");
                } else if (imageObj instanceof File) {
                    // 处理File类型
                    pdImage = PDImageXObject.createFromFileByExtension((File) imageObj, document);
                } else {
                    throw new IllegalArgumentException("不支持的图片类型: " + imageObj.getClass().getName());
                }

                // 获取页面尺寸
                PDRectangle pageSize = page.getMediaBox();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                // 计算图片缩放比例，使其适应页面
                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();

                float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;

                // 居中显示图片
                float x = (pageWidth - scaledWidth) / 2;
                float y = (pageHeight - scaledHeight) / 2;

                // 将图片添加到页面
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
                }
            }

            // 将PDF转换为字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("生成PDF文件失败: " + e.getMessage(), e);
        }
    }
}