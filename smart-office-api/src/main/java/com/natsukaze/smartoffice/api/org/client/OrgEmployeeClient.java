package com.natsukaze.smartoffice.api.org.client;

import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = ServiceNames.ORG, path = "/internal/org/employees")
public interface OrgEmployeeClient {

    @GetMapping("/user/{userId}")
    Result<OrgEmployeeDTO> getByUserId(@PathVariable("userId") Long userId);
}
