package com.practice.pointservice.service.v2;

import com.practice.pointservice.entity.Point;
import com.practice.pointservice.entity.PointBalance;
import com.practice.pointservice.entity.PointType;
import com.practice.pointservice.repository.PointBalanceRepository;
import com.practice.pointservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 *
 * 포인트 적립 처리:
 * 1. 분산락 획득
 * 2. 캐시된 잔액 조회 (없으면 DB에서 조회)
 * 3. 포인트 잔액 증가
 * 4. DB 저장 및 캐시 업데이트
 * 5. 포인트 이력 저장
 *
 */
@Service
@RequiredArgsConstructor
public class PointRedisService {

    private static final String POINT_BALANCE_MAP = "point:balance";
    private static final String POINT_LOCK_PREFIX = "point:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 3L;

    private final RedissonClient redissonClient;
    private final PointBalanceRepository pointBalanceRepository;
    private final PointRepository pointRepository;

    @Transactional
    public Point earnPoints(Long userId, Long amount, String description){
        // 분산락 획득
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);

        try{
            // 락 획득 시도
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(!locked){
                // 락 획득 실패
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            // 캐시된 잔액 조회
            Long currentBalance = getBalanceFromCache(userId);
            if(currentBalance == null){
                // 캐시된 잔액이 없으면 DB에서 조회
                currentBalance = getBalanceFromDB(userId);
                // 캐시 업데이트
                updateBalanceCache(userId, currentBalance);
            }

            // 포인트 잔액 증가
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseGet(() -> PointBalance.builder()
                            .userId(userId)
                            .balance(0L)
                            .build());
            pointBalance.addBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);
            // 캐시 업데이트
            updateBalanceCache(userId, pointBalance.getBalance());

            // 포인트 이력 저장
            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.EARNED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();
            return pointRepository.save(point);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    private void updateBalanceCache(Long userId, Long currentBalance) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        balanceMap.fastPut(String.valueOf(userId), currentBalance);
    }

    private Long getBalanceFromDB(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    private Long getBalanceFromCache(Long userId) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        return balanceMap.get(String.valueOf(userId));
    }


    @Transactional
    public Point usePoints(Long userId, Long amount, String description){
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);

        try{
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(!locked){
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            Long currentBalance = getBalanceFromCache(userId);
            if(currentBalance == null){
                currentBalance = getBalanceFromDB(userId);
                updateBalanceCache(userId, currentBalance);
            }

            if(currentBalance < amount){
                // 잔액 부족
                throw new IllegalStateException("Insufficient balance");
            }

            // 포인트 잔액 감소
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            pointBalance.subtractBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);

            // 포인트 이력 저장
            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.USED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();
            return pointRepository.save(point);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        } finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    @Transactional
    public Point cancelPoints(Long pointId, String description){
        Point originalPoint = pointRepository.findById(pointId)
                .orElseThrow(() -> new IllegalStateException("Point not found"));

        Long userId = originalPoint.getUserId();

        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX + userId);

        try{
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(!locked){
                throw new IllegalStateException("Failed to acquire lock for user: " + userId);
            }

            if(originalPoint.getType() == PointType.CANCELED){
                throw new IllegalStateException("Already cancelled point");
            }

            PointBalance pointBalance = originalPoint.getPointBalance();
            if(originalPoint.getType() == PointType.EARNED){
                pointBalance.subtractBalance(pointBalance.getBalance());
            }else{
                pointBalance.addBalance(pointBalance.getBalance());
            }

            pointBalance = pointBalanceRepository.save(pointBalance);
            updateBalanceCache(userId, pointBalance.getBalance());

            Point point = Point.builder()
                    .userId(userId)
                    .amount(originalPoint.getAmount())
                    .type(PointType.CANCELED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();
            return pointRepository.save(point);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId){
        Long cachedBalance = getBalanceFromCache(userId);
        if(cachedBalance != null){
            return cachedBalance;
        }

        Long dbBalance = getBalanceFromDB(userId);
        updateBalanceCache(userId, dbBalance);

        return dbBalance;
    }

}