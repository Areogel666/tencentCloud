package cn.lxr.example.tencentcloudapi.common.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadBO {

    private String uploadPath;
    private String downPath;
}
