package ru.neoflex.conveor.service;

import ru.neoflex.conveor.model.CreditDTO;
import ru.neoflex.conveor.model.ScoringDataDTO;

import java.math.BigDecimal;

public interface CalculatorService {

    BigDecimal calculateTotalAmount(BigDecimal amount, Boolean isInsuranceEnabled);

    BigDecimal calculateRate(Boolean isInsuranceEnabled, Boolean isSalaryClient);

    BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, Integer term, BigDecimal rate);

    CreditDTO calculateCredit(ScoringDataDTO scoringData);
}
