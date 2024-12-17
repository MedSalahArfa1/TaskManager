package com.fsb.taskmanager.Model;

import java.util.Date;

public class TaskModel {
    private int id;
    private String titre;
    private String description;
    private Date date_echeance;
    private int statut;
    private int rappel;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate_echeance() {
        return date_echeance;
    }

    public void setDate_echeance(Date date_echeance) {
        this.date_echeance = date_echeance;
    }

    public int getStatut() {
        return statut;
    }

    public void setStatut(int statut) {
        this.statut = statut;
    }

    public int getRappel() {
        return rappel;
    }

    public void setRappel(int rappel) {
        this.rappel = rappel;
    }

}
