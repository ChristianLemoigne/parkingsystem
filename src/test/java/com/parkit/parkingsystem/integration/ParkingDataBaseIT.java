package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final String  VEHICLE_REGISTRATION_NUMBER = "ABCDEF" ;
    private static final int TYPE_VEHICLE_INPUT_CAR = 1 ;
    private static final float REDUCTION_FOR_RECURRENT_USER = (float) 0.95;
    private static final double COEFF_ROUND_2_DECIMALS =  100.0;

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private ParkingService parkingService ;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REGISTRATION_NUMBER);
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @AfterAll
    private static void tearDown(){
    }

    @Test
    @DisplayName("When parking a car, a ticket should be saved in DB Parking table is updated with availability")
    public void testParkingACar(){

        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(TYPE_VEHICLE_INPUT_CAR);
        //WHEN
        parkingService.processIncomingVehicle();
        //THEN
        String requestTestParkingACar = "select p.TYPE , p.AVAILABLE , t.OUT_TIME , t.PRICE from ticket t,parking p where p.parking_number = t.parking_number " +
               "and t.VEHICLE_REG_NUMBER=" + "'" + VEHICLE_REGISTRATION_NUMBER + "'";

        Connection connection = null;
        try{
            connection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = connection.prepareStatement(requestTestParkingACar);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next()) ;
            assertEquals(ParkingType.CAR.toString(), rs.getString(1));
            assertFalse(rs.getBoolean(2));
            assertNull(rs.getTimestamp(3));
            assertEquals(0.0 , rs.getDouble(4));
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }
    }

    @Test
    @DisplayName("When Exit a car from a parking, price should be calculated and  DB Parking table is updated with availability")
    public void testParkingLotExit() {

        //GIVEN
        Connection connection = null;
        try {
            connection = dataBaseTestConfig.getConnection();

            String requestInsetTicket =
                    "insert into ticket values ( 1, 1 ,  '" + VEHICLE_REGISTRATION_NUMBER + "' , 0, DATE_SUB(now() , INTERVAL 1 HOUR), null)";
            connection.prepareStatement(requestInsetTicket).execute();

            String requestUpdateParking =
                    "update parking set  AVAILABLE = false where  PARKING_NUMBER=1";
            connection.prepareStatement(requestUpdateParking).execute();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataBaseTestConfig.closeConnection(connection);
        }
        //WHEN
        parkingService.processExitingVehicle();
        //THEN
        String requestTestExiting = "select t.PRICE  , t.OUT_TIME , p.AVAILABLE from ticket t,parking p where p.parking_number = t.parking_number " +
                "and t.VEHICLE_REG_NUMBER=" + "'" + VEHICLE_REGISTRATION_NUMBER + "'";

        try {
            connection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = connection.prepareStatement(requestTestExiting);
            ResultSet rs = ps.executeQuery();
            boolean ticketSaved = false;
            if (rs.next()) {
                ticketSaved = true;
                assertTrue(rs.getBoolean(3));
                //we have to use a "delta" since  the duration isn't exactly one hour
                assertEquals(rs.getDouble(1), Fare.CAR_RATE_PER_HOUR,0.03);
                assertNotNull(rs.getTimestamp(2));
                Date datecurrent = new Date();
                assertEquals(datecurrent.getTime(), rs.getTimestamp(2).getTime(), 100);
            }
            assertTrue(ticketSaved);
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataBaseTestConfig.closeConnection(connection);
        }

    }

    @Test
    @DisplayName("Price should be reducted for a recurrent user")
    public void testParkingLotExitForRecurrentUser(){

        //GIVEN
        Connection connection = null;
        try{
            connection = dataBaseTestConfig.getConnection();

            String requestInsertOldTicket =
                    "insert into ticket values ( 1, 1 ,  '"  + VEHICLE_REGISTRATION_NUMBER + "' , 0, DATE_SUB(now(), INTERVAL 10 HOUR), DATE_SUB(now(), INTERVAL 9 HOUR))" ;
            connection.prepareStatement(requestInsertOldTicket).execute();

            String requestInsertNewTicket =
                    "insert into ticket values ( 2, 2 ,  '"  + VEHICLE_REGISTRATION_NUMBER + "' , 0, DATE_SUB(now() , INTERVAL 1 HOUR), null)" ;
            connection.prepareStatement(requestInsertNewTicket).execute();

            String requestUpdateParking =
                    "update parking set  AVAILABLE = false where  PARKING_NUMBER=2" ;
            connection.prepareStatement(requestUpdateParking).execute();

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }

        //WHEN
        parkingService.processExitingVehicle();

        //THEN
        String requestTestExiting = "select t.PRICE  , t.OUT_TIME , p.AVAILABLE from ticket t,parking p where " +
                "t.id =2 " +
                "and p.parking_number = t.parking_number " ;

        try{
            connection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = connection.prepareStatement(requestTestExiting);
            ResultSet rs = ps.executeQuery();
            boolean ticketSaved = false ;
            if(rs.next()) {
                ticketSaved = true ;
                assertTrue(rs.getBoolean(3));
                //we have to use a "delta" since  the duration isn't exactly one hour
                assertEquals(rs.getDouble(1), REDUCTION_FOR_RECURRENT_USER *  Fare.CAR_RATE_PER_HOUR, 0.03);
                assertNotNull(rs.getTimestamp(2));
                Date datecurrent = new Date() ;
                assertEquals(datecurrent.getTime(),rs.getTimestamp(2).getTime(), 30000);
            }
            assertTrue (ticketSaved) ;
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }
    }


    private double round (double decimalNumber)  {
        return Math.round((decimalNumber + 0.001) * COEFF_ROUND_2_DECIMALS) / COEFF_ROUND_2_DECIMALS ;
    }

}
