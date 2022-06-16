package cn.lxr.example.tencentcloudapi.util;

import cn.lxr.example.tencentcloudapi.constant.StorageType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {

    public static final String DOT = ".";

    /**
     * 获取目录下所有文件
     *
     * @param dirPath
     * @return
     */
    public static List<File> extractFolderFile(String dirPath){
        File dir = new File(dirPath);
        if (dir == null || !dir.exists()) {
            return new ArrayList<>();
        }
        List<File> fileList = Arrays.stream(dir.listFiles()).flatMap(file -> {
            if (file.isDirectory()) {
                return extractFolderFile(file.getPath()).stream();
            }
            List<File> singleFileList = new ArrayList<>();
            singleFileList.add(file);
            return singleFileList.stream();
        }).collect(Collectors.toList());
        return fileList;
    }

    /**
     * 修改文件后缀类型
     * @param input
     * @param storageType
     */
    public static String fixStorageType(String input, StorageType storageType) {
        int lastIndex;
        if ((lastIndex = input.lastIndexOf(DOT)) > -1) {
            input = input.substring(0, lastIndex + 1);
        }
        input += storageType.name();
        return input;
    }
}
