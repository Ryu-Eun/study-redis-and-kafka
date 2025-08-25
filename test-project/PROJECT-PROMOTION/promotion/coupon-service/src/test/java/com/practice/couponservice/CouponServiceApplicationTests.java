package com.practice.couponservice;

import com.practice.couponservice.config.UserIdInterceptor;
import com.practice.couponservice.dto.v1.CouponDto;
import com.practice.couponservice.entity.Coupon;
import com.practice.couponservice.entity.CouponPolicy;
import com.practice.couponservice.exception.CouponIssueException;
import com.practice.couponservice.exception.CouponNotFoundException;
import com.practice.couponservice.repository.CouponPolicyRepository;
import com.practice.couponservice.repository.CouponRepository;
import com.practice.couponservice.service.v1.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceApplicationTests {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    private CouponPolicy couponPolicy;
    private Coupon coupon;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_COUPON_ID = 1L;
    private static final Long TEST_ORDER_ID = 1L;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .id(1L)
                .name("테스트 쿠폰")
                .discountType(CouponPolicy.DiscountType.FIXED_AMOUNT)
                .discountValue(1000)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(1000)
                .totalQuantity(100)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        coupon = coupon.builder()
                .id(TEST_COUPON_ID)
                .userId(TEST_USER_ID)
                .couponPolicy(couponPolicy)
                .couponCode("TEST123")
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issuedCoupon_Success(){
        //given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(1L)
                .build();

        // 쿠폰 정책은 존재하지만, 아직 발급된 쿠폰은 없는 상황
        when(couponPolicyRepository.findByIdWithLock(any())).thenReturn(Optional.of(couponPolicy)); // findByIdWithLock 메서드가 테스트중 호출되면 인자가 뭐든지 결과값으로 Optional.of(couponPolicy)로 반환하라
        when(couponRepository.countByCouponPolicyId(any())).thenReturn(0L);
        when(couponRepository.save(any())).thenReturn(coupon);

        // MockedStatic 사용해서 UserIdInterceptor도 mock으로 구현
        try(MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)){
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            //when
            CouponDto.Response response = CouponDto.Response.from(couponService.issueCoupon(request));

            //then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            verify(couponRepository).save(any());
        }
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success(){
        //given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID)).thenReturn(Optional.of(coupon));

        try(MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)){
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            //when
            CouponDto.Response response = CouponDto.Response.from(couponService.useCoupon(TEST_COUPON_ID, TEST_ORDER_ID));

            //then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(response.getStatus()).isEqualTo(Coupon.Status.USED);
        }
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 쿠폰이 존재하지 않거나 권한 없음")
    void useCoupon_Fail_NotFoundOrUnathorized(){
        //given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID)).thenReturn(Optional.empty());

        try(MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)){
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            //when & then
            assertThatThrownBy(() -> couponService.useCoupon(TEST_COUPON_ID, TEST_ORDER_ID))
                    .isInstanceOf(CouponNotFoundException.class)
                    .hasMessage("쿠폰을 찾을 수 없거나 접근 권한이 없습니다.");
        }
    }

    @Test
    @DisplayName("쿠폰 취소 성공")
    void cancelCoupon_Success(){
        //given
        coupon.use(TEST_ORDER_ID);
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID)).thenReturn(Optional.of(coupon));

        try(MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)){
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            //when
            CouponDto.Response response = CouponDto.Response.from(couponService.cancelCoupon(TEST_COUPON_ID));

            //then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getStatus()).isEqualTo(Coupon.Status.CANCELLED);
        }
    }

    @Test
    @DisplayName("쿠폰 취소 실패 - 쿠폰이 존재하지 않거나 권한 없음")
    void cancelCoupon_Fail_NotFoundOrUnathorized(){
        //given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID)).thenReturn(Optional.empty());

        try(MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)){
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            //when & then
            assertThatThrownBy(() -> couponService.cancelCoupon(TEST_COUPON_ID))
                    .isInstanceOf(CouponNotFoundException.class)
                    .hasMessage("쿠폰을 찾을 수 없거나 접근 권한이 없습니다.");
        }
    }


}
