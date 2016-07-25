/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author raoul
 */
public class BetaOption {
    
    private boolean isTraining; //défini le contexte d'exécution : soit on se base sur l'etat réel des points d'intérêt, soit on estime l'état des points d'intérêts
    private String state; //état du point d'intérêt en fonction du contexte choisi
    
    
    public BetaOption(boolean istraining, String state){
        
        this.isTraining = istraining;
        this.state = state;
    }

    /**
     * @return the isTraining
     */
    public boolean isIsTraining() {
        return isTraining;
    }

    /**
     * @param isTraining the isTraining to set
     */
    public void setIsTraining(boolean isTraining) {
        this.isTraining = isTraining;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }
    
}
