package com.mysite.sbb.question;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import com.mysite.sbb.answer.Answer;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mysite.sbb.answer.AnswerDTO;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequestMapping("/api/question")
@RequiredArgsConstructor
@RestController
public class QuestionRestController {

    private final QuestionService questionService;
    private final UserService userService;

    @GetMapping("/list")
    public ResponseEntity<Page<QuestionDTO>> list(@RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "kw", defaultValue = "") String kw) {
        log.info("page:{}, kw:{}", page, kw);
        Page<Question> paging = this.questionService.getList(page, kw);
        Page<QuestionDTO> dtoPaging = paging.map(this::toDTO);
        return new ResponseEntity<>(dtoPaging, HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<QuestionDTO> detail(@PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        QuestionDTO questionDTO = toDTO(question);
        return new ResponseEntity<>(questionDTO, HttpStatus.OK);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public ResponseEntity<QuestionDTO> questionCreate(@Valid @RequestBody QuestionForm questionForm, Principal principal) {
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Question question = this.questionService.create(questionForm.getSubject(), questionForm.getContent(), siteUser);
        QuestionDTO questionDTO = toDTO(question);
        return new ResponseEntity<>(questionDTO, HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/modify/{id}")
    public ResponseEntity<QuestionDTO> questionModify(@Valid @RequestBody QuestionForm questionForm,
                                                      Principal principal,
                                                      @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        QuestionDTO questionDTO = toDTO(question);
        return new ResponseEntity<>(questionDTO, HttpStatus.OK);
    }


    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> questionDelete(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.questionService.delete(question);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/vote/{id}")
    public ResponseEntity<Void> questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.vote(question, siteUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private QuestionDTO toDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setSubject(question.getSubject());
        dto.setContent(question.getContent());
        dto.setCreateDate(question.getCreateDate());
        dto.setModifyDate(question.getModifyDate());

        // Null check for author
        if (question.getAuthor() != null) {
            dto.setAuthorUsername(question.getAuthor().getUsername());
        } else {
            dto.setAuthorUsername("Anonymous"); // or any default value you prefer
        }

        dto.setAnswerList(question.getAnswerList().stream().map(this::toDTO).collect(Collectors.toList()));
        return dto;
    }

    private AnswerDTO toDTO(Answer answer) {
        AnswerDTO dto = new AnswerDTO();
        dto.setId(answer.getId());
        dto.setContent(answer.getContent());
        dto.setCreateDate(answer.getCreateDate());
        dto.setModifyDate(answer.getModifyDate());

        // Null check for author
        if (answer.getAuthor() != null) {
            dto.setAuthorUsername(answer.getAuthor().getUsername());
        } else {
            dto.setAuthorUsername("Anonymous"); // or any default value you prefer
        }

        return dto;
    }
}
