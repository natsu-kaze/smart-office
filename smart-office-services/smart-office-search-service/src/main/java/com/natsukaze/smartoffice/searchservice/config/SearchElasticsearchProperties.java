package com.natsukaze.smartoffice.searchservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-office.search.elasticsearch")
public class SearchElasticsearchProperties {

    private boolean enabled = true;
}
