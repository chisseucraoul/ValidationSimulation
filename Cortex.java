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
public class Cortex {
    

    private static String[] STATE = {"VRAI", "FAUX"}; //etats possibles
    //private static double[] ERROR_RATE = {0.05, 0.1, 0.4, 0.0}; //Taux d'erreur des participants
    private static double[] ERROR_RATE = {0.0, 0.1, 0.4, 0.5, 0.8, 0.9}; //Taux d'erreur des participants
    private ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix;  //Matrice d'observation
    private ConcurrentHashMap<Integer, PointOfInterest> mapPoints; // map contenant tous les points 
    private ConcurrentHashMap<Integer, Contributor> mapContributors; //map contenant tous les participants
    private BetaEngine betaEngine; //processeur de la betareputation
    private BetaEnginev2 betaEnginev2; //processeur de la Beta reputation v2(approche basé sur l'algorithme de détection d'anomalie)
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



    public Cortex() {

        this.nbTotalObservations = 0;
        this.currentContributorList = new ArrayList<Contributor>();
        this.observationMatrix = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Contributor>>();
        this.mapPoints = new ConcurrentHashMap<>();
        this.timer = new Timer();
        this.running = true;
        this.mapContributors = new ConcurrentHashMap<>();
        this.nbcontributors = 110; //nbre de participants de l'etude
        this.nbpoints = 80;//nbre de points d'intérêt de l'etude 
        this.betaEngine = new BetaEngine(this.nbpoints);
        this.betaEnginev2 = new BetaEnginev2(this.nbpoints);
         this.gompertzEngine = new GompertzEngine(this.nbpoints);
        this.armEngine = new ARMEngine(this.nbpoints);
        this.robustAveragingEngine = new RobustAveragingEngine(this.nbpoints);
        this.maxLikewoodEngine = new MaxLikewoodEngine();
        this.avalaibleContributor = new HashSet(this.nbcontributors);
        this.nbObservationPerContributor = 100;
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
            u.setErrorRate(ERROR_RATE[new Random().nextInt(ERROR_RATE.length)]); //on choisit aléatoirement un taux d'erreur
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
                           
                           // on ajoute l'observation du contributeur dans la matrice et on calcule les réputations selon les différentes méthodes
                            addPoint(p, u); 

                    }

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Cortex.class.getName()).log(Level.SEVERE, null, ex);
                    }         
              
                    //s'il n 'y a plus de contributeur disponible on arrête la simulation
                     if(this.avalaibleContributor.isEmpty()){
                                      
                      // if(this.nbTotalObservations == 900){  
                        
                        try {
                        Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        Logger.getLogger(Cortex.class.getName()).log(Level.SEVERE, null, ex);
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

                   //on détermine l'etat reel de la colonne du point p(contexte reel)
                   rep = this.betaEngine.ProcessPoiReputation(true, observationMatrix, p, u);

                   //on met à jour les reputations des participants
                   this.betaEngine.UpdateContributorReputation(u,p,rep,observationMatrix, this.mapContributors);
                    
                   //on détermine l'etat predictif de la colonne du point p(contexte predictif)
                   rep = this.betaEngine.ProcessPoiReputation(false, observationMatrix, p, u);
                    
                   //on met à jour les reputations des participants pour chacune des méthodes
                   this.betaEngine.UpdateContributorReputation(u,p,rep,observationMatrix, this.mapContributors);
                  
                   this.betaEnginev2.UpdateContributorReputation(observationMatrix, mapContributors, p, u);

                   this.gompertzEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
            
                   this.robustAveragingEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
                   
                   this.armEngine.UpdateContributorReputation(observationMatrix, mapContributors, p, u);
                  
                 //  this.maxLikewoodEngine.algorithmEM(currentContributorList, observationMatrix, nbpoints, nbcontributors, u);
            
            
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
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator3 = this.betaEngine.getuReputations().entrySet().iterator();
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
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator4 = this.betaEngine.getuReputations2().entrySet().iterator();
        while(iterator4.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next2 = iterator4.next();
            System.out.print("Participant "+ next2.getKey()+ ": "+ next2.getValue()+ "\t");
    
        }

    
        
        System.out.println("");
        System.out.println("-----------------------------beta reputation state");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, String>> iterator2 = this.betaEngine.getpReputations().entrySet().iterator();
        while(iterator2.hasNext()) {
            ConcurrentHashMap.Entry<Integer, String> next3 = iterator2.next();
            System.out.print("Point "+ next3.getKey()+ ": "+ next3.getValue()+ "\t");
        }
        System.out.println("");
        
         System.out.println("");
        System.out.println("-----------------------------beta reputation v2");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator5 = this.betaEnginev2.getuReputations().entrySet().iterator();
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
     
        System.out.println("-----------------------------beta reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator4 = this.betaEngine.getuReputations2().entrySet().iterator();
        while(iterator4.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next2 = iterator4.next();
                        int t = next2.getValue().size()-1;
                       sumb += Math.abs(next2.getValue().get(t) - this.betaEngine.getuReputations().get(next2.getKey()).get(t) );
                
        }

        System.err.print("(Performance beta reputation: "+ (1 - (1/(double)this.nbcontributors) *sumb) + ")\t");

        System.out.println("");
        
         System.out.println("");
        System.out.println("-----------------------------beta reputation v2");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator5 = this.betaEnginev2.getuReputations().entrySet().iterator();
        while(iterator5.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next4 = iterator5.next();
            int t = next4.getValue().size()-1;
                       sumbv2 += Math.abs(next4.getValue().get(t) - this.betaEngine.getuReputations().get(next4.getKey()).get(t) );
            
        }
        System.err.print("(Performance beta reputation v2: "+ (1 - (1/(double)this.nbcontributors)*sumbv2) + ")\t");
        
         

        System.out.println("");
        System.out.println("-----------------------------gompertz reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator7 = this.gompertzEngine.getuReputations().entrySet().iterator();
        while(iterator7.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next5 = iterator7.next();
           int t = next5.getValue().size()-1;
                       sumg += Math.abs(next5.getValue().get(t) - this.betaEngine.getuReputations().get(next5.getKey()).get(t) );
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
                       sumro += Math.abs(next6.getValue().get(t) - this.betaEngine.getuReputations().get(next6.getKey()).get(t) );
        }
         System.err.print("(Performance robust Average: "+ (1 - (1/(double)this.nbcontributors)*sumro) + ")\t");

        
        System.out.println("");
        System.out.println("-----------------------------Accumulated Reputation");
        System.out.print("\t");
        Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator9 = this.armEngine.getuReputations().entrySet().iterator();
        while(iterator9.hasNext()) {
            ConcurrentHashMap.Entry<Integer, List<Double>> next7 = iterator9.next();
      int t = next7.getValue().size()-1;
                       sumacc += Math.abs(next7.getValue().get(t) - this.betaEngine.getuReputations().get(next7.getKey()).get(t) );
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
            if (!(next15.getValue().getRealState().equals(this.betaEngine.getpReputations().get(next15.getKey())))){
                lstbeta.add(next15.getKey());
            }
        }
        
         System.err.print("robust : "+ lstro+ "; beta: "+lstbeta);
        
        
    }
    


}
