package com.natsukaze.smartoffice.fileservice.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileRecordPageQuery extends PageQuery {

    private String keyword;

    private String businessType;
}
