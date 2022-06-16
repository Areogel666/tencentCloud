package cn.lxr.example.tencentcloudapi.proxy;

import cn.lxr.example.tencentcloudapi.common.bo.MediaBO;
import cn.lxr.example.tencentcloudapi.constant.StorageType;
import com.qcloud.cos.COS;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.MultipleFileDownload;
import com.qcloud.cos.transfer.MultipleFileUpload;
import com.qcloud.cos.transfer.Transfer;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferProgress;
import com.qcloud.cos.transfer.Upload;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileProxy {

    /**
     * 列出云端文件
     *
     * @param cosClient
     * @param media
     * @param nextMarker
     * @return
     */
    public List<String> listFileKey(COSClient cosClient, MediaBO media, String nextMarker) {
        List<String> keyList = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        // 设置 bucket 名称
        listObjectsRequest.setBucketName(media.getBucketName());
        // 设置列出的对象名以 prefix 为前缀
        listObjectsRequest.setPrefix(media.getKey());
        // 设置最大列出多少个对象, 一次 listobject 最大支持1000
        listObjectsRequest.setMaxKeys(1000);
        // 设置被截断开始的位置
        if (nextMarker != null) {
            listObjectsRequest.setMarker(nextMarker);
        }
        // 保存列出的结果
        ObjectListing objectListing = null;
        try {
            objectListing = cosClient.listObjects(listObjectsRequest);
        } catch (CosServiceException e) {
            log.error("listFileKey error", e);
        } catch (CosClientException e) {
            log.error("listFileKey error", e);
        }
        // object summary 表示此次列出的对象列表
        List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();
        for (COSObjectSummary cosObjectSummary : cosObjectSummaries) {
            // 对象的 key
            String key = cosObjectSummary.getKey();
            keyList.add(key);
        }
        if (objectListing.isTruncated()) {
            // 表示还没有列完，被截断了
            listFileKey(cosClient, media, objectListing.getNextMarker());
        }
        return keyList;
    }

    /**
     * 根据文件类型列出云端文件
     *
     * @param cosClient
     * @param media
     * @param storageType
     * @param nextMarker
     * @return
     */
    public List<String> listFileKey(COSClient cosClient, MediaBO media, StorageType storageType, String nextMarker) {
        List<String> keyList = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        // 设置 bucket 名称
        listObjectsRequest.setBucketName(media.getBucketName());
        // 设置列出的对象名以 prefix 为前缀
        listObjectsRequest.setPrefix(media.getKey());
        // 设置最大列出多少个对象, 一次 listobject 最大支持1000
        listObjectsRequest.setMaxKeys(1000);
        // 设置被截断开始的位置
        if (nextMarker != null) {
            listObjectsRequest.setMarker(nextMarker);
        }
        // 保存列出的结果
        ObjectListing objectListing = null;
        try {
            objectListing = cosClient.listObjects(listObjectsRequest);
        } catch (CosServiceException e) {
            log.error("listFileKey error", e);
        } catch (CosClientException e) {
            log.error("listFileKey error", e);
        }
        // object summary 表示此次列出的对象列表
        List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();
        for (COSObjectSummary cosObjectSummary : cosObjectSummaries) {
            // 对象的 key
            String key = cosObjectSummary.getKey();
            // 对象的存储类型
            String storageClasses = cosObjectSummary.getStorageClass();
            if (storageType != null && storageType.name().equalsIgnoreCase(storageClasses)) {
                keyList.add(key);
            }
        }
        if (objectListing.isTruncated()) {
            // 表示还没有列完，被截断了
            listFileKey(cosClient, media, storageType, objectListing.getNextMarker());
        }
        return keyList;
    }

    /**
     * 文件基础处理后返回byte[]
     * @param cosClient
     * @param media
     * @return
     */
    public byte[] baseTransfer(COS cosClient, MediaBO media) {
        GetObjectRequest getObj = new GetObjectRequest(media.getBucketName(), media.getKey());
        getObj.putCustomQueryParameter(media.getRule(), null);
        COSObject cosObject = cosClient.getObject(getObj);
        COSObjectInputStream cosObjectInput = cosObject.getObjectContent();
        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(cosObjectInput);
        } catch (IOException e) {
            log.error("baseTransfer error", e);
        } finally {
            // 用完流之后一定要调用 close()
            try {
                cosObjectInput.close();
            } catch (IOException e) {
                log.error("baseTransfer close cosObjectInput error", e);
            }
        }
        return bytes;
    }

    /**
     * 文件基础处理后返回stream
     * @param cosClient
     * @param media
     * @return
     */
    public COSObjectInputStream baseTransferStream(COS cosClient, MediaBO media) {
        GetObjectRequest getObj = new GetObjectRequest(media.getBucketName(), media.getKey());
        getObj.putCustomQueryParameter(media.getRule(), null);
        COSObject cosObject = cosClient.getObject(getObj);
        return cosObject.getObjectContent();
    }

    /**
     * 文件基础处理后下载
     * @param cosClient
     * @param media
     * @param output
     */
    public void baseTransferDownload(COSClient cosClient, MediaBO media, String output) {
        GetObjectRequest getObj = new GetObjectRequest(media.getBucketName(), media.getKey());
        getObj.putCustomQueryParameter(media.getRule(), null);
        cosClient.getObject(getObj, new File(output));
    }

    /**
     * 文件基础处理后下载（支持断点下载）
     * https://cloud.tencent.com/document/product/436/65937
     * @param transferManager
     * @param media
     * @param output
     */
    public void baseTransferResumableDownload(TransferManager transferManager, MediaBO media, String output) {
        GetObjectRequest getObj = new GetObjectRequest(media.getBucketName(), media.getKey());
        getObj.putCustomQueryParameter(media.getRule(), null);
        try {
            // 返回一个异步结果 Donload, 可同步的调用 waitForCompletion 等待下载结束, 成功返回 void, 失败抛出异常
            Download download = transferManager.download(getObj, new File(output), true);
            download.waitForCompletion();
        } catch (CosServiceException e) {
            log.error("baseTransferMultipartDownload error", e);
        } catch (CosClientException e) {
            log.error("baseTransferMultipartDownload error", e);
        } catch (InterruptedException e) {
            log.error("baseTransferMultipartDownload error", e);
        }
    }

    /**
     * 下载整个目录
     *
     * @param transferManager
     * @param media
     * @param prefix
     * @param outputDir
     */
    public void downloadDirectory(TransferManager transferManager, MediaBO media, String prefix, String outputDir) {
        try {
            // 返回一个异步结果 Donload, 可同步的调用 waitForCompletion 等待下载结束, 成功返回 void, 失败抛出异常
            MultipleFileDownload download = transferManager.downloadDirectory(media.getBucketName(), prefix, new File(outputDir));
            // 可以选择查看下载进度
            showTransferProgress(download);
            download.waitForCompletion();
        } catch (CosServiceException e) {
            log.error("baseTransferMultipartDownload error", e);
        } catch (CosClientException e) {
            log.error("baseTransferMultipartDownload error", e);
        } catch (InterruptedException e) {
            log.error("baseTransferMultipartDownload error", e);
        }
    }

    /**
     * 上传文件到云
     *
     * @param transferManager
     * @param media
     * @param input
     * @return
     */
    public UploadResult upload(TransferManager transferManager, MediaBO media, String input) {
        // 本地文件路径
        File localFile = new File(input);
        PutObjectRequest putObjectRequest = new PutObjectRequest(media.getBucketName(), media.getKey(), localFile);
        UploadResult uploadResult = null;
        try {
            // 高级接口会返回一个异步结果Upload
            // 可同步地调用 waitForUploadResult 方法等待上传完成，成功返回UploadResult, 失败抛出异常
            Upload upload = transferManager.upload(putObjectRequest);
            uploadResult = upload.waitForUploadResult();
        } catch (CosServiceException e) {
            log.error("upload error, file: {}", input, e);
        } catch (CosClientException e) {
            log.error("upload error, file: {}", input, e);
        } catch (InterruptedException e) {
            log.error("upload error, file: {}", input, e);
        }
        return uploadResult;
    }

    /**
     * 上传inputStream到云
     *
     * @param transferManager
     * @param media
     * @param inputStream
     * @return
     */
    public UploadResult upload(TransferManager transferManager, MediaBO media, InputStream inputStream, int inputStreamLength) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        // 上传的流如果能够获取准确的流长度，则推荐一定填写 content-length
        // 如果确实没办法获取到，则下面这行可以省略，但同时高级接口也没办法使用分块上传了
        if (inputStreamLength > 0) {
            objectMetadata.setContentLength(inputStreamLength);
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(media.getBucketName(), media.getKey(), inputStream, objectMetadata);
        UploadResult uploadResult = null;
        try {
            // 高级接口会返回一个异步结果Upload
            // 可同步地调用 waitForUploadResult 方法等待上传完成，成功返回UploadResult, 失败抛出异常
            Upload upload = transferManager.upload(putObjectRequest);
            uploadResult = upload.waitForUploadResult();
        } catch (CosServiceException e) {
            log.error("upload inputStream error", e);
        } catch (CosClientException e) {
            log.error("upload inputStream error", e);
        } catch (InterruptedException e) {
            log.error("upload inputStream error", e);
        }
        return uploadResult;
    }

    /**
     * 上传整个目录到云（目录结构与本地相同）
     *
     * @param transferManager
     * @param bucketName
     * @param inputDir
     * @return
     */
    public void uploadDirectory(TransferManager transferManager, String bucketName, String inputDir) {
        // 本地文件目录路径
        File localFileDir = new File(inputDir);
        for (File localFile : localFileDir.listFiles()) {
            if (localFile.isDirectory()) {
                uploadDirectory(transferManager, bucketName, localFile.getPath());
                continue;
            }
            log.info("uploadDirectory localFile.getPath(): {}", localFile.getPath());
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, localFile.getPath(), localFile);
            UploadResult uploadResult = null;
            try {
                // 高级接口会返回一个异步结果Upload
                // 可同步地调用 waitForUploadResult 方法等待上传完成，成功返回UploadResult, 失败抛出异常
                Upload upload = transferManager.upload(putObjectRequest);
                upload.waitForUploadResult();
            } catch (CosServiceException e) {
                log.error("uploadDirectory error, file: {}", localFile.getPath(), e);
            } catch (CosClientException e) {
                log.error("uploadDirectory error, file: {}", localFile.getPath(), e);
            } catch (InterruptedException e) {
                log.error("uploadDirectory error, file: {}", localFile.getPath(), e);
            }
        }
    }

    /**
     * 上传整个目录到云
     *
     * @param transferManager
     * @param media
     * @param inputDir
     * @return
     */
    public void uploadDirectory(TransferManager transferManager, MediaBO media, String inputDir) {
        // 本地文件目录路径
        File localFileDir = new File(inputDir);
        if (!localFileDir.isDirectory()) {
            return;
        }
        UploadResult uploadResult = null;
        try {
            // 高级接口会返回一个异步结果Upload
            // 可同步地调用 waitForUploadResult 方法等待上传完成，成功返回UploadResult, 失败抛出异常
            MultipleFileUpload upload = transferManager.uploadDirectory(media.getBucketName(), media.getKey(),localFileDir, true);
            showTransferProgress(upload);
            upload.waitForCompletion();
        } catch (CosServiceException e) {
            log.error("uploadDirectory error, fileDir: {}", inputDir, e);
        } catch (CosClientException e) {
            log.error("uploadDirectory error, fileDir: {}", inputDir, e);
        } catch (InterruptedException e) {
            log.error("uploadDirectory error, fileDir: {}", inputDir, e);
        }
    }

    /**
     * 显示进度
     * @param transfer
     */
    private void showTransferProgress(Transfer transfer) {
        // 这里的 Transfer 是异步上传结果 Upload 的父类
        System.out.println(transfer.getDescription());

        // transfer.isDone() 查询上传是否已经完成
        while (transfer.isDone() == false) {
            try {
                // 每 2 秒获取一次进度
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }

            TransferProgress progress = transfer.getProgress();
            long sofar = progress.getBytesTransferred();
            long total = progress.getTotalBytesToTransfer();
            double pct = progress.getPercentTransferred();
            log.info("upload progress: [{} / {}] = {}%", sofar, total, pct);
        }

        // 完成了 Completed，或者失败了 Failed
        System.out.println(transfer.getState());
    }
}
