package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class FareCalculatorService {


    private TicketDAO ticketDAO;

    private static final double  NB_HOUR_PER_MILLISECOND =  1.0 / (60.0 * 60 * 1000)  ;
    private static final long NB_MILLISECONDS_IN_30MIN = 30 * 60 * 1000 ;

    // Arrondi :    précision de l'arrondi à 2 décimales , avec méthode math
    // TODO  :    méthode à se faire valider par la maitrise d'ouvrage

    private static final double COEFF_ARRONDI =  1 / 0.01 ;
    private static final double REDUCTION_RECURRENT_VEHICULE = 0.95 ;

    public FareCalculatorService(TicketDAO ticketDAO){
        this.ticketDAO = ticketDAO;
    }


    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }


        if ((ticket.getOutTime().getTime() - ticket.getInTime().getTime()) <= NB_MILLISECONDS_IN_30MIN) {
            // Free in less than 30 min
            ticket.setPrice(0);
            return;
        }

        double CoeffReduction  ;
        if  ( ticketDAO.isRecurrentUser(ticket.getVehicleRegNumber()) ) {
            CoeffReduction = REDUCTION_RECURRENT_VEHICULE ;
        } else {
            CoeffReduction = 1.0;
        }
        double  durationInHours = NB_HOUR_PER_MILLISECOND * (ticket.getOutTime().getTime() - ticket.getInTime().getTime());

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(Math.round(CoeffReduction * durationInHours * Fare.CAR_RATE_PER_HOUR * COEFF_ARRONDI)/COEFF_ARRONDI);
                break;
            }
            case BIKE: {
                ticket.setPrice(Math.round(CoeffReduction * durationInHours * Fare.BIKE_RATE_PER_HOUR * COEFF_ARRONDI)/COEFF_ARRONDI);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }

    }


    private boolean isRecurrentUser( String   VehicleRegNumber) {
        return ticketDAO.isRecurrentUser(VehicleRegNumber) ;
    }


}