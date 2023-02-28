package ru.neoflex.conveor.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.neoflex.conveor.model.CreditDTO;
import ru.neoflex.conveor.model.LoanApplicationRequestDTO;
import ru.neoflex.conveor.model.LoanOfferDTO;
import ru.neoflex.conveor.model.ScoringDataDTO;
import ru.neoflex.conveor.service.CalculatorService;
import ru.neoflex.conveor.service.ConveyorService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(ConveyorController.BASE_PATH)
@AllArgsConstructor
public class ConveyorController {

    public final static String BASE_PATH = "/conveyor";

    private final static String OFFERS_PATH = "/offers";

    private final static String CALCULATION_PATH = "/calculation";

    private final CalculatorService calculatorService;

    private final ConveyorService conveyorService;

    @PostMapping(OFFERS_PATH)
    public ResponseEntity<List<LoanOfferDTO>> offers(@RequestBody @Valid LoanApplicationRequestDTO req, BindingResult bindingResult) {
        System.out.println("bindingResult.hasErrors() + " + bindingResult.hasErrors());
        if (bindingResult.hasErrors()) {
            throw new RuntimeException();
        }
        return ResponseEntity.ok(conveyorService.prepareOffers(req));
    }

    @PostMapping(CALCULATION_PATH)
    public ResponseEntity<CreditDTO> calculation(@RequestBody ScoringDataDTO req) {
        return ResponseEntity.ok(calculatorService.calculateCredit(req));
    }

    @GetMapping("/")
    public ResponseEntity<String> dockerTest() {
        return ResponseEntity.ok("Hello Docker!");
    }
}
