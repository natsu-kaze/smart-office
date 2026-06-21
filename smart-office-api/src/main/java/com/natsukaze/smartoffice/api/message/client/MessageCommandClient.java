package com.natsukaze.smartoffice.api.message.client;

import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ServiceNames.MESSAGE, path = "/internal/messages",
        fallbackFactory = MessageCommandClientFallback.class)
public interface MessageCommandClient {

    @PostMapping("/todos")
    Result<Void> createTodo(@RequestBody TodoCreateCommand command);

    @PutMapping("/todos/complete")
    Result<Void> completeTodo(@RequestParam("userId") Long userId,
                              @RequestParam("businessType") String businessType,
                              @RequestParam("businessId") Long businessId);

    @PostMapping("/notices")
    Result<Void> createNotice(@RequestBody NoticeCreateCommand command);
}
