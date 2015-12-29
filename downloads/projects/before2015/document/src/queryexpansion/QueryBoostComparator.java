package queryexpansion;

import java.util.*;

import org.apache.lucene.search.*;

/**
 * Create a query boost comparator, in order to sort the terms of a query with the weights in descending oder
 * @author Zheyun Feng
 * Contact: fengzhey@msu.edu
 */
public class QueryBoostComparator implements Comparator<Object>
{
    
    /* Create a new instance of QueryBoostComparator */
    public QueryBoostComparator(){}
    
    /*
     * Compares queries based according to their boosts
     */
    public int compare(Object obj1, Object obj2)
    {
        Query q1 = (Query) obj1;
        Query q2 = (Query) obj2;
        
        if ( q1.getBoost() > q2.getBoost() )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
    
}
