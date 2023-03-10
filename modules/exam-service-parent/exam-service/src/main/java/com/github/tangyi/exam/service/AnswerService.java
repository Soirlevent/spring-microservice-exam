package com.github.tangyi.exam.service;

import com.github.pagehelper.PageInfo;
import com.github.tangyi.common.core.constant.CommonConstant;
import com.github.tangyi.common.core.constant.MqConstant;
import com.github.tangyi.common.core.exceptions.CommonException;
import com.github.tangyi.common.core.model.ResponseBean;
import com.github.tangyi.common.core.service.CrudService;
import com.github.tangyi.common.core.utils.JsonMapper;
import com.github.tangyi.common.core.utils.PageUtil;
import com.github.tangyi.common.core.utils.ResponseUtil;
import com.github.tangyi.common.core.utils.SysUtil;
import com.github.tangyi.common.core.vo.UserVo;
import com.github.tangyi.exam.api.constants.AnswerConstant;
import com.github.tangyi.exam.api.dto.AnswerDto;
import com.github.tangyi.exam.api.dto.RankInfoDto;
import com.github.tangyi.exam.api.dto.StartExamDto;
import com.github.tangyi.exam.api.dto.SubjectDto;
import com.github.tangyi.exam.api.enums.SubmitStatusEnum;
import com.github.tangyi.exam.api.module.Answer;
import com.github.tangyi.exam.api.module.Examination;
import com.github.tangyi.exam.api.module.ExaminationRecord;
import com.github.tangyi.exam.api.module.ExaminationSubject;
import com.github.tangyi.exam.enums.SubjectTypeEnum;
import com.github.tangyi.exam.handler.AnswerHandleResult;
import com.github.tangyi.exam.handler.impl.ChoicesAnswerHandler;
import com.github.tangyi.exam.handler.impl.JudgementAnswerHandler;
import com.github.tangyi.exam.handler.impl.MultipleChoicesAnswerHandler;
import com.github.tangyi.exam.handler.impl.ShortAnswerHandler;
import com.github.tangyi.exam.mapper.AnswerMapper;
import com.github.tangyi.exam.utils.AnswerHandlerUtil;
import com.github.tangyi.exam.utils.ExamRecordUtil;
import com.github.tangyi.user.api.feign.UserServiceClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ??????service
 *
 * @author tangyi
 * @date 2018/11/8 21:17
 */
@Slf4j
@AllArgsConstructor
@Service
public class AnswerService extends CrudService<AnswerMapper, Answer> {

	private final UserServiceClient userServiceClient;

    private final AmqpTemplate amqpTemplate;

    private final SubjectService subjectService;

    private final ExamRecordService examRecordService;

    private final ExaminationService examinationService;

    private final ExaminationSubjectService examinationSubjectService;

    private final ChoicesAnswerHandler choicesHandler;

	private final MultipleChoicesAnswerHandler multipleChoicesHandler;

	private final JudgementAnswerHandler judgementHandler;

	private final ShortAnswerHandler shortAnswerHandler;

	private final RedisTemplate<String, String> redisTemplate;

	/**
     * ????????????
     *
     * @param answer answer
     * @return Answer
     * @author tangyi
     * @date 2019/1/3 14:27
     */
    @Override
    @Cacheable(value = "answer#" + CommonConstant.CACHE_EXPIRE, key = "#answer.id")
    public Answer get(Answer answer) {
        return super.get(answer);
    }

    /**
     * ????????????ID?????????ID???????????????ID?????????ID????????????
     *
     * @param answer answer
     * @return Answer
     * @author tangyi
     * @date 2019/01/21 19:41
     */
    public Answer getAnswer(Answer answer) {
        return this.dao.getAnswer(answer);
    }

    /**
     * ????????????
     *
     * @param answer answer
     * @return int
     * @author tangyi
     * @date 2019/1/3 14:27
     */
    @Override
    @Transactional
    @CacheEvict(value = "answer", key = "#answer.id")
    public int update(Answer answer) {
    	answer.setAnswer(AnswerHandlerUtil.replaceComma(answer.getAnswer()));
        return super.update(answer);
    }

    /**
     * ??????????????????
     *
     * @param answer answer
     * @return int
     * @author tangyi
     * @date 2019/1/3 14:27
     */
    @Transactional
    @CacheEvict(value = "answer", key = "#answer.id")
    public int updateScore(Answer answer) {
        answer.setAnswer(AnswerHandlerUtil.replaceComma(answer.getAnswer()));
        // ??????????????????
        Answer oldAnswer = this.get(answer);
        if (!oldAnswer.getAnswerType().equals(answer.getAnswerType())) {
            ExaminationRecord record = new ExaminationRecord();
            record.setId(oldAnswer.getExamRecordId());
            record = examRecordService.get(record);
            if (record == null) {
                throw new CommonException("ExamRecord is null");
            }
            Double oldScore = record.getScore();
            if (AnswerConstant.RIGHT.equals(answer.getAnswerType())) {
                // ??????
                record.setCorrectNumber(record.getInCorrectNumber() + 1);
                record.setInCorrectNumber(record.getInCorrectNumber() - 1);
                record.setScore(record.getScore() + answer.getScore());
            } else if (AnswerConstant.WRONG.equals(answer.getAnswerType())) {
                // ??????
                record.setCorrectNumber(record.getInCorrectNumber() - 1);
                record.setInCorrectNumber(record.getInCorrectNumber() + 1);
                record.setScore(record.getScore() - answer.getScore());
            }
            if (examRecordService.update(record) > 0) {
                log.info("Update answer success, examRecordId: {}, oldScore: {}, newScore: {}", oldAnswer.getExamRecordId(), oldScore, record.getScore());
            }
        }
        return super.update(answer);
    }

    /**
     * ????????????
     *
     * @param answer answer
     * @return int
     * @author tangyi
     * @date 2019/1/3 14:27
     */
    @Override
    @Transactional
    @CacheEvict(value = "answer", key = "#answer.id")
    public int delete(Answer answer) {
        return super.delete(answer);
    }

    /**
     * ??????????????????
     *
     * @param ids ids
     * @return int
     * @author tangyi
     * @date 2019/1/3 14:27
     */
    @Override
    @Transactional
    @CacheEvict(value = "answer", allEntries = true)
    public int deleteAll(Long[] ids) {
        return super.deleteAll(ids);
    }

    /**
     * ??????
     *
     * @param answer answer
     * @return int
     * @author tangyi
     * @date 2019/04/30 18:03
     */
    @Transactional
    public int save(Answer answer) {
        answer.setCommonValue(SysUtil.getUser(), SysUtil.getSysCode(), SysUtil.getTenantCode());
		answer.setAnswer(AnswerHandlerUtil.replaceComma(answer.getAnswer()));
		return super.save(answer);
    }

    /**
     * ????????????????????????????????????
     *
     * @param answerDto       answerDto
     * @param type            0???????????????1????????????
     * @param nextSubjectId   nextSubjectId
     * @param nextSubjectType ??????????????????????????????????????????
     * @return SubjectDto
     * @author tangyi
     * @date 2019/05/01 11:42
     */
    @Transactional
    public SubjectDto saveAndNext(AnswerDto answerDto, Integer type, Long nextSubjectId, Integer nextSubjectType) {
		String userCode = SysUtil.getUser();
		String sysCode = SysUtil.getSysCode();
		String tenantCode = SysUtil.getTenantCode();
		if (this.save(answerDto, userCode, sysCode, tenantCode) > 0) {
			// ???????????????
			return this.subjectAnswer(answerDto.getSubjectId(), answerDto.getExamRecordId(),
					type, nextSubjectId, nextSubjectType);
		}
        return null;
    }

	/**
	 * ????????????
	 *
	 * @param answerDto       answerDto
	 * @param userCode       userCode
	 * @param sysCode       sysCode
	 * @param tenantCode       tenantCode
	 * @return int
	 * @author tangyi
	 * @date 2019/05/01 11:42
	 */
	@Transactional
	public int save(AnswerDto answerDto, String userCode, String sysCode, String tenantCode) {
		Answer answer = new Answer();
		BeanUtils.copyProperties(answerDto, answer);
		Answer tempAnswer = this.getAnswer(answer);
		if (tempAnswer != null) {
			tempAnswer.setCommonValue(userCode, sysCode, tenantCode);
			tempAnswer.setAnswer(answer.getAnswer());
			tempAnswer.setType(answer.getType());
			tempAnswer.setEndTime(tempAnswer.getModifyDate());
			return this.update(tempAnswer);
		} else {
			answer.setCommonValue(userCode, sysCode, tenantCode);
			answer.setMarkStatus(AnswerConstant.TO_BE_MARKED);
			answer.setAnswerType(AnswerConstant.WRONG);
			answer.setEndTime(answer.getModifyDate());
			return this.insert(answer);
		}
	}

    /**
     * ??????????????????????????????????????????
     *
     * @param answer answer
     * @author tangyi
     * @date 2018/12/26 14:09
     */
    @Transactional
    public void submit(Answer answer) {
        long start = System.currentTimeMillis();
        String currentUsername = answer.getModifier();
        // ????????????????????????
        List<Answer> answerList = findList(answer);
        if (CollectionUtils.isEmpty(answerList))
            return;
        // ??????
        ExaminationRecord record = new ExaminationRecord();
        // ????????????
        Map<String, List<Answer>> distinctAnswer = this.distinctAnswer(answerList);
        // ???????????????????????????????????????????????????????????????????????????????????????
		AnswerHandleResult choiceResult = choicesHandler.handle(distinctAnswer.get(SubjectTypeEnum.CHOICES.name()));
		AnswerHandleResult multipleResult = multipleChoicesHandler.handle(distinctAnswer.get(SubjectTypeEnum.MULTIPLE_CHOICES.name()));
		AnswerHandleResult judgementResult = judgementHandler.handle(distinctAnswer.get(SubjectTypeEnum.JUDGEMENT.name()));
        AnswerHandleResult shortAnswerResult = shortAnswerHandler.handle(distinctAnswer.get(SubjectTypeEnum.SHORT_ANSWER.name()));
		AnswerHandleResult result = AnswerHandlerUtil.addAll(Arrays.asList(choiceResult, multipleResult, judgementResult, shortAnswerResult));
		// ????????????????????????????????????????????????
		record.setScore(result.getScore());
		record.setCorrectNumber(result.getCorrectNum());
		record.setInCorrectNumber(result.getInCorrectNum());
		// ??????????????????
		distinctAnswer.values().forEach(answers -> answers.forEach(this::update));
        // ??????????????????????????????????????????????????????????????????????????????
        record.setSubmitStatus(SubmitStatusEnum.CALCULATED.getValue());
        // ????????????
        record.setCommonValue(currentUsername, SysUtil.getSysCode());
        record.setId(answer.getExamRecordId());
        record.setEndTime(record.getCreateDate());
        examRecordService.update(record);
        // ??????????????????
		updateRank(record);
        log.debug("Submit examination, username: {}???time consuming: {}ms", currentUsername, System.currentTimeMillis() - start);
    }

    /**
     * ??????????????????
	 * ??????Redis???sort set????????????
     * @param record record
     * @author tangyi
     * @date 2019/12/8 23:21
     */
    private void updateRank(ExaminationRecord record) {
		redisTemplate.opsForZSet().add(AnswerConstant.CACHE_PREFIX_RANK + record.getExaminationId(), JsonMapper.getInstance().toJson(record), record.getScore());
	}

    /**
     * ??????mq????????????
     * 1. ???????????????
     * 2. ???????????????????????????????????????????????????????????????????????????
     * 3. ???????????????????????????
     *
     * @param answer answer
     * @return boolean
     * @author tangyi
     * @date 2019/05/03 14:35
     */
    @Transactional
    public boolean submitAsync(Answer answer) {
        long start = System.currentTimeMillis();
        String currentUsername = SysUtil.getUser();
        String applicationCode = SysUtil.getSysCode();
        String tenantCode = SysUtil.getTenantCode();
        answer.setModifier(currentUsername);
        answer.setApplicationCode(applicationCode);
        answer.setTenantCode(tenantCode);

        ExaminationRecord examRecord = new ExaminationRecord();
        examRecord.setCommonValue(currentUsername, applicationCode, tenantCode);
        examRecord.setId(answer.getExamRecordId());
        // ????????????
        examRecord.setEndTime(examRecord.getCreateDate());
        examRecord.setSubmitStatus(SubmitStatusEnum.SUBMITTED.getValue());
        // 1. ????????????
        amqpTemplate.convertAndSend(MqConstant.SUBMIT_EXAMINATION_QUEUE, answer);
        // 2. ??????????????????
        boolean success = examRecordService.update(examRecord) > 0;
		log.debug("Submit examination, username: {}???time consuming: {}ms", currentUsername, System.currentTimeMillis() - start);
		return success;
    }

    /**
     * ????????????
     *
     * @param examRecord examRecord
     * @return StartExamDto
     * @author tangyi
     * @date 2019/04/30 23:06
     */
    @Transactional
    public StartExamDto start(ExaminationRecord examRecord) {
        StartExamDto startExamDto = new StartExamDto();
        String currentUsername = SysUtil.getUser();
        String applicationCode = SysUtil.getSysCode();
        String tenantCode = SysUtil.getTenantCode();
        // ??????????????????
        if (examRecord.getExaminationId() == null)
            throw new CommonException("???????????????????????????id?????????");
        if (examRecord.getUserId() == null)
            throw new CommonException("???????????????????????????id?????????");
		// ??????????????????
        Examination examination = examinationService.get(examRecord.getExaminationId());
        examRecord.setCommonValue(currentUsername, applicationCode, tenantCode);
        examRecord.setStartTime(examRecord.getCreateDate());
        // ?????????????????????
        examRecord.setSubmitStatus(SubmitStatusEnum.NOT_SUBMITTED.getValue());
        // ??????????????????
        if (examRecordService.insert(examRecord) > 0) {
            startExamDto.setExamination(examination);
            startExamDto.setExamRecord(examRecord);
            // ????????????ID???????????????????????????????????????
            SubjectDto subjectDto = subjectService.findFirstSubjectByExaminationId(examRecord.getExaminationId());
            startExamDto.setSubjectDto(subjectDto);
            // ????????????????????????
            Answer answer = new Answer();
            answer.setCommonValue(currentUsername, applicationCode, tenantCode);
            answer.setExamRecordId(examRecord.getId());
            answer.setSubjectId(subjectDto.getId());
            // ?????????????????????
            answer.setMarkStatus(AnswerConstant.TO_BE_MARKED);
            answer.setAnswerType(AnswerConstant.WRONG);
            answer.setStartTime(answer.getCreateDate());
            // ????????????
            this.save(answer);
            subjectDto.setAnswer(answer);
        }
        return startExamDto;
    }

    /**
     * ?????????????????????
     *
     * @param subjectId       subjectId
     * @param examRecordId    examRecordId
     * @param nextType        -1??????????????????0???????????????1????????????
     * @param nextSubjectId   nextSubjectId
     * @param nextSubjectType ??????????????????????????????????????????
     * @return SubjectDto
     * @author tangyi
     * @date 2019/04/30 17:10
     */
    @Transactional
    public SubjectDto subjectAnswer(Long subjectId, Long examRecordId, Integer nextType, Long nextSubjectId, Integer nextSubjectType) {
		// ??????????????????
    	ExaminationRecord examRecord = examRecordService.get(examRecordId);
        if (examRecord == null)
            throw new CommonException("?????????????????????.");

        // ??????ID?????????ID??????????????????
        ExaminationSubject examinationSubject = new ExaminationSubject();
        examinationSubject.setExaminationId(examRecord.getExaminationId());
        examinationSubject.setSubjectId(subjectId);
        PageInfo<ExaminationSubject> examinationSubjectPageInfo = examinationSubjectService.findPage(
                PageUtil.pageInfo(CommonConstant.PAGE_NUM_DEFAULT, CommonConstant.PAGE_SIZE_DEFAULT, "id",
                        CommonConstant.PAGE_ORDER_DEFAULT), examinationSubject);
        if (CollectionUtils.isEmpty(examinationSubjectPageInfo.getList()))
            throw new CommonException("?????????" + subjectId + "??????????????????.");

        // ???????????????
        SubjectDto subject;
        if (nextSubjectId != null) {
            subject = subjectService.get(nextSubjectId, nextSubjectType);
        } else {
            subject = subjectService.getNextByCurrentIdAndType(examRecord.getExaminationId(), subjectId, examinationSubjectPageInfo.getList().get(0).getType(), nextType);
        }
        if (subject == null) {
            log.error("Subject does not exist: {}", subjectId);
            return null;
        }

        // ????????????
        Answer answer = new Answer();
        answer.setSubjectId(subject.getId());
        answer.setExamRecordId(examRecordId);
        Answer userAnswer = this.getAnswer(answer);
        userAnswer = userAnswer == null ? new Answer() : userAnswer;
        // ????????????
        subject.setAnswer(userAnswer);
        subject.setExaminationRecordId(examRecordId);
        return subject;
    }

    /**
     * ????????????
     *
     * @param answers answers
     * @return Map
     * @author tangyi
     * @date 2019/06/18 16:32
     */
    private Map<String, List<Answer>> distinctAnswer(List<Answer> answers) {
        Map<String, List<Answer>> distinctMap = new HashMap<>();
        answers.stream().collect(Collectors.groupingBy(Answer::getType, Collectors.toList())).forEach((type, temp) -> {
            // ????????????
            SubjectTypeEnum subjectType = SubjectTypeEnum.matchByValue(type);
            if (subjectType != null) {
                switch (subjectType) {
                    case CHOICES:
                        distinctMap.put(SubjectTypeEnum.CHOICES.name(), temp);
                        break;
					case MULTIPLE_CHOICES:
						distinctMap.put(SubjectTypeEnum.MULTIPLE_CHOICES.name(), temp);
						break;
                    case SHORT_ANSWER:
                        distinctMap.put(SubjectTypeEnum.SHORT_ANSWER.name(), temp);
                        break;
					case JUDGEMENT:
						distinctMap.put(SubjectTypeEnum.JUDGEMENT.name(), temp);
						break;
					default:
						break;
                }
            }
        });
        return distinctMap;
    }

    /**
     * ????????????
     *
     * @param recordId         recordId
     * @param currentSubjectId currentSubjectId
     * @param nextSubjectType  nextSubjectType
     * @param nextType         nextType
     * @return AnswerDto
     * @author tangyi
     * @date 2019/06/18 23:05
     */
    public AnswerDto answerInfo(Long recordId, Long currentSubjectId, Integer nextSubjectType, Integer nextType) {
        ExaminationRecord record = examRecordService.get(recordId);
        SubjectDto subjectDto;
        // ?????????????????????????????????
        if (currentSubjectId == null) {
            subjectDto = subjectService.findFirstSubjectByExaminationId(record.getExaminationId());
        } else {
            ExaminationSubject examinationSubject = new ExaminationSubject();
            examinationSubject.setExaminationId(record.getExaminationId());
            examinationSubject.setSubjectId(currentSubjectId);

            // ??????????????????????????????????????????????????????
            // ?????????
            if (AnswerConstant.NEXT.equals(nextType)) {
                examinationSubject = examinationSubjectService.getByPreviousId(examinationSubject);
            } else if (AnswerConstant.PREVIOUS.equals(nextType)) {
                // ?????????
                examinationSubject = examinationSubjectService.getPreviousByCurrentId(examinationSubject);
            } else {
                examinationSubject = examinationSubjectService.findByExaminationIdAndSubjectId(examinationSubject);
            }
            if (examinationSubject == null)
                throw new CommonException("ID???" + currentSubjectId + "??????????????????");
            // ???????????????????????????
            subjectDto = subjectService.get(examinationSubject.getSubjectId(), examinationSubject.getType());
        }
        AnswerDto answerDto = new AnswerDto();
        answerDto.setSubject(subjectDto);
        // ????????????
        Answer answer = new Answer();
        answer.setSubjectId(subjectDto.getId());
        answer.setExamRecordId(recordId);
        Answer userAnswer = this.getAnswer(answer);
        if (userAnswer == null)
            userAnswer = answer;
        BeanUtils.copyProperties(userAnswer, answerDto);
        answerDto.setDuration(ExamRecordUtil.getExamDuration(userAnswer.getStartTime(), userAnswer.getEndTime()));
        // ????????????
        SubjectTypeEnum subjectType = SubjectTypeEnum.matchByValue(subjectDto.getType());
        if (subjectType != null) {
            switch (subjectType) {
                case CHOICES:
                    choicesHandler.judgeOptionRight(userAnswer, subjectDto);
                    break;
                case MULTIPLE_CHOICES:
                    multipleChoicesHandler.judgeOptionRight(userAnswer, subjectDto);
                    break;
                case SHORT_ANSWER:
                    shortAnswerHandler.judgeRight(userAnswer, subjectDto);
                    break;
                case JUDGEMENT:
                    judgementHandler.judgeRight(userAnswer, subjectDto);
                    break;
                default:
                    break;
            }
        }
        ResponseBean<List<UserVo>> userVoResponseBean = userServiceClient.findUserById(new Long[] {record.getUserId()});
        if (ResponseUtil.isSuccess(userVoResponseBean) && CollectionUtils.isNotEmpty(userVoResponseBean.getData())) {
            UserVo userVo = userVoResponseBean.getData().get(0);
            answerDto.setUserName(userVo.getName());
        }
        return answerDto;
    }

    /**
     * ????????????
     *
     * @param examRecord examRecord
     * @return Boolean
     * @author tangyi
     * @date 2019/06/19 14:44
     */
    public Boolean completeMarking(ExaminationRecord examRecord) {
        long start = System.currentTimeMillis();
        examRecord = examRecordService.get(examRecord);
        if (examRecord == null)
            throw new CommonException("?????????????????????.");
        Answer answer = new Answer();
        answer.setExamRecordId(examRecord.getId());
        List<Answer> answers = this.findList(answer);
        if (CollectionUtils.isNotEmpty(answers)) {
            long correctNumber = answers.stream()
                    .filter(tempAnswer -> tempAnswer.getAnswerType().equals(AnswerConstant.RIGHT)).count();
            // ??????
            Double score = answers.stream().mapToDouble(Answer::getScore).sum();
            examRecord.setScore(score);
            examRecord.setSubmitStatus(SubmitStatusEnum.CALCULATED.getValue());
            examRecord.setCorrectNumber((int) correctNumber);
            examRecord.setInCorrectNumber(answers.size() - examRecord.getCorrectNumber());
            examRecordService.update(examRecord);
            log.debug("Submit done, username: {}, examinationId: {}, score: {}, time consuming: {}ms", examRecord.getCreator(), examRecord.getExaminationId(),
                    score, System.currentTimeMillis() - start);
        }
        return Boolean.TRUE;
    }

    /**
     * ??????????????????
     * @param recordId recordId
     * @return List
     * @author tangyi
     * @date 2019/12/8 23:36
     */
	public List<RankInfoDto> getRankInfo(Long recordId) {
		List<RankInfoDto> rankInfos = new ArrayList<>();
		// ????????????
		Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
				.reverseRangeByScoreWithScores(AnswerConstant.CACHE_PREFIX_RANK + recordId, 0, Integer.MAX_VALUE);
		if (typedTuples != null) {
			// ??????ID??????
			Set<Long> userIds = new HashSet<>();
			typedTuples.forEach(typedTuple -> {
				ExaminationRecord record = JsonMapper.getInstance()
						.fromJson(typedTuple.getValue(), ExaminationRecord.class);
				if (record != null) {
					RankInfoDto rankInfo = new RankInfoDto();
					rankInfo.setUserId(record.getUserId());
					userIds.add(record.getUserId());
					rankInfo.setScore(typedTuple.getScore());
					rankInfos.add(rankInfo);
				}
			});
			if (!userIds.isEmpty()) {
				ResponseBean<List<UserVo>> userResponse = userServiceClient.findUserById(userIds.toArray(new Long[0]));
				if (ResponseUtil.isSuccess(userResponse)) {
					rankInfos.forEach(rankInfo -> {
						userResponse.getData().stream().filter(user -> user.getId().equals(rankInfo.getUserId()))
								.findFirst().ifPresent(user -> {
							// ??????????????????
							rankInfo.setName(user.getName());
							rankInfo.setAvatarUrl(user.getAvatarUrl());
						});
					});
				}
			}
		}
		return rankInfos;
	}

    /**
     * ??????????????????
     *
     * @param pageNum  pageNum
     * @param pageSize pageSize
     * @param sort     sort
     * @param order    order
     * @param recordId recordId
     * @param answer   answer
     * @return List
     * @author tangyi
     * @date 2020/02/19 22:50
     */
	public PageInfo<AnswerDto> answerListInfo(String pageNum, String pageSize, String sort, String order, Long recordId, Answer answer) {
        List<AnswerDto> answerDtos = new ArrayList<>();
        answer.setExamRecordId(recordId);
        PageInfo<Answer> answerPageInfo = this.findPage(PageUtil.pageInfo(pageNum, pageSize, sort, order), answer);
        if (CollectionUtils.isNotEmpty(answerPageInfo.getList())) {
            answerDtos = answerPageInfo.getList().stream().map(tempAnswer -> {
                AnswerDto answerDto = new AnswerDto();
                BeanUtils.copyProperties(tempAnswer, answerDto);
                SubjectDto subjectDto = subjectService.get(tempAnswer.getSubjectId(), tempAnswer.getType());
                answerDto.setSubject(subjectDto);
                // ????????????
                SubjectTypeEnum subjectType = SubjectTypeEnum.matchByValue(subjectDto.getType());
                if (subjectType != null) {
                    switch (subjectType) {
                        case CHOICES:
                            choicesHandler.judgeOptionRight(tempAnswer, subjectDto);
                            break;
                        case MULTIPLE_CHOICES:
                            multipleChoicesHandler.judgeOptionRight(tempAnswer, subjectDto);
                            break;
                        case SHORT_ANSWER:
                            shortAnswerHandler.judgeRight(tempAnswer, subjectDto);
                            break;
                        case JUDGEMENT:
                            judgementHandler.judgeRight(tempAnswer, subjectDto);
                            break;
                        default:
                            break;
                    }
                }
                return answerDto;
            }).collect(Collectors.toList());
        }
        PageInfo<AnswerDto> answerDtoPageInfo = new PageInfo<>();
        answerDtoPageInfo.setList(answerDtos);
        answerDtoPageInfo.setTotal(answerPageInfo.getTotal());
        answerDtoPageInfo.setPageNum(answerPageInfo.getPageNum());
        answerDtoPageInfo.setPageSize(answerPageInfo.getPageSize());
        return answerDtoPageInfo;
    }

	/**
	 * ??????examRecordId??????
	 * @param examRecordId examRecordId
	 * @return List
	 * @author tangyi
	 * @date 2020/2/21 1:08 ??????
	 */
	public List<Answer> findListByExamRecordId(Long examRecordId) {
		return this.dao.findListByExamRecordId(examRecordId);
	}
}
