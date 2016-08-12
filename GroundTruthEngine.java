
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author raoul
 */
public class GroundTruthEngine {
    
        private ConcurrentHashMap<Integer, List<Double>> uReputations; //reputations des participants (reels)
        private ConcurrentHashMap<Integer, String> pReputations; //reputations des points d'intérêt
        private double lamda_user;
         private int nbpoints;
        
        public GroundTruthEngine(int n){
         
            uReputations = new ConcurrentHashMap<>();
            pReputations = new ConcurrentHashMap<>();
            //lamda_user = 1;
            lamda_user = 0.9; //facteur d'oubli des participants
            this.nbpoints = n; //nombre de points d'intérêt
            
        }
        

        
        /**
        * permet de mettre à jour la réputation et/ou les paramètres alpha et beta des contributeurs d'un pt d'intérêt
        * @param observationMatrix la matrice d'observation
        * @param mapParticipants la map de tous les participants
        * @param pt le pt d'intérêt concerné par l'observation
        * @param u le contributeur qui fait l'observation
        * @param option option spécifiant le contexte d'exécution
        */
        
        public synchronized void UpdateContributorReputation(Contributor u, PointOfInterest pt, ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix, ConcurrentHashMap<Integer, Contributor> mapParticipants){
            

            double E;
            double uro, uso, r_new = 0, s_new = 0;
       
 
          
                    List<Double> lstReputation;
             
                    uro = u.getBetaR(); //valeur du paramètre alpha du contributeur u
                    uso = u.getBetaS(); //valeur du paramètre beta du contributeur u
                    int obs = u.getArrayValues()[pt.getIndex()]; //observation du contributeur u concernant le point d'intérêt pt
                    
                    switch (pt.getRealState()){
                                
                    case "VRAI":
                                    
                            switch (obs) {
                                case 1: 
                                           
                                        r_new = this.lamda_user *uro + 1;
                                        s_new = this.lamda_user *uso;
                                        break;

                                case 0:
                                        s_new = this.lamda_user *uso + 1;
                                        r_new = this.lamda_user *uro;
                                        break;

                                default: 
                                            break;
                            }
                            break;
                                
                    case "FAUX":
                        
                            switch (obs) {
                                case 1: 
                                           
                                        s_new = this.lamda_user *uso + 1;
                                        r_new = this.lamda_user *uro;
                                        break;

                                case 0:
                                        r_new = this.lamda_user *uro + 1;
                                        s_new = this.lamda_user *uso; 
                                        break;

                                default: 
                                            break;
                            }        
                            break;
                    default: 
                                break;                
                                
                        }      
                        
 
                        //Mise à jour des paramètres du contributeur u
                         u.setBetaR(r_new);
                         u.setBetaS(s_new);

 
                        //calcul de la réputation du contributeur u
                        E = (((double) r_new)/(r_new + s_new)); 

                        //Mise à jour de la réputation
                        if(this.getuReputations().containsKey(u.getIndex())){

                            lstReputation = this.getuReputations().get(u.getIndex());
                            lstReputation.set(pt.getIndex(), round2(E, 2));


                        }else{


                            lstReputation = new ArrayList<Double>(Collections.nCopies(nbpoints, 0.0));
                            lstReputation.set(pt.getIndex(), round2(E, 2));
                            this.getuReputations().put(u.getIndex(), lstReputation);

                        }
                        

                

  
        }

    /**
     * @return the uReputations
     */
    public Map<Integer, List<Double>> getuReputations() {
        return uReputations;
    }

    /**
     * @param uReputations the uReputations to set
     */
    public void setuReputations(ConcurrentHashMap<Integer, List<Double>> uReputations) {
        this.uReputations = uReputations;
    }

    /**
     * @return the pReputations
     */
    public Map<Integer, String> getpReputations() {
        return pReputations;
    }

    /**
     * @param pReputations the pReputations to set
     */
    public void setpReputations(ConcurrentHashMap<Integer, String> pReputations) {
        this.pReputations = pReputations;
    }
    
    /**
     * permet de calculer l'arrondi
     * @param number le nombre à arrondir
     * @param scale
     * @return l'arrondi
    */
    public static double round2(double number, int scale) {
    int pow = 10;
    for (int i = 1; i < scale; i++)
        pow *= 10;
    double tmp = number * pow;
    return (double) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }



}
