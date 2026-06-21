package com.natsukaze.smartoffice.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.enums.TodoStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.message.dto.MessagePageQuery;
import com.natsukaze.smartoffice.message.dto.TodoPageQuery;
import com.natsukaze.smartoffice.message.entity.MessageNotice;
import com.natsukaze.smartoffice.message.entity.MessageTodo;
import com.natsukaze.smartoffice.message.mapper.MessageNoticeMapper;
import com.natsukaze.smartoffice.message.mapper.MessageTodoMapper;
import com.natsukaze.smartoffice.message.vo.MessageNoticeVO;
import com.natsukaze.smartoffice.message.vo.MessageTodoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageNoticeMapper noticeMapper;

    private final MessageTodoMapper todoMapper;

    public PageResult<MessageNoticeVO> myMessages(Long userId, MessagePageQuery query) {
        LambdaQueryWrapper<MessageNotice> wrapper = new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getUserId, userId)
                .eq(query.getReadStatus() != null, MessageNotice::getReadStatus, query.getReadStatus())
                .eq(StringUtils.hasText(query.getBusinessType()), MessageNotice::getBusinessType,
                        BusinessType.ofNullable(query.getBusinessType()))
                .orderByDesc(MessageNotice::getCreateTime);
        Page<MessageNotice> page = noticeMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toNoticeVO));
    }

    public Long unreadCount(Long userId) {
        return noticeMapper.selectCount(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getUserId, userId)
                .eq(MessageNotice::getReadStatus, 0));
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        MessageNotice notice = requireNotice(userId, id);
        notice.setReadStatus(1);
        notice.setReadTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    @Transactional
    public void deleteMessage(Long userId, Long id) {
        requireNotice(userId, id);
        noticeMapper.deleteById(id);
    }

    public PageResult<MessageTodoVO> myTodos(Long userId, TodoPageQuery query) {
        LambdaQueryWrapper<MessageTodo> wrapper = new LambdaQueryWrapper<MessageTodo>()
                .eq(MessageTodo::getUserId, userId)
                .eq(StringUtils.hasText(query.getStatus()), MessageTodo::getStatus,
                        TodoStatus.ofNullable(query.getStatus()))
                .eq(StringUtils.hasText(query.getBusinessType()), MessageTodo::getBusinessType,
                        BusinessType.ofNullable(query.getBusinessType()))
                .orderByDesc(MessageTodo::getCreateTime);
        Page<MessageTodo> page = todoMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toTodoVO));
    }

    public Long todoCount(Long userId) {
        return todoMapper.selectCount(new LambdaQueryWrapper<MessageTodo>()
                .eq(MessageTodo::getUserId, userId)
                .eq(MessageTodo::getStatus, TodoStatus.PENDING));
    }

    @Transactional
    public void completeTodo(Long userId, Long id) {
        MessageTodo todo = requireTodo(userId, id);
        todo.setStatus(TodoStatus.DONE);
        todo.setCompletedTime(LocalDateTime.now());
        todoMapper.updateById(todo);
    }

    private MessageNotice requireNotice(Long userId, Long id) {
        MessageNotice notice = noticeMapper.selectOne(new LambdaQueryWrapper<MessageNotice>()
                .eq(MessageNotice::getId, id)
                .eq(MessageNotice::getUserId, userId)
                .last("LIMIT 1"));
        if (notice == null) {
            throw new BusinessException("message not found");
        }
        return notice;
    }

    private MessageTodo requireTodo(Long userId, Long id) {
        MessageTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MessageTodo>()
                .eq(MessageTodo::getId, id)
                .eq(MessageTodo::getUserId, userId)
                .last("LIMIT 1"));
        if (todo == null) {
            throw new BusinessException("todo not found");
        }
        return todo;
    }

    private MessageNoticeVO toNoticeVO(MessageNotice notice) {
        return MessageNoticeVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .senderName(notice.getSenderName())
                .businessType(notice.getBusinessType() == null ? null : notice.getBusinessType().getCode())
                .businessId(notice.getBusinessId())
                .readStatus(notice.getReadStatus())
                .readTime(notice.getReadTime())
                .createTime(notice.getCreateTime())
                .build();
    }

    private MessageTodoVO toTodoVO(MessageTodo todo) {
        return MessageTodoVO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .businessType(todo.getBusinessType() == null ? null : todo.getBusinessType().getCode())
                .businessId(todo.getBusinessId())
                .status(todo.getStatus() == null ? null : todo.getStatus().getCode())
                .dueTime(todo.getDueTime())
                .completedTime(todo.getCompletedTime())
                .createTime(todo.getCreateTime())
                .build();
    }
}
