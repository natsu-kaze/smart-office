package com.natsukaze.smartoffice.messageservice.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.messageservice.dto.AnnouncementCreateRequest;
import com.natsukaze.smartoffice.messageservice.dto.MessageBatchRequest;
import com.natsukaze.smartoffice.messageservice.dto.MessagePageQuery;
import com.natsukaze.smartoffice.messageservice.dto.TodoPageQuery;
import com.natsukaze.smartoffice.messageservice.service.MessageService;
import com.natsukaze.smartoffice.messageservice.vo.AnnouncementSendVO;
import com.natsukaze.smartoffice.messageservice.vo.MessageNoticeVO;
import com.natsukaze.smartoffice.messageservice.vo.MessageTodoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<MessageNoticeVO>> myMessages(@RequestHeader(USER_ID_HEADER) Long userId,
                                                          @ModelAttribute MessagePageQuery query) {
        return Result.success(messageService.myMessages(userId, query));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader(USER_ID_HEADER) Long userId) {
        return Result.success(messageService.unreadCount(userId));
    }

    @PostMapping("/announcements")
    public Result<AnnouncementSendVO> publishAnnouncement(@RequestHeader(USER_ID_HEADER) Long userId,
                                                         @RequestHeader(USERNAME_HEADER) String username,
                                                         @Valid @RequestBody AnnouncementCreateRequest request) {
        return Result.success(messageService.publishAnnouncement(userId, username, request));
    }

    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        messageService.markRead(userId, id);
        return Result.success();
    }

    @PatchMapping("/read")
    public Result<Void> batchMarkRead(@RequestHeader(USER_ID_HEADER) Long userId,
                                      @Valid @RequestBody MessageBatchRequest request) {
        messageService.batchMarkRead(userId, request.getIds());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        messageService.deleteMessage(userId, id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDeleteMessage(@RequestHeader(USER_ID_HEADER) Long userId,
                                           @Valid @RequestBody MessageBatchRequest request) {
        messageService.batchDeleteMessage(userId, request.getIds());
        return Result.success();
    }

    @GetMapping("/todos")
    public Result<PageResult<MessageTodoVO>> myTodos(@RequestHeader(USER_ID_HEADER) Long userId,
                                                     @ModelAttribute TodoPageQuery query) {
        return Result.success(messageService.myTodos(userId, query));
    }

    @GetMapping("/todos/count")
    public Result<Long> todoCount(@RequestHeader(USER_ID_HEADER) Long userId) {
        return Result.success(messageService.todoCount(userId));
    }

    @PatchMapping("/todos/{id}/complete")
    public Result<Void> completeTodo(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        messageService.completeTodo(userId, id);
        return Result.success();
    }
}
