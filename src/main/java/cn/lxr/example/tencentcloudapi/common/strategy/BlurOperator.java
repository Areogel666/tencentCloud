package cn.lxr.example.tencentcloudapi.common.strategy;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.bo.UploadBO;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.transfer.TransferManager;

public class BlurOperator extends TransferOperator {

    private static final String RULE = "imageMogr2/blur/10x100";

    @Override
    public void run(TransferManager transferManager, MediaBO media, String output) {
        media.setRule(RULE);
        String key = media.getKey();
        // 本地output默认存储在temp目录下
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
