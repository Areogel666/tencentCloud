package cn.lxr.example.tencentcloudapi.executor;

import cn.lxr.example.tencentcloudapi.constant.BucketConstant;
import cn.lxr.example.tencentcloudapi.util.COSUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketResponse;

public class MediaBucketExecutor {

    public static void main(String[] args) {
        COSClient client = COSUtils.initCOSClient();
        //1.创建模板请求对象
        MediaBucketRequest request = new MediaBucketRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName(BucketConstant.MEDIA_BUCKET);
        //3.调用接口,获取桶响应对象
        MediaBucketResponse response = client.describeMediaBuckets(request);
        client.shutdown();
    }
}
