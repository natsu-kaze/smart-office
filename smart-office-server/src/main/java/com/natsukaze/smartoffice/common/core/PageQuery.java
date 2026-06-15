package com.natsukaze.smartoffice.common.core;

import lombok.Data;

@Data
public class PageQuery {

    private Long current = 1L;

    private Long size = 10L;

    public Long getCurrent() {
        return current == null || current < 1 ? 1L : current;
    }

    public Long getSize() {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, 100L);
    }
}
