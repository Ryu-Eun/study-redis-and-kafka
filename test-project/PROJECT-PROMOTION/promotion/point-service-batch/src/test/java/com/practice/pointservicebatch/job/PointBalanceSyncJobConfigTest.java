package com.practice.pointservicebatch.job;



import com.practice.pointservicebatch.repository.DailyPointReportRepository;
import com.practice.pointservicebatch.repository.PointBalanceRepository;
import com.practice.pointservicebatch.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.batch.job.enabled=false"
})
class PointBalanceSyncJobConfigTest {

    // org.springframework.batch.test에서 제공하는 기본 테스트 클래스
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private PointRepository pointRepository;

    @MockBean
    private PointBalanceRepository pointBalanceRepository;

    @Autowired
    private DailyPointReportRepository dailyPointReportRepository;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private RMap<String, Long> balanceMap;

    @BeforeEach
    void setUp() {
        // Redis mock 설정
        when(redissonClient.<String, Long>getMap(anyString())).thenReturn(balanceMap);

        // 테스트 데이터 초기화
        dailyPointReportRepository.deleteAll();

    }

    @Test
    @DisplayName("포인트 동기화 Job 실행 성공 테스트")
    void jobExecutionTest() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    @DisplayName("Redis 캐시 동기화 Step 테스트")
    void syncPointBalanceStepTest() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        // when
        JobExecution jobExection = jobLauncherTestUtils.launchStep("syncPointBalanceStep", jobParameters);

        // then
        assertThat(jobExection.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExection.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("일별 리포트 생성 Step 테스트")
    void generateDeilyReportStepTest() throws Exception{
        //given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("generateDailyReportStep", jobParameters);

        //then
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    }

}