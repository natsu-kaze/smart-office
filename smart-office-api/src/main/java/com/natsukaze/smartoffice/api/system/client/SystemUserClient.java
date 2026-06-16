package com.natsukaze.smartoffice.api.system.client;

import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = ServiceNames.SYSTEM, path = "/internal/system/users")
public interface SystemUserClient {

    @GetMapping("/{userId}")
    Result<CurrentUserDTO> getById(@PathVariable("userId") Long userId);
}
