package com.natsukaze.smartoffice.message.controller;

import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.message.dto.MessagePageQuery;
import com.natsukaze.smartoffice.message.dto.TodoPageQuery;
import com.natsukaze.smartoffice.message.service.MessageService;
import com.natsukaze.smartoffice.message.vo.MessageNoticeVO;
import com.natsukaze.smartoffice.message.vo.MessageTodoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<MessageNoticeVO>> myMessages(@AuthenticationPrincipal UserPrincipal principal,
                                                          @ModelAttribute MessagePageQuery query) {
        return Result.success(messageService.myMessages(principal.getUserId(), query));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(messageService.unreadCount(principal.getUserId()));
    }

    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        messageService.markRead(principal.getUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        messageService.deleteMessage(principal.getUserId(), id);
        return Result.success();
    }

    @GetMapping("/todos")
    public Result<PageResult<MessageTodoVO>> myTodos(@AuthenticationPrincipal UserPrincipal principal,
                                                     @ModelAttribute TodoPageQuery query) {
        return Result.success(messageService.myTodos(principal.getUserId(), query));
    }

    @GetMapping("/todos/count")
    public Result<Long> todoCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(messageService.todoCount(principal.getUserId()));
    }

    @PatchMapping("/todos/{id}/complete")
    public Result<Void> completeTodo(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        messageService.completeTodo(principal.getUserId(), id);
        return Result.success();
    }
}
