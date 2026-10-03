package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.service.ReservationService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

public class ConcurrentReservationSimulator {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-commerciale");
    private static final ReservationService service = new ReservationService(emf);

    private static Long utilisateurId;
    private static Long salleId;

    public static void main(String[] args) throws InterruptedException {
        creerUtilisateurEtSalle();

        System.out.println("\n=== Conflit SANS retry ===");
        Long id1 = creerReservation();
        sansRetry(id1);
        afficherEtat(id1);

        System.out.println("\n=== Conflit AVEC retry ===");
        Long id2 = creerReservation();
        avecRetry(id2);
        afficherEtat(id2);

        emf.close();
    }

    private static void creerUtilisateurEtSalle() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Utilisateur utilisateur = new Utilisateur("Dupont", "Jean", "jean.dupont@example.com");
        Salle salle = new Salle("Salle A101", 30);
        em.persist(utilisateur);
        em.persist(salle);

        em.getTransaction().commit();
        utilisateurId = utilisateur.getId();
        salleId = salle.getId();
        em.close();
    }

    private static Long creerReservation() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        LocalDateTime debut = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Reservation reservation = new Reservation(debut, debut.plusHours(2), "Reunion d'equipe");
        reservation.setUtilisateur(em.find(Utilisateur.class, utilisateurId));
        reservation.setSalle(em.find(Salle.class, salleId));
        em.persist(reservation);

        em.getTransaction().commit();
        Long id = reservation.getId();
        em.close();
        return id;
    }

    private static void sansRetry(Long id) throws InterruptedException {
        CountDownLatch lecture = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            Reservation r = service.findById(id);
            System.out.println("Thread 1 : version lue = " + r.getVersion());
            attendreLesDeux(lecture);

            pause(1000);
            r.setMotif("Modifie par Thread 1");
            try {
                service.update(r);
                System.out.println("Thread 1 : mise a jour reussie");
            } catch (OptimisticLockException e) {
                System.out.println("Thread 1 : CONFLIT detecte (OptimisticLockException)");
            }
        });

        Thread thread2 = new Thread(() -> {
            Reservation r = service.findById(id);
            System.out.println("Thread 2 : version lue = " + r.getVersion());
            attendreLesDeux(lecture);

            r.setDateDebut(r.getDateDebut().plusHours(1));
            r.setDateFin(r.getDateFin().plusHours(1));
            try {
                service.update(r);
                System.out.println("Thread 2 : mise a jour reussie");
            } catch (OptimisticLockException e) {
                System.out.println("Thread 2 : CONFLIT detecte (OptimisticLockException)");
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    private static void avecRetry(Long id) throws InterruptedException {
        OptimisticLockingRetryHandler handler = new OptimisticLockingRetryHandler(service, 3);

        Thread thread1 = new Thread(() -> {
            handler.executeWithRetry(id, r -> {
                System.out.println("Thread 1 : modification du motif");
                r.setMotif("Modifie par Thread 1");
                pause(1000);
            });
        });

        Thread thread2 = new Thread(() -> {
            pause(200);
            handler.executeWithRetry(id, r -> {
                System.out.println("Thread 2 : modification des dates");
                r.setDateDebut(r.getDateDebut().plusHours(1));
                r.setDateFin(r.getDateFin().plusHours(1));
            });
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    private static void afficherEtat(Long id) {
        Reservation r = service.findById(id);
        System.out.println("\nEtat final :");
        System.out.println("Motif : " + r.getMotif());
        System.out.println("Debut : " + r.getDateDebut());
        System.out.println("Fin : " + r.getDateFin());
        System.out.println("Version : " + r.getVersion());
    }

    private static void attendreLesDeux(CountDownLatch latch) {
        latch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
