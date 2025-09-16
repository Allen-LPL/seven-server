package cn.iocoder.yudao.module.system.service.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PdfParseResultDTO {

    @JsonProperty("authors")
    private List<String> authors;

    @JsonProperty("author_affiliations")
    private List<List<String>> authorAffiliations;

    @JsonProperty("doi")
    private String doi;

    @JsonProperty("issn")
    private String issn;

    @JsonProperty("journal")
    private String journal;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("page")
    private String page;

    @JsonProperty("publication_date")
    private String publicationDate;

    @JsonProperty("publisher")
    private String publisher;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("title")
    private String title;

    @JsonProperty("used_endpoint")
    private String usedEndpoint;

    @JsonProperty("volume")
    private String volume;

    // 错误信息字段（非API返回，用于内部处理）
    private String errorMessage;
}