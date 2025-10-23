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

}