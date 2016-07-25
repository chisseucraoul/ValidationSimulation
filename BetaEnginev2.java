

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
public class BetaEnginev2 {
    
        private double lamda_user ; //facteur d'oubli du contributeur
        private ConcurrentHashMap<Integer, List<Double>> uReputations; //reputations des participants
        private int nbpoints;

        
        public BetaEnginev2(int n){

            this.nbpoints = n;
            this.lamda_user = 0.9;
            uReputations = new ConcurrentHashMap<>();
            
        }
        
        /**
        * permet de mettre à jour la réputation des contributeurs d'un pt d'intérêt
        * @param observationMatrix la matrice d'observation
        * @param mapContributors la map de tous les participants
        * @param pt le pt d'intérêt concerné par l'observation
        * @param u le contributeur qui fait l'observation
        */
       public void UpdateContributorReputation(ConcurrentHashMap<Integer,ConcurrentHashMap<Integer, Contributor>> observationMatrix, ConcurrentHashMap<Integer, Contributor> mapContributors, PointOfInterest pt, Contributor u){
           
  
            // on récupère dans n le nombre de contributeurs du pt d'intérêt pt
            int n = 0;
            ConcurrentHashMap<Integer, Contributor> lstContributors = observationMatrix.get(pt.getIndex());
            n = lstContributors.size();

             
             Iterator<ConcurrentHashMap.Entry<Integer, Contributor>> iterator1 = lstContributors.entrySet().iterator();
             
                int[] user_ids = new int[n]; //tableau contenant les identifiants de tous les contributeurs du pt d'intérêt pt
                double[] x = new double[n]; //tableau contenant les observations de tous les contributeurs du point d'intérêt pt
                double[] p = new double[n]; //tableau contenant les indices de confiance(probabilités) de tous les contributeurs du point d'intérêt pt
                double eps = Math.pow(10, -20);//epsilon
                double[] a = new double[n]; //tableau utilisé pour les calculs intermédiaires
                double[] a2 = new double[n];//tableau utilisé pour les calculs intermédiaires
                double[] p_old = new double[n];//tableau contenant les indices de confiances précédents de tous les contributeurs du point d'intérêt pt
                double[] p_norm = new double[n]; //tableau contenant les indices de confiance normalisés 
                int k = 0;
               
                //initialisation 
                while(iterator1.hasNext()) {
                    
          
                    ConcurrentHashMap.Entry<Integer, Contributor> next2 = iterator1.next();
                    Contributor contrib = next2.getValue();
                        user_ids[k] = contrib.getIndex();
                        
                         x[k] = contrib.getArrayValues()[pt.getIndex()];
                         p[k] = 1.0 / n;
                         k++;
                                 

                }

            double d = Double.POSITIVE_INFINITY;
            double r, s, b;
           
           //Itération(convergence) 
            while (d > 0.0001)
            {
                r = 0;
                for (int i = 0; i < n; i++){
                    r += p[i] * x[i];  //equation 3
                }
                s = 0;
                for (int i = 0; i < n; i++)
                {
                    a[i] = Math.pow(x[i] - r, 2);
                    s += a[i];
                }

                b = 0;
                for (int i = 0; i < n; i++)
                {
                    if (s > 0)
                       // a2[i] = (s / (a[i]) + eps;
                     a2[i] = (s / a[i]) + eps;
                    else
                        a2[i] = 1 / eps;
                    b += a2[i];
                }
                
                    d = 0;
                for (int i = 0; i < n; i++)
                {
                    p_old[i] = p[i];
                    p[i] = (a2[i] / b) + eps; // equation 4
                    d += Math.abs(p[i] - p_old[i]);
                }


            }

            
            
            //Normalisation
            double pmin = Double.POSITIVE_INFINITY, pmax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++)
            {
                if (pmin > p[i])
                    pmin = p[i];
                if (pmax < p[i])
                    pmax = p[i];
            }

            for (int i = 0; i < n; i++){
                
                 p_norm[i] = (double)Math.signum(2 * (p[i] - pmin + eps) / (pmax - pmin + eps) - 1);
            }
               

             double R = 0, lamda, rating;
            // List<Double> c;
             double [] c;
        

            int ind;
            for (int i = 0; i < n; i++)
            {
                ind = user_ids[i];

                c = mapContributors.get(ind).getCooperativeRatings5();

                c[pt.getIndex()] = p_norm[i];//on met à jour l'indice de confiance du contributeur

                if(ind == u.getIndex()){ //s'il s'agit du contributeur ayant fait l'observation, on calcule sa réputation
                  
                
                    double sum = 0, suml = 0, pnorm; double rb = 1, sb = 1;
                    for (int j = 0; j < c.length; j++)
                    {

                        pnorm = c[j];

                        if (c[j] == 1)  //succès
                            rb = this.lamda_user * rb + 1;
                        else if (c[j] == -1) //échec
                            sb = this.lamda_user * sb + 1;
                        else {}


                    }
                
                //calcul de la réputation
                R = (double)rb/(rb+sb); //beta reputation


                
                     mapContributors.get(ind).setPredictedTrust((int)(R * 100));
                     
                     
                     List<Double> lstReputation; 

                     if(this.getuReputations().containsKey(ind)){

                                lstReputation = this.getuReputations().get(ind);
                                lstReputation.set(pt.getIndex(), round2(R, 2));


                     }else{


                                lstReputation = new ArrayList<Double>(Collections.nCopies(nbpoints, 0.0));
                                lstReputation.set(pt.getIndex(), round2(R, 2));
                                this.getuReputations().put(ind, lstReputation);

                    }
                }
                
                
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
