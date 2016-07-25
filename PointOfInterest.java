/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author raoul
 */
public class PointOfInterest {
    
    private int index;
    private float reputationScore;
    private String realState; //etat réel u point d'intérêt
    private String predictedState; //état prédit du point d'intérêt

    public PointOfInterest() {
    }

    public PointOfInterest(int index) {
        this.index = index;
    }
    
    

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public float getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(float reputationScore) {
        this.reputationScore = reputationScore;
    }

    /**
     * @return the State
     */
    public String getRealState() {
        return realState;
    }

    /**
     * @param State the State to set
     */
    public void setRealState(String State) {
        this.realState = State;
    }

    /**
     * @return the predictedState
     */
    public String getPredictedState() {
        return predictedState;
    }

    /**
     * @param predictedState the predictedState to set
     */
    public void setPredictedState(String predictedState) {
        this.predictedState = predictedState;
    }
    
}
