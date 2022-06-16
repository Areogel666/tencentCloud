package cn.lxr.example.tencentcloudapi.common.strategy;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.bo.UploadBO;
import cn.lxr.example.tencentcloudapi.constant.StorageType;
import cn.lxr.example.tencentcloudapi.util.FileUtils;
import com.qcloud.cos.COS;
import com.qcloud.cos.transfer.TransferManager;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TransferChain {
    private List<TransferOperator> chain = new ArrayList<>();
    private COS cosClient;
    private TransferManager transferManager;
    private MediaBO media;

    public TransferChain(TransferManager transferManager, MediaBO media) {
        this.transferManager = transferManager;
        this.media = media;
        this.cosClient = transferManager.getCOSClient();
    }

    public TransferChain(COS cosClient, MediaBO media) {
        this.cosClient = cosClient;
        this.media = media;
    }

    /**
     * 依次执行操作，上一个处理结果文件作为下一个输入文件
     */
    public void execute() {
        execute(true, false, null);
    }

    /**
     * 依次执行操作，上一个处理结果文件作为下一个输入文件
     *
     * @param uploadPath 文件上传路径（默认覆盖源文件）
     */
    public void execute(String uploadPath) {
        execute(true, false, uploadPath);
    }

    /**
     * 依次执行操作，上一个处理结果文件作为下一个输入文件
     *
     * @param lastDownload 最后一步是否下载文件
     * @param tempUpload 临时文件是否在云端保存
     * @param uploadPath 文件上传路径（默认覆盖源文件）
     */
    public void execute(boolean lastDownload, boolean tempUpload, String uploadPath) {
        String output = media.getKey();
        uploadPath = StringUtils.defaultString(uploadPath, output);
        int num = 1;
        UploadBO upload = null;
        for (int i = 0; i < chain.size() - 1; i++) {
            TransferOperator operator = chain.get(i);
            if (tempUpload) {
                uploadPath = uploadPath + "(" + num++ + ")";
            }
            upload = new UploadBO(uploadPath, output);
            operator.execute(transferManager, media, upload, false);
        }
        if (lastDownload) {
            //最后一次下载的时候设置下output
            chain.get(chain.size() - 1).runAndDown(transferManager, media, upload.getDownPath());
        } else {
            chain.get(chain.size() - 1).execute(transferManager, media, new UploadBO(uploadPath + "(" + num + ")", output), false);
        }
    }

    /**
     * 对同一文件依次执行操作，输出文件名为{key}(x)
     */
    public void run() {
        int num = 1;
        for (TransferOperator operator: chain) {
            operator.runAndDown(transferManager, media, media.getKey() + "(" + num++ + ")");
        }
    }

    public void add(TransferOperator operator) {
        this.chain.add(operator);
    }

    public void addMore(TransferOperator operator, int more) {
        for (int i = 0; i < more; i++) {
            this.chain.add(operator);
        }
    }

    public void remove(TransferOperator operator) {
        this.chain.remove(operator);
    }

    public void remove(int index) {
        this.chain.remove(index);
    }

    public COS getCosClient() {
        return cosClient;
    }

    public void setCosClient(COS cosClient) {
        this.cosClient = cosClient;
    }

    public TransferManager getTransferManager() {
        return transferManager;
    }

    public void setTransferManager(TransferManager transferManager) {
        this.transferManager = transferManager;
    }

    public MediaBO getMedia() {
        return media;
    }

    public void setMedia(MediaBO media) {
        this.media = media;
    }
}
