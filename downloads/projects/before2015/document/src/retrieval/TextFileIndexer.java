package retrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.Version;


/**
 * This application creates index using Lucene, and stores the index in the assigned directory
 * @param index directory: folder to store the index
 * @param docs directory: the directory contains all the documents waiting for index, which could be a directory 
 * 							containing all the documents, and also a .csv file indicating the documents' paths.
 * 							The columns of .csv document are: query number, relevant sign, document name, document path.
 * 
 * @param FieldsName: the defaults are: contents, name, order, resp. Contents: the vectors of a document; name: the name of a document; 
 * 										order: the indexing order of a document; resp: relevant(1) and irrelevant(0) sign, including not assessed(-1);
 * 
 * @author Zheyun Feng - fengzhey@msu.edu
 */

public class TextFileIndexer 
{
  public FSDirectory dir;
  public TrecAnalyzer analyzer; 
  public Vector<String> FieldsName;
 
  private ArrayList<File> queue = new ArrayList<File>();

  public static void main(String[] args) throws Exception 
  {
	System.out.println("Setting rightnow (r) or using default setting(d)? ");
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	String setting = br.readLine();
	String index_dir = null, docs_dir = null, stopfile = null;
	
	if(setting.equals("b")){
		index_dir = "C:/zfeng/TrecJava/Index/Trec";
//		String docs_dir = "C:/zfeng/TrecJava/data/484/Col";
		docs_dir = "C:/zfeng/TrecJava/test/seed_dir1.csv";
		stopfile = "C:/zfeng/TrecJava/souce/stopfile.txt";
	}else{
		 //**********************************************************************
		 //                   Index directory
	    System.out.println("Enter the path where the index will be created: ");
	    br = new BufferedReader(new InputStreamReader(System.in));
	    index_dir = br.readLine();
		
		//**********************************************************************
		//            Document directory or document path file
	    System.out.println("Enter the path of the standard seed file or the root directory of documents: ");
	    br = new BufferedReader(new InputStreamReader(System.in));
	    docs_dir = br.readLine();
	 
		//**********************************************************************
		//         		Customer defined stop word list
		System.out.println("Would you like to use another stop word list? ");
	    br = new BufferedReader(new InputStreamReader(System.in));
	    if(br.readLine().startsWith("y"))
	    {
	    	System.out.println("Please enter the path of your stop word list:");
	        br = new BufferedReader(new InputStreamReader(System.in));
	        stopfile = br.readLine();
	    } 
	}
	// Fields name, not necessary to change.
	String content_name = "contents";
	String doc_name = "name";

    TextFileIndexer indexer = null;    
    try 
    {
    	if(stopfile==null){
    		indexer = new TextFileIndexer( index_dir ); // create the index application
    	}else{
    		indexer = new TextFileIndexer( index_dir, stopfile ); // create the index application
    	}
    } catch (Exception ex) 
    {
    	System.out.println("Cannot create index..." + ex.getMessage());
    	System.exit(-1);
    }
    
    // list of fields' names
    // If a field name is needed to expand, add the name here first.
    indexer.FieldsName.add( content_name );
    indexer.FieldsName.add( doc_name );   
    indexer.FieldsName.add( "order" );
    indexer.FieldsName.add( "resp");
    
    // solution: when index already exists in the assigned directory.
    // Press "y" or "yes" ==> re-index; otherwise, use the old index
    if (IndexReader.indexExists(indexer.dir))
	{
      System.out.println("Index exists. Do you want to index again?\n ");
      br = new BufferedReader(new InputStreamReader(System.in));
      if(br.readLine().startsWith("y"))
      {
    	  indexer.buildIndexer( docs_dir );   
      }   	
    }else
    {
    	indexer.buildIndexer( docs_dir );    	   	
    }
    
    
    indexer.close();
    System.out.println("Index Finished. ");
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
  }

  /*
   * Constructor: index application
   * @param index_dir: the name of the folder in which the index would be created
   * @throws java.io.IOException
   */
public TextFileIndexer(String index_dir ) throws IOException 
  { 
	this.dir = FSDirectory.open(new File(index_dir));
	this.analyzer = new TrecAnalyzer(  ); // this analyzer can also be changed to others.
	this.FieldsName = new Vector<String>();	
  }

/*
 * Constructor: index application
 * @param index_dir: the name of the folder in which the index would be created
 * @param stopfile: the path of new stop word list file; if use the default stop word, let it be null.
 * 
 * The stopfile is a .txt file, with a stop word in each line. If a stop word is necessary to remove, 
 * just add "#" ahead of the line and comment it.
 * 
 * @throws java.io.IOException
 */
public TextFileIndexer(String index_dir, String stopfile ) throws IOException 
{ 
	this.dir = FSDirectory.open(new File(index_dir));
	this.analyzer = new TrecAnalyzer( stopfile  ); 
	this.FieldsName = new Vector<String>();	
}

/*
 * Close all the defined registers
 */
public void close() throws IOException 
{ 
	dir.close();
	analyzer.close();
	queue.clear();
	FieldsName.clear();
}


/*
 * Create a queue(arraylist), and then push the full paths of all the files under the input directory 
 * into the queue. This function is used when an action is taken to all the files in a directory.
 */
private void addFiles(File file) 
  {

    if (!file.exists()) //check if the input directory exists or not
    {
      System.out.println(file + " does not exist.");
    }
    if (file.isDirectory()) //check if the input path is a directory or not
    {
      for (File f : file.listFiles()) 
      {
        addFiles(f);
      }
    } else 
    {
      String filename = file.getName().toLowerCase();
      //===================================================
      // Only index text files
      //===================================================
      if (filename.endsWith(".htm") || filename.endsWith(".html") ||
              filename.endsWith(".xml") || filename.endsWith(".txt")) 
      {
        queue.add(file);
      } else 
      {
        System.out.println("Skipped " + filename);
      }
    }
  }


/*
 * Read a file and add its contents into the index. The function has full default fields: content, file name, index order, relevant sign.
 * The relevant sign is supposed to be -1 if the input documents is a directory rather than a seed document (end with .csv).
 * The relevant sign equals 1 if the document is relevant to the topic, 0 if irrelevant, and -1 in other cases.
 * 
 * If the customer need to expand the fields, he/she should first expand the definition of "FieldsName" in the main function; 
 * and then add the correspondent content to the document's index.
 */
private void addDocToIndexer(IndexWriter writer, String filePath, String fileName, String resp_sgn, int order) throws IOException
  {
	  FileReader fr = null;
	  try 
	  {
	  	Document doc = new Document();

	    //===================================================
	    // add contents of a file
	    //===================================================
	    fr = new FileReader( filePath );
	    doc.add(new Field(FieldsName.elementAt(0), fr, Field.TermVector.YES ));
	    doc.add(new Field(FieldsName.elementAt(1), fileName, Field.Store.YES,Field.Index.NO)); // for returning the retrieval results, but not for index nor analyzed
	    doc.add(new Field(FieldsName.elementAt(2), Integer.toString(order), Field.Store.YES,Field.Index.NO));
	    doc.add(new Field(FieldsName.elementAt(3), resp_sgn, Field.Store.YES,Field.Index.NO));
	    
	    // different document can be set with different weights (boost)
//		        doc.setBoost(0.1F);
	    writer.addDocument(doc);
	    
	    System.out.println("Added: " + fileName);
	        
	  } catch (Exception e) {
		System.out.println("Could not add: " + fileName);
	  } finally {
	    fr.close();
	  }
    
  }
  

/*
 * The seed document should in such format that each line represents a document, and the columns represent respectively: 
 * topic number, relevant sign, doc name, doc path
 * 
 * If there's no seed file or the judgment file is not in standard format, this function can be used to regulate it. If the judgment is different 
 * as the listed ones, the customer need to write a regulate function to create a standard seed file.
 * 
 * But actually, even if the seed file is not created, the index procedure can still undergo.
 */
public void CreateSeedDic( String seedDir, String dataType ) throws IOException
  // Find out the paths of the seed documents
  {
	  String s, datafile, separate_dot;
	  if( dataType.equals( "Trec" ))
	  {
		  s = "C:/zfeng/TrecJava/data/2010/seed.csv"; // trec
		  datafile = "C:/zfeng/TrecJava/data/2010/edrmv2txt-v2"; // trec
		  separate_dot = ",";
	  }else if( dataType.equals( "484" ))
	  {
		  s = "C:/zfeng/TrecJava/data/484/query/qrels.txt"; // MIR from course 484
		  datafile = "C:/zfeng/TrecJava/data/484/Col"; // 484
		  separate_dot = " ";
	  }else
	  {	  
		  BufferedReader br = new BufferedReader(
	            new InputStreamReader(System.in));
		  System.out.println("Enter the path and name of the seed document: ");
		  s = br.readLine();
  
		  System.out.println("Enter the path of the data document: ");
		  datafile = br.readLine();
		  
		  System.out.println("Enter the symbole of separation ");
		  separate_dot = br.readLine();
	  }

	  
	  addFiles(new File(datafile));	

	  BufferedReader CSVFile =  new BufferedReader(new FileReader(s));
	  String dataRow = CSVFile.readLine(); // Read first line.
	  
	  if( dataType.equals( "Trec" ))
	  {
		  FileWriter newCSV = new FileWriter( seedDir );
		  while (dataRow != null)
		  {
			  String[] dataArray = dataRow.split(separate_dot); 
	   
			  // if( Double.valueOf(dataArray[2]) >= 0 ) // if the not assessed documents need to be removed, uncomment this line.
			   {
				   System.out.println(dataArray[3]);
				   for (File f : queue) 
				    {
					    String path = f.getName();		    
				        if( path.equals(dataArray[3]+".txt")  )
				        {
				        	newCSV.append( dataArray[0] + "," );
				        	newCSV.append( dataArray[2] + ",");
				        	newCSV.append( dataArray[3] + ",");
				        	newCSV.append( f.getAbsolutePath());
				        	newCSV.append( "\n" );
				        }
				    }
			   }
			   dataRow = CSVFile.readLine(); // Read next line of data.
		  }
		  newCSV.close();
	  }else if( dataType.equals( "484" ))
	  {		  
		  String numDoc = "1";
		  FileWriter newCSV = new FileWriter( seedDir+"/seed_dir"+numDoc+".csv" );
		  
		  while (dataRow != null)
		  {
			  String[] dataArray = dataRow.split(separate_dot);
			  if(!dataArray[0].equals(numDoc))
			  {
				  newCSV.close();
				  numDoc = dataArray[0];
				  newCSV = new FileWriter( seedDir+"/seed_dir"+numDoc+".csv" );				  
			  }
			  
			   for (File f : queue) 
			    {
				    String path = f.getName();		    
			        if( path.equals(dataArray[2]+".txt")  )
			        {
			        	newCSV.append( dataArray[0] + "," );
			        	newCSV.append( dataArray[3] + ",");
			        	newCSV.append( dataArray[2] + ",");
			        	newCSV.append( f.getAbsolutePath());
			        	newCSV.append( "\n" );
			        }
			    }
			   dataRow = CSVFile.readLine(); // Read next line of data.
		  }
		  newCSV.close();
	  }
	  // Close the file once all data has been read.
	  CSVFile.close();
	  queue.clear();	  
	  System.out.println("The seed file has been created from the documents in directory: " + datafile);
  } // CSVRead


/*
 * Build the index for the collection, whose directory or document path list is provided. 
 * @datalist if datalist is a ".csv" file, build the indexer using the files listed in the .csv document
 *           otherwise, if datalist is a directory, build the indexer using all the documents in that directory
 */
@SuppressWarnings("deprecation")
public void buildIndexer(String datalist) throws IOException 
  {
	
	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);// define the index filter
	IndexWriter writer = new IndexWriter(dir, config);
    
	writer.deleteAll();// Initialization: delete all the old indexing    
	writer.optimize();// optimize the current index, otherwise, the number of document, order of doc might be wrong.
	writer.commit();
		
	if(datalist.endsWith(".csv"))// if given a list of docs, index the documents listed.
	{
		BufferedReader CSVFile =  new BufferedReader(new FileReader(datalist));
	
		int order = 0;
		String dataRow = CSVFile.readLine(); // Read first line.
		while (dataRow != null)
		{
			String[] dataArray = dataRow.split(",");
			addDocToIndexer(writer, dataArray[3], dataArray[2], dataArray[1], order);	      
			dataRow = CSVFile.readLine(); // Read next line of data.
			order++;
		}			
		CSVFile.close();
	}
	else
	{
		File fileList = new File(datalist);
		if( fileList.isDirectory() )
		{
			int order = 0;
			queue.clear();
			addFiles(fileList);	
			for (File f : queue) 
			{
				String[] filename = f.getName().split(".txt");
				addDocToIndexer(writer, f.getAbsolutePath(),filename[0], "-1", order );	 
				order++;
			}
			queue.clear();				
		}else
		{
			System.out.println("List input for index is wrong.\n");
				
		}
	}
					
	writer.optimize();
	writer.commit();
	writer.close();
	
  }


/*
 * Get the top terms in descending order of frequency
 * @param field: the field where the top terms are required to extract.
 * @param numTerm: the number of terms required to extract.
 */
public TermStats[] getHighFreqTerms(String field, int numTerm) throws Exception 
{
	IndexReader reader = IndexReader.open( dir );
	  
	// Create a queue, in which all the elements are ranked in descending order. Once an element is pushed in, 
	// it will be placed in the right place according to its frequency
	// So finally, the one-by-one poped out elements have already been sorted in descending order.
	TermInfoWiTFQueue tiq = new TermInfoWiTFQueue(numTerm);
	if (field != null) 
	{
		TermEnum termList = reader.terms(new Term(field));
		if (termList != null && termList.term() != null) 
		{
			do 
	        {
	          if (!termList.term().field().equals(field)) 
	          {
	            break;
	          }
	          tiq.insertWithOverflow(new TermStats(termList.term(), termList.docFreq()));
	        } while (termList.next());
	      } 
	} else 
	{
		TermEnum terms = reader.terms();
		while (terms.next()) {
	        tiq.insertWithOverflow(new TermStats(terms.term(), terms.docFreq()));
	      }
	}

	TermStats[] result = new TermStats[tiq.size()];

	int count = tiq.size() - 1;
	while (tiq.size() != 0) 
	{
		result[count] = tiq.pop();
		count--;
	} 
	  
	// print out all the terms with their frequencies in descending order
//	for (int i = 0; i < result.length; i++) 
//	{
//		System.out.printf(result[i].term.text() + ":"+ result[i].docFreq+"\n");
//	}
//	System.out.println("Number of keywords: " +  result.length +"\n");
	    
	tiq.clear();
	reader.close();
	return result;
}


/*
 * rank the terms according to the document frequency( the number of document containing a term) 
 */
public TermStats[] sortByTotalTermFreq(TermStats[] terms, int relvDoc) throws Exception 
{ //each term appears how many documents.
	IndexReader reader = IndexReader.open( dir );
	
	    long totalTF;
	    int j = 0;
	    for (int i = 0; i < terms.length; i++) 
	    {
	      totalTF = getTotalTermFreq(reader, terms[i].term);
	      if( totalTF < relvDoc )//for classification
	      {  continue;}
	      j ++;
	    }
	    
	    TermStats[] ts = new TermStats[j]; // array for sorting
	    j = 0;
	    for (int i = 0; i < terms.length; i++) 
	    {
	      totalTF = getTotalTermFreq(reader, terms[i].term);
	      if( totalTF < relvDoc )//for classification
	    	  continue;
	      
	      ts[j] = new TermStats( terms[i].term, terms[i].docFreq, totalTF);
	      j ++;
	    }
	    
	    Comparator<TermStats> c = new TotalTermFreqComparatorSortDescending();// compare the document frequency using a self-defined comparator
	    Arrays.sort(ts, c);
	    
	    reader.close();
	    return ts;
  }


/*
 * Get the number of documents(doc frequency) containing the input term
 * @param term: the input term
 */
public static long getTotalTermFreq(IndexReader reader, Term term) throws Exception 
  {
	    long totalTF = 0;
	    	    
	    TermDocs td = reader.termDocs(term);// extract all the related documents
	    while (td.next()) // estimate the total number
	    {
	      totalTF += td.freq();
	    }
	    return totalTF;
  }

  
/*
 * Once the frequencies of each term appears in both each document and the whole collection are obtained, the feature matrix can be derived.
 * In this matrix, each row represent a document. The first column represents the label of a document(or assessed topic number); 
 * and for the other columns,  the value in a column represent the term frequency in the very document.
 * This matrix is written exactly in the SVM format, please refer to the manual of library libSVM. 
 * 
 * @param: the terms concluded in the matrix.
 * @param: the judgment file path. If no judgment, the label of a document would be 0.
 * @param: the directory to stored the final feature matrix, which is a set of .csv files.
 */
public void creatSVMmatrix( TermStats[] termList, String dataList, String featureDir ) throws Exception 
  {  		  
	 // Create hash table, in order to rank the keywords in order
	 Map<String,Integer> termIdMap = new HashMap<String,Integer>();
	 for (int i = 0; i < termList.length; i++) 
	 {
		 String term = termList[i].term.text();
	     if (termIdMap.containsKey(term)) 
	     {
	    	 continue;
	     }
	     termIdMap.put(term, i+1);
	  }
	  
	 String label = null;
	  // read the judgment file
	 BufferedReader CVSFile = null;
	 File f = new File( dataList  );
	 if(f.exists())
	 {
		 CVSFile = new BufferedReader(new FileReader( dataList ));
	 }else{
		 label = "0";
	 }

	  PrintWriter features = new PrintWriter(new FileWriter(featureDir+"_features.txt"));
	  
	  IndexReader reader = IndexReader.open( dir );
	  int numDocs = reader.numDocs();
	    	    
	  for (int i = 0; i < numDocs; i++) 
	  {
		  if(f.exists())
		  {
			  String dataRow = CVSFile.readLine();
			  String[] dataArray = dataRow.split(",");
			  label = dataArray[1];
		  }
			
	      TermFreqVector vector = reader.getTermFreqVector(i, FieldsName.elementAt(0));
	      if (vector==null )
	      {
	    	  continue;
	      }
	      String[] terms = vector.getTerms();
	      int[] frequencies = vector.getTermFrequencies();	  
	      
	      List<TermSVM> svmf = new ArrayList<TermSVM>();
	      for (int j = 0; j < terms.length; j++) 
	      {
	    	  if( termIdMap.containsKey(terms[j]))
	    	  {
	    		  svmf.add(new TermSVM( terms[j], termIdMap.get(terms[j]), frequencies[j] ));
	    	  }
	      }
	      
		  Collections.sort(svmf, new rowSort());
		  
		  features.printf( label );
		  for( TermSVM e:svmf)
		  {
			  features.printf(" "+e.freqOrder+":" + e.freq );
		  }
		  features.printf("\n");
	}
	    	    
	features.close();
	termIdMap.clear();
	CVSFile.close();
	reader.close();
	  
  }

  
/*
 * Delete all the files in the index directory
 */
public void deleteFiles( ) throws IOException
  {
	  String[] files = dir.listAll();
	  if( files == null )
		  return;
	  
	  for( int i=0; i<files.length;i++)
	  {
		  File sing_file = new File( files[i] );
		  if( !sing_file.delete())
			  throw new IOException( "Cannot delete " + sing_file );
	  }	  
  }

}


final class TermStats 
{
	  public Term term;
	  public int docFreq;
	  public long totalTermFreq;
	  
	  public TermStats(Term t, int df) 
	  {
	    this.term = t;
	    this.docFreq = df;
	  }
	  
	  public TermStats(Term t, int df, long tf) 
	  {
	    this.term = t;
	    this.docFreq = df;
	    this.totalTermFreq = tf;
	  }
	}

final class TermSVM 
{
	  public String term;
	  public int freqOrder;
	  public int freq;
	  
	  public TermSVM(String t, int fo, int fq) 
	  {
	    this.term = t;
	    this.freqOrder = fo;
	    this.freq = fq;
	  }
	}


final class TermEval 
{
	  int NumHits;
	  float precision;
	  float recall;
	  float F1;
	  float map;
	  
	  public TermEval(int num, float pre, float rec, float f1, float mp) 
	  {
	    this.NumHits = num;
	    this.precision = pre;
	    this.recall = rec;
	    this.F1 = f1;
	    this.map = mp;
	  }
}

final class HitStat 
{
	  int rank;
	  String docName;
	  float relevance;
	  
	  public HitStat(int rk, String name, float relv) 
	  {
	    this.rank = rk;
	    this.docName = name;
	    this.relevance = relv;
	  }
}



/**
 * Priority queue for TermStats objects ordered by TermStats.docFreq
 **/
final class TermInfoWiTFQueue extends PriorityQueue<TermStats> 
{
  TermInfoWiTFQueue(int size) 
  {
    initialize(size);
  }
  
  @Override
  protected boolean lessThan(TermStats termInfoA,TermStats termInfoB) 
  {
    return termInfoA.docFreq < termInfoB.docFreq;
  }
}



/*
 * Comparator
 * 
 * Reverse of normal Comparator. i.e. returns 1 if a.totalTermFreq is less than
 * b.totalTermFreq So we can sort in descending order of totalTermFreq
 */
final class TotalTermFreqComparatorSortDescending implements Comparator<TermStats> 
{
  public int compare(TermStats a, TermStats b) 
  {
    if (a.totalTermFreq < b.totalTermFreq) 
    {
      return 1;
    } else if (a.totalTermFreq > b.totalTermFreq) 
    {
      return -1;
    } else 
    {
      return 0;
    }
  }
}



final class rowSort implements Comparator<TermSVM>
{
	public int compare( TermSVM a, TermSVM b)
	{
		if (a.freqOrder > b.freqOrder )
		{
			return 1;
		}
		else if (a.freqOrder < b.freqOrder )
		{
			return -1;
		}
		else
		{
			return 0;		
		}
	}
}
