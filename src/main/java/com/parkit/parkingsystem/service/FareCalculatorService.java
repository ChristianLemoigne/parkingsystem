package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    private static final double  NB_HOUR_PER_MILLISECOND =  1.0 / (60.0 * 60 * 1000);
    private static final long NB_MILLISECONDS_IN_30MIN = 30L * 60 * 1000;
    private static final double COEFF_ROUND_2_DECIMALS =  100.0;
    private static final double REDUCTION_RECURRENT_VEHICULE = 0.95;

    private TicketDAO ticketDAO;
    public FareCalculatorService(TicketDAO ticketDAO){
        this.ticketDAO = ticketDAO;
    }

    public void calculateFare(Ticket ticket){

        checkTicket(ticket);
        // STORY#1 : Free 30-min parking
        if ((ticket.getOutTime().getTime() - ticket.getInTime().getTime()) <= NB_MILLISECONDS_IN_30MIN) {
            ticket.setPrice(0);
            return;
        }
        // STORY#2 : 5%-discount for recurring users
        double coeffReduction = isRecurrentUser(ticket.getVehicleRegNumber()) ?  REDUCTION_RECURRENT_VEHICULE : 1.0 ;

        // TASK#1 : Fix the code to make the unit tests pass
        double  durationInHours = NB_HOUR_PER_MILLISECOND * (ticket.getOutTime().getTime() - ticket.getInTime().getTime());

        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                setPriceTicket(ticket, coeffReduction, durationInHours, Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                setPriceTicket(ticket, coeffReduction, durationInHours, Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            //default:
            //    throw new IllegalArgumentException("Unkown Parking Type");
        }
    }

    private void  setPriceTicket (Ticket ticket, double coeffReduction,double durationInHours, double ratePerHour ) {
        ticket.setPrice(round(coeffReduction * durationInHours * ratePerHour ));
    }
    private boolean isRecurrentUser( String   vehicleRegNumber) {
        return ticketDAO.isRecurrentUser(vehicleRegNumber) ;
    }
    private double round (double decimalNumber)  {
        return (Math.round(decimalNumber * COEFF_ROUND_2_DECIMALS))/COEFF_ROUND_2_DECIMALS ;
    }
    private void  checkTicket (Ticket ticket) {
        if ((ticket.getOutTime() == null)) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + "null");
        }
        if ((ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        if ((ticket.getParkingSpot() == null)) {
            throw new IllegalArgumentException("ParkingSpot null");
        }
        if ((ticket.getParkingSpot().getParkingType() == null)) {
            throw new IllegalArgumentException("ParkingType null");
        }
    }

}