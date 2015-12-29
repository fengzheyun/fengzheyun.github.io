package queryexpansion;


import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

/**
 * Doing the query expansion using Rocchio's algorithm
 * 
 * The expansion is following such formula:
 * new query = alpha*query + beta / total amount of retrieved documents * sum( document * (1 - decay* rank) )
 * 
 * @author Zheyun Feng - fengzhey@msu.edu
 *
 */

public class QueryExpansion
{
    public static final String DECAY_FLD = "QE.decay";
    public static final String DOC_NUM_FLD = "QE.doc.num";
    public static final String TERM_NUM_FLD = "QE.term.num";
    public static final String ROCCHIO_ALPHA_FLD = "rocchio.alpha";
    public static final String ROCCHIO_BETA_FLD = "rocchio.beta";  
    public static final String FIELD_NAME = "field.name";

    private Properties prop;
    private Analyzer analyzer;
    @SuppressWarnings("deprecation")
	private Searcher searcher;
    private Similarity similarity;
    private Vector<TermQuery> expandedTerms; 
    
    /* Define the query expansion structure.
     * The input of similarity is usually for Lucene's default scoring method.
     * 
     * @param analyzer: analyzer used to sparse the input query text
     * @param searcher: sercher used to search specific query
     * @param similarity: similarity between a query and every document in the collection. 
     * 					  Only if the similarity is input, the expanded terms will be weighted using tf*idf, otherwise, just tf.
     * @param prop: the set of parameters used in the expansion
     */
    public QueryExpansion( Analyzer analyzer, @SuppressWarnings("deprecation") Searcher searcher, Similarity similarity, Properties prop )
    {
        this.analyzer = analyzer;
        this.searcher = searcher;
        this.similarity = similarity;
        this.prop = prop;
    }
    
    /* Define the query expansion structure.
     * The input of similarity is usually for okapi scoring method.
     * 
     * @param analyzer: analyzer used to sparse the input query text
     * @param searcher: sercher used to search specific query
     * @param prop: the set of parameters used in the expansion
     */
    public QueryExpansion( Analyzer analyzer, @SuppressWarnings("deprecation") Searcher searcher, Properties prop )
    {
        this.analyzer = analyzer;
        this.searcher = searcher;
        this.similarity = null;
        this.prop = prop;
    }


    /*
     * parse the parameters, and regulate the expansion
     */
    public Query expandQuery( String queryStr, Vector<TermFreqVector> hits, Properties prop )
    throws IOException, ParseException
    {
        // Load Necessary Values from Properties (Input)
        float alpha = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
        float beta = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
        float decay = Float.valueOf( prop.getProperty( QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue();
        int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();
        int termNum = Integer.valueOf( prop.getProperty( QueryExpansion.TERM_NUM_FLD ) ).intValue();    
        
        // Create combine documents term vectors from the retrieved documents in the first time
        // sum ( real term vectors * (1-decay * rank)  )
        Vector<QueryTermVector> docsTermVector = getDocsTerms( hits, docNum, analyzer );
                
        // Adjust term features of the docs with alpha * query; and beta; and assign weights/boost to terms (tf*idf)
        Query expandedQuery = adjust( docsTermVector, queryStr, alpha, beta, decay, docNum, termNum );
        
        return expandedQuery;
    }
       
    /*
     *  When the input document is input in the format of vectors, there's no need to regulate the documents before combine them.
     *  Load parameters and expand query
     */
    public Query expandQuery( Vector<QueryTermVector> docsTermVector, String queryStr, Properties prop )
    throws IOException, ParseException
    {
    	// Load Necessary Values from Properties
    	float alpha = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
    	float beta = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
    	float decay = Float.valueOf( prop.getProperty( QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue();
    	int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();
    	int termNum = Integer.valueOf( prop.getProperty( QueryExpansion.TERM_NUM_FLD ) ).intValue();    
    	        
    	Query expandedQuery = adjust( docsTermVector, queryStr, alpha, beta, decay, docNum, termNum );
    	        
    	return expandedQuery;
    }
    
    
    /*
     * Same as expandQuery. Expand query to a set of term vectors, for the compatibility of further developing.
     */
    public Vector<TermQuery> expandQueryToTerm( String queryStr, Vector<TermFreqVector> hits, Properties prop )
    throws IOException, ParseException
    {
    	// Load Necessary Values from Properties
    	float alpha = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
    	float beta = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
    	float decay = Float.valueOf( prop.getProperty( QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue();
    	int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();   
    	        
    	// Create combine documents term vectors - sum ( rel term vectors )
    	Vector<QueryTermVector> docsTermVector = getDocsTerms( hits, docNum, analyzer );
    	                
    	// Adjust term features of the docs with alpha * query; and beta; and assign weights/boost to terms (tf*idf)
    	Vector<TermQuery> expandedQuery = adjust( docsTermVector, queryStr, alpha, beta, decay, docNum);
    	        
    	return expandedQuery;
    }
       
    /*
     * Same as expandQuery. Expand query from doc terms into term vectors.
     */
    public Vector<TermQuery> expandQueryToTerm( Vector<QueryTermVector> docsTermVector, String queryStr, Properties prop )
    throws IOException, ParseException
    {
    	    	// Load Necessary Values from Properties
    	float alpha = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_ALPHA_FLD ) ).floatValue();
    	float beta = Float.valueOf( prop.getProperty( QueryExpansion.ROCCHIO_BETA_FLD ) ).floatValue();
    	float decay = Float.valueOf( prop.getProperty( QueryExpansion.DECAY_FLD, "0.0" ) ).floatValue();
    	int docNum = Integer.valueOf( prop.getProperty( QueryExpansion.DOC_NUM_FLD ) ).intValue();   
    	    	                
    	    	// Adjust term features of the docs with alpha * query; and beta; and assign weights/boost to terms (tf*idf)
    	Vector<TermQuery> expandedQuery = adjust( docsTermVector, queryStr, alpha, beta, decay, docNum);
    	    	        
    	return expandedQuery;
    }
     
    
    /*
     * Adjust term features of the docs with alpha * query; and beta;
     * and assign weights/boost to terms (tf*idf).
     *
     * @param docsTermsVector of the terms of the top
     *        <code> docsRelevantCount </code>
     *        documents returned by original query
     * @param queryStr - that will be expanded
     * @param alpha - factor of the equation
     * @param beta - factor of the equation
     * @param docsRelevantCount - number of the top documents to assume to be relevant
     * @param maxExpandedQueryTerms - maximum number of terms in expanded query
     * @return expandedQuery with boost factors adjusted using Rocchio's algorithm
     *
     * @throws IOException
     * @throws ParseException
     */
	public Query adjust( Vector<QueryTermVector> docsTermsVector, String queryStr, 
                         float alpha, float beta, float decay, int docsRelevantCount, 
                         int maxExpandedQueryTerms )
    throws IOException, ParseException
    {
        Query expandedQuery;
        
        // setBoost of docs terms
        beta = beta / docsTermsVector.size();
        Vector<TermQuery> docsTerms = setBoost( docsTermsVector, beta, decay );        
        
        // Get queryTerms from the query and setBoost of query terms
        QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );        
        Vector<TermQuery> queryTerms = setBoost( queryTermsVector, alpha );        
        
        // combine weights according to expansion formula
        Vector<TermQuery> expandedQueryTerms = combine( queryTerms, docsTerms );
        setExpandedTerms( expandedQueryTerms ); 
        
        // Sort by boost=weight
        Comparator<Object> comparator = new QueryBoostComparator();
        Collections.sort( expandedQueryTerms, comparator );

        // Create Expanded Query
        expandedQuery = mergeQueries( expandedQueryTerms, maxExpandedQueryTerms );
        
        return expandedQuery;
    }
		
	public Vector<TermQuery> adjust( Vector<QueryTermVector> docsTermsVector, String queryStr, 
            float alpha, float beta, float decay, int docsRelevantCount )
	throws IOException, ParseException
	{		
		// setBoost of docs terms
		beta = beta / docsTermsVector.size();
		Vector<TermQuery> docsTerms = setBoost( docsTermsVector, beta, decay );
		
		// Get queryTerms from the query, setBoost of query terms
		QueryTermVector queryTermsVector = new QueryTermVector( queryStr, analyzer );        
		Vector<TermQuery> queryTerms = setBoost( queryTermsVector, alpha );        
		
		// combine weights according to expansion formula
		Vector<TermQuery> expandedQueryTerms = combine( queryTerms, docsTerms );
		setExpandedTerms( expandedQueryTerms ); 
		
		// Sort by boost=weight
		Comparator<Object> comparator = new QueryBoostComparator();
		Collections.sort( expandedQueryTerms, comparator );
		
		return expandedQueryTerms;
	}
	
	       
    /*
     * Merges query terms into a single query with each term appending a weight 
     * @param termQueries - to merge
     *
     * @return query created from termQueries including boost parameters
     */    
    public Query mergeQueries( Vector<TermQuery> termQueries, int maxTerms )
    throws ParseException
    {
        Query query;
        
        // Select only the maxTerms number of terms
        int termCount = Math.min( termQueries.size(), maxTerms );
        
        // Create Query String
        StringBuffer qBuf = new StringBuffer();
        for ( int i = 0; i < termCount; i++ )
        {
            TermQuery termQuery = termQueries.elementAt(i); 
            Term term = termQuery.getTerm();
            qBuf.append( term.text() + "^" + termQuery.getBoost() + " " );
        }     
        
        // Parse StringQuery to create Query
        query = new QueryParser(Version.LUCENE_36, prop.getProperty(QueryExpansion.FIELD_NAME), analyzer).parse( qBuf.toString() );      
        
        return query;
    }
       
    /*
     * Extracts terms from the first retrieved documents
     *
     * @param doc - from which to extract terms
     * @param docsRelevantCount - number of the top documents to assume to be relevant
     * @param analyzer - to extract terms
     */
    public Vector<QueryTermVector> getDocsTerms( Vector<TermFreqVector> hits, int docsRelevantCount, Analyzer analyzer )
    throws IOException
    {     
		Vector<QueryTermVector> docsTerms = new Vector<QueryTermVector>();
        
        // Process each of the documents
        for ( int i = 0; ( (i < docsRelevantCount) && (i < hits.size()) ); i++ )
        {
        	TermFreqVector vector = hits.elementAt( i );

            // Get text of the document and append it
	        StringBuffer docTxtBuffer = new StringBuffer();	
	        String[] docTxtFlds = vector.getTerms();
	        int[] frequencies = vector.getTermFrequencies();
            for ( int j = 0; j < docTxtFlds.length; j++ )
            {
            	for ( int k = 0; k < frequencies[j]; k++ )
            	{
            		docTxtBuffer.append( docTxtFlds[j] + " " );
            	}
            }      
			
			// Create termVector and add it to vector
			QueryTermVector docTerms = new QueryTermVector( docTxtBuffer.toString(), analyzer );
			docsTerms.add(docTerms );
        }        
        
        return docsTerms;
    }
    
    /*
     * Set boost for the original query
     */
    public Vector<TermQuery> setBoost( QueryTermVector termVector, float factor )
    throws IOException
    {
		Vector<QueryTermVector> v = new Vector<QueryTermVector>();
		v.add( termVector );
		
		return setBoost( v, factor, 0 );
    }
	
    /*
     * Set boost according to the input parameters in "prop"
     */
    public Vector<TermQuery> setBoost( Vector<QueryTermVector> docsTerms, float factor, float decayFactor )
    throws IOException
    {
        Vector<TermQuery> terms = new Vector<TermQuery>();
		
		// setBoost for each of the terms of each of the docs
		for ( int g = 0; g < docsTerms.size(); g++ )
		{
			// Extract terms from existing documents
			QueryTermVector docTerms = docsTerms.elementAt( g );
	        String[] termsTxt = docTerms.getTerms();
	        int[] termFrequencies = docTerms.getTermFrequencies();
			
			// Increase decay according to the rank of the document
			float decay = decayFactor * g;

	        // Populate terms: with TermQuries and set boost
	        for ( int i = 0; i < docTerms.size(); i++ )
	        {
	            // Create Term
	            String termTxt = termsTxt[i];
	            Term term = new Term( prop.getProperty(QueryExpansion.FIELD_NAME), termTxt );
	            
	            // Calculate weight
	            float tf = termFrequencies[i];
	            float weight = tf; // if no similarity is input when request query expansion
	            
	            if(similarity != null )//else, similarity is input, the original boost is tf*idf
	            {
	            	float idf = similarity.idfExplain( term, searcher ).getIdf();  //okapi without idf; lucene:with okapi
	            	weight = weight * idf;
	            }
	            
				// Adjust weight by decay factor
				weight = weight - (weight * decay);
				if(weight < 0){			
					continue;
				}
					            
	            // Create TermQuery and add it to the collection
	            TermQuery termQuery = new TermQuery( term );
	            // Calculate and set boost
	            termQuery.setBoost( factor * weight );
	            terms.add( termQuery );
	        }
		}
		
		// Get rid of duplicates by merging termQueries with equal terms
		merge( terms );		
        
        return terms;
    }
    
    
	/*
	 * Remove duplicates by merging termQueries with equal terms
	 * 
	 * @param terms
	 */
    private void merge(Vector<TermQuery> terms) 
    {
		for ( int i = 0; i < terms.size(); i++ )
		{
			TermQuery term = terms.elementAt( i );
			// Check through terms and if term is equal then merge: combine the boost and delete one of the terms
			for ( int j = i + 1; j < terms.size(); j++ )
			{
				TermQuery tmpTerm = terms.elementAt( j );

				// If equal then merge
				if ( tmpTerm.getTerm().text().equals( term.getTerm().text() ) )
				{
					// Add boost factors of terms
					term.setBoost( term.getBoost() + tmpTerm.getBoost() );
					
					// delete duplicated term
					terms.remove( j );					
					// decrement j so that term is not skipped
					j--;
				}
			}
		}
	}


	/*
     * combine weights according to expansion formula
     */
    public Vector<TermQuery> combine( Vector<TermQuery> queryTerms, Vector<TermQuery> docsTerms )
    {
        Vector<TermQuery> terms = new Vector<TermQuery>();
        // Add Terms from the docsTerms
        terms.addAll( docsTerms );
        
        // Add Terms from queryTerms. If term already exists just combine their boosts
        for ( int i = 0; i < queryTerms.size(); i++ )
        {
            TermQuery qTerm = queryTerms.elementAt(i);
            TermQuery term = find( qTerm, terms );
            
            // Term already exists update its boost
            if ( term != null )
            {
                float weight = qTerm.getBoost() + term.getBoost();
                term.setBoost( weight );
            }
            // Term does not exist, add it
            else
            {
                terms.add( qTerm );
            }
        }
        
        return terms;
    }
    
    
    /*
     * Find out duplicated terms
     */
    public TermQuery find( TermQuery term, Vector<TermQuery> terms )
    {
        TermQuery termF = null;

        // using loop to check every term
        Iterator<TermQuery> iterator = terms.iterator();
        while ( iterator.hasNext() )
        {
            TermQuery currentTerm = iterator.next();
            if ( term.getTerm().equals( currentTerm.getTerm() ) )
            {
                termF = currentTerm;
            }
        }
        
        return termF;
    }



    /*
     * Truncate the list of terms, the number of terms is set in "prop"
     */
    public Vector<TermQuery> getExpandedTerms()
    {
        int termNum = Integer.valueOf( prop.getProperty( QueryExpansion.TERM_NUM_FLD ) ).intValue();
        Vector<TermQuery> terms = new Vector<TermQuery>();
        
        // Return only necessary number of terms
        List<TermQuery> list = this.expandedTerms.subList( 0, termNum );
        terms.addAll( list );
        
        return terms;
    }
    
    /*
     * return expanded terms
     */
    private void setExpandedTerms( Vector<TermQuery> expandedTerms )
    {
        this.expandedTerms = expandedTerms;
    }
        
}
