
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author raoul
 */
public class MaxLikewoodEngine {
    
        private ConcurrentHashMap<Integer, List<Double>> uReputations; //reputations des participants
        private ConcurrentHashMap<Integer, String> pReputations; //reputations des points d'intérêt
    
    public MaxLikewoodEngine(){

         uReputations = new ConcurrentHashMap<>();
         pReputations = new ConcurrentHashMap<>();

    }
    
	/**
	 * Ccalcule en utilisant l'algorithme EM 
	 * 
	 * @param currentContributorList liste des contributeurs ayant déjà faits au moins une observation
	 * @param observationMatrix matrice d'observation
	 * @param pt le POI concerné par l'observation
         * @param u le contributeur qui fait l'observation
         * @param nbPoints nombre total de POI
         * @param nbContributors nombre total de contributeurs
	 */
	public void algorithmEM(List<Contributor> currentContributorList, ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix, int nbPoints, int nbContributors,PointOfInterest pt, Contributor u) {
            
            // initialisation
            
		int N= nbPoints;  //nombre de Points
                int M = nbContributors;  //nombre de participants             
                double[] Z = new double[N]; //tableau représentant Z(t,j)
                double[] Ai = new double[M]; //tableau contenant les ai
                double[] Bi = new double[M]; //tableau contenant les bi
                double d = new Random().nextDouble(); // d est un nombre aléatoire dans (0,1)
               
                //ici on initialise ai=si et bi = o.5*si pour tous les participants        
                for (Contributor user : currentContributorList) {

                    Ai[user.getIndex()] = ((double)user.getCurrentPointsList().size()/observationMatrix.size());//ai=si
                   // System.out.println("Ai["+user.getIndex()+"] : "+ Ai[user.getIndex()]);
                    Bi[user.getIndex()] = ((double)Ai[user.getIndex()]*0.5) ;// bi = o.5*si
                     // System.out.println("Bi["+user.getIndex()+"] : "+ Bi[user.getIndex()]);
                }
		
		// boucle (jusqu'à la convergence ou 5000 iterations)
		double convergence;
                double denominator; double numerator, znew;
                double a, b, p0;
                double R = 0;
                
		for(int loop=0;loop<5000;loop++) {
			convergence=0;
			// ---- E Step ----

                            //on parcours chaque point de la matrice d'observation
                            Iterator<ConcurrentHashMap.Entry<Integer, ConcurrentHashMap<Integer, Contributor>>> iterator2 = observationMatrix.entrySet().iterator();
                            while(iterator2.hasNext()) {
                                 //initialisation                             
                                 a = 1;b = 1; p0 = 0;denominator = 0; numerator = 0;

                                ConcurrentHashMap.Entry<Integer, ConcurrentHashMap<Integer, Contributor>> next1 = iterator2.next();
                                ConcurrentHashMap<Integer, Contributor> c = next1.getValue();
                                Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator1 = c.entrySet().iterator();
                                
                                //on parcours la liste des participants de chaque point 
                                while(iterator1.hasNext()) {

                                    ConcurrentHashMap.Entry<Integer, Contributor> next2 = iterator1.next();
                                    Contributor contrib = next2.getValue();
                               
                                    p0 = contrib.getArrayValues()[next1.getKey()];//observation du participant concernant ce point
                                    a = a * Math.pow(Ai[contrib.getIndex()], p0) * Math.pow((1 - Ai[contrib.getIndex()]), (1 - p0)); //equation 12
                                    b = b * Math.pow(Bi[contrib.getIndex()], p0) * Math.pow((1 - Bi[contrib.getIndex()]), (1 - p0)); //equation 12
 
                                }

                                numerator = a * d;
                                denominator = numerator + b *(1 - d);
                                
                                if((numerator == 0)){
                                    
                                    znew = 0;
                                    
                                }else{
                                    
                                   znew = numerator/denominator; //equation 11
                                 //  System.out.println("Z : "+ znew);
                                }
                               
                                
                                if (znew < 0.01)
                                  znew = 0.01;
                                if (znew > 0.99)
                                    znew = 0.99;
                               Z[next1.getKey()] = Math.ceil(znew * 99) / 99; 
                          

                            }

                        
                        
			
			// ---- M Step ----
			
                        double s = 0;
                        
                         for(int g=0;g<Z.length;g++) {

                                s+= Z[g];  //on calcule la somme des Z(t,j)
                         }
			for (Contributor user : currentContributorList) {
                            
                            double r = 0;
                            
                            for (Integer index : user.getCurrentPointsList()) {
                                 
                                    r+= Z[index];  
                               
                            }
                            
                            double savedAi= Ai[user.getIndex()];
                            double savedBi= Bi[user.getIndex()];
                            double saved = d;
                        
                            //equation 17
                        
                            if (!(s == 0 )){
                                
                                if(r == 0){
                              
                                    Ai[user.getIndex()] = 0;
                               
                                }else{
                                
                                    Ai[user.getIndex()] = r/s; 
                                }
                                
                            }
                          
                            
                           
                            Bi[user.getIndex()] = (user.getCurrentPointsList().size() - r)/(observationMatrix.size() - s);
                            
                            if(s==0){
                                
                                d=0;
                                
                            }else{
                                
                                d = s / observationMatrix.size();  
                               
                            }

                            
                            convergence+=(savedAi - Ai[user.getIndex()])*(savedBi - Bi[user.getIndex()])*(saved - d);
                            
				
			}
			
	 
			if( convergence < 1E-10 ) break;
		}
                
                // on évalue l'état des variables mesurées(Vrai ou Faux)
                
                for(int g=0;g<Z.length;g++) {
       
                         
                         if(Z[g] >= 0.5){
                             if(pReputations.containsKey(g)){
                                 
                                  String rep = this.pReputations.get(g);
                                  this.pReputations.replace(g, rep, "VRAI");
                                  
                             }else{
                                 this.pReputations.put(g, "VRAI");
                             }
                             
                         }else{
                             
                             if(pReputations.containsKey(g)){
                                 
                                  String repu = this.pReputations.get(g);
                                  this.pReputations.replace(g, repu, "FAUX");
                                  
                             }else{
                                 
                                 this.pReputations.put(g, "FAUX");
                                 
                             }
                             
                         }

                }
                
                
                //on calcule la réputation des participants
                
                numerator = 0; denominator = 0;
                for (Contributor user : currentContributorList) {
                    
                    numerator = Ai[user.getIndex()]*d;
                    denominator = Bi[user.getIndex()] - Bi[user.getIndex()]*d + numerator;
                    
                    if(u.getIndex() == user.getIndex()){
                    R = numerator/denominator;
                    
                 //   System.out.println("R : "+ R);
                    if (R < 0.01)
                        R = 0.01;
                    if (R > 0.99)
                        R = 0.99;
                    R = Math.ceil(R * 99) / 99;
                    
                     List<Double> lstReputation; 

                     if(this.getuReputations().containsKey(user.getIndex())){

                                lstReputation = this.getuReputations().get(user.getIndex());
                                lstReputation.set(pt.getIndex(), round2(R, 2));

                     }else{

                                lstReputation = new ArrayList<Double>(Collections.nCopies(nbPoints, 0.0));
                                lstReputation.set(pt.getIndex(), round2(R, 2));
                                this.getuReputations().put(user.getIndex(), lstReputation);

                    }
                     
                    
                    }
                    
                }

                
                System.out.println("-----------------------------Maximum Likewood Estimation");
                System.out.print("\t");
                Iterator<ConcurrentHashMap.Entry<Integer, List<Double>>> iterator10 = this.getuReputations().entrySet().iterator();
                while(iterator10.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, List<Double>> next = iterator10.next();
                    System.out.print("Participant "+ next.getKey()+ ": "+ next.getValue()+ "\t");
                }

                System.out.println("");
                System.out.println("-----------------------------Maximum Likewood state");
                System.out.print("\t");
                Iterator<ConcurrentHashMap.Entry<Integer, String>> iterator6 = this.getpReputations().entrySet().iterator();
                while(iterator6.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, String> next = iterator6.next();
                    System.out.print("Point "+ next.getKey()+ ": "+ next.getValue()+ "\t");
                }
                

	}

    /**
     * @return the uReputations
     */
    public ConcurrentHashMap<Integer, List<Double>> getuReputations() {
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
    public ConcurrentHashMap<Integer, String> getpReputations() {
        return pReputations;
    }

    /**
     * @param pReputations the pReputations to set
     */
    public void setpReputations(ConcurrentHashMap<Integer, String> pReputations) {
        this.pReputations = pReputations;
    }
    
        
    public static double round2(double number, int scale) {
    int pow = 10;
    for (int i = 1; i < scale; i++)
        pow *= 10;
    double tmp = number * pow;
    return (double) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }
        
        
        

}
