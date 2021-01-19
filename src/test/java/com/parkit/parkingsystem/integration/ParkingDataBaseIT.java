package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.daoTest.DAOTestIT;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    private static DAOTestIT dAOTestIT;

    // TODO   reviser le  static
    private static final String  VEHICLE_REGISTRATION_NUMBER = "ABCDEF" ;
    private static final int TYPE_VEHICLE_INPUT_CAR = 1 ;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();

        dAOTestIT = new DAOTestIT();
        dAOTestIT.dataBaseConfig = dataBaseTestConfig;
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REGISTRATION_NUMBER);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
    }


    @Test
    public void testParkingACar(){

        //Check that a ticket is actualy saved in DB and Parking table is updated with availability

        //GIVEN

        when(inputReaderUtil.readSelection()).thenReturn(TYPE_VEHICLE_INPUT_CAR);    // mi ici car inutile pour le test 2
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        //WHEN

        parkingService.processIncomingVehicle();

        //THEN

        String requestTestParkingACar = "select p.TYPE , p.AVAILABLE from ticket t,parking p where p.parking_number = t.parking_number " +
               "and t.VEHICLE_REG_NUMBER=" + "'" + VEHICLE_REGISTRATION_NUMBER + "'";


        Connection connection = null;
        try{
            connection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = connection.prepareStatement(requestTestParkingACar);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next()) ;

            assertEquals(ParkingType.CAR.toString(), rs.getString(1));
            assertFalse(rs.getBoolean(2));

            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }
    }


    @Test
    public void testParkingLotExit(){


        //GIVEN

        // 2 façon de voir les choses sur le  given :   s'appuyzr sur un   process rentrant     ou bien le créer à la main
        // 2 ième façon est la mieux car  garanti indépendance avec le process incoming  ( notamment  il n'est peut être pas développé

        // ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        // parkingService.processIncomingVehicle();

        Connection connection = null;
        try{
            connection = dataBaseTestConfig.getConnection();

            String requestInsetTicket =
                    "insert into ticket values ( 1, 1 ,  '"  + VEHICLE_REGISTRATION_NUMBER + "' , 0, DATE_SUB(now() , INTERVAL 1 HOUR), null)" ;
            connection.prepareStatement(requestInsetTicket).execute();

            String requestUpdateParking =
                    "update parking set  AVAILABLE = false where  PARKING_NUMBER=1" ;
            connection.prepareStatement(requestUpdateParking).execute();

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }


        //WHEN

        // à  voir et à faire   : mettre dans le befor each
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        //THEN
        String requestTestExiting = "select t.PRICE  , t.OUT_TIME , p.AVAILABLE from ticket t,parking p where p.parking_number = t.parking_number " +
                "and t.VEHICLE_REG_NUMBER=" + "'" + VEHICLE_REGISTRATION_NUMBER + "'";

        try{
            connection = dataBaseTestConfig.getConnection();
            PreparedStatement ps = connection.prepareStatement(requestTestExiting);
            ResultSet rs = ps.executeQuery();
            boolean ticketSaved = false ;
            if(rs.next()) {
                ticketSaved = true ;
                assertTrue(rs.getBoolean(3));
                assertEquals(rs.getDouble(1) , Fare.CAR_RATE_PER_HOUR, 0.001);
                assertNotNull(rs.getTimestamp(2));
                Date datecurrent = new Date() ;
                assertEquals(datecurrent.getTime(),rs.getTimestamp(2).getTime(), 100);
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

}
