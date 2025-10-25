package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable,
) {

    /**
     * 유저의 포인트를 조회
     */
    fun readUserPoint(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    /**
     * 유저의 포인트 충전/이용 내역을 조회
     */
    fun readUserPointHistories(userId: Long): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(userId)

    }

    /**
     * 유저의 포인트를 충전하고, 히스토리에 기록
     * - synchronized 키워드를 사용하여 동시성 문제 방지
     *   - DB 사용 x, 단일 서버이기 때문에 synchronized 로 처리, 같은 이유로 Redis 사용한 분산락 사용 X
     */
    @Synchronized
    fun chargePoint(userId: Long, amount: Long): UserPoint {
        val userPoint = readUserPoint(userId) // 포인트 존재 여부 확인
        val toChargeAmount = userPoint.point + amount   // 충전 후 포인트 계산

        val chargedUserPoint = userPointTable.insertOrUpdate(userId, toChargeAmount)

        pointHistoryTable.insert(
            id = chargedUserPoint.id,
            amount = chargedUserPoint.point,
            transactionType = TransactionType.CHARGE,
            updateMillis = chargedUserPoint.updateMillis
        )

        return chargedUserPoint
    }

    /**
     * 유저의 포인트를 사용하고, 히스토리에 기록
     * - 보유하고 있는 포인트가 사용할 포인트보다 적을 경우 예외 처리
     * - synchronized 키워드를 사용하여 동시성 문제 방지
     *   - DB 사용 x, 단일 서버이기 때문에 synchronized 로 처리, 같은 이유로 Redis 사용한 분산락 사용 X
     */
    @Synchronized
    fun usePoint(userId: Long, amount: Long): UserPoint {
        val userPoint = readUserPoint(userId) // 포인트 존재 여부 확인

        // 포인트 부족 시 예외 처리
        if (userPoint.point < amount) {
            throw IllegalStateException("포인트가 부족합니다.")
        }

        // 포인트 계산 후 나머지 포인트를 기록
        val afterUseUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point - amount)

        pointHistoryTable.insert(
            id = afterUseUserPoint.id,
            amount = afterUseUserPoint.point,
            transactionType = TransactionType.USE,
            updateMillis = afterUseUserPoint.updateMillis
        )

        return afterUseUserPoint
    }

}