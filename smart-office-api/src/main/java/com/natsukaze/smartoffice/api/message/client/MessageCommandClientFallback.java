package com.natsukaze.smartoffice.api.message.client;

import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageCommandClientFallback implements FallbackFactory<MessageCommandClient> {

    private static final Logger log = LoggerFactory.getLogger(MessageCommandClientFallback.class);

    @Override
    public MessageCommandClient create(Throwable cause) {
        log.error("MessageCommandClient fallback triggered", cause);
        return new MessageCommandClient() {
            @Override
            public Result<Void> createTodo(TodoCreateCommand command) {
                return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "message service unavailable");
            }

            @Override
            public Result<Void> completeTodo(Long userId, String businessType, Long businessId) {
                return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "message service unavailable");
            }

            @Override
            public Result<Void> createNotice(NoticeCreateCommand command) {
                return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "message service unavailable");
            }
        };
    }
}
