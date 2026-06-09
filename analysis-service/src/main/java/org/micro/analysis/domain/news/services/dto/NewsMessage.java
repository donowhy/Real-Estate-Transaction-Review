package org.micro.analysis.domain.news.services.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewsMessage {
    private String title;
    private String source;
    private String url;
}
