package com.natsukaze.smartoffice.api.search.client;

import com.natsukaze.smartoffice.api.search.dto.PolicyDocumentDTO;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ServiceNames.SEARCH, path = "/internal/search/policies",
        fallbackFactory = PolicySearchClientFallback.class)
public interface PolicySearchClient {

    @GetMapping
    Result<PageResult<PolicyDocumentDTO>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam("current") long current,
                                                 @RequestParam("size") long size);
}
