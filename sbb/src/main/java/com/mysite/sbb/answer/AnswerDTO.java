package com.mysite.sbb.answer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnswerDTO {
    private Integer id;
    private String content;
    private LocalDateTime createDate;
    private String authorUsername;
    private LocalDateTime modifyDate;
}
