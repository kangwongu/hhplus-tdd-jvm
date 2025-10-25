package io.hhplus.tdd.point

import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * 이렇게 나누는게 맞는지 모르겠다..
 * [PointServiceTest] 단위 테스트, [PointConcurrencyTest] 통합(동시성) 테스트??
 * 하나의 Service 를 테스트하는데 두개의 테스트 클래스로 나누는게 맞는건가??
 */
@SpringBootTest
class PointConcurrencyTest @Autowired constructor(
    private val pointService: PointService,
    private val userPointTable: UserPointTable,
) {

    @Test
    @DisplayName("동시에 여러번 포인트를 충전하는 경우에도 정상적으로 충전된다")
    fun chargePointConcurrency() {
        // given
        val requestCount = 100
        val executorService = Executors.newFixedThreadPool(10) // 쓰레드 풀 10개로 가정
        val latch = CountDownLatch(requestCount) // 모든 작업이 완료될 때까지 대기하기 위한 래치
        val toChargeAmount = 100L

        // when
        // 동시에 100개의 요청이 들어와서 각각 100 포인트씩 충전하는 상황 가정
        for (i in 1..requestCount) {
            executorService.submit {
                try {
                    pointService.chargePoint(1L, toChargeAmount) // 사용자 ID 1번에게 100포인트씩 충전 가정
                } finally {
                    latch.countDown() // 작업 완료 시 래치 카운트 다운
                }
            }
        }
        latch.await() // 모든 작업이 완료될 때까지 대기

        // then
        val result = userPointTable.selectById(1L)
        assertThat(result.point).isEqualTo(toChargeAmount * requestCount)
    }

    @Test
    @DisplayName("동시에 여러번 포인트를 사용하는 경우에도 정상적으로 차감된다")
    fun usePointConcurrency() {
        // given
        val requestCount = 100
        val executorService = Executors.newFixedThreadPool(10) // 쓰레드 풀 10개로 가정
        val latch = CountDownLatch(requestCount) // 모든 작업이 완료될 때까지 대기하기 위한 래치
        val toUseAmount = 100L

        // 먼저 충분한 포인트 충전
        userPointTable.insertOrUpdate(1L, 10000L)

        // when
        // 동시에 100개의 요청이 들어와서 각각 100 포인트씩 사용하는 상황 가정
        for (i in 1..requestCount) {
            executorService.submit {
                try {
                    pointService.usePoint(1L, toUseAmount) // 사용자 ID 1번에게 100포인트씩 사용 가정
                } finally {
                    latch.countDown() // 작업 완료 시 래치 카운트 다운
                }
            }
        }
        latch.await() // 모든 작업이 완료될 때까지 대기

        // then
        val result = pointService.readUserPoint(1L)
        assertThat(result.point).isEqualTo(0L)
    }

}
