package cn.lxr.example.tencentcloudapi.common.strategy;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.bo.UploadBO;
import cn.lxr.example.tencentcloudapi.constant.StorageType;
import cn.lxr.example.tencentcloudapi.util.FileUtils;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.transfer.TransferManager;

public class BlurThumbnailOperator extends TransferOperator {

    private static final String RULE = "imageMogr2/thumbnail/48x/blur/50x50";

    @Override
    public void run(TransferManager transferManager, MediaBO media, String output) {
        media.setRule(RULE);
        String key = media.getKey();
        // 本地output默认存储在download目录下
        if (output == null) {
            output = key;
        }
        output = !output.startsWith(DOWNLOAD) ? DOWNLOAD + output : output;

        fileProxy.baseTransferResumableDownload(transferManager, media, output);
        // 处理完成后重置key，这样之后upload操作会把文件上传到output目录，方便后续操作
        media.setKey(output);
    }

    @Override
    public COSObjectInputStream runByStream(TransferManager transferManager, MediaBO media, UploadBO uploadPath) {
        media.setRule(RULE);
        return fileProxy.baseTransferStream(transferManager.getCOSClient(), media);
    }

}
