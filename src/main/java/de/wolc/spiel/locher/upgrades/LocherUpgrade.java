package de.wolc.spiel.locher.upgrades;

import de.wolc.spiel.locher.SimLocher;

/**
 * Stellt ein Locher Upgrade dar.
 * Bitte Subklassen erstellen.
 */
public abstract class LocherUpgrade {
    /**
     * Modifiziert die Stanzer des Lochers.
     * @param locher Der Locher.
     * @param stanzer Die Anzahl der Stanzer.
     * @return Die neue Anzahl der Stanzer.
     */
    public int upgradeStanzer(SimLocher locher, int stanzer) {
        return stanzer;
    }

    /**
     * Modifiziert die Stärke des Lochers.
     * @param locher Der Locher.
     * @param stanzer Die Stärke.
     * @return Die neue Stärke.
     */
    public int upgradeStaerke(SimLocher locher, int staerke) {
        return staerke;
    }
}