package cn.lxr.example.tencentcloudapi.service;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.strategy.TransferChain;
import cn.lxr.example.tencentcloudapi.constant.StorageType;
import cn.lxr.example.tencentcloudapi.proxy.FileProxy;
import com.google.gson.Gson;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.transfer.TransferManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PictureService {

    private FileProxy fileProxy;

    public PictureService(FileProxy fileProxy) {
        this.fileProxy = fileProxy;
    }

    /**
     * 获取单个图片主色调
     *
     * @param cosClient
     * @param media
     * @return
     */
    public String dominantHue(COSClient cosClient, MediaBO media) {
        media.setRule("imageAve");
        HashMap hueMap = new Gson().fromJson(new String(fileProxy.baseTransfer(cosClient, media)), HashMap.class);
        String dominantHue = ObjectUtils.defaultIfNull(hueMap.get("RGB"), "").toString();
        log.info("dominantHue: {}", dominantHue);
        return dominantHue;
    }

    /**
     * 获取目录下所有图片的主色调
     *
     * @param cosClient
     * @param bucketName
     * @param keyList
     */
    public List<MediaBO> dominantHueAll(COSClient cosClient, String bucketName, List<String> keyList) {
        return keyList.stream().map(key -> {
            MediaBO media = new MediaBO(bucketName, key, "imageAve");
            byte[] hue = fileProxy.baseTransfer(cosClient, media);
            HashMap hueMap = new Gson().fromJson(new String(hue), HashMap.class);
            media.setDominantHue(ObjectUtils.defaultIfNull(hueMap.get("RGB"), "").toString());
            return media;
        }).collect(Collectors.toList());
    }

    /**
     * 模糊缩放压缩
     *
     * @param transferManager
     * @param media
     * @param output
     */
    public void blurThumbnailCompress(TransferManager transferManager, MediaBO media, String output) {
        media.setRule("imageMogr2/thumbnail/48x/format/webp/interlace/0/quality/100/blur/10x100");
//        pictureService.baseTransferDownload(cosClient, gaussianBlur, "tiktok_vague4.png");
        fileProxy.baseTransferResumableDownload(transferManager, media, output);
    }

    /**
     * 高斯模糊
     *
     * @param transferManager
     * @param media
     * @param output
     */
    public void blur(TransferManager transferManager, MediaBO media, String output) {
        media.setRule("imageMogr2/blur/10x100");
        fileProxy.baseTransferResumableDownload(transferManager, media, output);
    }

    /**
     * 单个文件通用链式处理
     *
     * @param transferChain
     */
    public void transfer(TransferChain transferChain) {
        transferChain.execute();
    }

    /**
     * 多个文件通用链式处理
     *
     * @param transferChain
     */
    public void transferAll(TransferChain transferChain, List<String> keyList, StorageType storageType) {
        keyList.forEach(key -> {
            transferChain.getMedia().setKey(key);
            transferChain.execute();
        });
    }

    /**
     * 多个文件通用链式处理
     * 不覆盖云上的源文件，不保存临时文件
     *
     * @param transferChain
     * @param dirPath 下载的文件目录
     */
    public void transferAllUncover(TransferChain transferChain, List<String> keyList, String dirPath) {
        keyList.forEach(key -> {
            transferChain.getMedia().setKey(key);
            transferChain.execute(dirPath + key);
        });
    }

}
