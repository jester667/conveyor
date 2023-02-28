package ru.neoflex.conveor.service;

import org.springframework.stereotype.Service;
import ru.neoflex.conveor.model.CreditDTO;
import ru.neoflex.conveor.model.EmploymentDTO;
import ru.neoflex.conveor.model.PaymentScheduleElement;
import ru.neoflex.conveor.model.ScoringDataDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    private static final String INSURANCE_PRICE = "15000.00";

    private static final String LOAN_AMOUNT = "250000.00";

    private static final String INSURANCE_MULTIPLICAND = "0.06";

    private static final String RATE = "13.00";

    private static final String INSURANCE_DISCOUNT = "3.0";

    private static final String SALARY_CLIENT_DISCOUNT = "2.00";

    private static final String PERIODS_AMOUNT_IN_YEAR = "12";


    @Override
    public BigDecimal calculateTotalAmount(BigDecimal amount, Boolean isInsuranceEnabled) {
        if (isInsuranceEnabled) {
            BigDecimal loanAmount = new BigDecimal(LOAN_AMOUNT);
            BigDecimal insuranceMultiplicand = new BigDecimal(INSURANCE_MULTIPLICAND);
            BigDecimal insurancePrice = (amount.compareTo(loanAmount)) > 0 ?
                    new BigDecimal(INSURANCE_PRICE).add(amount.multiply(insuranceMultiplicand)) :
                    new BigDecimal(INSURANCE_PRICE);
            return amount.add(insurancePrice);
        } else {
            return amount;
        }
    }

    @Override
    public BigDecimal calculateRate(Boolean isInsuranceEnabled, Boolean isSalaryClient) {
        BigDecimal rate = new BigDecimal(RATE);
        rate = calculateRateByFlags(rate, isInsuranceEnabled, isSalaryClient);
        return rate;
    }

    @Override
    public BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, Integer term, BigDecimal rate) {
        BigDecimal monthlyRateAbsolute = rate.divide(BigDecimal.valueOf(100), 5, RoundingMode.CEILING);
        BigDecimal monthlyRate = monthlyRateAbsolute.divide(new BigDecimal(PERIODS_AMOUNT_IN_YEAR), 6, RoundingMode.CEILING);
        BigDecimal intermediateCoefficient = (BigDecimal.ONE.add(monthlyRate)).pow(term)
                .setScale(5, RoundingMode.CEILING);
        BigDecimal annuityCoefficient = monthlyRate.multiply(intermediateCoefficient)
                .divide(intermediateCoefficient.subtract(BigDecimal.ONE), RoundingMode.CEILING);
        BigDecimal monthlyPayment = totalAmount.multiply(annuityCoefficient).setScale(2, RoundingMode.CEILING);
        return monthlyPayment;
    }

    @Override
    public CreditDTO calculateCredit(ScoringDataDTO scoringData) {
        BigDecimal rate = calculateCurrentRate(scoringData);
        BigDecimal totalAmount = calculateTotalAmount(scoringData.getAmount(), scoringData.getIsInsuranceEnabled());
        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, scoringData.getTerm(), rate);
        List<PaymentScheduleElement> paymentSchedule = calculatePaymentSchedule(totalAmount, scoringData.getTerm(), rate, monthlyPayment);

        return CreditDTO.builder()
                .isInsuranceEnabled(scoringData.getIsInsuranceEnabled())
                .isSalaryClient(scoringData.getIsSalaryClient())
                .monthlyPayment(monthlyPayment)
                .paymentSchedule(paymentSchedule)
                .term(scoringData.getTerm())
                .rate(rate)
                .amount(totalAmount)
                .psk(calculatePSK(paymentSchedule, scoringData.getAmount(), scoringData.getTerm()))
                .build();
    }

    public BigDecimal calculatePSK(List<PaymentScheduleElement> paymentSchedule,
                                   BigDecimal requestedAmount, Integer term) {
        BigDecimal paymentAmount = paymentSchedule
                .stream()
                .map(PaymentScheduleElement::getTotalPayment)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);


        BigDecimal termInYears = divide(BigDecimal.valueOf(term),
                new BigDecimal(PERIODS_AMOUNT_IN_YEAR));

        BigDecimal intermediateCoefficient = divide(paymentAmount, requestedAmount)
                .subtract(BigDecimal.ONE);

        BigDecimal psk = intermediateCoefficient.divide(termInYears, 3, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(100));
        return psk;
    }

    public BigDecimal divide(BigDecimal number, BigDecimal divisor) {
        return number.divide(divisor, 2, RoundingMode.CEILING);
    }

    private List<PaymentScheduleElement> calculatePaymentSchedule(BigDecimal totalAmount, Integer term, BigDecimal rate, BigDecimal monthlyPayment) {

        BigDecimal remainingDebt = totalAmount.setScale(2, RoundingMode.CEILING);
        List<PaymentScheduleElement> paymentSchedule = new ArrayList<>();
        LocalDate paymentDate = LocalDate.now();

        paymentSchedule.add(PaymentScheduleElement.builder()
                .number(0)
                .date(paymentDate)
                .totalPayment(BigDecimal.ZERO)
                .remainingDebt(remainingDebt)
                .interestPayment(BigDecimal.ZERO)
                .build()
        );

        for (int i = 1; i < term + 1; i++) {
            paymentDate = paymentDate.plusMonths(1);

            BigDecimal interestPayment = calculateInterest(remainingDebt, rate).setScale(2, RoundingMode.CEILING);
            BigDecimal debtPayment = monthlyPayment.subtract(interestPayment);

            remainingDebt = remainingDebt.subtract(debtPayment);

            paymentSchedule.add(PaymentScheduleElement.builder()
                    .number(i)
                    .date(paymentDate)
                    .totalPayment(monthlyPayment)
                    .remainingDebt(remainingDebt)
                    .interestPayment(interestPayment)
                    .build()
            );
        }

        return paymentSchedule;
    }

    public BigDecimal calculateInterest(BigDecimal remainingDebt, BigDecimal rate) {
        BigDecimal monthlyRateAbsolute = rate.divide(BigDecimal.valueOf(100), RoundingMode.CEILING);

        BigDecimal monthlyRate = monthlyRateAbsolute.divide(new BigDecimal(PERIODS_AMOUNT_IN_YEAR), 10, RoundingMode.CEILING);

        return remainingDebt.multiply(monthlyRate);
    }

    private BigDecimal calculateCurrentRate(ScoringDataDTO scoringData) {
        BigDecimal rate = new BigDecimal(RATE);
        List<String> scoringRefuseCauses = checkRefuseCauses(scoringData, scoringData.getEmployment());

        if (scoringRefuseCauses.size() > 0) {
            throw new RuntimeException();
        }

        rate = calculateRateByEmploymentStatus(rate, scoringData.getEmployment().getEmploymentStatus(), scoringRefuseCauses);
        rate = calculateRateByPosition(rate, scoringData.getEmployment().getPosition());
        rate = calculateRateByGender(rate, scoringData.getGender(), scoringData.getBirthdate());
        rate = calculateRateByMaritalStatus(rate, scoringData.getMaritalStatus());
        rate = calculateRateByTerm(rate, scoringData.getTerm());
        rate = calculateRateByDependentAmount(rate, scoringData.getDependentAmount());
        rate = calculateRateByFlags(rate, scoringData.getIsInsuranceEnabled(), scoringData.getIsSalaryClient());

        return rate;
    }

    private BigDecimal calculateRateByFlags(BigDecimal rate, Boolean isInsuranceEnabled, Boolean isSalaryClient) {
        if (isInsuranceEnabled) {
            rate = rate.subtract(new BigDecimal(INSURANCE_DISCOUNT));
        }
        if (isSalaryClient) {
            rate = rate.subtract(new BigDecimal(SALARY_CLIENT_DISCOUNT));
        }
        return rate;
    }

    private BigDecimal calculateRateByEmploymentStatus(BigDecimal rate,
                                                       EmploymentDTO.EmploymentStatusEnum employmentStatus,
                                                       List<String> scoringRefuseCauses) {
        if (employmentStatus.equals(EmploymentDTO.EmploymentStatusEnum.SELF_EMPLOYED)) {
            return rate.add(BigDecimal.ONE);
        } else if (employmentStatus.equals(EmploymentDTO.EmploymentStatusEnum.BUSINESS_OWNER)) {
            return rate.add(BigDecimal.valueOf(3));
        }
        return rate;
    }

    private BigDecimal calculateRateByPosition(BigDecimal rate, EmploymentDTO.PositionEnum position) {
        if (position.equals(EmploymentDTO.PositionEnum.MID_MANAGER)) {
            return rate.subtract(BigDecimal.valueOf(2));
        } else if (position == EmploymentDTO.PositionEnum.TOP_MANAGER) {
            return rate.subtract(BigDecimal.valueOf(4));
        }
        return rate;
    }

    private BigDecimal calculateRateByGender(BigDecimal rate, ScoringDataDTO.GenderEnum gender, LocalDate birthdate) {
        long clientAge = ChronoUnit.YEARS.between(birthdate, LocalDate.now());
        if (gender == ScoringDataDTO.GenderEnum.NON_BINARY) {
            return rate.add(BigDecimal.valueOf(3));
        } else if (gender == ScoringDataDTO.GenderEnum.FEMALE && (clientAge > 35 && clientAge < 60)) {
            return rate.subtract(BigDecimal.valueOf(3));
        } else if (gender == ScoringDataDTO.GenderEnum.FEMALE && (clientAge > 30 && clientAge < 55)) {
            return rate.subtract(BigDecimal.valueOf(3));
        }
        return rate;
    }

    private BigDecimal calculateRateByMaritalStatus(BigDecimal rate, ScoringDataDTO.MaritalStatusEnum maritalStatus) {
        if (maritalStatus.equals(ScoringDataDTO.MaritalStatusEnum.MARRIED)) {
            return rate.subtract(BigDecimal.valueOf(3));
        } else if (maritalStatus.equals(ScoringDataDTO.MaritalStatusEnum.DIVORCED)) {
            return rate.add(BigDecimal.ONE);
        }
        return rate;
    }

    private BigDecimal calculateRateByTerm(BigDecimal rate, Integer term) {
        if (term < 12) {
            return rate.add(BigDecimal.valueOf(5));
        } else if (term >= 120) {
            return rate.subtract(BigDecimal.valueOf(2));
        }
        ;
        return rate;
    }

    private BigDecimal calculateRateByDependentAmount(BigDecimal rate, Integer dependentAmount) {
        if (dependentAmount > 1) {
            return rate.add(BigDecimal.ONE);
        }
        return rate;
    }

    private List<String> checkRefuseCauses(ScoringDataDTO scoringData,
                                           EmploymentDTO employment) {
        List<String> scoringRefuseCauses = new ArrayList<>();
        long clientAge = ChronoUnit.YEARS.between(scoringData.getBirthdate(), LocalDate.now());

        if (employment.getEmploymentStatus().equals(EmploymentDTO.EmploymentStatusEnum.UNEMPLOYED)) {
            scoringRefuseCauses.add("Refuse cause: Client unemployed.");
        }

        if (scoringData.getAmount()
                .compareTo(employment.getSalary().multiply(BigDecimal.valueOf(20))) > 0) {
            scoringRefuseCauses.add("Refuse cause: Too much loan amount due to client's salary.");
        }

        if (clientAge > 60) {
            scoringRefuseCauses.add("Refuse cause: Client too old.");
        } else if (clientAge < 20) {
            scoringRefuseCauses.add("Refuse cause: Client too young.");
        }

        if (employment.getWorkExperienceTotal() < 12) {
            scoringRefuseCauses.add("Refuse cause: Too small total working experience.");
        }
        if (employment.getWorkExperienceCurrent() < 3) {
            scoringRefuseCauses.add("Refuse cause: Too small current working experience.");
        }

        return scoringRefuseCauses;
    }
}
