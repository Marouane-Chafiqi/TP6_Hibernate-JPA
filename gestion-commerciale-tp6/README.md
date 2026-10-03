# Gestion commerciale - TP 6 : Optimistic locking avec @Version

## Objectif
Simuler un conflit de réservation concurrent et le détecter avec `@Version` (JPA / Hibernate, base H2).

## Technologies
Java 8, Maven, JPA 2.2, Hibernate 5.6, H2

## Structure
```
src/main/java/com/example
├── ConcurrentReservationSimulator.java   (classe principale : les 2 scénarios)
├── OptimisticLockingRetryHandler.java    (réessaie en cas de conflit)
├── model                                 (Utilisateur, Salle, Reservation avec @Version)
└── service                               (ReservationService)
```

## Exécution
1. Ouvrir le projet (NetBeans : File > Open Project / IntelliJ : File > Open > pom.xml).
2. Attendre le chargement des dépendances Maven.
3. Lancer `ConcurrentReservationSimulator` (F6 sous NetBeans).

## Principe
- `@Version` ajoute une colonne `version` à `Reservation`, incrémentée à chaque modification.
- Deux threads lisent la même réservation (version 0) et la modifient.
- Le premier à enregistrer réussit (version 1). Le second a une version périmée :
  Hibernate lève une `OptimisticLockException`.
- Avec le retry, le thread en conflit relit la réservation (version 1) et réessaie : les deux modifications sont conservées (version 2).

## Résultat attendu
- **Sans retry** : Thread 2 réussit, Thread 1 : conflit. Version finale = 1, seules les dates sont modifiées.
- **Avec retry** : Thread 2 réussit, Thread 1 : conflit puis réussite à la 2e tentative. Version finale = 2, motif et dates modifiés.
