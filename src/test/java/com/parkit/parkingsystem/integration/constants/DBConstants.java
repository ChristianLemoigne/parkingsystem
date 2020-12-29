package com.parkit.parkingsystem.integration.constants;

public class DBConstants {

       public static final String GET_TICKET = "select t.PARKING_NUMBER, t.ID, t.PRICE, t.IN_TIME, t.OUT_TIME, p.TYPE , p.AVAILABLE from ticket t,parking p where p.parking_number = t.parking_number and t.VEHICLE_REG_NUMBER=? order by t.IN_TIME  limit 1";
       public static final String COUNT_TICKET = "select count(*) from ticket";


}
