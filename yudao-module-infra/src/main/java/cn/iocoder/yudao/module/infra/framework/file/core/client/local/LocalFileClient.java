package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.hutool.core.io.FileUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.AbstractFileClient;

import java.io.File;

/**
 * 本地文件客户端
 *
 * @author 芋道源码
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 执行写入
        String filePath = getFilePath(path);
        FileUtil.writeBytes(content, filePath);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public void delete(String path) {
        String filePath = getFilePath(path);
        FileUtil.del(filePath);
    }

    @Override
    public byte[] getContent(String path) {
        String filePath = getFilePath(path);
        return FileUtil.readBytes(filePath);
    }

    private String getFilePath(String path) {
        // 从path中提取taskId，假设path格式为: taskId/filename 或者 /taskId/filename
        String taskId = extractTaskIdFromPath(path);
        
        // 构建项目根目录下的task-file/{taskId}路径
        String projectRoot = System.getProperty("user.dir");
        String taskFileDir = projectRoot + File.separator + "task-file" + File.separator + taskId;
        
        // 确保目录存在
        File dir = new File(taskFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 获取文件名部分（去掉taskId前缀）
        String fileName = getFileNameFromPath(path);
        
        return taskFileDir + File.separator + fileName;
    }
    
    /**
     * 从路径中提取taskId
     * 支持格式: taskId/filename 或 /taskId/filename
     */
    private String extractTaskIdFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "default";
        }
        
        // 移除开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // 查找第一个斜杠的位置
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }
        
        // 如果没有斜杠，可能整个path就是taskId，或者使用默认值
        return "default";
    }
    
    /**
     * 从路径中获取文件名部分
     */
    private String getFileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown_file";
        }
        
        // 移除开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // 查找第一个斜杠的位置
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0 && slashIndex < path.length() - 1) {
            return path.substring(slashIndex + 1);
        }
        
        // 如果没有斜杠，整个path就是文件名
        return path;
    }

}
