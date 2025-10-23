package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.lang.IllegalStateException

class PointServiceTest {

    /**
     * PointService 가 의존하고 있는 객체들을 Mock 객체로 생성합니다.
     * PointService 에 생성한 Mock 객체들을 주입해 테스트합니다.
     */
    private val pointHistoryTable: PointHistoryTable = mock()
    private val userPointTable: UserPointTable = mock()
    private val pointService: PointService = PointService(
        pointHistoryTable = pointHistoryTable,
        userPointTable = userPointTable,
    )

    /**
     * 특정 유저의 포인트를 조회했을 때, 올바른 값을 반환하는지 테스트하기 위해 작성합니다.
     */
    @Test
    @DisplayName("특정 유저의 포인트를 조회할 수 있다")
    fun readUserPoint() {
        // given
        val userId = 1L
        val point = 1000L
        val updateMillis = System.currentTimeMillis()

        // 특정 유저의 포인트 조회 시, stub 데이터를 반환
        whenever(userPointTable.selectById(userId))
            .thenReturn(UserPoint(id = userId, point = point, updateMillis = updateMillis))

        // when
        val result = pointService.readUserPoint(userId)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(point)
        assertThat(result.updateMillis).isEqualTo(updateMillis)

        // mock, 해당 메소드를 호출했는지 검증
        verify(userPointTable).selectById(userId)
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회했을 때, 올바른 값을 반환하는지 테스트하기 위해 작성합니다.
     */
    @Test
    @DisplayName("특정 유저의 포인트 충전/이용 내역을 조회할 수 있다")
    fun readUserPointHistory() {
        // given
        val userId = 1L
        val chargePoint = 1000L
        val chargeTimeMillis = System.currentTimeMillis()
        val usePoint = 500L
        val useTimeMillis = System.currentTimeMillis()

        // 특정 유저의 포인트 히스토리 조회 시, stub 데이터를 반환
        whenever(pointHistoryTable.selectAllByUserId(userId))
            .thenReturn(
                // 충전, 사용 각각 1건
                listOf(
                    PointHistory(
                        id = 1L,
                        userId = userId,
                        type = TransactionType.CHARGE,
                        amount = chargePoint,
                        timeMillis = chargeTimeMillis,
                    ),
                    PointHistory(
                        id = 2L,
                        userId = userId,
                        type = TransactionType.USE,
                        amount = usePoint,
                        timeMillis = useTimeMillis,
                    )
                )
            )

        // when
        val result = pointService.readUserPointHistories(userId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result)
            .extracting("userId", "type", "amount", "timeMillis")
            .containsExactlyInAnyOrder(
                tuple(userId, TransactionType.CHARGE, chargePoint, chargeTimeMillis),
                tuple(userId, TransactionType.USE, usePoint, useTimeMillis),
            )

        verify(pointHistoryTable).selectAllByUserId(userId)
    }

    /**
     * 유저가 포인트를 충전했을 때, 올바른 값을 반환하는지 테스트하기 위해 작성합니다.
     */
    @Test
    @DisplayName("유저는 포인트를 충전하면 해당 amount 만큼 충전되고, 히스토리에 기록된다")
    fun chargePoint() {
        // given
        val userId = 1L
        val toChargeAmount = 1000L
        val updateMillis = System.currentTimeMillis()

        // 포인트 충전 시, stub 데이터를 반환
        whenever(userPointTable.insertOrUpdate(userId, toChargeAmount))
            .thenReturn(
                UserPoint(
                    id = userId,
                    point = toChargeAmount,
                    updateMillis = updateMillis,
                )
            )

        // when
        val result = pointService.chargePoint(userId, toChargeAmount)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(toChargeAmount)
        assertThat(result.updateMillis).isEqualTo(updateMillis)

        // mock, 포인트를 충전하는 로직과 히스토리에 기록하는 로직이 호출되었는지 검증
        verify(userPointTable).insertOrUpdate(userId, toChargeAmount)
        verify(pointHistoryTable).insert(
            id = userId,
            amount = toChargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = updateMillis,
        )
    }

    @Test
    @DisplayName("유저는 포인트를 사용하면 amount 만큼 차감하고, 히스토리에 기록된다")
    fun usePoint() {
        // given
        val userId = 1L
        val toUseAmount = 200L
        // 포인트 초기 세팅
        val initialMillis = System.currentTimeMillis()
        val initialUserPoint = UserPoint(
            id = userId,
            point = 1000L,
            updateMillis = initialMillis,
        )
        // 포인트 사용 후 세팅
        val afterUseUserPoint = initialUserPoint.point - toUseAmount
        val afterUseMillis = System.currentTimeMillis()

        // 유저의 포인트 조회 & 포인트 사용 시, stub 데이터 반환
        whenever(userPointTable.selectById(userId))
            .thenReturn(initialUserPoint)
        whenever(userPointTable.insertOrUpdate(userId, initialUserPoint.point - toUseAmount))
            .thenReturn(
                UserPoint(
                    id = userId,
                    point = afterUseUserPoint,
                    updateMillis = afterUseMillis,
                )
            )

        // when
        val result = pointService.usePoint(userId, toUseAmount)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(afterUseUserPoint)
        assertThat(result.updateMillis).isEqualTo(afterUseMillis)

        // mock, 유저의 포인트를 조회하는 로직과 포인트를 사용하는 로직, 히스토리에 기록하는 로직이 호출되었는지 검증
        verify(userPointTable).selectById(userId)
        verify(userPointTable).insertOrUpdate(userId, initialUserPoint.point - toUseAmount)
        verify(pointHistoryTable).insert(
            id = userId,
            amount = afterUseUserPoint,
            transactionType = TransactionType.USE,
            updateMillis = afterUseMillis,
        )

    }

    @Test
    @DisplayName("유저가 가지고 있는 포인트보다 많은 amount 차감을 시도하면 예외가 발생한다")
    fun usePointFail() {
        // given
        val userId = 1L
        val toUseAmount = 2000L
        // 포인트 초기 세팅
        val initialMillis = System.currentTimeMillis()
        val initialUserPoint = UserPoint(
            id = userId,
            point = 1000L,
            updateMillis = initialMillis,
        )
        // 유저의 포인트 조회 시, stub 데이터 반환
        whenever(userPointTable.selectById(userId))
            .thenReturn(initialUserPoint)

        // when
        // then
        // 가지고 있는 포인트보다 많은 양을 차감하려고 할 때 예외 반환
        assertThatThrownBy { pointService.usePoint(userId, toUseAmount) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("포인트가 부족합니다.")

        verify(userPointTable).selectById(userId)

    }

}
