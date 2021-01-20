package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FareCalculatorServiceTest {

    private static final String  VEHICULE_REG_NUMBER = "68 DE 43";
    private static final int  NB_MILLISECOND_IN_AN_HOUR = 60 * 60 * 1000 ;
    private static final int  NB_MILLISECOND_IN_45_MINUTES = 45 * 60 * 1000 ;
    private static final float REDUCTION_FOR_RECURRENT_USER = (float) 0.95;
    private static final double delta = 0.01;
    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @Mock
    private static TicketDAO ticketDAO;

    /*
    @BeforeAll
    private static void setUp() {
        //fareCalculatorService = new FareCalculatorService();
    }
*/
    @BeforeEach
    private void setUpPerTest() {
        fareCalculatorService = new FareCalculatorService(ticketDAO);
        ticket = new Ticket();
        ticket.setInTime(new Date());
        ticket.setOutTime(new Date());
        ticket.getOutTime().setTime( ticket.getInTime().getTime() + NB_MILLISECOND_IN_AN_HOUR );
        ticket.setVehicleRegNumber(VEHICULE_REG_NUMBER);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        ticket.setParkingSpot(parkingSpot);
    }

    @Nested
    @DisplayName("calculate Fare Car Tests")
    class calculateFareCarTest {

        @Test
        @DisplayName("calculate fare car")
        public void calculateFareCar() {
            //GIVEN WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), delta);
        }

        @DisplayName("Fare Car With Less Than 30 Minutes Parking Time should be free")
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 10, 30})
        public void calculateFareCarWithLessThan30MinutesParkingTimeShouldBeFree(int durationParkingMinuts) {
            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() +
                            durationParkingMinuts * 60 * 1000);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(0.0, ticket.getPrice(), delta);
        }

        @Test
        @DisplayName("calculate Fare Car With Less Than One Hour Parking Time")
        public void calculateFareCarWithLessThanOneHourParkingTime() {
            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() + NB_MILLISECOND_IN_45_MINUTES);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals((0.75 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice(), delta);
        }

        @DisplayName("Fare Car With Hours  Parking Time should be ratePerHour * nb of hours ")
        @ParameterizedTest
        @ValueSource(ints = {1, 24, 36})
        public void calculateFareCarWithHoursParkingTimeShouldBeMultipleOfRatePerHour(int durationParkingHours) {
            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() +
                    durationParkingHours * NB_MILLISECOND_IN_AN_HOUR);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.CAR_RATE_PER_HOUR * durationParkingHours , ticket.getPrice(), delta);
        }

        @Test
        @DisplayName("calculate Fare Car With Discount for Recurrent User")
        public void calculateFareCarWithDiscountforRecurrentUser() {

            //GIVEN
            when(ticketDAO.isRecurrentUser(VEHICULE_REG_NUMBER)).thenReturn(true);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.CAR_RATE_PER_HOUR * REDUCTION_FOR_RECURRENT_USER, ticket.getPrice(), delta);
        }
    }

    @Nested
    @DisplayName("calculate Fare  Bike Tests")
    class calculateFareBikeTest {

        @Test
        @DisplayName("calculate Fare Bike")
        public void calculateFareBike() {
            //GIVEN
            ticket.getParkingSpot().setParkingType(ParkingType.BIKE);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), delta);
        }

        @DisplayName("calculate Fare Bike With Less Than 30 Minutes Parking Time")
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 10, 30})
        public void calculateFareBikeWithLessThan30MinutesParkingTime(int durationParkingMinuts) {
            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() +
                    durationParkingMinuts * 60 * 1000);
            ticket.getParkingSpot().setParkingType(ParkingType.BIKE);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(0.0, ticket.getPrice(), delta);
        }

        @Test
        @DisplayName("calculate Fare Bike With Less Than One Hour Parking Time")
        public void calculateFareBikeWithLessThanOneHourParkingTime() {

            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() + NB_MILLISECOND_IN_45_MINUTES);
            ticket.getParkingSpot().setParkingType(ParkingType.BIKE);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice(), delta);
        }

        @DisplayName("Fare Car With Hours  Parking Time should be ratePerHour * nb of hours ")
        @ParameterizedTest
        @ValueSource(ints = {1, 24, 36})
        public void calculateFareCarWithHoursParkingTimeShouldBeMultipleOfRatePerHour(int durationParkingHours) {
            //GIVEN
            ticket.getOutTime().setTime(ticket.getInTime().getTime() +
                    durationParkingHours * NB_MILLISECOND_IN_AN_HOUR);
            ticket.getParkingSpot().setParkingType(ParkingType.BIKE);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.BIKE_RATE_PER_HOUR * durationParkingHours , ticket.getPrice(), delta);
        }

        @Test
        @DisplayName("calculate Fare Bike With Discount for Recurrent User")
        public void calculateFareBikeWithDiscountforRecurrentUser() {
            //GIVEN
            ticket.getParkingSpot().setParkingType(ParkingType.BIKE);
            when(ticketDAO.isRecurrentUser(VEHICULE_REG_NUMBER)).thenReturn(true);
            //WHEN
            fareCalculatorService.calculateFare(ticket);
            //THEN
            assertEquals(Fare.BIKE_RATE_PER_HOUR * REDUCTION_FOR_RECURRENT_USER, ticket.getPrice(), delta);
        }
    }

    @Nested
    @DisplayName("calculate Fare Other Tests")
    class calculateFareOtherTest {

        @Test
        @DisplayName("calculate Fare With Entry After Exit Shoud Throw IlleglArgumentException")
        public void calculateFareWithEntryAfterExitShoudThrowIlleglArgumentException() {
            //GIVEN
            ticket.getInTime().setTime(ticket.getOutTime().getTime() + NB_MILLISECOND_IN_AN_HOUR);
            //WHEN  THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }

        @Test
        @DisplayName("calculate Fare With exit Time Null Should Throw Exception")
        public void calculateFareWithExitTimeNullShouldThrowException() {
            //GIVEN
            ticket.setOutTime(null);
            //WHEN  THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }

        @Test
        @DisplayName("calculate Fare Unkown Type")
        public void calculateFareUnkownType() {
            //GIVEN
            ticket.getParkingSpot().setParkingType(null);
            //WHEN THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }
    }


}
