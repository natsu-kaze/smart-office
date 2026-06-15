package com.natsukaze.smartoffice.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {

    private String originalName;

    private String storageName;

    private String bucket;

    private String objectKey;

    private String contentType;

    private Long size;

    private String url;

    private Long uploaderId;

    private String businessType;

    private Long businessId;
}
