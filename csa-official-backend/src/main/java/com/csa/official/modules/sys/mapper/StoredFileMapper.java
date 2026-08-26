package com.csa.official.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csa.official.modules.sys.entity.StoredFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StoredFileMapper extends BaseMapper<StoredFile> {

    @Select("""
            SELECT *
            FROM sys_stored_file
            WHERE storage_key = #{storageKey} AND status = 'ACTIVE'
            LIMIT 1
            """)
    StoredFile findActiveByStorageKey(@Param("storageKey") String storageKey);

    @Select("""
            SELECT COUNT(*)
            FROM sys_stored_file
            WHERE storage_key = #{storageKey}
            """)
    int countByStorageKey(@Param("storageKey") String storageKey);

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0)
            FROM sys_stored_file
            WHERE owner_user_id = #{ownerUserId} AND status = 'ACTIVE'
            """)
    Long sumActiveBytesByOwner(@Param("ownerUserId") Long ownerUserId);

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0)
            FROM sys_stored_file
            WHERE status = 'ACTIVE'
            """)
    Long sumAllActiveBytes();

    @Update("""
            UPDATE sys_stored_file
            SET last_access_time = CURRENT_TIMESTAMP
            WHERE storage_key = #{storageKey} AND status = 'ACTIVE'
            """)
    int markAccessed(@Param("storageKey") String storageKey);

    @Update("""
            UPDATE sys_stored_file
            SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'ACTIVE'
            """)
    int markDeleted(@Param("id") Long id);

    @Select("""
            SELECT sf.*
            FROM sys_stored_file sf
            WHERE sf.status = 'ACTIVE'
              AND sf.create_time < #{before}
              AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.file_url = sf.storage_key AND r.deleted = 0)
              AND NOT EXISTS (SELECT 1 FROM sys_carousel c WHERE c.img_url = sf.storage_key AND c.deleted = 0)
              AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.avatar = sf.storage_key AND u.deleted = 0)
              AND NOT EXISTS (SELECT 1 FROM biz_competition bc WHERE bc.cover_img = sf.storage_key AND bc.deleted = 0)
            ORDER BY sf.create_time
            LIMIT #{limit}
            """)
    List<StoredFile> selectOrphans(@Param("before") java.time.LocalDateTime before,
                                   @Param("limit") int limit);
}
