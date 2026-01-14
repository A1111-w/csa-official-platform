package com.csa.official.modules.biz.service;

import com.csa.official.common.exception.CsaException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class GitService {

    // 之前你在 application.yml 配置的上传路径
    @Value("${csa.upload-path}") 
    private String basePath;

    /**
     * 同步代码仓库 (Clone 或 Pull)
     * @param userId 用户ID
     * @param repoUrl 仓库地址 (如 https://github.com/xxx/xxx.git)
     */
    public void syncRepository(Long userId, String repoUrl) {
        if (!repoUrl.startsWith("http")) {
            throw new CsaException("仅支持 HTTP/HTTPS 协议的仓库地址");
        }

        // 存放路径: /csa-upload/git-repos/{userId}/
        // 这样每个用户的代码都独立分开
        File localPath = new File(basePath + File.separator + "git-repos" + File.separator + userId);

        try {
            if (localPath.exists() && new File(localPath, ".git").exists()) {
                // === 情况1：仓库已存在，执行 git pull ===
                log.info("仓库已存在，开始更新: {}", localPath.getAbsolutePath());
                try (Git git = Git.open(localPath)) {
                    git.pull().call(); // 相当于 git pull
                    log.info("更新成功");
                }
            } else {
                // === 情况2：仓库不存在，执行 git clone ===
                log.info("仓库不存在，开始克隆: {} -> {}", repoUrl, localPath.getAbsolutePath());
                
                // 为了防坑，如果目录存在但没 .git，先删干净
                if (localPath.exists()) {
                    deleteDir(localPath);
                }
                localPath.mkdirs();

                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localPath)
                        .setDepth(1) // 💡 优化：只拉取最后一次提交，省流量省空间！
                        .call();
                log.info("克隆成功");
            }
        } catch (GitAPIException | IOException e) {
            log.error("Git操作失败", e);
            throw new CsaException("代码同步失败，请检查仓库地址是否公开，或网络是否通畅");
        }
    }

    // 辅助：递归删除文件夹
    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
}
