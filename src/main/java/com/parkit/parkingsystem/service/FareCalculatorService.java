package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    private static final double  NB_HOUR_PER_MILLISECOND =  1.0 / (60.0 * 60 * 1000)  ;
    private static final long NB_MILLISECONDS_IN_30MIN = 30L * 60 * 1000 ;
    private static final double COEFF_ARRONDI =  1 / 0.01 ;
    private static final double REDUCTION_RECURRENT_VEHICULE = 0.95 ;

    private TicketDAO ticketDAO;

    public FareCalculatorService(TicketDAO ticketDAO){
        this.ticketDAO = ticketDAO;
    }

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null)  ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ "null");
        }
        if( (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        // Parking is free  if duration  less than 30 min
        if ((ticket.getOutTime().getTime() - ticket.getInTime().getTime()) <= NB_MILLISECONDS_IN_30MIN) {
            ticket.setPrice(0);
            return;
        }
        double  durationInHours = NB_HOUR_PER_MILLISECOND * (ticket.getOutTime().getTime() - ticket.getInTime().getTime());
        // reduction for recurrent user
        double coeffReduction = isRecurrentUser(ticket.getVehicleRegNumber()) ?  REDUCTION_RECURRENT_VEHICULE : 1.0 ;

        /*
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                setPriceTicket( ticket, coeffReduction,  durationInHours, Fare.CAR_RATE_PER_HOUR  ) ;
                break;
            }
            case BIKE: {
                setPriceTicket( ticket, coeffReduction,  durationInHours, Fare.BIKE_RATE_PER_HOUR  ) ;
                break;
            }
         */

        //TODO  GEOFFREY:  !!                       !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //  ********************
        //   PB   Tests unitaires
        //   enlever cette ligne dans laquelleon passe jamais        ????
        //  ou ajouter un  3 ième parking type dans l'enum pour rentrer dans la lisse dessous par un TU
        //     en +   si parkinType  null  :  null pointer !
        //default: throw new IllegalArgumentException("Unkown Parking Type");



        //  ASK  GEOFFREY    :  ajouter ce test ?
        //if( (ticket.getParkingSpot() == null)  ){
        //    throw new IllegalArgumentException("ParkingSpot null");
        //}

        if (ParkingType.CAR ==  ticket.getParkingSpot().getParkingType()) {
            setPriceTicket(ticket, coeffReduction, durationInHours, Fare.CAR_RATE_PER_HOUR);
        } else {
            if (ParkingType.BIKE ==  ticket.getParkingSpot().getParkingType()){
                setPriceTicket( ticket, coeffReduction, durationInHours, Fare.BIKE_RATE_PER_HOUR  ) ;
            } else {
                throw new IllegalArgumentException("Unkown Parking Type");
            }
        }

    }

    //  ASK GEOFFREY  !  faire methode setPriceTicket ( , , ,)  sur classe ticket ??? :
    private void  setPriceTicket (Ticket ticket, double coeffReduction,double durationInHours, double ratePerHour ) {
        ticket.setPrice ( Math.round(coeffReduction * durationInHours * ratePerHour * COEFF_ARRONDI)/COEFF_ARRONDI );
    }
    private boolean isRecurrentUser( String   vehicleRegNumber) {
        return ticketDAO.isRecurrentUser(vehicleRegNumber) ;
    }

}