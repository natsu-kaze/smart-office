package com.natsukaze.smartoffice.messageservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MessageBatchRequest {

    @NotEmpty(message = "message ids must not be empty")
    private List<Long> ids;
}
