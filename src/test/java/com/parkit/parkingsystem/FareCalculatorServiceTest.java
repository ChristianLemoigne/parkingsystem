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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.stream.Stream;

import static com.parkit.parkingsystem.constants.ParkingType.BIKE;
import static com.parkit.parkingsystem.constants.ParkingType.CAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FareCalculatorServiceTest {

    private static final String  VEHICULE_REG_NUMBER = "ABCDEF";
    private static final long  NB_MILLISECOND_IN_AN_HOUR = 60L * 60 * 1000 ;
    private static final long  NB_MILLISECOND_IN_HALF_AN_HOUR = 30L * 60 * 1000 ;
    private static final float REDUCTION_FOR_RECURRENT_USER = (float) 0.95;
    private static final double COEFF_ROUND_2_DECIMALS =  100.0;
    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        fareCalculatorService = new FareCalculatorService(ticketDAO);
        ticket = new Ticket();
        ticket.setInTime(new Date());
        ticket.setVehicleRegNumber(VEHICULE_REG_NUMBER);
    }

    @Test
    @DisplayName("Fare should be equal to Fare.CAR_RATE_PER_HOUR when parking a Car an Hour")
    public void calculateFareCarForAnHour() {
        //GIVEN
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + NB_MILLISECOND_IN_AN_HOUR));
        ticket.setParkingSpot(new ParkingSpot(1, CAR,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    @DisplayName("Fare should be free when parking a Car half an Hour")
    public void FareShouldBeFreeWhenParkingACarHalfAnHour() {
        //GIVEN
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + NB_MILLISECOND_IN_HALF_AN_HOUR));
        ticket.setParkingSpot(new ParkingSpot(1, CAR,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(0.0, ticket.getPrice());
    }

    @Test
    @DisplayName("Fare should be discount when recurrent user parking a Car for an Hour")
    public void FareShouldBeDiscountWhenRecurrentUserParkACarForAnHour() {
        //GIVEN
        when(ticketDAO.isRecurrentUser(VEHICULE_REG_NUMBER)).thenReturn(true);
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + NB_MILLISECOND_IN_AN_HOUR));
        ticket.setParkingSpot(new ParkingSpot(1, CAR,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(1.43, ticket.getPrice());
    }

    @DisplayName("Fare should be equal to ratePerHour *  nb of hours when this nb is entire")
    @ParameterizedTest(name = "{index} -   Vehicule: {0}   Duration: {1} Hour(s)")
    @MethodSource("provideDataForTestMoreThan30MinParkingTime")
    public void FareShouldBeEqualtoRatePerHourMultiplyByNbHours(ParkingType parkingType,int durationParkingHours) {
        //GIVEN
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + durationParkingHours * NB_MILLISECOND_IN_AN_HOUR));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(round(fareRatePerHour(parkingType) * durationParkingHours) , ticket.getPrice());
    }
    static Stream<Arguments> provideDataForTestMoreThan30MinParkingTime() {
        return Stream.of(Arguments.of(CAR, 1), Arguments.of(CAR, 24), Arguments.of(CAR, 36), Arguments.of(CAR, 24 * 7),
                Arguments.of(BIKE, 1), Arguments.of(BIKE, 15), Arguments.of(BIKE, 36), Arguments.of(BIKE, 24 * 7));
    }

    @DisplayName("Fare should be correct when calculated from number of milliseconds")
    @ParameterizedTest(name = "{index} -   Vehicule: {0}   Duration: {1} Milliseconds(s) ")
    @MethodSource("provideDataForTestMoreThan30MinParkingTimeInMilliseconds")
    public void FareShouldBeCorrectWhenCalculatedFromNumberOfMilliseconds(ParkingType parkingType,long durationParkingInMilliseconds) {
        //GIVEN
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + durationParkingInMilliseconds));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(round(fareRatePerHour(parkingType) * durationParkingInMilliseconds / NB_MILLISECOND_IN_AN_HOUR), ticket.getPrice());
    }
    static Stream<Arguments> provideDataForTestMoreThan30MinParkingTimeInMilliseconds() {
        return Stream.of(Arguments.of(CAR,1800001), Arguments.of(CAR,2700333), Arguments.of(CAR,3800000), Arguments.of(CAR,99999999),
                Arguments.of(BIKE,1800001), Arguments.of(BIKE,2700333), Arguments.of(BIKE,3800000), Arguments.of(BIKE,99999999));
    }

    @DisplayName("Fare should be free when parking less than 30 minutes")
    @ParameterizedTest(name = "{index} -  Vehicule: {0}  duration: {1} Minute(s)")
    @MethodSource("provideDataForTestLessThan30MinParkingTime")
    public void FareShouldBeFreeWhenParkingLessThan30Minutes(ParkingType parkingType, int durationParkingInMinuts) {
        //GIVEN
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + durationParkingInMinuts * 60L * 1000));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType,false));
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(0.0, ticket.getPrice());
    }
    static Stream<Arguments> provideDataForTestLessThan30MinParkingTime() {
        return Stream.of(Arguments.of(CAR, 0), Arguments.of(CAR, 1), Arguments.of(CAR, 10), Arguments.of(CAR, 30),
                Arguments.of(BIKE, 0), Arguments.of(BIKE, 1), Arguments.of(BIKE, 10), Arguments.of(BIKE, 30));
    }

    @DisplayName("Fare should be discount when recurrent user")
    @ParameterizedTest(name = "{index} -  Vehicule: {0}  duration: {1} Milliseconds(s) ")
    @MethodSource("provideDataForTestMoreThan30MinParkingTimeInMilliseconds")
    public void FareShouldBeDiscountWhenRecurrentUser(ParkingType parkingType,long durationParkingInMilliseconds) {
        //GIVEN
        when(ticketDAO.isRecurrentUser(VEHICULE_REG_NUMBER)).thenReturn(true);
        ticket.setOutTime(new Date(ticket.getInTime().getTime() + durationParkingInMilliseconds));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType,false));
        double ExpectedPrice =  round(REDUCTION_FOR_RECURRENT_USER * fareRatePerHour(parkingType)
                * durationParkingInMilliseconds / NB_MILLISECOND_IN_AN_HOUR);
        //WHEN
        fareCalculatorService.calculateFare(ticket);
        //THEN
        assertEquals(ExpectedPrice, ticket.getPrice());
    }

    @Nested
    @DisplayName("calculate fare  should throw  Exceptions")
    class calculateFareShouldThrowException {

        @Test
        @DisplayName("Fare  Should Throw IlleglArgumentException when  entry after exit")
        public void calculateFareShouldThrowIlleglArgumentExceptionWhenEntryAfterExit() {
            //GIVEN
            ticket.setOutTime(new Date(ticket.getInTime().getTime() -1 ));
            ticket.setParkingSpot(new ParkingSpot(1, CAR,false));
            //WHEN  THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }

        @Test
        @DisplayName("Fare Should Throw IllegalArgumentException when exit Time Null")
        public void calculateFareShouldThrowIllegalArgumentExceptionWhenExitTimeNull() {
            //GIVEN
            ticket.setOutTime(null);
            ticket.setParkingSpot(new ParkingSpot(1, CAR,false));
            //WHEN  THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }

        @Test
        @DisplayName("Fare should throw IllegalArgumentException when parking type is null")
        public void calculateFareShouldThrowIllegalArgumentExceptionWhenParkingTypeIsNull() {
            //GIVEN
            ticket.setOutTime(new Date(ticket.getInTime().getTime() + NB_MILLISECOND_IN_AN_HOUR));
            ticket.setParkingSpot(new ParkingSpot(1, null,false));
            //WHEN THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }

        @Test
        @DisplayName("Fare should throw IllegalArgumentException when parking spot is null")
        public void calculateFareShouldThrowIllegalArgumentExceptionWhenParkingSpotIsNull() {
            //GIVEN
            ticket.setOutTime(new Date(ticket.getInTime().getTime() + NB_MILLISECOND_IN_AN_HOUR));
            ticket.setParkingSpot(null);
            //WHEN THEN
            assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
        }
    }

    private double round (double decimalNumber)  {
        return Math.round((decimalNumber + 0.0001) * COEFF_ROUND_2_DECIMALS) / COEFF_ROUND_2_DECIMALS ;
    }
    private  double fareRatePerHour( ParkingType parkingType ) {
        return parkingType == CAR ? Fare.CAR_RATE_PER_HOUR : Fare.BIKE_RATE_PER_HOUR;
    }

}
