package com.natsukaze.smartoffice.api.search.client;

import com.natsukaze.smartoffice.api.search.dto.PolicyDocumentDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicySearchClientFallback implements FallbackFactory<PolicySearchClient> {

    private static final Logger log = LoggerFactory.getLogger(PolicySearchClientFallback.class);

    @Override
    public PolicySearchClient create(Throwable cause) {
        log.error("PolicySearchClient fallback triggered", cause);
        return new PolicySearchClient() {
            @Override
            @SuppressWarnings("unchecked")
            public Result<PageResult<PolicyDocumentDTO>> search(String keyword, String status, long current, long size) {
                return (Result<PageResult<PolicyDocumentDTO>>) (Object)
                        Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "search service unavailable");
            }
        };
    }
}
