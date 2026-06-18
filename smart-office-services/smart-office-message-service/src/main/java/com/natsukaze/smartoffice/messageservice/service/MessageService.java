package com.natsukaze.smartoffice.messageservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.enums.TodoStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.messageservice.dto.MessagePageQuery;
import com.natsukaze.smartoffice.messageservice.dto.TodoPageQuery;
import com.natsukaze.smartoffice.messageservice.entity.MessageNotice;
import com.natsukaze.smartoffice.messageservice.entity.MessageTodo;
import com.natsukaze.smartoffice.messageservice.mapper.MessageNoticeMapper;
import com.natsukaze.smartoffice.messageservice.mapper.MessageTodoMapper;
import com.natsukaze.smartoffice.messageservice.mq.NoticeMessageProducer;
import com.natsukaze.smartoffice.messageservice.vo.MessageNoticeVO;
import com.natsukaze.smartoffice.messageservice.vo.MessageTodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {

    private final MessageNoticeMapper noticeMapper;

    private final MessageTodoMapper todoMapper;

    private final NoticeMessageProducer noticeMessageProducer;

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
        finishTodo(todo);
    }

    @Transactional
    public void createTodo(TodoCreateCommand command) {
        MessageTodo todo = new MessageTodo();
        todo.setUserId(command.userId());
        todo.setTitle(command.title());
        todo.setBusinessType(BusinessType.ofNullable(command.businessType()));
        todo.setBusinessId(command.businessId());
        todo.setStatus(TodoStatus.PENDING);
        todo.setDueTime(command.dueTime());
        todoMapper.insert(todo);
    }

    @Transactional
    public void completeTodo(Long userId, String businessType, Long businessId) {
        MessageTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MessageTodo>()
                .eq(MessageTodo::getUserId, userId)
                .eq(MessageTodo::getBusinessType, BusinessType.ofNullable(businessType))
                .eq(MessageTodo::getBusinessId, businessId)
                .eq(MessageTodo::getStatus, TodoStatus.PENDING)
                .last("LIMIT 1"));
        if (todo != null) {
            finishTodo(todo);
        }
    }

    public void createNotice(NoticeCreateCommand command) {
        if (!noticeMessageProducer.asyncEnabled()) {
            saveNotice(command);
            return;
        }
        try {
            noticeMessageProducer.send(command);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ notice delivery failed, fallback to synchronous save. userId={}, businessType={}, businessId={}, reason={}",
                    command.userId(), command.businessType(), command.businessId(), ex.getMessage());
            saveNotice(command);
        }
    }

    @Transactional
    public void saveNotice(NoticeCreateCommand command) {
        MessageNotice notice = new MessageNotice();
        notice.setUserId(command.userId());
        notice.setTitle(command.title());
        notice.setContent(command.content());
        notice.setBusinessType(BusinessType.ofNullable(command.businessType()));
        notice.setBusinessId(command.businessId());
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }

    private void finishTodo(MessageTodo todo) {
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
