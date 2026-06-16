package com.natsukaze.smartoffice.attendanceservice;

import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/health")
public class InternalHealthController {

    @GetMapping
    public Result<String> health() {
        return Result.success(ServiceNames.ATTENDANCE);
    }
}
