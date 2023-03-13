package ru.neoflex.conveor.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
public class CalculateServiceTest {

    private final CalculatorService calculatorService = new CalculatorServiceImpl();
    private final static BigDecimal TOTAL_AMOUNT_REQUEST = new BigDecimal("15000.00");
    private final static BigDecimal TOTAL_AMOUNT_NOT_INSURANCE = new BigDecimal("15000.00");
    private final static BigDecimal TOTAL_AMOUNT_IS_INSURANCE = new BigDecimal("30000.00");

    @Test
    public void calculateTotalAmountTest() {
        Assertions.assertThat(calculatorService.calculateTotalAmount(TOTAL_AMOUNT_REQUEST, false))
                .isEqualTo(TOTAL_AMOUNT_NOT_INSURANCE);
        Assertions.assertThat(calculatorService.calculateTotalAmount(TOTAL_AMOUNT_REQUEST, true))
                .isEqualTo(TOTAL_AMOUNT_IS_INSURANCE);
    }
}
