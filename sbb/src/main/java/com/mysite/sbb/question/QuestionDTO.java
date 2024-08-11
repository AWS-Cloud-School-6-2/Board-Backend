package com.mysite.sbb.question;

import com.mysite.sbb.answer.AnswerDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class QuestionDTO {
    private Integer id;
    private String subject;
    private String content;
    private LocalDateTime createDate;
    private String authorUsername;
    private LocalDateTime modifyDate;
    private List<AnswerDTO> answerList;
}
