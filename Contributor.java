
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author raoul
 */
public class Contributor implements Cloneable{
    
    private int index;
    private double betaR;//paramètre alpha de la methode de beta reputation
    private double betaS;//paramètre beta de la methode de beta reputation
    private double betaR2;//paramètre alpha de la methode de beta reputation v2
    private double betaS2;//paramètre beta de la methode de beta reputation v2
    private double trustScore;//réputation du participant pour la beta reputation
    private double predictedTrust;
    private double errorRate;// taux d'erreur du participant
    private  int [] arrayValues; // tableau contenant les feedbacks générées par le participant pour chaque points
    private  String [] arrayState;//tableau contenant l'etat des reputations des points au moment du feedback du participant
    private  int [] arrayoldValues; // tableau contenant les feedbacks générées par le participant pour chaque points
    private  String [] arrayoldState;//tableau contenant l'etat des reputations des points au moment du feedback du participant
    private HashSet avalaiblePoints;// hashset contenant l'index des points qui n'ont pas encore reçu leur feedback de ce participant
    private double [] cooperativeRatings;//tableau des indices de confiance pour la methode Gompertz
    private double [] cooperativeRatings5;//tableau des indices de confiance pour la methode beta reputation v2
    private double [] cooperativeRatings2;//tableau des indices de confiance pour la methode Robust Averaging
    private double [] cooperativeRatings3;//tableau des indices de confiance pour la methode ARM
    private List<Integer> currentPointsList; //represente les index des points pour lesquelles ce contributeur a émis une observation
    private int nbObservations; // représente le nombre de feedbacks du participant à un moment donné
    private boolean [] firstContributor; //tableau indiquant si ce contributeur est le premier contributeur ou non

  

    
    public Contributor(int index) {

        this.index = index;
        this.betaR = 1;
        this.betaS = 1;
        this.betaR2 = 1;
        this.betaS2 = 1;
        this.trustScore = 0.5;  //on initialise la reputation des participants à 0.5
        this.nbObservations = 0;
        this.currentPointsList = new ArrayList<Integer>();





        
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double tauxerreur) {
        this.errorRate = tauxerreur;
    }

    
    @Override
	protected Object clone() throws CloneNotSupportedException 
	{
		Contributor p = null;

	    try {
	    	// On récupère l'instance à renvoyer par l'appel de la 
	      	// méthode super.clone()
	      	p = (Contributor) super.clone();
	    } catch(CloneNotSupportedException cnse) {
	      	// Ne devrait jamais arriver car nous implémentons 
	      	// l'interface Cloneable
	      	cnse.printStackTrace(System.err);
	    }

	    // on renvoie le clone

            return p;
            
	}

    /**
     * @return the betaR
     */
    public double getBetaR() {
        return betaR;
    }

    /**
     * @param betaR the betaR to set
     */
    public void setBetaR(double betaR) {
        this.betaR = betaR;
    }

    /**
     * @return the betaS
     */
    public double getBetaS() {
        return betaS;
    }

    /**
     * @param betaS the betaS to set
     */
    public void setBetaS(double betaS) {
        this.betaS = betaS;
    }

    /**
     * @return the betaR2
     */
    public double getBetaR2() {
        return betaR2;
    }

    /**
     * @param betaR2 the betaR2 to set
     */
    public void setBetaR2(double betaR2) {
        this.betaR2 = betaR2;
    }

    /**
     * @return the betaS2
     */
    public double getBetaS2() {
        return betaS2;
    }

    /**
     * @param betaS2 the betaS2 to set
     */
    public void setBetaS2(double betaS2) {
        this.betaS2 = betaS2;
    }

    /**
     * @return the predictedTrust
     */
    public double getPredictedTrust() {
        return predictedTrust;
    }

    /**
     * @param predictedTrust the predictedTrust to set
     */
    public void setPredictedTrust(double predictedTrust) {
        this.predictedTrust = predictedTrust;
    }

 
    /**
     * @return the arrayValues
     */
    public int[] getArrayValues() {
        return arrayValues;
    }

    /**
     * @param arrayValues the arrayValues to set
     */
    public void setArrayValues(int[] arrayValues) {
        this.arrayValues = arrayValues;
    }

    /**
     * @return the arrayState
     */
    public String[] getArrayState() {
        return arrayState;
    }

    /**
     * @param arrayState the arrayState to set
     */
    public void setArrayState(String[] arrayState) {
        this.arrayState = arrayState;
    }

    /**
     * @return the avalaiblePoints
     */
    public HashSet getAvalaiblePoints() {
        return avalaiblePoints;
    }

    /**
     * @param avalaiblePoints the avalaiblePoints to set
     */
    public void setAvalaiblePoints(HashSet avalaiblePoints) {
        this.avalaiblePoints = avalaiblePoints;
    }

    /**
     * @return the nbObservations
     */
    public int getNbObservations() {
        return nbObservations;
    }

    /**
     * @param nbObservations the nbObservations to set
     */
    public void setNbObservations(int nbObservations) {
        this.nbObservations = nbObservations;
    }

    /**
     * @return the cooperativeRatings
     */
    public double[] getCooperativeRatings() {
        return cooperativeRatings;
    }

    /**
     * @param cooperativeRatings the cooperativeRatings to set
     */
    public void setCooperativeRatings(double[] cooperativeRatings) {
        this.cooperativeRatings = cooperativeRatings;
    }



    /**
     * @return the cooperativeRatings3
     */
    public double[] getCooperativeRatings3() {
        return cooperativeRatings3;
    }

    /**
     * @param cooperativeRatings3 the cooperativeRatings3 to set
     */
    public void setCooperativeRatings3(double[] cooperativeRatings3) {
        this.cooperativeRatings3 = cooperativeRatings3;
    }


    
    public void addToCurrentPointList(int val){
        if(!this.currentPointsList.contains(val)){
            
            this.getCurrentPointsList().add(val);
        }

    }

    /**
     * @return the currentPointsList
     */
    public List<Integer> getCurrentPointsList() {
        return currentPointsList;
    }

    /**
     * @param currentPointsList the currentPointsList to set
     */
    public void setCurrentPointsList(List<Integer> currentPointsList) {
        this.currentPointsList = currentPointsList;
    }
    
    /**
     * @return the trustScore
     */
    public double getTrustScore() {
        
        this.trustScore = (((double) this.betaR2)/(this.betaR2 + this.betaS2));
         
        return this.trustScore;
    }

    /**
     * @param trustScore the trustScore to set
     */
    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    /**
     * @return the firstContributor
     */
    public boolean[] getFirstContributor() {
        return firstContributor;
    }

    /**
     * @param firstContributor the firstContributor to set
     */
    public void setFirstContributor(boolean[] firstContributor) {
        this.firstContributor = firstContributor;
    }

    /**
     * @return the cooperativeRatings5
     */
    public double[] getCooperativeRatings5() {
        return cooperativeRatings5;
    }

    /**
     * @param cooperativeRatings5 the cooperativeRatings5 to set
     */
    public void setCooperativeRatings5(double[] cooperativeRatings5) {
        this.cooperativeRatings5 = cooperativeRatings5;
    }

    /**
     * @return the cooperativeRatings2
     */
    public double[] getCooperativeRatings2() {
        return cooperativeRatings2;
    }

    /**
     * @param cooperativeRatings2 the cooperativeRatings2 to set
     */
    public void setCooperativeRatings2(double[] cooperativeRatings2) {
        this.cooperativeRatings2 = cooperativeRatings2;
    }

    /**
     * @return the arrayoldValues
     */
    public int[] getArrayoldValues() {
        return arrayoldValues;
    }

    /**
     * @param arrayoldValues the arrayoldValues to set
     */
    public void setArrayoldValues(int[] arrayoldValues) {
        this.arrayoldValues = arrayoldValues;
    }

    /**
     * @return the arrayoldState
     */
    public String[] getArrayoldState() {
        return arrayoldState;
    }

    /**
     * @param arrayoldState the arrayoldState to set
     */
    public void setArrayoldState(String[] arrayoldState) {
        this.arrayoldState = arrayoldState;
    }










       

}
