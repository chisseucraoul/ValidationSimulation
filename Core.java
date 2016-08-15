/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static java.lang.Math.random;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author raoul
 */
public class Core {
    

    private static String[] STATE = {"VRAI", "FAUX"}; //etats possibles
     private static List<Double> ERROR_RATE = new ArrayList<Double>();
    private ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix;  //Matrice d'observation
    private ConcurrentHashMap<Integer, PointOfInterest> mapPoints; // map contenant tous les points 
    private ConcurrentHashMap<Integer, Contributor> mapContributors; //map contenant tous les participants
    private GroundTruthEngine groundTruthEngine; //processeur de la betareputation
    private BetaEngine betaEngine; //processeur de la Beta reputation v2(approche basé sur l'algorithme de détection d'anomalie)
    private GompertzEngine gompertzEngine;//processeur de la méthode de Gompertz
    private ARMEngine armEngine;//processeur de la méthode ARM
    private RobustAveragingEngine robustAveragingEngine; //processeur de la méthode de moyenne robuste
    private MaxLikewoodEngine maxLikewoodEngine; //processeur de la méthode du maximum de vraisemblance
    private int nbcontributors;//nombre total de participants
    private int nbpoints;//nombre total de points d'intérêt
    private boolean running;
    private HashSet avalaibleContributor;//liste des contributeurs n'ayant pas effectué toutes leurs observations
    private Timer timer;
    private int nbTotalObservations; //  nombre total d'observations à un moment donné
    private List<Contributor> currentContributorList; //liste des contributeurs ayant déjà faits au moins une observation
    private int nbObservationPerContributor;//nombre d'observations par contributeur
    private int nb_inex ; //nombre de participants inexpérimentés
    private int nb_mal  ; // nombre de participants malicieux
    private int nb_norm ; //nombre de participants normaux



    public Core() {

        this.nbTotalObservations = 0;
        this.currentContributorList = new ArrayList<Contributor>();
        this.observationMatrix = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Contributor>>();
        this.mapPoints = new ConcurrentHashMap<>();
        this.timer = new Timer();
        this.running = true;
        this.ERROR_RATE .add(0.0);
        this.ERROR_RATE .add(0.1);
        this.ERROR_RATE .add(0.4);
        this.ERROR_RATE .add(0.5);
        this.ERROR_RATE .add(0.8);
        this.ERROR_RATE .add(0.9);
        this.mapContributors = new ConcurrentHashMap<>();
        this.nbcontributors = 10; //nbre de participants de l'etude
        this.nbpoints = 10;//nbre de points d'intérêt de l'etude 
        this.nb_inex = (int) Math.round((double) 0.2 * this.nbcontributors);
        this.nb_norm =  (int) Math.round((double) 0.7 * this.nbcontributors);
        this.nb_mal =  (int) Math.round((double) 0.1 * this.nbcontributors);
        this.groundTruthEngine = new GroundTruthEngine(this.nbpoints);
        this.betaEngine = new BetaEngine(this.nbpoints);
         this.gompertzEngine = new GompertzEngine(this.nbpoints);
        this.armEngine = new ARMEngine(this.nbpoints);
        this.robustAveragingEngine = new RobustAveragingEngine(this.nbpoints);
        this.maxLikewoodEngine = new MaxLikewoodEngine();
        this.avalaibleContributor = new HashSet(this.nbcontributors);
        this.nbObservationPerContributor = 10;
        fillContributors(nbcontributors);// on cree tous les participants
    }
    
    
    /**
     * permet de créer tous les contributeurs
     * @param n nombre de contributeurs
    */
    private void fillContributors(int n) {
        
    
        for(int i = 0; i < n; i++) {
            Contributor u = new Contributor(i); // on cree un objet participant
            u.setAvalaiblePoints(new HashSet(this.nbpoints));//initialise la liste des points d'intérêt pour lesquels il peut faire un signalement
            this.fillPoints(this.nbpoints, u);// initialise les points disponibles du participant
            u.setArrayState(new String[this.nbpoints]);//initialise le tableau des états pour chaque point d'intérêt
            u.setArrayValues(new int[this.nbpoints]);//initialise le tableau des observations du contributeur pour chaque point d'intérêt
            u.setArrayoldState(new String[this.nbpoints]);//initialise le tableau des états précédents pour chaque point d'intérêt
            u.setArrayoldValues(new int[this.nbpoints]);//initialise le tableau des observations précédentes du contributeur pour chaque point d'intérêt
            double val = this.ERROR_RATE.get(new Random().nextInt(this.ERROR_RATE.size()));
            u.setErrorRate(val); //on choisit aléatoirement un taux d'erreur

            if((val== 0.0)||(val == 0.1)){

                this.nb_norm--;
                if(this.nb_norm == 0){

                    this.ERROR_RATE.remove(0.0);
                    this.ERROR_RATE.remove(0.1);
                }

            }else if((val== 0.4)||(val == 0.5)) {

                this.nb_inex--;
                if (this.nb_inex == 0) {

                    this.ERROR_RATE.remove(0.4);
                    this.ERROR_RATE.remove(0.5);
                }
            }else{

                this.nb_mal--;
                if (this.nb_mal == 0) {

                    this.ERROR_RATE.remove(0.8);
                    this.ERROR_RATE.remove(0.9);
                }
            }
            
            //System.out.println("nb inex "+ this.nb_inex);
           // System.out.println("nb norm "+ this.nb_norm);
           // System.out.println("nb mal "+ this.nb_mal);
            u.setCooperativeRatings(new double[this.nbpoints]); // initialise le tableau des indices de confiance pour la methode Gompertz
            u.setCooperativeRatings5(new double[this.nbpoints]);// initialise le tableau des indices de confiance pour la methode beta reputation v2
            u.setCooperativeRatings2(new double[this.nbpoints]);//initialise le tableau des indices de confiance pour la methode Robust Averaging
            u.setCooperativeRatings3(new double[this.nbpoints]);//initialise le tableau des indices de confiance pour la methode ARM
            u.setFirstContributor(new boolean[this.nbpoints]);// tableau indiquant s'il est le premier contributeur d'un point
            this.mapContributors.put(i,u);//on ajoute l'objet dans la map des participants
            this.avalaibleContributor.add(i); // ajout de l'index du contributeur dans la liste des contributeurs pouvant faire des signalements
        }
    }
    
    /**
     * permet d'initialiser la liste des points d'intérêt pour lesquels le contributeur peut encore faire une observation
     * @param m nombre total de points
     * @param u le contributeur concerné
    */
    private void fillPoints(int m, Contributor u){
        
        HashSet hs = u.getAvalaiblePoints();
        
        for(int i = 0; i < m; i++) {
            
            hs.add(i);
        }

        
    }
    
    public void start() {
  
        
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {
                
                displayObservationMatrix();  //affiche la matrice d'observation
                displayReputations(); // affiche les réputations des participants
            }
        }, 2000, 2000);
        
        
        while(this.running) {
            
      
                    boolean test = true; int index=0;
                    
                    Contributor u = null;
                        
                    Random random = new Random();

                    //on génère un indice d'un participant
                    index = random.nextInt(this.avalaibleContributor.toArray().length);

                    //on recupere le tableau contenant les participants encore disponibles c'est à dire n'ayant pas faits toutes leurs observations 
                    Object [] o = this.avalaibleContributor.toArray();

                    //on recupere l'objet participant correspondant à cet index parmis les participants encore disponibles 
                    u = this.mapContributors.get((int)o[index]);
                   
                    //on récupere le tableau contenant les index des points pour lesquels le participant n'a pas encore fait d'observation
                    Object [] ob = u.getAvalaiblePoints().toArray();
                    
                    //on ajoute l'index du contributeur s'il n'est pas deja présent dans la liste des contributeurs en cours
                    if(!this.currentContributorList.contains(u)){
            
                        this.currentContributorList.add(u);
                    }

                    //on genere un indice d'un point d'interet
                    int id;
                    int indexP = new Random().nextInt(ob.length);
                    id = (int)ob[indexP];

                    PointOfInterest p = null;

                    // si le point d'intérêt existe
                    if(this.mapPoints.containsKey(id)) {
                            p = this.mapPoints.get(id);
                    }
                    else {  //sinon        
                            //on instancie un nouveau point d'interêt
                            p = new PointOfInterest(id);
                            p.setRealState((STATE[new Random().nextInt(STATE.length)])); // on genere un état definitif pour le point d'intérêt
                            this.mapPoints.put(id, p);//on ajoute le point dans la map
                            this.observationMatrix.put(id, new ConcurrentHashMap<Integer, Contributor>()); // on cree une entrée pour ce point d'intérêt dans la matrice d'observation
                            u.getFirstContributor()[id] = true; //on définit ce contributeur comme étant le premier contributeur

                    }
                    
                    if(p != null) {
                        
                            u.getCurrentPointsList().add(p.getIndex());
                           // on ajoute l'observation du contributeur dans la matrice et on calcule les réputations selon les différentes méthodes
                            addPoint(p, u); 

                    }

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
                    }         
              
                    //s'il n 'y a plus de contributeur disponible on arrête la simulation
                     if(this.avalaibleContributor.isEmpty()){
                                      
                      // if(this.nbTotalObservations == 900){  
                        
                        try {
                        Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
                        }     
                        this.running = false;
                        
                        //on calcule les performances des méthodes
                        this.calculatePerformance();
                    
                        break;
                         
                     }
 
        }
        
              
        if(!running){
           
            
            System.exit(0);
        }
        
     
        

    }
 
    /**
     * génère une observation pour le participant u à propos du point d'intérêt p et rajoute l'observation dans la matrice d'observation; 
     * calcule les réputations selon les différentes méthodeq
     * @param p le point d'intérêt concerné par l'observation
     * @param u le contributeur faisant l'observation
    */
    
    private synchronized void addPoint(PointOfInterest p, Contributor u) {
        
 
         ConcurrentHashMap<Integer, Contributor> users = null; boolean found =  false;BetaOption rep ;
        
         //si la matrice d'observation contient déjà ce point d'intérêt p
        if(this.observationMatrix.containsKey(p.getIndex())) {
            
                    //récupérer la colonne des observations associé à ce point
                    users = this.observationMatrix.get(p.getIndex());


                    if(u.getFirstContributor()[p.getIndex()] == false){  // si u n'est pas le premier contributeur de ce point d'intérêt p



                            //on génère l'observation du participant en fonction de son taux d'erreur
                            int indexval = new Random().nextInt(100);

                            double f = (double)indexval/100; 


                            if(f < u.getErrorRate()){

                                if(p.getRealState().equals("VRAI")){

                                    u.getArrayValues()[p.getIndex()] = 0;

                                }else{

                                    u.getArrayValues()[p.getIndex()] = 1;
                                }

                            }else{

                                if(p.getRealState().equals("VRAI")){

                                    u.getArrayValues()[p.getIndex()] = 1;

                                }else{

                                    u.getArrayValues()[p.getIndex()] = 0;

                                }
                            }

                    }else { // si u est le premier contributeur alors il renvoit 1

                        u.getArrayValues()[p.getIndex()] = 1;
                    }


                
                    System.out.println(u.getIndex() + " => " + u.getArrayValues()[p.getIndex()] + ":" + p.getIndex());
                    
                    //on incrémente le nombre d'observations du participant u
                    u.setNbObservations(u.getNbObservations() + 1);
                    
                    //si le participant u a atteint son quota d'observations on le retire de la liste des participants disponibles
                    if(u.getNbObservations() == this.nbObservationPerContributor){
                        
                         this.avalaibleContributor.remove(u.getIndex());

                    }
                    

                    //si le point d'intérêt n'avait pas déjà recu une observation de ce contributeur alors on ajoute le contributeur
                    if(!(users.containsKey(u.getIndex()))) {
                        
                           users.put(u.getIndex(),u); // ajouter le contributeur u à la colonne du point d'intérêt p dans la matrice dobservation
                         
                     }
                    
              
                    
                   this.nbTotalObservations = this.nbTotalObservations + 1;//on incremente le nombre total dobservations
                   
                   System.out.println("Nombre d'observations: "+ this.nbTotalObservations);


                   //on met à jour la reputation réelle des participants
                   this.groundTruthEngine.UpdateContributorReputation(u,p,observationMatrix, this.mapContributors);
                  
                   this.betaEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);

                   this.gompertzEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
            
                   this.robustAveragingEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
                   
                   this.armEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
                  
                   this.maxLikewoodEngine.algorithmEM(this.currentContributorList, observationMatrix, nbpoints, nbcontributors, p , u);
            
            
        }

        
    }
    
    
    /**
     * affiche la matrice d'observation
    */
    private synchronized void displayObservationMatrix() {
        
        System.out.println("-----------------------------");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, PointOfInterest>> iterator10 = mapPoints.entrySet().iterator();
        while(iterator10.hasNext()) {
            ConcurrentHashMap.Entry<Integer, PointOfInterest> next9 = iterator10.next();
            PointOfInterest p = next9.getValue();
            System.out.print(p.getIndex() + "("+ p.getRealState()+")\t");
        }

        System.out.println("");
        
        Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator = mapContributors.entrySet().iterator();
        while(iterator.hasNext()) {
            ConcurrentHashMap.Entry<Integer, Contributor> next = iterator.next();
            Contributor u = next.getValue();
            System.out.print(next.getKey()+"("+String.format("%.2f", u.getErrorRate())+")");
            System.out.print(" ");
            
            Iterator<ConcurrentHashMap.Entry<Integer, PointOfInterest>> iterator1 = mapPoints.entrySet().iterator();
            while(iterator1.hasNext()) {
                ConcurrentHashMap.Entry<Integer, PointOfInterest> next1 = iterator1.next();
                PointOfInterest p = next1.getValue();

                    System.out.print(retrieveValueOfPoint(p, u) + "\t");
              
            }
            System.out.println("");
            System.out.println("--");
        }
    }
    
    
    /**
     * affiche les réputations des participants
    */
    private synchronized void displayReputations() {
      
        double sumb = 0, sumbv2 = 0, sumg = 0, sumro = 0, sumacc = 0, sumoy = 0;
        double [] realval = new double[this.nbcontributors];
        int [] counter = new int[this.nbcontributors];
        
      System.out.println("-----------------------------ground truth(real)");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator3 = this.groundTruthEngine.getuReputations().entrySet().iterator();
        while(iterator3.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next1 = iterator3.next();
            System.out.print("Participant "+ next1.getKey()+ ": "+ next1.getValue()+ "\t");
            
            synchronized(realval){
                if((next1.getValue().size() == this.nbpoints)&&(realval[next1.getKey()]== 0)){
                realval[next1.getKey()] = next1.getValue().get(nbpoints - 1);

            }
            
            }
        }
         System.out.println("");


        
         System.out.println("");
        System.out.println("-----------------------------beta reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator5 = this.betaEngine.getuReputations().entrySet().iterator();
        while(iterator5.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next4 = iterator5.next();
            System.out.print("Participant "+ next4.getKey()+ ": "+ next4.getValue()+ "\t");

        }
        
         

        System.out.println("");
        System.out.println("-----------------------------gompertz reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator7 = this.gompertzEngine.getuReputations().entrySet().iterator();
        while(iterator7.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next5 = iterator7.next();
            System.out.print("Participant "+ next5.getKey()+ ": "+ next5.getValue()+ "\t");

        }

         System.out.println("");
        
        System.out.println("");
        System.out.println("-----------------------------robust Average");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator8 = this.robustAveragingEngine.getuReputations().entrySet().iterator();
        while(iterator8.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next6 = iterator8.next();
            System.out.print("Participant "+ next6.getKey()+ ": "+ next6.getValue()+ "\t");

        }


        
         System.out.println("");
        System.out.println("-----------------------------detection outlier state");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, String>> iterator6 = this.robustAveragingEngine.getpReputations().entrySet().iterator();
        while(iterator6.hasNext()) {
            ConcurrentHashMap.Entry<Integer, String> next8 = iterator6.next();
            System.out.print("Point "+ next8.getKey()+ ": "+ next8.getValue()+ "\t");
        }
         System.out.println("");
        
        System.out.println("");
        System.out.println("-----------------------------Accumulated Reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator9 = this.armEngine.getuReputations().entrySet().iterator();
        while(iterator9.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next7 = iterator9.next();
            System.out.print("Participant "+ next7.getKey()+ ": "+ next7.getValue()+ "\t");

        }

        System.out.println("");

      
    }
      
    
    /**
     * permet d'extraire de la matrice d'observations, l'observation faite par un contributeur à propos d'un point d'intérêt
     * @param p point d'intérêt
     * @param u contributeur
     * @return observation du contributeur à propos du point d'intérêt
    */
    private String retrieveValueOfPoint(PointOfInterest p, Contributor u) {
        
        String value = "";
        
        if(this.observationMatrix.containsKey(p.getIndex())){
            
            ConcurrentHashMap<Integer, Contributor> users = this.observationMatrix.get(p.getIndex());
        
            if(users.containsKey(u.getIndex())){

                return Integer.toString( users.get(u.getIndex()).getArrayValues()[p.getIndex()]);
            }
        }
        
        return value;
    }

    /**
     * @return the nbTotalObservations
     */
    public int getNbTotalObservations() {
        return nbTotalObservations;
    }

    /**
     * @param nbTotalObservations the nbTotalObservations to set
     */
    public void setNbTotalObservations(int nbTotalObservations) {
        this.nbTotalObservations = nbTotalObservations;
    }
    
    
     /**
     * permet de calculer les performances de chaque méthode 
    */
    public void calculatePerformance(){
        
     double sumb = 0, sumbv2 = 0, sumg = 0, sumro = 0, sumacc = 0;
     List<Integer> lstro = new ArrayList<Integer>(); 
     List<Integer> lstbeta = new ArrayList<Integer>();
     

        System.out.println("");
        
         System.out.println("");
        System.out.println("-----------------------------beta reputation ");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator5 = this.betaEngine.getuReputations().entrySet().iterator();
        while(iterator5.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next4 = iterator5.next();
            int t = next4.getValue().size();
            
                double sumtmp = 0;
                
                for(int i = 0;  i<t ; i++){
                    
                     sumtmp += Math.abs(next4.getValue().get(i) - this.groundTruthEngine.getuReputations().get(next4.getKey()).get(i) );
                }
                            
          
                sumbv2 += (double)sumtmp/t;
            
        }
        System.err.print("(Performance beta reputation : "+ (1 - (1/(double)this.nbcontributors)*sumbv2) + ")\t");
        
         

        System.out.println("");
        System.out.println("-----------------------------gompertz reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator7 = this.gompertzEngine.getuReputations().entrySet().iterator();
        while(iterator7.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next5 = iterator7.next();
           int t = next5.getValue().size()-1;
                       sumg += Math.abs(next5.getValue().get(t) - this.groundTruthEngine.getuReputations().get(next5.getKey()).get(t) );
        }
          System.err.print("(Performance gompertz reputation: "+ (1 - (1/(double)this.nbcontributors)*sumg) + ")\t");
         System.out.println("");
        
        System.out.println("");
        System.out.println("-----------------------------robust Average");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator8 = this.robustAveragingEngine.getuReputations().entrySet().iterator();
        while(iterator8.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next6 = iterator8.next();
       int t = next6.getValue().size()-1;
                       sumro += Math.abs(next6.getValue().get(t) - this.groundTruthEngine.getuReputations().get(next6.getKey()).get(t) );
        }
         System.err.print("(Performance robust Average: "+ (1 - (1/(double)this.nbcontributors)*sumro) + ")\t");

        
        System.out.println("");
        System.out.println("-----------------------------Accumulated Reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator9 = this.armEngine.getuReputations().entrySet().iterator();
        while(iterator9.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next7 = iterator9.next();
      int t = next7.getValue().size()-1;
                       sumacc += Math.abs(next7.getValue().get(t) - this.groundTruthEngine.getuReputations().get(next7.getKey()).get(t) );
        }
        
         System.err.print("(Performance Accumulated Reputation: "+ (1 - (1/(double)this.nbcontributors)*sumacc) + ")\t");
        
        System.out.println("");
        System.out.println("-----------------------------false positive/false negative ");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, PointOfInterest>> iterator11 = this.mapPoints.entrySet().iterator();
        while(iterator11.hasNext()) {
            ConcurrentHashMap.Entry<Integer,PointOfInterest> next15 = iterator11.next();
            if (!(next15.getValue().getRealState().equals(this.robustAveragingEngine.getpReputations().get(next15.getKey())))){
                lstro.add(next15.getKey());
            }

        }
        
 
    }
    


}
