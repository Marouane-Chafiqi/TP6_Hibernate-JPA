# TP 6 : Optimistic locking avec @Version

## Objectif
Simuler un conflit de réservation concurrent et le détecter avec `@Version` (JPA / Hibernate, H2).

## Lancer
Ouvrir le projet (NetBeans ou IntelliJ), attendre Maven, puis exécuter `ConcurrentReservationSimulator`.

## Principe
- `@Version` ajoute une colonne `version`, incrémentée à chaque modification.
- Deux threads modifient la même réservation : le premier réussit, le second a une version périmée et reçoit une `OptimisticLockException`.
- Avec le retry, le second thread relit la réservation et réessaie : les deux modifications sont conservées.

## Résultat attendu
<img width="443" height="419" alt="Capture d&#39;écran 2026-10-03 022825" src="https://github.com/user-attachments/assets/004f2402-e920-4a03-ba54-a640f817c2d4" />

