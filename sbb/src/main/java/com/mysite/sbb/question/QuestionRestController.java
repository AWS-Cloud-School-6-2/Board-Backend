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
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@RestController
public class QuestionRestController {

    private final QuestionService questionService;
    private final UserService userService;

    // 전체 질문 목록 조회
    @GetMapping("/")
    public ResponseEntity<Page<QuestionDTO>> getQuestionsWithSlash(@RequestParam(value = "page", defaultValue = "0") int page,
                                                                   @RequestParam(value = "kw", defaultValue = "") String kw) {
        log.info("Received request to get questions - page: {}, kw: {}", page, kw);
        Page<Question> paging = this.questionService.getList(page, kw);
        Page<QuestionDTO> dtoPaging = paging.map(this::toDTO);
        log.info("Returning {} questions", dtoPaging.getTotalElements());
        return new ResponseEntity<>(dtoPaging, HttpStatus.OK);
    }

    // 특정 질문 조회
    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestion(@PathVariable("id") Integer id) {
        log.info("Received request to get question with ID: {}", id);
        Question question = this.questionService.getQuestion(id);
        QuestionDTO questionDTO = toDTO(question);
        return new ResponseEntity<>(questionDTO, HttpStatus.OK);
    }

    // 질문 생성
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<QuestionDTO> createQuestion(@Valid @RequestBody QuestionForm questionForm, Principal principal) {
        log.info("Received request to create question - Subject: {}, Content: {}", questionForm.getSubject(), questionForm.getContent());
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Question question = this.questionService.create(questionForm.getSubject(), questionForm.getContent(), siteUser);
        QuestionDTO questionDTO = toDTO(question);
        log.info("Question created successfully with ID: {}", question.getId());
        return new ResponseEntity<>(questionDTO, HttpStatus.CREATED);
    }

    // 질문 수정
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(@Valid @RequestBody QuestionForm questionForm,
                                                      Principal principal,
                                                      @PathVariable("id") Integer id) {
        log.info("Received request to update question with ID: {}", id);
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            log.warn("Unauthorized attempt to update question ID: {}", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        QuestionDTO questionDTO = toDTO(question);
        log.info("Question updated successfully with ID: {}", id);
        return new ResponseEntity<>(questionDTO, HttpStatus.OK);
    }

    // 질문 삭제
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(Principal principal, @PathVariable("id") Integer id) {
        log.info("Received request to delete question with ID: {}", id);
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            log.warn("Unauthorized attempt to delete question ID: {}", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        this.questionService.delete(question);
        log.info("Question deleted successfully with ID: {}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 질문에 대한 투표
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> voteQuestion(Principal principal, @PathVariable("id") Integer id) {
        log.info("Received request to vote on question with ID: {}", id);
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.vote(question, siteUser);
        log.info("Vote recorded successfully for question ID: {}", id);
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
