package ru.neoflex.conveor.controller;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class ConveyorControllerTest {

    private final static String URI_DOCKER_TEST = "/conveyor/";
    private final static String DOCKER_TEST_SUCCESS = "Hello Docker!";

    @InjectMocks
    private ConveyorController conveyorController;

    @Test
    public void dockerTest() {
        Assertions.assertThat(conveyorController.dockerTest().getStatusCode())
                .isEqualTo(HttpStatus.OK);
        Assertions.assertThat(conveyorController.dockerTest().getBody())
                .isEqualTo(DOCKER_TEST_SUCCESS);
    }
}
