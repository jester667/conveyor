package ru.neoflex.conveor.service;

import ru.neoflex.conveor.model.CreditDTO;
import ru.neoflex.conveor.model.LoanApplicationRequestDTO;
import ru.neoflex.conveor.model.LoanOfferDTO;
import ru.neoflex.conveor.model.ScoringDataDTO;

import java.util.List;

public interface ConveyorService {
    List<LoanOfferDTO> prepareOffers(LoanApplicationRequestDTO loanAppReq);
}
