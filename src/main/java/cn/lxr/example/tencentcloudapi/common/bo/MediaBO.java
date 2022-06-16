package cn.lxr.example.tencentcloudapi.common.bo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;


@Data
@NoArgsConstructor
public class MediaBO implements Cloneable{
    // 桶
    private String bucketName;
    // 云端路径
    private String key;
    // 操作规则
    private String rule;

    /* 以下为文件属性字段 */

    // 主色调
    private String dominantHue;

    public MediaBO(String bucketName, String key) {
        this.bucketName = bucketName;
        this.key = key;
    }

    public MediaBO(String bucketName, String key, String rule) {
        this.bucketName = bucketName;
        this.key = key;
        this.rule = rule;
    }

    @Override
    public MediaBO clone() throws CloneNotSupportedException {
        return (MediaBO)super.clone();
    }
}
