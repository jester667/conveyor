package ru.neoflex.conveor.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.neoflex.conveor.model.LoanApplicationRequestDTO;
import ru.neoflex.conveor.model.LoanOfferDTO;

import java.util.List;

import static java.util.Arrays.asList;

@Service
@AllArgsConstructor
public class ConveyorServiceImpl implements ConveyorService {

    private final CalculatorService calculatorService;

    @Override
    public List<LoanOfferDTO> prepareOffers(LoanApplicationRequestDTO loanAppReq) {
        return asList(
                createOffer(loanAppReq, false, false),
                createOffer(loanAppReq, false, true),
                createOffer(loanAppReq, true, false),
                createOffer(loanAppReq, true, true)
        );
    }


    private LoanOfferDTO createOffer(LoanApplicationRequestDTO loanAppReq,
                                     Boolean isInsuranceEnabled,
                                     Boolean isSalaryClient) {
        var totalAmount = calculatorService.calculateTotalAmount(loanAppReq.getAmount(), isInsuranceEnabled);
        var rate = calculatorService.calculateRate(isInsuranceEnabled, isSalaryClient);
        return LoanOfferDTO.builder()
                .isInsuranceEnabled(isInsuranceEnabled)
                .isSalaryClient(isSalaryClient)
                .requestedAmount(loanAppReq.getAmount())
                .totalAmount(totalAmount)
                .rate(rate)
                .monthlyPayment(calculatorService.calculateMonthlyPayment(totalAmount, loanAppReq.getTerm(), rate))
                .term(loanAppReq.getTerm())
                .build();
    }
}
