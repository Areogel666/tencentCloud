package cn.lxr.example.tencentcloudapi.common.strategy;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.bo.UploadBO;
import cn.lxr.example.tencentcloudapi.proxy.FileProxy;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public abstract class TransferOperator {
    public static String DOWNLOAD = "download/";
    FileProxy fileProxy = new FileProxy();

    public abstract void run(TransferManager transferManager, MediaBO media, String output);
    public abstract COSObjectInputStream runByStream(TransferManager transferManager, MediaBO media, UploadBO uploadPath);

    /**
     * 执行转换操作
     *
     * @param transferManager
     * @param media
     * @param upload 转换操作后文件的上传路径（值为null时默认覆盖源文件）
     * @param localDownload 判断是否下载临时文件到本地
     */
    public void execute(TransferManager transferManager, MediaBO media, UploadBO upload, boolean localDownload){
        // run方法会把临时文件下载到本地操作，而runByStream则只会保存inputStream
        if (localDownload) {
            run(transferManager, media, null);
            fileProxy.upload(transferManager, media, media.getKey());
        } else {
            COSObjectInputStream cosObjectInputStream = runByStream(transferManager, media, upload);
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(cosObjectInputStream);
                InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(bytes));
                if (upload != null && upload.getUploadPath() != null) {
                    // 上传临时文件
                    media.setKey(upload.getUploadPath());
                }
                if (bytes != null) {
                    fileProxy.upload(transferManager, media, inputStream, bytes.length);
                } else {
                    fileProxy.upload(transferManager, media, inputStream, -1);
                }
            } catch (IOException e) {
                log.error("TransferOperator error", e);
            } finally {
                try {
                    cosObjectInputStream.close();
                } catch (IOException e) {
                    log.error("TransferOperator close cosObjectInput error", e);
                }
            }
        }
    }

    /**
     * 执行转换操作并下载
     *
     * @param transferManager
     * @param media
     * @param output
     */
    public void runAndDown(TransferManager transferManager, MediaBO media, String output){
        if (output == null) {
            run(transferManager, media, null);
        } else {
            run(transferManager, media, output);
        }
    }
}
