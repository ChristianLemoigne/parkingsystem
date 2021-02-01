package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static final String VEHICULE_REG_NUMBER = "ABCDEF";
    private static final int  VEHICULE_TYPE_CAR = 1 ;
    private static final int  VEHICULE_TYPE_BIKE = 2 ;

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @Nested
    @DisplayName("process exiting vehicle")
    class processExitingVehicle {

        @Test
        @DisplayName("process exiting vehicle   Should update parking when vehicule exit")
        public void processExitingVehicle() {

            //GIVEN
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICULE_REG_NUMBER);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(VEHICULE_REG_NUMBER);
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            //WHEN
            parkingService.processExitingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        }

        @Test
        @DisplayName("process exiting vehicle   Should not update parking when exception on readind registration number")
        public void processExitingVehicleShouldNotUpdateParkingWhenExceptionOnReadindRegistrationNumber() {

            //GIVEN
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new Exception("test Exception"));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }
            //WHEN
            parkingService.processExitingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        }

        @Test
        @DisplayName("process exiting vehicle   Should catch exception when updateticket false ")
        public void processExitingVehicleShouldCatchException() {

            //GIVEN
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICULE_REG_NUMBER);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(VEHICULE_REG_NUMBER);
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
            //WHEN
            parkingService.processExitingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        }

    }


    @Nested
    @DisplayName("process incoming vehicle")
    class processIncomingVehicle {

        @Test
        public void processIncomingVehicleCAR() {

            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_CAR);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(4);
            //WHEN
            parkingService.processIncomingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        }

        @Test
        @DisplayName("process incoming vehicle   Should write WelcomeMessage when recurrentuser")
        public void processIncomingVehicleShouldWriteWelcomeMessageWhenRecurrentuser() {

            //GIVEN
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICULE_REG_NUMBER);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }

            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_CAR);
            when(ticketDAO.isRecurrentUser(anyString())).thenReturn(true);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(4);
            //WHEN
            parkingService.processIncomingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
            // verify   text for "recurrent  user" is written  ...  ( How ??? )


        }

        @Test
        @DisplayName("process incoming vehicle   Should not save anything when  WelcomeMessage when no available slot")
        public void processIncomingVehicleWhenNoParkingNumber() {

            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_CAR);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
            //WHEN
            parkingService.processIncomingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
            try {
                verify(inputReaderUtil, Mockito.times(0)).readVehicleRegistrationNumber();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Test
        @DisplayName("process incoming Bike   Should save ticket and  update Parking")
        public void processIncomingVehicleBIKE() {

            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_BIKE);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(4);
            //WHEN
            parkingService.processIncomingVehicle();
            //THEN
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        }

        @Test
        @DisplayName("process incoming Bike   Should not save ticket when exception")
        public void processIncomingVehicleShouldNotsaveTicketWhenException() {

            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_BIKE);
            try {
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new Exception("test Exception"));
                // parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }

            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(4);
            //WHEN
            assertDoesNotThrow(() -> parkingService.processIncomingVehicle());
            //THEN
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
        }

    }

    @Nested
    @DisplayName("getNextParkingNumberIfAvailable")
    class getNextParkingNumberIfAvailable {

        @Test
        @DisplayName("processget next parkingnumberifavailable should return null when no AvailableSpot")
        public void processgetNextParkingNumberIfAvailableshouldreturnNullWhenNoAvailableSpot() {

            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(VEHICULE_TYPE_CAR);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
            //WHEN
            ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();
            //THEN
            assertNull(parkingSpot);
        }

        @Test
        @DisplayName("processget next parkingnumberifavailable should return null when no CAR nor BIKE")
        public void processgetNextParkingNumberIfAvailableshouldreturnNullWhenNoCARNorBike() {
            //GIVEN
            when(inputReaderUtil.readSelection()).thenReturn(3);
            //WHEN
            ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();
            //THEN
            assertNull(parkingSpot);
        }
    }

}
