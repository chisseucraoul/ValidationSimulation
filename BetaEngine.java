
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
public class BetaEngine {
    
        private ConcurrentHashMap<Integer, List<Double>> uReputations; //reputations des participants (reels)
        private ConcurrentHashMap<Integer, List<Double>> uReputations2; //reputations des participants (prediction)
        private ConcurrentHashMap<Integer, String> pReputations; //reputations des points d'intérêt
        private double lamda_user;
         private int nbpoints;
        
        public BetaEngine(int n){
         
            uReputations = new ConcurrentHashMap<>();
            uReputations2 = new ConcurrentHashMap<>();
            pReputations = new ConcurrentHashMap<>();
            //lamda_user = 1;
            lamda_user = 0.9; //facteur d'oubli des participants
            this.nbpoints = n; //nombre de points d'intérêt
            
        }
        
        
        /**
        * permet de mettre à jour la réputation des contributeurs d'un pt d'intérêt
        * @param observationMatrix la matrice d'observation
        * @param pt le pt d'intérêt concerné par l'observation
        * @param u le contributeur qui fait l'observation
        * @return option spécifiant le contexte d'exécution
        */
        public synchronized BetaOption ProcessPoiReputation(boolean isTraining, ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix, PointOfInterest pt, Contributor u){
            
            String predictedState; int user_feedback;
            int user_index;
            double E = 0;
            double uro = 0, uso = 0, v, r, s, r_new = 0, s_new = 0;
           
            if(isTraining){  //contexte dans lequel on tient compte de l'etat réel des points d'intérêt afin de calculer les réputations des contributeurs réellement attendus.
                
                predictedState = pt.getRealState();
                
                
            }else{ //sinon on détermine l'etat prédictif des points d'intérêt
                
                
                //ici on calcule la reputation du point d'intérêt
                if(observationMatrix.containsKey(pt.getIndex())){
                    
                        ConcurrentHashMap<Integer, Contributor> users = observationMatrix.get(pt.getIndex());
                        Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator3 = users.entrySet().iterator();
                         while(iterator3.hasNext()) {
                                ConcurrentHashMap.Entry<Integer, Contributor> next = iterator3.next();
                                Contributor u2 = next.getValue(); //on récupère un contributeur du point d'intérêt
                                user_feedback = u2.getArrayValues()[pt.getIndex()];//on recupere l'observation du contributeur pour ce point d'intérêt

                                if((user_feedback == 1)){

                                    uro = uro + u2.getTrustScore(); //on incremente alpha avec la réputation de ce contributeur
                                    uso = uso;


                                } else {

                                    uso = uso + u2.getTrustScore(); //on incremente beta avec la réputation de ce contributeur
                                    uro = uro;

                                }  

                         }

                         E = (((double) uro)/(uro + uso)); //calcul de la reputation du point d'intérêt pt 

                }
                
                //on classifie la valeur obtenue
                if(E < 0.5){
                   
                    predictedState = "FAUX"; //le point d'intérêt pt n'est pas présent
                    
                }else{
                    
                    predictedState = "VRAI"; //le point d'intérêt pt est bien présent
                }
                
                
                if(this.pReputations.containsKey(pt.getIndex())){
                    
                    String val;
                    
                    val = this.pReputations.get(pt.getIndex());
                    this.pReputations.replace(pt.getIndex(), val, predictedState);
                    
                }else{
                    
                    this.pReputations.put(pt.getIndex(), predictedState);
                    
                }
                
                //on met à jour les alpha et beta predictif du contributeur u
                    uro = u.getBetaR2(); //alpha
                    uso = u.getBetaS2(); //beta
                    int currentval = u.getArrayValues()[pt.getIndex()]; // observation du contributeur u concernant le point d'intérêt pt 
                    String oldstate; //etat précédent
                    int oldval;//observation précédente
                 
                 //si le contributeur u n'a jamais fait d'observation sur ce point d'intérêt pt
                 if(!(u.getCurrentPointsList().contains(pt.getIndex()))){
                            switch (predictedState){

                              case "VRAI":

                                      switch (currentval) {
                                          case 1: 

                                                  uro = this.lamda_user *uro + 1;
                                                  uso = this.lamda_user *uso;
                                                  break;

                                          case 0:
                                                  uso =  this.lamda_user *uso + 1;
                                                  uro = this.lamda_user *uro;
                                                  break;

                                          default: 
                                                      break;
                                      }
                                      break;

                              case "FAUX":

                                      switch (currentval) {
                                          case 1: 

                                                  uso =  this.lamda_user *uso + 1;
                                                  uro = this.lamda_user *uro;
                                                  break;

                                          case 0:
                                                  uro =  this.lamda_user *uro + 1;
                                                  uso = this.lamda_user *uso;
                                                  break;

                                          default: 
                                                      break;
                                      }        
                                      break;
                              default: 
                                          break;                

                          }
                            
                           u.addToCurrentPointList(pt.getIndex());//ajouter le point d'intérêt pt à la liste des points d'intérêt pour lesquels le contribueteur u a fait une observation

                     
                     
                 }else{ //si le contributeur u a fait auparavant des observations sur ce point d'intérêt pt
                     
                      oldval = u.getArrayoldValues()[pt.getIndex()];  //observation précédente du contributeur u concernant le point d'intérêt pt
                      oldstate = u.getArrayoldState()[pt.getIndex()]; //etat du point d'intérêt pt lors de l'observation précédente du contributeur u 
                     
                      switch (oldstate) {
                        case "FAUX":  
                            
                            switch (predictedState){
                                
                                case "VRAI":
                                    
                                    switch (oldval) {
                                       case 1: 
                                                 switch (currentval){

                                                        case 1:
                                                                uso = (uso - 1)/this.lamda_user;//correction
                                                                uro = uro/this.lamda_user; //correction
                                                                uso = this.lamda_user *uso; //mise à jour
                                                                uro =  this.lamda_user *uro + 1;//mise à jour
                                                                
                                                                break;

                                                        case 0:

                                                                //on ne fait rien dans ce cas     
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }


                                        break;

                                       case 0:
                                           
                                           switch (currentval){

                                                        case 1:

                                                                //on ne fait rien dans ce cas 
                                                                break;

                                                        case 0:
                                                            
                                                                uro = (uro - 1)/this.lamda_user;//correction
                                                                uso = uso/this.lamda_user; //correction
                                                                uro = this.lamda_user *uro; //mise à jour
                                                                uso =  this.lamda_user *uso + 1;//mise à jour
                                                                      
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }
                                           break;

                                       default: 
                                                break;
                                    }
                                break;
                                
                                case "FAUX":
                                    
                                     switch (oldval) {
                                       case 1: 
                                                 switch (currentval){

                                                        case 1:

                                                                //on ne fait rien dans ce cas 
                                                                break;

                                                        case 0:

                                                                uso = (uso - 1)/this.lamda_user;//correction
                                                                uro = uro/this.lamda_user; //correction
                                                                uso = this.lamda_user *uso; //mise à jour
                                                                uro =  this.lamda_user *uro + 1;//mise à jour      
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }


                                        break;

                                       case 0:
                                           
                                           switch (currentval){

                                                        case 1:

                                                                uro = (uro - 1)/this.lamda_user;//correction
                                                                uso = uso/this.lamda_user; //correction
                                                                uro = this.lamda_user *uro; //mise à jour
                                                                uso =  this.lamda_user *uso + 1;//mise à jour
                                                                
                                                                break;

                                                        case 0:

                                                                //on ne fait rien dans ce cas      
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }
                                           break;

                                       default: 
                                                break;
                                    }
                                     
                                    break;
                                    
                                
                            }
                            
                        break;
                            
                        case "VRAI": 
                            
                            switch (predictedState){
                                
                                case "VRAI":
                                    
                                    switch (oldval) {
                                       case 1: 
                                                 switch (currentval){

                                                        case 1:

                                                                //on ne fait rien dans ce cas   
                                                                break;

                                                        case 0:

                                                                uro = (uro - 1)/this.lamda_user;//correction
                                                                uso = uso/this.lamda_user; //correction
                                                                uro = this.lamda_user *uro; //mise à jour
                                                                uso =  this.lamda_user *uso + 1;//mise à jour
                                                                
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }


                                        break;

                                       case 0:
                                           
                                           switch (currentval){

                                                        case 1:

                                                                uso = (uso - 1)/this.lamda_user;//correction
                                                                uro = uro/this.lamda_user; //correction
                                                                uso = this.lamda_user *uso; //mise à jour
                                                                uro =  this.lamda_user *uro + 1;//mise à jour
                                                                
                                                                break;

                                                        case 0:

                                                                //on ne fait rien dans ce cas       
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }
                                           break;

                                       default: 
                                                break;
                                    }
                                break;
                                
                                case "FAUX":
                                    
                                     switch (oldval) {
                                       case 1: 
                                                 switch (currentval){

                                                        case 1:

                                                                uro = (uro - 1)/this.lamda_user;//correction
                                                                uso = uso/this.lamda_user; //correction
                                                                uro = this.lamda_user *uro; //mise à jour
                                                                uso =  this.lamda_user *uso + 1;//mise à jour
                                                                break;

                                                        case 0:

                                                                //on ne fait rien dans ce cas       
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }


                                        break;

                                       case 0:
                                           
                                           switch (currentval){

                                                        case 1:

                                                                //on ne fait rien dans ce cas 
                                                                break;

                                                        case 0:

                                                                uso = (uso - 1)/this.lamda_user;//correction
                                                                uro = uro/this.lamda_user; //correction
                                                                uso = this.lamda_user *uso; //mise à jour
                                                                uro =  this.lamda_user *uro + 1;//mise à jour
                                                                
                                                                break;
                                                        default: 
                                                                    break;                

                                                    }
                                           break;

                                       default: 
                                                break;
                                    }
                                     
                                    break;
                                    
                                
                            }
                            
                        break;
                        
                        default:
                                break;
                    }
                        
                     
                     
                     
                     
                 }
                 
                //on sauvegarde les valeurs et la prediction dans l'etat precedent
                u.getArrayoldValues()[pt.getIndex()] =  currentval;
                u.getArrayoldState()[pt.getIndex()] = predictedState;
                
                //on met à jour les valeurs de alpha et beta du contributeur u
                 u.setBetaR2(uro);
                 u.setBetaS2(uso);
                //on met à jour la valeur de l'etat du point d'intérêt pt au moment de l'observation du contributeur u              
                u.getArrayState()[pt.getIndex()]= predictedState;
                

                
               
            }
            
            return new BetaOption(isTraining, predictedState);
            
        }
        
        /**
        * permet de mettre à jour la réputation et/ou les paramètres alpha et beta des contributeurs d'un pt d'intérêt
        * @param observationMatrix la matrice d'observation
        * @param mapParticipants la map de tous les participants
        * @param pt le pt d'intérêt concerné par l'observation
        * @param u le contributeur qui fait l'observation
        * @param option option spécifiant le contexte d'exécution
        */
        
        public synchronized void UpdateContributorReputation(Contributor u, PointOfInterest pt, BetaOption option, ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix, ConcurrentHashMap<Integer, Contributor> mapParticipants){
            
            String user_feedback;
            int user_index;
            double E;
            double uro, uso, v, r, s, r_new = 0, s_new = 0;
            String oldprediction = null;    
            
            user_index = u.getIndex(); 
 
          
            
            if(option.isIsTraining()){ //contexte dans lequel on tient compte de l'etat réel des points d'intérêt afin de calculer les réputations des contributeurs réellement attendu.
                
                    List<Double> lstReputation;
             
                    uro = u.getBetaR(); //valeur du paramètre alpha du contributeur u
                    uso = u.getBetaS(); //valeur du paramètre beta du contributeur u
                    int var1 = u.getArrayValues()[pt.getIndex()]; //observation du contributeur u concernant le point d'intérêt pt
                    
                    switch (option.getState()){
                                
                    case "VRAI":
                                    
                            switch (var1) {
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
                        
                            switch (var1) {
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
                        

                
            }else{ //contexte dans lequel on se base plutôt sur l'etat prédictif du point d'intérêt
                
                double rnew = 0, snew = 0;
                List<Double> lstReputation2; 
                ConcurrentHashMap<Integer, Contributor> t = observationMatrix.get(pt.getIndex());
                Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator = t.entrySet().iterator();
                
                //Pour chaque contributeur du point d'intérêt pt, on met à jour ses paramètres alpha et beta
                while(iterator.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, Contributor> next = iterator.next();
                    Contributor user = next.getValue();
                                       
                     //on recupere alpha et beta du participant pour les calculs predictifs

                    uro = user.getBetaR2(); //valeur du paramètre alpha du contributeur
                    uso = user.getBetaS2();//valeur du paramètre beta du contributeur
                    
                    oldprediction = user.getArrayState()[pt.getIndex()]; //etat du point d'intérêt pt lors de l'observation du contributeur 
                    int var2 = user.getArrayValues()[pt.getIndex()];  // observation du contributeur sur le point d'intérêt pt
                    
                      switch (option.getState()) {  //option.getState() renvoit l'etat prédictif courant du point d'intérêt pt
                          
                        case "FAUX":  
                            
                            switch (oldprediction){
                                
                                case "VRAI":
                                    
                                    switch (var2) {
                                       case 1: 
                                           
                                           uro = (uro - 1)/this.lamda_user;//correction
                                           uso = uso/this.lamda_user; //correction
                                           rnew = this.lamda_user *uro; //mise à jour
                                           snew =  this.lamda_user *uso + 1;//mise à jour
                                           break;

                                       case 0:
                                           uso = (uso - 1)/this.lamda_user;//correction
                                           uro = uro/this.lamda_user; //correction
                                           snew = this.lamda_user *uso; //mise à jour
                                           rnew =  this.lamda_user *uro + 1;//mise à jour
                                           break;

                                       default: 
                                                break;
                                    }
                                break;
                                
                                case "FAUX":
                                    snew = uso;
                                    rnew = uro;
                                    break;
                                    
                                
                            }
                        break;
                        case "VRAI": 
                            
                            switch (oldprediction){
                                
                                case "FAUX":
                                
                                 switch (var2) {
                                     
                                    case 1: 
                                        uso = (uso - 1)/this.lamda_user;//correction
                                        uro = uro/this.lamda_user; //correction
                                        snew = this.lamda_user *uso; //mise à jour
                                        rnew =  this.lamda_user *uro + 1;//mise à jour
                                        break;
                                        
                                    case 0:
                                        uro = (uro - 1)/this.lamda_user;//correction
                                        uso = uso/this.lamda_user; //correction
                                        rnew = this.lamda_user *uro; //mise à jour
                                        snew =  this.lamda_user *uso + 1;//mise à jour
                                        break;
                                        
                                    default: 
                                            break;
                                 }
                                break;
                                    
                               case "VRAI":
                                    snew = uso;
                                    rnew = uro;
                                    break;   
                                
                            }
                        break;
                        
                        default:
                                break;
                    }


                        //on met à jour alpha et beta predictif
                        user.setBetaR2(rnew);
                        user.setBetaS2(snew);
                        user.getArrayState()[pt.getIndex()] = option.getState(); //on sauvegarde l'etat du point d'intérêt pt lors de l'observation du contributeur 
                        user.getArrayoldState()[pt.getIndex()] = option.getState(); //on sauvegarde l'etat précédent du point d'intérêt pt lors de l'observation du contributeur 
                    
                        //calcul de la réputation du contributeur
                        E = (((double) rnew)/(rnew + snew));
                        

                        if (E < 0.01)
                        E = 0.01;
                        if (E > 0.99)
                           E = 0.99;
                        E = Math.ceil(E * 99) / 99;
                        user.setTrustScore(round2(E, 2));//on update la reputation du contributeur qui sera utilisé à la ligne 60
                        
                        
                        //on met à jour la réputation du contributeur u dans la liste des réputations
                        int ind = user.getIndex();
                        if(ind == u.getIndex()){
                            

                        if(this.getuReputations2().containsKey(ind)){

                            lstReputation2 = this.getuReputations2().get(ind);

                            lstReputation2.set(pt.getIndex(), round2(E, 2));


                        }else{


                            lstReputation2 = new ArrayList<Double>(Collections.nCopies(nbpoints, 0.0));
                            lstReputation2.set(pt.getIndex(), round2(E, 2));
                            this.getuReputations2().put(ind, lstReputation2);

                    }
                        
                        
                    }
              
                    
                }
                
         
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

    /**
     * @return the uReputations2
     */
    public ConcurrentHashMap<Integer, List<Double>> getuReputations2() {
        return uReputations2;
    }

    /**
     * @param uReputations2 the uReputations2 to set
     */
    public void setuReputations2(ConcurrentHashMap<Integer, List<Double>> uReputations2) {
        this.uReputations2 = uReputations2;
    }

}
