package cn.lxr.example.tencentcloudapi.executor;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.common.strategy.BlurOperator;
import cn.lxr.example.tencentcloudapi.common.strategy.BlurThumbnailCompressOperator;
import cn.lxr.example.tencentcloudapi.common.strategy.BlurThumbnailOperator;
import cn.lxr.example.tencentcloudapi.common.strategy.TransferChain;
import cn.lxr.example.tencentcloudapi.constant.BucketConstant;
import cn.lxr.example.tencentcloudapi.constant.StorageType;
import cn.lxr.example.tencentcloudapi.proxy.FileProxy;
import cn.lxr.example.tencentcloudapi.service.PictureService;
import cn.lxr.example.tencentcloudapi.util.COSUtils;
import cn.lxr.example.tencentcloudapi.util.FileUtils;
import cn.lxr.example.tencentcloudapi.util.TransferManagerUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.transfer.TransferManager;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class VagueExecutor {

    public static void main(String[] args) {
        FileProxy fileProxy = new FileProxy();
        PictureService pictureService = new PictureService(fileProxy);
        COSClient cosClient = COSUtils.initCOSClient();
//        singleVagueProcess(fileProxy, pictureService, cosClient);
        multipleVagueProcess(fileProxy, pictureService, cosClient);
//        multipleHueProcess(fileProxy, pictureService, cosClient);
        // 关闭cosClient资源
        cosClient.shutdown();
    }

    /**
     * 处理单个文件
     * 处理过程：
     * 1.文件保存在本地
     * 2.上传到云
     * 3.获取主色调
     * 4.高斯模糊 + 缩小 + 压缩
     * 5.下载到本地
     *
     * @param fileProxy
     * @param pictureService
     * @param cosClient
     */
    private static void singleVagueProcess(FileProxy fileProxy, PictureService pictureService, COSClient cosClient) {
        TransferManager transferManager = TransferManagerUtils.createTransferManager(cosClient);
        MediaBO media = new MediaBO(BucketConstant.TEST_BUCKET, "upload/uc.png");
        // 上传
        fileProxy.upload(transferManager, media, "upload/uc.png");
        // 取色
        pictureService.dominantHue(cosClient, media);
        // 高斯模糊
//        pictureService.blur(transferManager, media, "download/uc.png");
        // 高斯模糊 + 缩放 + WebP压缩
        pictureService.blurThumbnailCompress(transferManager, media, "download/uc.webp");
        // 确定本进程不再使用 transferManager 实例之后，关闭之
        TransferManagerUtils.shutdownTransferManager(transferManager, false);
    }

    /**
     * 处理多个文件
     * 处理过程：
     * 1.文件保存在本地目录中
     * 2.上传到云
     * 3.获取主色调
     * 4.重复高斯模糊，并把结果文件上传到云
     * 5.下载最终结果到本地
     *
     * @param fileProxy
     * @param pictureService
     * @param cosClient
     */
    private static void multipleVagueProcess(FileProxy fileProxy, PictureService pictureService, COSClient cosClient) {
        TransferManager transferManager = TransferManagerUtils.createTransferManager(cosClient);
//        // 上传
//        String inputDir = "upload1/";
        MediaBO media = new MediaBO(BucketConstant.TEST_BUCKET, "upload");
//        fileProxy.uploadDirectory(transferManager, media, inputDir);
        // 云端文件列表
        List<String> keyList = fileProxy.listFileKey(cosClient, media, null);
//        // 取色
//        List<MediaBO> mediaBOList = pictureService.dominantHueAll(cosClient, BucketConstant.TEST_BUCKET, keyList);
//        mediaBOList.stream().forEach(m -> {
////            log.info("---- name:{}, dominantHue:{}", m.getKey(), m.getDominantHue());
//            System.out.println(m.getKey() + "\t" + m.getDominantHue());
//        });
        // 图片转换
        TransferChain transferChain = new TransferChain(transferManager, new MediaBO(BucketConstant.TEST_BUCKET, ""));
        // 高斯模糊 + 缩放
        transferChain.add(new BlurThumbnailOperator());
        // 高斯模糊
        transferChain.addMore(new BlurOperator(), 1);
        pictureService.transferAllUncover(transferChain, keyList, "result/");

        // 确定本进程不再使用 transferManager 实例之后，关闭之
        TransferManagerUtils.shutdownTransferManager(transferManager, false);
    }

    /**
     * 获取多个图片主色调
     *
     * @param fileProxy
     * @param pictureService
     * @param cosClient
     */
    private static void multipleHueProcess(FileProxy fileProxy, PictureService pictureService, COSClient cosClient) {
        TransferManager transferManager = TransferManagerUtils.createTransferManager(cosClient);
        // 上传
        long timePoint1 = System.currentTimeMillis();
        String inputDir = "upload/";
        fileProxy.uploadDirectory(transferManager, BucketConstant.TEST_BUCKET, inputDir);
        // 取色（这里本地和云的目录结构一样，所以keyList解析本地路径获得）
        long timePoint2 = System.currentTimeMillis();
        List<File> fileList = FileUtils.extractFolderFile(inputDir);
        List<String> keyList = fileList.stream().map(File::getPath).collect(Collectors.toList());
        List<MediaBO> mediaBOList = pictureService.dominantHueAll(cosClient, BucketConstant.TEST_BUCKET, keyList);
        long timePoint3 = System.currentTimeMillis();
        log.info("all finished, upload cost:{}, dominantHue cost:{}", timePoint2 - timePoint1, timePoint3 - timePoint2);
        mediaBOList.stream().forEach(media -> {
//            log.info("---- name:{}, dominantHue:{}", media.getKey(), media.getDominantHue());
            System.out.println(media.getKey() + "\t" + media.getDominantHue());
        });
        // 确定本进程不再使用 transferManager 实例之后，关闭之
        TransferManagerUtils.shutdownTransferManager(transferManager, false);
    }

}
