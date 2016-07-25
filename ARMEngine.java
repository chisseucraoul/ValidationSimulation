

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
public class ARMEngine {
    
    private ConcurrentHashMap<Integer, List<Double>> uReputations; //reputations des participants
    private int nbpoints; //nombre de points d'intérêt
    
    
        public ARMEngine(int n){
            
            this.uReputations = new ConcurrentHashMap<>();
             this.nbpoints = n;
    
        }
        
        /**
        * permet de mettre à jour la réputation des contributeurs d'un pt d'intérêt
        * @param observationMatrix la matrice d'observation
        * @param mapContributors la map de tous les participants
        * @param pt le point d'intérêt concerné par l'observation
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

            //Itération
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
                // p_norm[i] = (double)Math.signum(2 * (p[i] - pmin) / (pmax - pmin ));
            }
               

             double R = 0, lamda, rating;
            // List<Double> c;
             double [] c;

            int ind;
            for (int i = 0; i < n; i++)
            {
                ind = user_ids[i];

                c =  mapContributors.get(ind).getCooperativeRatings3(); 

                c[pt.getIndex()] = Gompertz(p_norm[i]); //on met à jour l'indice de confiance du contributeur
                
                if(ind == u.getIndex()){ //s'il s'agit du contributeur ayant fait l'observation, on calcule sa réputation
                    
                    ArrayList<Double> cbis = new ArrayList<Double>();
                     for (int j = 0; j < c.length; j++)
                    {
                        if (c[j] != 0){
                            cbis.add(c[j]);
                        }
                    }

                     R = (double)Trim(cbis, 0.1);  //calcul de la réputation

                     if (R < 0.01)
                        R = 0.01;
                     if (R > 0.99)
                        R = 0.99;
                     R = Math.ceil(R * 99) / 99;
                     
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
        * permet de calculer la moyenne tronquée
        * @param rate liste des indices de confiance
        * @param percent taux à rejeter
        * @return la reputation du contributeur
        */
       
      private double Trim(List<Double> rate, double percent){
       
         int n = rate.size(); int count;
         List<Double> coprate = null;   List<Double> coprate2 = null;
         
        count = (int) Math.round( (double)percent * n);

        Collections.sort(rate);
        coprate = this.removeFirst(rate, count);

        coprate2 = this.removeLast(coprate, count);

        double s = 0;
        for (int i = 0; i < coprate2.size(); i++){

            s+= coprate2.get(i);
        }

        return (s/coprate2.size());
       
   }
     
    /**
     * permet d'éliminer des éléments en début de liste de la liste des indices de confiance
     * @param rate liste des indices de confiance
     * @param n nombre d'éléments à éliminer
     * @return une liste d'indices de confiance
    */
   private List<Double> removeFirst(List<Double> rate, int n){
 
       
        List<Double> test = null;
      
      if(rate.size()>= n){
          
         test = rate.subList(n, rate.size());
         
      }else{
          
          test = rate;
      }

       return test;
       
    }
   
       
    /**
     * permet d'éliminer des éléments en fin de liste de la liste des indices de confiance
     * @param rate liste des indices de confiance
     * @param n nombre d'éléments à éliminer
     * @return une liste d'indices de confiance
    */
    private List<Double> removeLast(List<Double> rate, int n){
       
           List<Double> test = null; int n1 = rate.size()- n;
       
           test = rate;
        
      if(rate.size()>= n){
          
          ListIterator<Double> iterator = test.listIterator(
            test.size() - n);
            while (iterator.hasNext()) {//et retirer tous les éléments après
                iterator.next();
                iterator.remove();
            }
            
      }

       return test;
    }
    
    
    /**
     * permet de calculer la fonction de Gompertz
     * @param x l'entrée de la fonction
     * @return la sortie de la fonction
    */
   private double Gompertz(double x)
        {
            double a = 1, b = -1, c = -2.5;
            return a * Math.exp(b * Math.exp(c * x));
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


}
