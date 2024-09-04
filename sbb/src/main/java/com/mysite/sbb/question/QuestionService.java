package com.mysite.sbb.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.user.SiteUser;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuestionService {

	private final QuestionRepository questionRepository;

	@SuppressWarnings("unused")
	private Specification<Question> search(String kw) {
		return new Specification<>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(true); // 중복을 제거
				Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
				Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
				Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
				return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목
						cb.like(q.get("content"), "%" + kw + "%"), // 내용
						cb.like(u1.get("username"), "%" + kw + "%"), // 질문 작성자
						cb.like(a.get("content"), "%" + kw + "%"), // 답변 내용
						cb.like(u2.get("username"), "%" + kw + "%")); // 답변 작성자
			}
		};
	}

	public Page<Question> getList(int page, String kw) {
		log.info("Fetching questions list - page: {}, keyword: {}", page, kw);
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
		return this.questionRepository.findAllByKeyword(kw, pageable);
	}

	public Question getQuestion(Integer id) {
		log.info("Fetching question with ID: {}", id);
		Optional<Question> question = this.questionRepository.findById(id);
		if (question.isPresent()) {
			return question.get();
		} else {
			log.error("Question with ID: {} not found", id);
			throw new DataNotFoundException("question not found");
		}
	}

	public Question create(String subject, String content, SiteUser user) {
		log.info("Creating question - Subject: {}, Content: {}, User: {}", subject, content, user.getUsername());
		Question q = new Question();
		q.setSubject(subject);
		q.setContent(content);
		q.setCreateDate(LocalDateTime.now());
		q.setAuthor(user);
		try {
			Question savedQuestion = this.questionRepository.save(q);
			log.info("Question saved successfully with ID: {}", savedQuestion.getId());
			return savedQuestion;
		} catch (Exception e) {
			log.error("Failed to save question", e);
			throw e;
		}
	}

	public void modify(Question question, String subject, String content) {
		log.info("Modifying question ID: {} - New Subject: {}, New Content: {}", question.getId(), subject, content);
		question.setSubject(subject);
		question.setContent(content);
		question.setModifyDate(LocalDateTime.now());
		this.questionRepository.save(question);
		log.info("Question modified successfully with ID: {}", question.getId());
	}

	public void delete(Question question) {
		log.info("Deleting question with ID: {}", question.getId());
		this.questionRepository.delete(question);
		log.info("Question deleted successfully with ID: {}", question.getId());
	}

	public void vote(Question question, SiteUser siteUser) {
		log.info("User {} voting on question ID: {}", siteUser.getUsername(), question.getId());
		question.getVoter().add(siteUser);
		this.questionRepository.save(question);
		log.info("Vote recorded successfully for question ID: {}", question.getId());
	}
}
