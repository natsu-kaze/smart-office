package com.natsukaze.smartoffice.searchservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("policy_document_chunk")
public class PolicyDocumentChunk extends BaseEntity {

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String vectorId;
}
