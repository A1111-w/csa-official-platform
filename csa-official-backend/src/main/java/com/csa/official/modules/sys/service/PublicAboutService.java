package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.cache.CacheKeys;
import com.csa.official.common.cache.CacheTtlPolicy;
import com.csa.official.common.cache.PublicReadCache;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.SysConfig;
import com.csa.official.modules.sys.mapper.SysConfigMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PublicAboutService {
    private static final Safelist CONTENT_SAFELIST = Safelist.basicWithImages()
            .addTags("h1", "h2", "h3", "blockquote", "pre", "code")
            .addAttributes("a", "target", "rel");
    private final SysConfigMapper mapper;
    private final PublicReadCache cache;
    private final CacheTtlPolicy ttl;
    private final AuditService audit;

    public PublicAboutService(SysConfigMapper mapper, PublicReadCache cache, CacheTtlPolicy ttl, AuditService audit) {
        this.mapper = mapper;
        this.cache = cache;
        this.ttl = ttl;
        this.audit = audit;
    }

    public String getAbout() {
        return cache.get("about", CacheKeys.about(), null, new TypeReference<String>() {}, ttl.aboutSeconds(), () -> {
            SysConfig config = find();
            return config == null || config.getConfigValue() == null ? "" : config.getConfigValue();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAbout(String content) {
        String safeContent = Jsoup.clean(content == null ? "" : content, CONTENT_SAFELIST);
        SysConfig config = find();
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey("CSA_INTRO");
            config.setDescription("Association introduction");
            config.setConfigValue(safeContent);
            mapper.insert(config);
        } else {
            config.setConfigValue(safeContent);
            config.setUpdateBy(SecurityUtils.getUserId());
            mapper.updateById(config);
        }
        audit.recordBestEffort("CONFIG_UPDATE", "SYSTEM_CONFIG", "CSA_INTRO",
                "SUCCESS", null, Map.of("contentLength", safeContent.length()));
        cache.invalidateAfterCommit("about", "CSA_INTRO");
    }

    private SysConfig find() {
        return mapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, "CSA_INTRO"));
    }
}
