
import java.util.ArrayList;
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
	 * Compute the mixture coefficients using EM algorithm
	 * 
	 * @param x sample values
	 * @param laws instances of the laws
	 * @return mixture coefficients 
	 */
	public void algorithmEM(List<Contributor> currentContributorList, ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> tab, int nbPoints, int nbContributors, Contributor u) {
            
            // initial guess for the mixture coefficients (uniform)
                double val1 = 0.0; 
                int k = 0;
		int N=nbPoints;
                int M = nbContributors;               
		//double[] h = new double[N];
		//double[] e = new double[M];
                double[] z = new double[N];
                double[] Ai = new double[M];
                double[] Bi = new double[M];
                double d = new Random().nextDouble();
               

             

                //on parcours la matrice d'observation

                for (Contributor user : currentContributorList) {

                    Ai[user.getIndex()] = ((double)user.getCurrentPointsList().size()/tab.size());//ai=si

                    Bi[user.getIndex()] = ((double)Ai[user.getIndex()]*0.5) ;// bi = o.5*si

                



                }
		
		// iterative loop (until convergence or 5000 iterations)
		double convergence;
                double denominator; double numerator, znew;
                double a, b, p0;
                double R = 0;
                
		for(int loop=0;loop<5000;loop++) {
			convergence=0;
			// ---- E Step ----
			
			//(Bayes inversion formula)

                         

                            Iterator<ConcurrentHashMap.Entry<Integer, ConcurrentHashMap<Integer, Contributor>>> iterator2 = tab.entrySet().iterator();
                            while(iterator2.hasNext()) {
                                                              
                                 a = 1;b = 1; p0 = 0;denominator = 0; numerator = 0;

                                ConcurrentHashMap.Entry<Integer, ConcurrentHashMap<Integer, Contributor>> next1 = iterator2.next();
                                ConcurrentHashMap<Integer, Contributor> c = next1.getValue();
                                Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator1 = c.entrySet().iterator();

                                while(iterator1.hasNext()) {


                                    ConcurrentHashMap.Entry<Integer, Contributor> next2 = iterator1.next();
                                    Contributor contrib = next2.getValue();
                               
                                    p0 = contrib.getArrayValues()[next1.getKey()];
                                    // System.out.println("Ai["+contrib.getIndex()+"] : "+ Ai[contrib.getIndex()]);
                                    a = a * Math.pow(Ai[contrib.getIndex()], p0) * Math.pow((1 - Ai[contrib.getIndex()]), (1 - p0)); //equation 12
                                    b = b * Math.pow(Bi[contrib.getIndex()], p0) * Math.pow((1 - Bi[contrib.getIndex()]), (1 - p0));
 
                                }

                               // System.out.println("a: "+ a);
                                // System.out.println("d : "+ d);
                                numerator = a * d;
                                //System.out.println("num : "+ numerator);
                                denominator = numerator + b *(1 - d);
                                //System.out.println("denom : "+ denominator);
                                
                                if((numerator == 0)){
                                    
                                    znew = 0;
                                    
                                }else{
                                    
                                   znew = numerator/denominator; 
                                }
                               
                               /* 
                                if (znew < 0.01)
                                  znew = 0.0;
                                if (znew > 0.99)
                                    znew = 1.0;*/
                               // z[next1.getKey()] = Math.ceil(znew * 99) / 99;
                                  System.out.println("Z : "+znew); 
                                z[next1.getKey()]=znew; //equation 11
                              // System.out.println("Z : "+ z[next1.getKey()]);   

                                

                            }

                        
                        
			
			// ---- M Step ----
			
			// mixture coefficients (maximum likelihood estimate of binomial distribution)and update the parameters of the laws
                        double r = 0, s = 0;
                        
                         for(int g=0;g<z.length;g++) {
                             
                             //if(!Double.isNaN(z[g])){
                                s+= z[g];  
                            // }
                             
                         }
			for (Contributor user : currentContributorList) {
                            
                            for (Integer index : user.getCurrentPointsList()) {
                                 
                                 //if(!Double.isNaN(z[index])){
                                    r+= z[index];  
                                // }                                
                            }
                            
                            double savedAi= Ai[user.getIndex()];
                            double savedBi= Bi[user.getIndex()];
                            double saved = d;
                        
                            //equation 17
                        
                            if((r == 0)||(s == 0)){
                              
                                Ai[user.getIndex()] = 0;
                              //  System.out.println("r : "+ r);
                              //  System.out.println("s : "+ s);
                                
                            }else{
                                
                               Ai[user.getIndex()] = r/s; 
                              // System.out.println("r/s : "+ r/s);
                            }
                            
                           // System.out.println("Ainew["+user.getIndex()+"] : "+ Ai[user.getIndex()]);
                           
                            Bi[user.getIndex()] = (user.getCurrentPointsList().size() - r)/(tab.size() - s);
                            //System.out.println("Binew["+user.getIndex()+"] : "+ Bi[user.getIndex()]);
                            
                            if(s==0){
                                d=0;
                            }else{
                               d = s / tab.size();  
                            }

                            
                            convergence+=(savedAi - Ai[user.getIndex()])*(savedBi - Bi[user.getIndex()])*(saved - d);
                            
				
			}
			
	 
			if( convergence < 1E-10 ) break;
		}
                
                for(int g=0;g<z.length;g++) {
                  
                       // System.out.println("val : "+ z[g]);
                     //if(!Double.isNaN(z[g])){
                         
                         
                         
                         if(z[g] >= 0.5){
                             if(pReputations.containsKey(g)){
                                 
                                 this.pReputations.replace(g, "VRAI");
                             }else{
                                 this.pReputations.put(g, "VRAI");
                             }
                             
                         }else{
                             
                             if(pReputations.containsKey(g)){
                                 
                                 this.pReputations.replace(g, "FAUX");
                             }else{
                                 this.pReputations.put(g, "FAUX");
                             }
                             
                         }
                     //}
                }
                
                numerator = 0; denominator = 0;
                for (Contributor user : currentContributorList) {
                    
                    numerator = Ai[user.getIndex()]*d;
                    denominator = Bi[user.getIndex()] - Bi[user.getIndex()]*d + numerator;
                    
                    if(u.getIndex() == user.getIndex()){
                    R = numerator/denominator;
                    
                    System.out.println("R : "+ R);
                    if (R < 0.01)
                        R = 0.01;
                    if (R > 0.99)
                        R = 0.99;
                   // R = Math.ceil(R * 99) / 99;
                    
                     List<Double> lstReputation; 

                     if(this.getuReputations().containsKey(user.getIndex())){

                                lstReputation = this.getuReputations().get(user.getIndex());
                                lstReputation.add(round2(R, 2));

                     }else{

                                lstReputation = new ArrayList<Double>();
                                lstReputation.add(round2(R, 2));
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
                /*System.out.println("SIZE P: "+this.getpReputations().size());
                System.out.println("");
                System.out.println("-----------------------------state");
                System.out.print("\t");
                Iterator<ConcurrentHashMap.Entry<Integer, String>> iterator6 = this.getpReputations().entrySet().iterator();
                while(iterator6.hasNext()) {
                    ConcurrentHashMap.Entry<Integer, String> next = iterator6.next();
                    System.out.print("Point "+ next.getKey()+ ": "+ next.getValue()+ "\t");
                }*/
                
                
                
		
		//return pi;
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
