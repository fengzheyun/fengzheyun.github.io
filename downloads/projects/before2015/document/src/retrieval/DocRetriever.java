package retrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryTermVector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.Version;
import org.apache.lucene.wordnet.SynExpand;
import org.apache.lucene.wordnet.Syns2Index;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;

import com.google.gson.Gson;
import queryexpansion.QueryExpansion;

/**
 * This application performs document retrieval which is based on the relevance score. 
 * There are two types of scoring formula, and three query expansion methods. 
 * 
 * The scoring are lucene default one, and okapi(BM25); and the expansions are such that pseudo relevance feedback, 
 * Goolge returned documents and random walk methods.
 * 
 * @author Zheyun Feng - fengzhey@msu.edu
 *
 */
@SuppressWarnings("deprecation")
public class DocRetriever {
	
	private FSDirectory dir; //index directory
	private TrecAnalyzer analyzer; 
	int NumHits1; // number of first returned documents, used for query expansion
	int NumHits2; // number of final returned documents.
	private Vector<String> FieldsName; 
	private Properties props; // set of parameters
	
	private IndexReader reader;
	private IndexSearcher searcher;
	private String mode; // scoring formula
	private String method; // query expansion method
	
	private int[] docLength;
	private double avLength;
	
	private static String WordNet_PROLOG; // path of wordnet file
	private static String WordNet_INDEX;  // index directory of wordnet
	private String StrExpand; // reserved space
	private int numterms; // number of terms extracted
	
	public static void main(String[] args) throws Exception 
	  {
		System.out.println("Setting right now(r) or using default setting(d)? ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    String setting = br.readLine();
	    String mode_type = null, QEmethod=null, option=null;
	    String index_dir = null, query_con=null, output_dir=null;
	    int numHits;
	    // ************************************ Setting for all methods *****************************
	    if(setting.equals("d")){	
	    	option = "w";//"w"-wordexpansion, "r"-retrieval
	    
	    	//Number of documents to retrieve or Number of words to expand to
	    	numHits = 50;//Integer.valueOf( br.readLine() );
	    	index_dir = "C:/zfeng/TrecJava/Index/Trec";  
	    	query_con = "C:/zfeng/TrecJava/data/0809/query.txt";//C:/zfeng/TrecJava/data/484/query/query.txt";C:/zfeng/TrecJava/data/0809/query.txt
	    	output_dir = "C:/zfeng/TrecJava/data/0809/results/"; //C:/zfeng/TrecJava/data/0809/results/
	    			    
	    	if(option.equals("r")){
	    		mode_type = "okapi"; //"okapi" "lucene" 
	    		QEmethod = "google"; //"rocchio" "google" "null" "wordnet"
	    	}else{
	    		QEmethod = "rw"; //"rocchio" "google""wordnet" "rw"	
	    		if(QEmethod.equals("rocchio")){
	    			mode_type = "okapi"; //"okapi" "lucene"
	    		}
	    	}
	    }else{
			System.out.println("Please choose the task: retrieval(r) or wordexpansion(w)? ");
		    br = new BufferedReader(new InputStreamReader(System.in));
		    option = br.readLine();//"w"-wordexpansion, "r"-retrieval
		    if(option.equals("r")){
		    	System.out.println("Number of documents to retrieve: ");
		    }else if(option.equals("w")){
		    	System.out.println("Number of words to expand to: ");		    
		    }else{
		    	System.out.println("No such task. ");
		    	System.exit(-1);
		    }
		    br = new BufferedReader(new InputStreamReader(System.in));
		    numHits = Integer.valueOf( br.readLine() );
		    
		    System.out.println("Enter the path of the index directory: ");
		    br = new BufferedReader(new InputStreamReader(System.in));
		    index_dir = br.readLine();
		    
		    System.out.println("Enter the path of query file: ");
		    br = new BufferedReader(new InputStreamReader(System.in));
		    query_con = br.readLine();//"C:/zfeng/TrecJava/data/484/query/query.txt";C:/zfeng/TrecJava/data/0809/query.txt
		    
		    System.out.println("Enter the output directory: ");
		    br = new BufferedReader(new InputStreamReader(System.in));
		    output_dir = br.readLine();//"C:/zfeng/TrecJava/data/484/results/"; C:/zfeng/TrecJava/data/0809/results/
		    
		    if(option.equals("r")){
		    	System.out.println("Which scoring way are you going to use:  okapi or lucene ?");
			    br = new BufferedReader(new InputStreamReader(System.in));
			    mode_type = br.readLine(); //"okapi" "lucene"
			    
			    System.out.println("Which query expansion method are you going to use:");
			    System.out.println("null, rocchio, google, or wordnet?");
			    br = new BufferedReader(new InputStreamReader(System.in));  
				QEmethod = br.readLine(); //"rocchio" "google" "null" "wordnet"
		    }else{
		    	System.out.println("Which query expansion way are you going to use:");
			    System.out.println("rocchio, google, wordnet or rw?");
			    br = new BufferedReader(new InputStreamReader(System.in));  
				QEmethod = br.readLine(); //"rocchio" "google""wordnet" "rw"	
				
				if(QEmethod.equals("rocchio")){
					System.out.println("Which scoring way are you going to use:  okapi or lucene ?");
				    br = new BufferedReader(new InputStreamReader(System.in));
				    mode_type = br.readLine(); //"okapi" "lucene"
				}
		    }
	    }
		    
		DocRetriever retriever = null;
		try{
		    String type = mode_type + "-" + QEmethod;
		    retriever = new DocRetriever(index_dir, numHits, type );
		} catch (Exception ex) {
		    System.out.println("Cannot get the index..." + ex.getMessage());
		    System.exit(-1);
		}		
			
		if(setting.equals("d")){
			if(QEmethod.equals("rocchio") || QEmethod.equals("google")){
		    	// ************************************ Rocchio & Google ************************************
			    retriever.NumHits1 = 5;
			    float alpha = 0.1f;
			    retriever.props.setProperty("rocchio.beta", Float.toString(alpha) );// alpha: parameter for rocchio's algorithm
		    }else if(QEmethod.equals("wordnet")){
		    	// ************************************ WordNet synonyms ************************************
			    float wn_param = 0.2f; // parameter for wordnet expansion
			    retriever.props.setProperty("WN.param", Float.toHexString(wn_param));
			    
			    //Enter the path of the WordNet file(wn_s.pl)
			    WordNet_PROLOG = "C:/zfeng/TrecJava/Sources/WNprolog-3.0/wn_s.pl"; //path of wordnet file
			    
			    // Enter the path of the index directory for WordNet synonym library
				WordNet_INDEX = "C:/zfeng/TrecJava/Index/Syn"; // index directory of wordnet
		    }else if(QEmethod.equals("rw")){		
				// ************************************ Query Expansion by Random Walk *******************************
				int RWorder = 0; // parameter for RW expansion
				retriever.props.setProperty("RW.dim", Integer.toString(RWorder));// parameter for RW expansion
		    }		
			// ************************************ Finish Setting **************************************		    	
		}else{    
		    if(QEmethod.equals("rocchio") || QEmethod.equals("google")){
		    	// ************************************ Rocchio & Google ************************************
		    	System.out.println("Enter the number of docuemnts used to expand the query or query word: ");
			    br = new BufferedReader(new InputStreamReader(System.in));
			    retriever.NumHits1 = Integer.valueOf(br.readLine());
			    
			    System.out.println("Enter the parameter for rocchio's algorithm: ");
			    br = new BufferedReader(new InputStreamReader(System.in));
			    retriever.props.setProperty("rocchio.beta", br.readLine());// alpha: parameter for rocchio's algorithm
		    }else if(QEmethod.equals("wordnet")){
		    	// ************************************ WordNet synonyms ************************************
		    	System.out.println("Enter the parameter for wordnet expansion: ");
			    br = new BufferedReader(new InputStreamReader(System.in));
			    retriever.props.setProperty("WN.param", br.readLine());
			    
			    System.out.println("Enter the path of the WordNet file(wn_s.pl): ");
			    br = new BufferedReader(new InputStreamReader(System.in));
			    WordNet_PROLOG = br.readLine(); //path of wordnet file
			    
			    System.out.println("Enter the path of the index directory for WordNet synonym library ");
			    br = new BufferedReader(new InputStreamReader(System.in));	
			    WordNet_INDEX = br.readLine(); // index directory of wordnet
		    }else if(QEmethod.equals("rw")){		
				// ************************************ Query Expansion by Random Walk *******************************
		    	System.out.println("Enter the order of the RW: ");
			    br = new BufferedReader(new InputStreamReader(System.in));
				retriever.props.setProperty("RW.dim", br.readLine());// parameter for RW expansion
		    }		
			// ************************************ Finish Setting **************************************
	    }
	    
	    String content_name = "contents";
		String doc_name = "name";
	    retriever.FieldsName.add( content_name );
	    retriever.FieldsName.add( doc_name );
	    retriever.FieldsName.add( "order" );
	    
	    retriever.props.setProperty("QE.decay", "0");// the decay parameters according to the rank of retrieved documents
	    retriever.props.setProperty("QE.doc.num", Integer.toString( retriever.NumHits1 ));
	    retriever.props.setProperty("QE.term.num", "1000"); // this could be changed. The maximum number of terms used after the query expansion
	    retriever.props.setProperty("rocchio.alpha", "1"); // the parameter of the original query terms	    
	    retriever.props.setProperty("field.name", retriever.FieldsName.elementAt(0));
	    	    
	    if(option.equals("r"))
	    {
	    	retriever.RetrievalTest( query_con,  output_dir );
	    }else if(option.equals("w")){
	    	retriever.WordsExpansion( query_con,  output_dir );
	    }
	    retriever.close();
	    System.out.println("\n****************** Finish! ******************");
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
	  }
	
	/*
	 * Constructor: document retrieval application
	 * @param index_dir: the name of the folder in which the index would be created
	 * @param numHit1: number of documents returned for query expansion
	 * @param numHit2: number of returned retrieved documents
	 * @param type: method type
	 * 
	 * @throws java.io.IOException
	 */
	public DocRetriever(String index_dir, int numHit2, String type) throws Exception 
	  { 
		this.dir = FSDirectory.open(new File(index_dir));
		this.analyzer = new TrecAnalyzer( );
		this.NumHits2 = numHit2;		
		this.FieldsName = new Vector<String>();
		this.props = new Properties();
		
		this.searcher = new IndexSearcher(dir, true);
		this.reader = IndexReader.open( dir );
		
		String[] typeArray = type.split("-");
		this.mode = typeArray[0];
		this.method = typeArray[1];
		this.StrExpand = null;
		
	  }
	
	/*
	 * Close all the defined registers
	 */
	public void close() throws IOException 
	  { 
		dir.close();
		analyzer.close();		
		FieldsName.clear();
		
		props.clear();
		searcher.close();
		reader.close();	
	  }
	
	/*
	 * Analyze different setting for different retrieval methods, and then call for the corresponding functions
	 * This is a function to call for different retrieve methods using different query expansions
	 * 
	 */
	public void RetrievalTest( String queryList, String outputDir ) throws Exception
	{
		// preparing
		File f = new File(outputDir +"rank/");
		if(!f.exists())
		{
			boolean success = f.mkdirs();
			if (success){
				System.out.println("Directory: " + outputDir + "rank/ has been created");
			}
		}		
		
		String params;
		String returnListName = null;
		if(method.equals("rocchio")){
			params = "_"+NumHits1+"_"+props.getProperty( QueryExpansion.ROCCHIO_BETA_FLD );
			returnListName =  outputDir +"rank/"+ mode + "_"+method + params+".txt";
		}else if(method.equals("google")){
			params = "_"+NumHits1+"_"+props.getProperty( QueryExpansion.ROCCHIO_BETA_FLD );
			returnListName =  outputDir +"rank/"+ mode + "_"+method + params+".txt";			
			File test = new File(outputDir+"GoogleResults/");
			if(!test.exists())
			{
				boolean success = test.mkdirs();
				if (success){
					System.out.println("Directory: " + outputDir+"GoogleResults/ has been created"); }
			}
		}		
		else if(method.equals("wordnet") ){
			params = "_"+props.getProperty( "WN.param" );
			returnListName =  outputDir +"rank/"+ mode + "_"+method + params+".txt";
		}else if(method.equals("null") ){
			params = "";	
			returnListName =  outputDir +"rank/"+ mode + "_"+method + params+".txt";
		}else{
			params = null;
			System.out.println("No such retrieval method!");
			System.exit(-1);
		}
		
		if(mode.equals("okapi") )
		{
			getAverageDocLength( FieldsName.elementAt(0) );
		}
		
	    BufferedReader queryFile =  new BufferedReader(new FileReader( queryList ));
	    PrintWriter outputFile = new PrintWriter(new FileWriter(returnListName));
	    ScoreDoc[] hits = null;
	   
	    String querystr = queryFile.readLine();
	    while(querystr != null)
	    {
		    
		    String[] queryArray = querystr.split(",");
		    String querytext = querystr.substring(queryArray[0].length()+1);
		    
		    if(mode.equals("okapi"))
		    {
		    	if(method.equals("rocchio") ){
		    		hits = okapiRocchio( querytext );
		    	}
		    	else if(method.equals("google") ){
		    		StrExpand = outputDir+"GoogleResults/"+queryArray[0]+".txt";
		    		hits = okapiGoogle(querytext);
		    	}
		    	else if(method.equals("wordnet") ){
		    		hits = okapiWordnet(querytext);
		    	}else{
		    		hits = okapiTopHits(querytext);
		    	}
		    	
		    }else if(mode.equals("lucene")){
		    	if(method.equals("rocchio") ){
		    		hits = luceneRocchio( querytext );
		    	}
		    	else if(method.equals("google") ){
		    		StrExpand = outputDir+"GoogleResults/"+queryArray[0]+".txt";
		    		hits = luceneGoogle(querytext);
		    	}
		    	else if(method.equals("wordnet") ){
		    		hits = luceneWordnet(querytext);
		    	}
		    	else{
		    		hits = returnTopHits(querytext);
		    	}
		    }
		    	
		    // output the retrieved results into a file
		    for(int i=0;i<hits.length;++i) 
		    {
		      int docId = hits[i].doc;
		      Document d = searcher.doc(docId);    	      
		      outputFile.println(queryArray[0] + " Q0 " + d.get(FieldsName.elementAt(1)) + " "+ (i+1) + " "+ hits[i].score + " Exp");
		    }
		    
		    querystr = queryFile.readLine();
	    }
	    
	    outputFile.close();
	    queryFile.close();
	}
	
	/*
	 * This function is just for query expansion with methods of Random Walk and rocchio's algorithm
	 */
	public void WordsExpansion( String queryList, String outputDir ) throws Exception
	{
		// preparing
		File f = new File(outputDir +"qExpan/");
		if(!f.exists())
		{
			boolean success = f.mkdirs();
			if (success){
				System.out.println("Directory: " + outputDir + "qExpan/ has been created");
			}
		}
		
		String params;
		String returnListName = null;
		if(method.equals("rocchio")){
			params = "_"+NumHits1+"_"+props.getProperty( QueryExpansion.ROCCHIO_BETA_FLD );
			returnListName =  outputDir +"qExpan/"+ mode + "_"+method + params+"/";
			if(mode.equals("okapi")){
				getAverageDocLength( FieldsName.elementAt(0) );
			}
		}else if(method.equals("google")){
			StrExpand = outputDir+"GoogleResults/";
			File test = new File(StrExpand);
			if(!test.exists())
			{
				boolean success = test.mkdirs();
				if (success){
					System.out.println("Directory: " + StrExpand + " has been created");
				}
			}		
			params = "_"+NumHits1+"_"+props.getProperty( QueryExpansion.ROCCHIO_BETA_FLD );
			returnListName =  outputDir +"qExpan/"+ method + params+"/";
		}
		else if(method.equals("wordnet")){
			params = "_"+props.getProperty( "WN.param" );
			returnListName =  outputDir +"qExpan/"+ method + params+"/";			
		}else if(method.equals("rw") ){
			returnListName =  outputDir +"qExpan/"+ method + "_"+"expan_"+props.getProperty( "RW.dim" )+"/"; 
			StrExpand = outputDir+"Temps/";	
		}else{
			params = null;
			System.out.println("No such query expansion method!");
			System.exit(-1);
		}
		
		f = new File(returnListName);
		if(!f.exists())
		{
			boolean success = f.mkdirs();
			if (success){
				System.out.println("Directory: " + returnListName + " has been created");
			}
		}
		
		if(method.equals("rw") ){
			RWExpandWords( queryList, returnListName );
		}else{
			ExpandWords( queryList, returnListName );
		}
		
	}
	
	/*
	 * Query expansion using standard RW
	 * @param queryList: the path of query file. In the file, each row represent a query with the number of query ahead of it.
	 * @param outputDir: the directory of the final expanded features.
	 */
	public void RWExpandWords( String queryList, String outputDir ) throws Exception
	{	
		BufferedReader queryname =  new BufferedReader(new FileReader( queryList ));
		queryList = RWgetPrepared( StrExpand, queryList );
	    BufferedReader queryFile =  new BufferedReader(new FileReader( queryList ));
	    
	    // get all the features in descending order of the frequency
	    TextFileIndexer indexer = new TextFileIndexer(dir.getDirectory().getCanonicalPath());
		TermStats[] termList = indexer.getHighFreqTerms(FieldsName.elementAt(0), 10000);
		indexer.close();
	    
	    ScoreDoc[] hits;
	   
	    String querystr = queryFile.readLine();
	    while(querystr != null) // get the query or query word
	    {
	    	String qname = queryname.readLine();
	    	System.out.println(qname);
	    	String[] queryNames = qname.split(",");
		    String nametext = qname.substring(queryNames[0].length()+1);
	    	FileWriter newCSV = new FileWriter( outputDir +  nametext + ".csv");
	    	
		    String[] queryArray = querystr.split(",");
		    String querytext = querystr.substring(queryArray[0].length()+1);
		    hits = RWQueryExpansion(querytext );
		    	
		    		
		    for(int i=0;i<hits.length;++i) 
		    {
		      int docId = hits[i].doc;
		      newCSV.append(termList[docId].term.text() + "," + hits[i].score+"\n");
		    }
		    newCSV.close();
		    querystr = queryFile.readLine();
	    }
	    
	    queryFile.close();
	    queryname.close();
	}

	/*
	 * Query expansion using rocchio's algorithm or WordNet library. Documents are retrieved from the original collection or by google engine.
	 * @param queryList: the path of query file. In the file, each row represent a query with the number of query ahead of it.
	 * @param outputDir: the directory of the final expanded features.
	 */
	public void ExpandWords( String queryList, String outputDir ) throws Exception
	{    
		String title = StrExpand;
	    BufferedReader queryFile =  new BufferedReader(new FileReader( queryList ));
	   
	    String querystr = queryFile.readLine();
	    while(querystr != null)
	    {
	    	// extract the query
	    	String[] queryArray = querystr.split(",");
		    String querytext = querystr.substring(queryArray[0].length()+1);
		    
			Query q = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( querytext );
			QueryExpansion queryExpansion = new QueryExpansion(analyzer, searcher, q.getSimilarity(searcher), props);
			String qtext = q.toString().replaceAll(FieldsName.elementAt(0)+":", "");			
			
			// get documents and expand the query			
			if(method.equals("google"))
			{
				StrExpand = title+queryArray[0]+".txt";
			    Vector<QueryTermVector> ReturnDocs;
				ReturnDocs = localTopHits(querytext);
				if(ReturnDocs.isEmpty())
				{
					ReturnDocs = googleTopHits(querytext);
				}				
			    q = queryExpansion.expandQuery(ReturnDocs, querytext,  props);
			}else if(method.equals("wordnet")){
				String eqstr = wordNetQueryExpansion(querytext);
				q = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( eqstr );	
			}else if(method.equals("rocchio"))
			{
				ScoreDoc[] hits = null;
				if(mode.equals("okapi")){
					hits = okapiTopHits(querytext);
				}else if(mode.equals("lucene")){
					hits = returnTopHits(searcher, q, NumHits1);// retrieve documents which will be used to expand the very query
				}
				// Create new query			     
				Vector<TermFreqVector> ReturnDoc = new Vector<TermFreqVector>();
				for(int i=0;i<hits.length;++i) 
				{    
					TermFreqVector vector = reader.getTermFreqVector(hits[i].doc, "contents");
					ReturnDoc.add( vector );
				}
				q = queryExpansion.expandQuery(querytext, ReturnDoc, props);
				
			}
		    
			// some further processing
		    String qstr = q.toString();
		    qstr = qstr.replaceAll(FieldsName.elementAt(0)+":", "");
	      
		    
		    String[] pairs = qstr.split(" ");
		    float qscore = 0;//in order to normalize the relevance score, get the score of the query itself
		    for(int i=0; i<pairs.length; i++)
		    {
		    	int phat =pairs[i].indexOf("^");
		    	if(phat==-1){
		    		if(pairs[i].equals(qtext)){
		    			qscore = 1;break;
		    			}else continue;
		    	}
		    	if(pairs[i].substring(0, phat).equals(qtext))
		    	{
		    		qscore = Float.valueOf(pairs[i].substring(phat+1));
		    		break;
		    	}	    	
		    }
		    
		    //write the feature document
		    FileWriter newCSV = new FileWriter( outputDir +  querytext + ".csv");
		    int numT = Math.min(NumHits2, pairs.length);
		    for(int i=0; i<numT; i++)
		    {
		    	int phat =pairs[i].indexOf("^");
		    	if(phat==-1){
		    		float val = 1.0f/qscore;
		    		newCSV.append(pairs[i]+","+val+"\n");
		    	}else{
			    	String term = pairs[i].substring(0, phat);
			    	String value = pairs[i].substring(phat+1);
			    	float val = Float.valueOf(value)/qscore;
			    	newCSV.append(term+","+val+"\n");
		    	}
		    }
		    newCSV.close();		    
		    querystr = queryFile.readLine();
	    }	    
	    queryFile.close();

	}
	

	
	/*
	 * retrieve documents using lucene default scoring formula
	 * @param querystr: query
	 */
	public ScoreDoc[] returnTopHits(String querystr) throws Exception 
	{		
		Query query = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( querystr );
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(NumHits2, true);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		return hits;
	}
	
	/*
	 * expand the query using rocchio's algorithm and then retrieve documents using lucene default scoring formula.
	 * @param querystr: query
	 */
	public ScoreDoc[] luceneRocchio( String querystr ) throws Exception
	{    	
		Query q = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( querystr );
		
		ScoreDoc[] hits = returnTopHits(searcher, q, NumHits1);
	    System.out.println("\nFound " + hits.length + " hits in the first retrieval:");
	    for(int i=0;i<hits.length;++i) 
	    {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        System.out.println((i + 1) + ". Doc " + docId+"\tName: " + d.get(FieldsName.elementAt(1))+"\tRelevance Score: "+ hits[i].score +"\n");
	    }
	         
	    // Create the expanded query			     
	    Vector<TermFreqVector> ReturnDoc = new Vector<TermFreqVector>();
	    for(int i=0;i<hits.length;++i) 
	    {    
	    	TermFreqVector vector = reader.getTermFreqVector(hits[i].doc, "contents");
	    	ReturnDoc.add( vector );
	    }
      
	    QueryExpansion queryExpansion = new QueryExpansion(analyzer, searcher, q.getSimilarity(searcher), props);
	    q = queryExpansion.expandQuery(querystr, ReturnDoc, props);
	    hits = returnTopHits(searcher, q, NumHits2);
	    System.out.println("\nFound " + hits.length + " hits in the second retrieval:");
	    for(int i=0;i<hits.length;++i) 
	    {
	    	int docId = hits[i].doc;
	    	Document d = searcher.doc(docId);
	    	System.out.println((i + 1) + ". Doc " + docId+"\tName: " + d.get(FieldsName.elementAt(1))+"\tRelevance Score: "+ hits[i].score +"\n");
	    }
      
	    return hits;
	}
	
	/*
	 * Expand the query using google search engine, and then using lucene default scoring formula to do the document search
	 * @param querystr: query
	 */
	public ScoreDoc[] luceneGoogle( String querystr ) throws Exception
	{   				
		Vector<QueryTermVector> ReturnDocs;
		ReturnDocs = localTopHits(querystr);
		if(ReturnDocs.isEmpty())
		{
			ReturnDocs = googleTopHits(querystr);
		}
		
		Query q = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( querystr );
		QueryExpansion queryExpansion = new QueryExpansion(analyzer, searcher, q.getSimilarity(searcher), props);
	    q = queryExpansion.expandQuery(ReturnDocs,querystr,  props);
	    ScoreDoc[] hits = returnTopHits(searcher, q, NumHits2);
	    ReturnDocs.clear();
	    return hits;
	}

	/*
	 * Expand the query using WordNet synonym library, and then using lucene default scoring formula to do the document search
	 * @param querystr: query
	 */
	public ScoreDoc[] luceneWordnet(String querystr) throws Exception 
	{			
		String eqstr = wordNetQueryExpansion(querystr);
		Query query = new QueryParser(Version.LUCENE_36, FieldsName.elementAt(0), analyzer).parse( eqstr );			
		ScoreDoc[] hits = returnTopHits(searcher, query, NumHits2);
		return hits;
	}
	
	
	
	/*
	 * retrieve documents using okapi scoring formula
	 * @param querystr: query
	 */
	public ScoreDoc[] okapiTopHits(String querystr) throws Exception 
	{		
		Vector<TermQuery> termList = okapiParseQuery( querystr );
		ScoreDoc[] hits = okapiScore(termList, NumHits2);
		return hits;
	}
	
	/*
	 * expand the query using rocchio's algorithm and then retrieve documents using okapi scoring formula.
	 * @param querystr: query
	 */
	public ScoreDoc[] okapiRocchio( String querystr ) throws Exception
	{   		
		Vector<TermQuery> termList = okapiParseQuery( querystr );
		ScoreDoc[] hits = okapiScore(termList, NumHits1);
	    System.out.println("\nFound " + hits.length + " hits in the first retrieval:");
	    for(int i=0;i<hits.length;++i) 
	    {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        System.out.println((i + 1) + ". Doc " + docId+"\tName: " + d.get(FieldsName.elementAt(1))+"\tRelevance Score: "+ hits[i].score +"\n");
	    }
      
	    QueryExpansion queryExpansion = new QueryExpansion(analyzer, searcher, props);
	    // Create new query			     
	    Vector<TermFreqVector> ReturnDoc = new Vector<TermFreqVector>();
	    for(int i=0;i<hits.length;++i) 
	    {    
	        TermFreqVector vector = reader.getTermFreqVector(hits[i].doc, "contents");
	        ReturnDoc.add( vector );
	    }
	    termList = queryExpansion.expandQueryToTerm(querystr, ReturnDoc, props);
	    hits = okapiScore(termList, NumHits2);
	    System.out.println("\nFound " + hits.length + " hits in the second retrieval:");
	    for(int i=0;i<hits.length;++i) 
	    {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        System.out.println((i + 1) + ". Doc " + docId+"\tName: " + d.get(FieldsName.elementAt(1))+"\tRelevance Score: "+ hits[i].score +"\n");
	    }
	      
	    return hits;
  }
		
	/*
	 * expand the query using documents retrieved by google search engine, and then retrieve new documents using okapi scoring formula.
	 * @param querystr: query
	 */
	public ScoreDoc[] okapiGoogle( String querystr ) throws Exception
	{   		
		Vector<QueryTermVector> ReturnDocs;
		ReturnDocs = localTopHits(querystr);
		if(ReturnDocs.isEmpty())
		{
			ReturnDocs = googleTopHits(querystr);
		}
     
	    QueryExpansion queryExpansion = new QueryExpansion(analyzer, searcher, props);
	    Vector<TermQuery> termList = queryExpansion.expandQueryToTerm( ReturnDocs, querystr, props);
	    ScoreDoc[] hits = okapiScore(termList, NumHits2);
	    ReturnDocs.clear();
        return hits;
	}
	
	/*
	 * expand the query using WordNet synonym library and then retrieve new documents using okapi scoring formula.
	 * @param querystr: query
	 */
	public ScoreDoc[] okapiWordnet(String querystr) throws Exception 
	{		
		String eqstr = wordNetQueryExpansion(querystr);
		Vector<TermQuery> termList = okapiParseQuery( eqstr );				
		ScoreDoc[] hits = okapiScore(termList, NumHits2);
		return hits;
	}

	
	
/*
 * Return documents retrieved by lucene's default scoring.	
 * @param numHits: numbe of returned documents
 */
private static ScoreDoc[] returnTopHits(Searcher searcher, Query query, int numHits) throws IOException 
{		
	TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
    searcher.search(query, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	return hits;
}

/*
 * Retrieve documents using google search engine. The documents returned will be stored in the local disk. 
 * Usually the application will create a directory called "GoogleResults" under the output directory, and store the returned documents.
 * This because Google has some restricts in the user's term, the user can't request too many search. So if the Google returned documents already exists,
 * the application will ask the customer if a re-search is needed. Only if the customer answers yes, the application will re-retrieve the documents in Google.
 * 
 * @param querystr: query
 */
public Vector<QueryTermVector> googleTopHits(String querystr) throws Exception 
{		
	Vector<QueryTermVector> ReturnDoc = new Vector<QueryTermVector>();   
	
	ReturnDoc.clear();
	PrintWriter outputFile = new PrintWriter(new FileWriter(StrExpand));
	String google = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
	int numDoc = 0;
	while(numDoc<NumHits1)
	{
		waitSec(2);
		// The user ip could be changed. The provided ip is frequently used, and also customer can use 192.168.0.1 and others
	    URL url = new URL(google + URLEncoder.encode(querystr, "UTF-8") + "&start="+numDoc+ "&userip=192.51.0.1");
	    Reader reader = new InputStreamReader(url.openStream(), "UTF-8");
	    GoogleResult results = new Gson().fromJson(reader, GoogleResult.class);
	    
	    if(results.getResponseData()==null)
	    {
	    	System.out.println("\nError "+results.getResponseStatus()+": "+results.getResponseDetail());
	    	if(results.getResponseStatus().equals("403"))
	    	{
	    		//Suspected Terms of Service Abuse. Please see http://code.google.com/apis/errors
	    		System.exit(-1);
	    	}else
	    	{
	    		continue;
	    	}
	    }
	    
	    int numI = Math.min(4, NumHits1-numDoc);
	    for(int i=0; i<numI;i++)
	    {
	    	String resulstr = results.getResponseData().getResults().get(i).getContent();
	    	if(resulstr.equals("")){
	    		resulstr = results.getResponseData().getResults().get(i).getTitle();
	    	}
	    	
	    	if(!resulstr.equals("")){
		    	resulstr = regulateData(resulstr);		    	
		    	QueryTermVector docTerms = new QueryTermVector( resulstr, analyzer );
		    	ReturnDoc.add(docTerms);
	    	}
	    	numDoc++;	    
	    	System.out.println(numDoc+": " + resulstr);	
	    	outputFile.println(numDoc+": " + resulstr);	
	    }
	    reader.close();
	}
	
	outputFile.close();
	System.out.println("Found " + numDoc + " hits using GOOGLE in the first retrieval.\n");
    
	return ReturnDoc;
}

/*
 * Extract the terms from local documents. This function is used to extract the terms from the google returned documents, but
 * if the customer wants to use some other document collection to expand the query, he/she can only put the collection under directory
 * output directory/GoogleResults, or define a new directory and change the path in this function
 */
public Vector<QueryTermVector> localTopHits(String querystr) throws Exception 
{		
	Vector<QueryTermVector> ReturnDoc = new Vector<QueryTermVector>();   

	File D = new File(StrExpand);
	if(!D.exists())
	{
		System.out.println("No corresponding document exists.\n");
		return ReturnDoc;
	}
	BufferedReader docFile =  new BufferedReader(new FileReader( StrExpand ));
	
	for(int i=0; i<NumHits1;i++)
	{
		String docstr = docFile.readLine();
		if(docstr == null)
		{
			System.out.println("No enough documents in the files. Using Google to retrieve documents.");
			ReturnDoc.clear();
			break;
		}
		String[] docArray = docstr.split(":");
		String doctext = docstr.substring(docArray[0].length()+2);
		QueryTermVector docTerms = new QueryTermVector( doctext, analyzer );
		ReturnDoc.add(docTerms);
	}
	
	docFile.close();
	return ReturnDoc;			
}

/*
 * Parse a query string into terms
 */
public Vector<TermQuery> okapiParseQuery(String querystr) throws Exception 
{			
	QueryTermVector docTerms = new QueryTermVector( querystr, analyzer );
	String[] termsTxt = docTerms.getTerms();
    int[] termFrequencies = docTerms.getTermFrequencies();
       
    Vector<TermQuery> terms = new Vector<TermQuery>();
    for(int i=0;i<docTerms.size();i++)
    {    
    	Term term = new Term( FieldsName.elementAt(0), termsTxt[i] );
    	TermQuery termQuery = new TermQuery( term );
    	termQuery.setBoost( termFrequencies[i] );
    	terms.add( termQuery );
    }
    
    return terms;    
}

/*
 * Expand query using WordNet library
 */
public String wordNetQueryExpansion(String querystr) throws Exception 
{			
	File wn_f = new File( WordNet_INDEX );// index file
	if(!wn_f.exists() )
	{
		wn_f = new File( WordNet_PROLOG );//wordnet library
		if(!wn_f.exists())
		{
			System.out.println("No Synonyms Index Exists in default path.");
			System.out.println("Please enter the index directory path or WordNet prolog file path.\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String path = br.readLine();
			if(path.endsWith(".pl"))
		    {
				WordNet_PROLOG =  path; 
				System.out.println("Please enter a directory path to store index.\n");
				br = new BufferedReader(new InputStreamReader(System.in));
				WordNet_INDEX = br.readLine();
							
				try {
					String[] prog = {WordNet_PROLOG, WordNet_INDEX};
					Syns2Index.main(prog);
				} catch (Throwable e) {
					e.printStackTrace();
				}
		    }else{
		    	WordNet_INDEX =  path; 
		    }
		}else{
			try {
				String[] prog = {WordNet_PROLOG, WordNet_INDEX};
				Syns2Index.main(prog);// do the indexing using the library
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}		
	}
	
	Searcher wnSearcher = null;
	try {
		wnSearcher = new IndexSearcher( FSDirectory.open(new File(WordNet_INDEX)) );
	} catch (Throwable e) {
		System.out.println("Synonym Index NOT Valid!");
		e.printStackTrace();
	}
	
	float param = Float.valueOf( props.getProperty( "WN.param" ) );
	StringBuffer qBuf = new StringBuffer();

	String[] arrayStr = querystr.split(" ");
	for(int i=0; i<arrayStr.length; i++)
	{
		Query eq = SynExpand.expand(arrayStr[i], wnSearcher, analyzer, FieldsName.elementAt(0), param);//expand each term in the query
		String wholeq = eq.toString();
		if(wholeq.isEmpty()){continue;}
		wholeq = wholeq.replaceAll(FieldsName.elementAt(0)+":", "");		
		qBuf.append( wholeq + " ");
	}
	
	return qBuf.toString();
}

/*
 * Expand query using Random Walk method
 */
public ScoreDoc[] RWQueryExpansion(String querystr ) throws Exception 
{		  	   
    //*********************************************************************************	
  	// Create expanded query matrix
  	//*********************************************************************************
	DoubleMatrix1D qmat = ATbyVt(StrExpand+"AT.txt", querystr, numterms, 0);
	
	int Order = Integer.valueOf( props.getProperty( "RW.dim" ) );
	for(int i=0; i<Order; i++)
	{
		qmat = AbyVd(StrExpand+"A.txt", qmat);
		qmat = ATbyVt(StrExpand+"AT.txt", qmat);
	}
	qmat = AbyVd(StrExpand+"A.txt", qmat);
	
	String[] s = querystr.split(":");
	int k = Integer.valueOf(s[0]);
	float selfsore = (float)qmat.getQuick(k);
    
    // sort according to the score
 	hitsRankQueue docQueue = new hitsRankQueue(numterms);
 	for(int j=0;j<numterms;j++)
 	{
 		float logScore =  (float) qmat.getQuick(j)/selfsore;
 		if(logScore==0){
 			continue;
 		}
 		docQueue.insertWithOverflow(new ScoreDoc(j, logScore));
 	}

 	int num = Math.min(50, qmat.cardinality());
 	ScoreDoc[] result = new ScoreDoc[num];
 	for(int j=0;j<num;j++)
 	{
 		result[j] = docQueue.pop();

 	}
 	docQueue.clear();	
 	return result;	
  
  }

/*
 * When given a query, this application estimates the relevance scores of all the documents in the collection using okapi method(BM25), 
 * and rank them in descending order.
 * @param terms: the term set of query
 * @param numHits: number of returned documents
 */
private ScoreDoc[] okapiScore(Vector<TermQuery> terms, int numHits) throws Exception 
{			    
    // estimate the relevance score
    double[] score = new double[reader.numDocs()];	

	for(int j=0;j<terms.size();j++)
	{		
		Term t = terms.elementAt(j).getTerm();
		double idf_byT = getIDF(t) * terms.elementAt(j).getBoost();	
		TermDocs termDocs = reader.termDocs(t);
		while(termDocs.next())
		{
			int doc = termDocs.doc();
			int freq = termDocs.freq();
			double s = idf_byT * getTF(freq, docLength[doc], avLength);
			score[doc] += s;
		}		
	}
	
	// sort according to the score
	hitsRankQueue docQueue = new hitsRankQueue(reader.numDocs());
	for(int j=0;j<reader.numDocs();j++)
	{
		float logScore =  (float) Math.log(score[j]);
		docQueue.insertWithOverflow(new ScoreDoc(j, logScore));
		
	}
	
	// return the top documents
	int returnHit = Math.min(numHits, docQueue.size());
	ScoreDoc[] result = new ScoreDoc[returnHit];
	int num = returnHit;
	for(int j=0;j<returnHit;j++)
	{
		result[j] = docQueue.pop();
		if(result[j].score < 0.00000000001)
		{
			num = j;
			break;
		}
	}
	docQueue.clear();

	ScoreDoc[] resultQ = new ScoreDoc[num];
	for(int i=0;i<num;i++)
	{
		resultQ[i] = result[i];
	}
	
	return resultQ;	
}

/*
 * Get the idf for a term in the whold collection. This function is used to estimate the okapi score, 
 * so all the definitions are based on okapi(BM25) algorithm. The user can make modification freely to adapt to other algorithms.
 * @param term: the term waiting for IDF
 */
private double getIDF(Term t) throws Exception 
{  
    int nq = reader.docFreq(t);  
    int numDocs = reader.numDocs();
    
    return (double) Math.log(((double) (numDocs - nq + 0.5))  
            / ((double) (nq + 0.5)) + 1.0);  
}

/*
 * Get the idf for a term in the defined collection. This function is used to estimate the okapi score, 
 * so all the definitions are based on okapi(BM25) algorithm. The user can make modification freely to adapt to other algorithms.
 * @param term: the term waiting for IDF
 * @param _reader: the index reader, which contains the index information, but this index could not be the default one in this application. 
 * It could be generated by another library rather than the default one coming from the whole collection
 */
public double getIDF(IndexReader _reader, Term t) throws Exception 
{  
    int nq = _reader.docFreq(t);  
    int numDocs = _reader.numDocs();
    
    return (double) Math.log(((double) (numDocs - nq + 0.5))  
            / ((double) (nq + 0.5)) + 1.0);  
}

/*
 * Get the term frequency for a term in the whold collection. This function is used to estimate the okapi score, 
 * so all the definitions are based on okapi(BM25) algorithm. The user can make modification freely to adapt to other algorithms.
 * @param tfind: the times a term appears in a document
 * @param doclength: the length of the processing document
 * @param avLength: the average length of document in the collection
 */
public double getTF(int tfind, int doclength, double avLength) throws Exception
{  
    double k = 2.0;  
    double b = 0.75;  
    double result = 0; 
    
    result = ((double) (tfind * (k + 1)))  
            / ((double) (tfind + k * ((1 - b) + b * doclength / avLength)));  
    return result;  
}

/*
 * Get the average length of document in the collection, as well as the length of each document.
 */
private void getAverageDocLength( String fieldName ) throws Exception
{ 
	int numDocs = reader.numDocs();
	double L = 0;
	docLength = new int[ numDocs ];
	    	    
	for (int i = 0; i < numDocs; i++) 
	{
		TermFreqVector vector = reader.getTermFreqVector(i, fieldName);
		if (vector == null )
		{
			docLength[i] = 0;
			continue;
		}
		int[] frequencies = vector.getTermFrequencies();
		docLength[i] = 0;
		for(int j : frequencies) 
		{
			docLength[i] += j;			
		}
		L += docLength[i];
	}
	    	
	try
	{
		avLength = L/numDocs;
	}catch (Exception e) {
		System.out.println("No Documents existing in the indexer! ");		
	}	
}

/*
 * Get preparation for RW. 
 * The RW method needs to use the feature matrix of the whole collection for several times. Instantly reading the feature matrix from the index 
 * is an possible way, but it is very time consuming, and the storing of feature matrix is much more efficient. 
 * 
 * @param outputDir: the output directory. The application will create a new directory called "Temps" under the output directory to store the temporary information.
 * @param queryList: the query list
 * @output: the path of processed query list. The query is represented by a sparse vector.
 */
public String RWgetPrepared( String outputDir, String queryList ) throws Exception
{
	File f = new File( StrExpand );
	if (!f.exists()){
      f.mkdir();
    }
	
	
	// get all the features in descending order of the frequency
	TextFileIndexer indexer = new TextFileIndexer( dir.getDirectory().getCanonicalPath() );
	TermStats[] termList = indexer.getHighFreqTerms(FieldsName.elementAt(0), 10000);
	numterms = termList.length;
	indexer.close();
	
	// rank all the feature in order	
	Map<String,Integer> termIdMap = new HashMap<String,Integer>();
	for (int i = 0; i < termList.length; i++) 
	{
		String term = termList[i].term.text();
		if (termIdMap.containsKey(term)) 
		{
			continue;
		}
		termIdMap.put(term, i);
	}
	
	// Turn queries to vectors
	String newQuery = StrExpand+"queries.txt";
	BufferedReader queryFile =  new BufferedReader(new FileReader( queryList ));
	PrintWriter queries = new PrintWriter(new FileWriter( newQuery ));
   
    String querystr = queryFile.readLine();
    int ql = 0;
    while(querystr != null)
    {
    	String[] queryArray = querystr.split(",");
	    String querytext = querystr.substring(queryArray[0].length()+1);
    	QueryTermVector docTerms = new QueryTermVector( querytext, analyzer );
    	String[] termsTxt = docTerms.getTerms();
        int[] termFrequencies = docTerms.getTermFrequencies();       
        int queryLength = 0;
        for(int i=0;i<docTerms.size();i++){    
        	if( termIdMap.containsKey(termsTxt[i])){
        		queryLength = queryLength + termFrequencies[i];
        	}
        }
        
        ql = ql+1;
        queries.print( ql+",");
        for(int i=0;i<docTerms.size();i++)
        {    
        	if( termIdMap.containsKey(termsTxt[i]))
        	{
        		double val = getValueTermDoc(termFrequencies[i], queryLength, termsTxt[i]);
        		queries.print(termIdMap.get(termsTxt[i])+":"+val+" ");
        	}
        }
        queries.println();
        querystr = queryFile.readLine();
    }
    queries.close();
    queryFile.close();
	
	// storing the documents in the format of sparse matrix. It's a same format as the libSVM matrix representation.	
	String ATstr = StrExpand+"AT.txt";
	f = new File( ATstr );
	if (!f.exists()){					
		// create document A
		getAverageDocLength( FieldsName.elementAt(0) );
		PrintWriter outputFile = new PrintWriter(new FileWriter(ATstr));
		for(int i=0;i<reader.numDocs(); i++)
		{
			TermFreqVector vector = reader.getTermFreqVector(i, FieldsName.elementAt(0));
			if (vector==null )
			{
				outputFile.println();
			    continue;
			}
		   
			String[] terms = vector.getTerms();
			int[] frequencies = vector.getTermFrequencies();		      
			for (int j = 0; j < terms.length; j++) 
			{
			    if( termIdMap.containsKey(terms[j]))
			    {
			    	double val = getValueTermDoc(frequencies[j], docLength[i], terms[j]);
			    	outputFile.print(termIdMap.get(terms[j])+":"+val+" ");
			    }
			}
			outputFile.println();
		}
		outputFile.close();		
	}
	termIdMap.clear();
	
	// The matrix of the transpose of A
	String Astr = StrExpand+"A.txt";
	f = new File( Astr );
	if (!f.exists()){
		if(docLength[0]==0 && docLength[reader.numDocs()] == 0){
			getAverageDocLength( FieldsName.elementAt(0) );
		}
		PrintWriter ATfile = new PrintWriter(new FileWriter(Astr));
		for(int i =0; i<termList.length; i++)
		{			
			Term t = termList[i].term;
			TermDocs td = reader.termDocs(t);
			while (td.next()) 
			{
				int docID = td.doc();
				double val = getValueTermDoc(td.freq(), docLength[docID], t.text());
				ATfile.print(docID+":"+val+" ");
			}
			ATfile.println();
		}
		ATfile.close();
	}
	
	return newQuery;
}

/*
 * The multiplication of a matrix(transpose of feature) with a vector(transpose). Both of them could be sparse or dense. The vector 
 * in this function is a string which transferred from the vector's sparse representation.
 * 
 * @param ATfile: the path of the file representing the sparse matrix
 * @param V: the vector in string format
 * @param Vlength: the length of vector, and the number of the matrix's columns. When they are sparse, the length is necessary.
 * @param sparse: indicate the sparsity of both the matrix and the vector. It's meanings are shown in the function.
 */
public DoubleMatrix1D ATbyVt(String ATfile, String V, int Vlength, int sparse) throws IOException
{	
	//sparse: 0 - ATfile sparse, V sparse
	//sparse: 1 - ATfile sparse, V dense
	//sparse: 2 - ATfile dense, V sparse
	//sparse: 3 - ATfile dense, V dense
	DoubleMatrix1D result = new SparseDoubleMatrix1D( reader.numDocs() );
	
	// read the vector
	DoubleMatrix1D Vt = new SparseDoubleMatrix1D( Vlength ); // t*1
	String[] elements = V.split(" ");
	if(sparse == 1 | sparse == 3)
	{
		for(int j=0; j<elements.length; j++)
		{
			double value = Double.valueOf(elements[j]);
			Vt.setQuick(j, value);
		}
	}else{
		for(int j=0; j<elements.length; j++)
		{
			String[] values = elements[j].split(":");
			int pos = Integer.valueOf( values[0] );
			double value = Double.valueOf(values[1]);
			Vt.setQuick(pos, value);
		}
	}
	
	// read the matrix
	BufferedReader ATmat =  new BufferedReader(new FileReader( ATfile )); // d*t
	// dby1 = A^T * v(1*t)
	for (int i = 0; i < reader.numDocs(); i++) 
	{
		String ATrow = ATmat.readLine();
		
		if(ATrow.length()==0)
		{
			continue;
		}
		String[] eles = ATrow.split(" ");
		DoubleMatrix1D tby1 = new SparseDoubleMatrix1D( Vlength );
		if(sparse == 2 | sparse == 3)
		{
			for(int j=0; j<eles.length; j++)
			{
				double value = Double.valueOf(eles[j]);
				tby1.setQuick(j, value);
			}
		}else{
			for(int j=0; j<eles.length; j++)
			{
				String[] values = eles[j].split(":");
				int pos = Integer.valueOf( values[0] );				
				double value = Double.valueOf(values[1]);
				tby1.setQuick(pos, value);
			}
		}
	    
		// multiplication: dot product
		double x = Vt.zDotProduct(tby1);
		if(x!=0)
		{
			result.setQuick(i, x);
		}
	}
	
	ATmat.close();

		
	return result;
}

/*
 * The multiplication of a matrix(transpose of feature) with a vector(transpose). Both of them could be sparse or dense. The vector 
 * in this function is a real vector. This function is usually called in the middle of the calculation.
 * 
 * @param ATfile: the path of the file representing the sparse matrix
 * @param Vt: the vector
 */
public DoubleMatrix1D ATbyVt(String ATfile, DoubleMatrix1D Vt) throws Exception
{	
	//sparse: 0 - ATfile sparse, V sparse
	//sparse: 1 - ATfile sparse, V dense
	//sparse: 2 - ATfile dense, V sparse
	//sparse: 3 - ATfile dense, V dense
	DoubleMatrix1D result = new SparseDoubleMatrix1D( reader.numDocs() );
	
	// read the matrix from the file
	BufferedReader ATmat =  new BufferedReader(new FileReader( ATfile )); // d*t
	// dby1 = A^T * v(1*t)
	for (int i = 0; i < reader.numDocs(); i++) 
	{
		String ATrow = ATmat.readLine();
		if(ATrow.length()==0)
		{
			continue;
		}
		String[] elements = ATrow.split(" ");
		DoubleMatrix1D tby1 = new SparseDoubleMatrix1D( numterms );

		for(int j=0; j<elements.length; j++)
		{
			String[] values = elements[j].split(":");
			int pos = Integer.valueOf( values[0] );
			double value = Double.valueOf(values[1]);
			tby1.setQuick(pos, value);
		}
	    
		result.setQuick(i, Vt.zDotProduct(tby1));
	}
	ATmat.close();
		
	return result;
}

/*
 * The multiplication of a matrix with a vector. Both of them could be sparse or dense. The vector 
 * in this function is a real vector. This function is usually called in the middle of the calculation.
 * 
 * @param Afile: the path of the file representing the sparse matrix
 * @param Vd: the vector
 */
public DoubleMatrix1D AbyVd(String Afile, DoubleMatrix1D Vd) throws Exception
{		
	DoubleMatrix1D result = new SparseDoubleMatrix1D( numterms );
	BufferedReader ATmat =  new BufferedReader(new FileReader( Afile )); // d*t
	// dby1 = A^T * v(1*t)
	for (int i = 0; i < numterms; i++) 
	{
		String ATrow = ATmat.readLine();
		String[] elements = ATrow.split(" ");
		DoubleMatrix1D dby1 = new SparseDoubleMatrix1D( reader.numDocs() );

		for(int j=0; j<elements.length; j++)
		{
			String[] values = elements[j].split(":");
			int pos = Integer.valueOf( values[0] );
			double value = Double.valueOf(values[1]);
			dby1.setQuick(pos, value);
		}

		result.setQuick(i, Vd.zDotProduct(dby1));
	}
	
	ATmat.close();
		
	return result;
}

/*
 * The multiplication of a matrix with a vector. Both of them could be sparse or dense. The vector 
 * in this function is a string which transferred from the vector's sparse representation.
 * 
 * @param Afile: the path of the file representing the sparse matrix
 * @param V: the vector in string format
 * @param Vlength: the length of vector, and the number of the matrix's columns. When they are sparse, the length is necessary.
 * @param sparse: indicate the sparsity of both the matrix and the vector. It's meanings are shown in the function.
 */
public DoubleMatrix1D AbyVd(String Afile, String V, int Vlength, int sparse) throws Exception
{	
	//sparse: 0 - ATfile sparse, V sparse
	//sparse: 1 - ATfile sparse, V dense
	//sparse: 2 - ATfile dense, V sparse
	//sparse: 3 - ATfile dense, V dense
	DoubleMatrix1D result = new SparseDoubleMatrix1D( numterms );
	
	DoubleMatrix1D Vt = new SparseDoubleMatrix1D( Vlength ); // t*1
	String[] elements = V.split(" ");
	if(sparse == 1 | sparse == 3)
	{
		for(int j=0; j<elements.length; j++)
		{
			double value = Double.valueOf(elements[j]);
			Vt.setQuick(j, value);
		}
	}else{
		for(int j=0; j<elements.length; j++)
		{
			String[] values = elements[j].split(":");
			int pos = Integer.valueOf( values[0] );
			double value = Double.valueOf(values[1]);
			Vt.setQuick(pos, value);
		}
	}
	
	BufferedReader ATmat =  new BufferedReader(new FileReader( Afile )); // d*t
	// dby1 = A^T * v(1*t)
	for (int i = 0; i < numterms; i++) 
	{
		String ATrow = ATmat.readLine();
		elements = ATrow.split(" ");
		DoubleMatrix1D tby1 = new SparseDoubleMatrix1D( Vlength );
		if(sparse == 2 | sparse == 3)
		{
			for(int j=0; j<elements.length; j++)
			{
				double value = Double.valueOf(elements[j]);
				tby1.setQuick(j, value);
			}
		}else{
			for(int j=0; j<elements.length; j++)
			{
				String[] values = elements[j].split(":");
				int pos = Integer.valueOf( values[0] );
				double value = Double.valueOf(values[1]);
				tby1.setQuick(pos, value);
			}
		}
	    
		result.setQuick(i, Vt.zDotProduct(tby1));
	}
	ATmat.close();	
	
	return result;
}


/*
 * Get the value of the feature matrix: the value for each term each document
 * @param tfind: the times the term appears in a document
 * @param doclenght: the length of the document
 * @param termtxt: the text of the term
 */
public double getValueTermDoc(int tfind, int doclength, String termtxt) throws Exception
{  
	double tf = (double)tfind / (double)doclength;
	
	Term t = new Term( FieldsName.elementAt(0), termtxt );
	int nq = reader.docFreq(t);
	double idf = Math.log(reader.numDocs()/ nq);
	
    return tf*idf;  
}

/*
 * Let the application wait for some seconds. This is mainly used in the Google search because Google prohibit frequently sending search request.
 * @param d: number of seconds
 */
public static void waitSec (double d)
{
    long t0, t1;
    t0 =  System.currentTimeMillis();
    do{
        t1 = System.currentTimeMillis();
    }while ((t1 - t0) < (d * 1000));
}

/*
 * Transfer the documents returned by Google engine into standard format: removing special symbols, changing bold words into normal one, 
 * removing document's creating data and time, etc ...
 */
public static String regulateData (String str)
{
	String ss = str.replace("<b>", "");
	ss = ss.replace("</b>", "");
	ss = ss.replace("&#39;", "");
	ss = ss.replace("&quot;", "");
	ss = ss.replace("“", "");
	ss = ss.replace("”", "");
	ss = ss.replace("&lt;", "<");
	ss = ss.replace("&gt;", ">");
	ss = ss.replace("&amp;", "&");
	ss = ss.replace("?", "");
	
	if(ss.startsWith("Jan ") || ss.startsWith("Feb ") || ss.startsWith("Mar ") || ss.startsWith("Apr ") ||
			ss.startsWith("May ") || ss.startsWith("Jun ") || ss.startsWith("Jul ") || ss.startsWith("Aug ") || 
			ss.startsWith("Sep ") || ss.startsWith("Oct ") || ss.startsWith("Nov ") || ss.startsWith("Dec ")  )
	{
		String[] array = ss.split(" ");
		String[] date = array[1].split(",");
		boolean month = array[0].equals("Jan") || array[0].equals("Feb") || array[0].equals("Mar") || array[0].equals("Apr") ||
				array[0].equals("May") || array[0].equals("Jun") || array[0].equals("Jul") || array[0].equals("Aug") || 
				array[0].equals("Sep") || array[0].equals("Oct") || array[0].equals("Nov") || array[0].equals("Dec");
		if(Integer.valueOf(date[0])<32 && month)
		{
			if(array[3].equals("...") )
			{
				ss = ss.substring((array[0]+array[1]+array[2]+array[3]).length()+4);
			}
			
			if(array[2].endsWith("..."))
			{
				ss = ss.substring((array[0]+array[1]+array[2]).length()+3);
			}
							
		}
		
		
	}
	
	String[] array = ss.split(" ");
	if(array[2].startsWith("ago"))
	{
		if(array[2].endsWith("...") || array[3].equals("..."))
		{
			boolean unit = array[1].startsWith("day") ||  array[1].startsWith("hour") || array[1].startsWith("minute");
			if(Integer.valueOf(array[0])<31 && unit)
			{
				String[] array1 = ss.split("ago");
				ss = ss.substring(array1[0].length()+7);
			}
		}
	}
	
	return ss;
	
}
 
}


final class hitsRankQueue extends PriorityQueue<ScoreDoc> // descending order queue
{
	hitsRankQueue(int size) 
  {
    initialize(size);
  }
  
  protected boolean lessThan(ScoreDoc hitA,ScoreDoc hitB) 
  {
    return hitA.score > hitB.score;
  }
}
