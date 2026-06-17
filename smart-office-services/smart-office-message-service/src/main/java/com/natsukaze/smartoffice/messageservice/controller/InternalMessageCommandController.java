package com.natsukaze.smartoffice.messageservice.controller;

import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/messages")
@RequiredArgsConstructor
public class InternalMessageCommandController {

    private final MessageService messageService;

    @PostMapping("/todos")
    public Result<Void> createTodo(@RequestBody TodoCreateCommand command) {
        messageService.createTodo(command);
        return Result.success();
    }

    @PutMapping("/todos/complete")
    public Result<Void> completeTodo(@RequestParam Long userId,
                                     @RequestParam String businessType,
                                     @RequestParam Long businessId) {
        messageService.completeTodo(userId, businessType, businessId);
        return Result.success();
    }

    @PostMapping("/notices")
    public Result<Void> createNotice(@RequestBody NoticeCreateCommand command) {
        messageService.createNotice(command);
        return Result.success();
    }
}
