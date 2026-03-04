package com.example.lms.enrollment.service;

import com.example.lms.enrollment.entity.PointTransactionEntity;
import com.example.lms.enrollment.entity.PointTransactionType;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {

    private final UserJpaRepository userJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;

    public PointService(UserJpaRepository userJpaRepository,
                        PointTransactionJpaRepository pointTransactionJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
    }

    @Transactional
    public int grantPoints(Long userId, int amount, String memo) {
        if (amount <= 0) throw new IllegalArgumentException("지급 포인트는 0보다 커야 합니다.");

        UserEntity user = userJpaRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int nextBalance = safe(user.getPointBalance()) + amount;
        user.setPointBalance(nextBalance);

        PointTransactionEntity tx = new PointTransactionEntity();
        tx.setUserId(user.getId());
        tx.setType(PointTransactionType.EARN);
        tx.setAmount(amount);
        tx.setBalanceAfter(nextBalance);
        tx.setMemo(memo == null || memo.isBlank() ? "초기지급" : memo);
        pointTransactionJpaRepository.save(tx);

        return nextBalance;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
