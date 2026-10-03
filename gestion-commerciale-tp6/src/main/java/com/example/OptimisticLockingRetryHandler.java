package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationService;

import javax.persistence.OptimisticLockException;
import java.util.function.Consumer;

public class OptimisticLockingRetryHandler {

    private final ReservationService service;
    private final int maxRetries;

    public OptimisticLockingRetryHandler(ReservationService service, int maxRetries) {
        this.service = service;
        this.maxRetries = maxRetries;
    }

    public void executeWithRetry(Long id, Consumer<Reservation> operation) {
        int tentative = 0;

        while (tentative < maxRetries) {
            tentative++;
            try {
                Reservation reservation = service.findById(id);
                System.out.println("Tentative " + tentative + " : version lue = " + reservation.getVersion());

                operation.accept(reservation);
                service.update(reservation);

                System.out.println("Operation reussie apres " + tentative + " tentative(s)");
                return;
            } catch (OptimisticLockException e) {
                System.out.println("Tentative " + tentative + " : conflit detecte !");
                if (tentative >= maxRetries) {
                    throw e;
                }
                try {
                    Thread.sleep(100L * tentative);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
