package com.natsukaze.smartoffice.messageservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnouncementSendVO {

    private int recipientCount;
}
