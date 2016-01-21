package br.ufpe.cin.nlp.sentence.base;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentencesLuceneIndex {
	
	private static final Logger log = LoggerFactory.getLogger(SentencesLuceneIndex.class);
    private transient  Directory dir;
    private   transient Analyzer analyzer;
    private transient IndexWriter writer;
    private transient SearcherManager manager;
    private transient QueryParser parser;
    
	public SentencesLuceneIndex(String indexPath, boolean reuse) throws IOException {
		File filePath = new File(indexPath);
        if(filePath.exists()) {
        	if (!reuse) {
	            String id = UUID.randomUUID().toString();
	            log.warn("Changing index path to" + id);
	            indexPath = id;
	            filePath = new File(indexPath);
        	}
        }
        try {
        	ensureDirExists(indexPath);			
			this.writer = createWriter();
			
			this.manager = new SearcherManager(this.writer, true, null);
		} catch (IOException e) {
			throw new IllegalStateException("Error creating writer for index at " + indexPath, e);
		}
		
	}
	
	private void ensureDirExists(String indexPath) throws IOException {
            File dir2 = new File(indexPath);
            if (!dir2.exists()) {
                log.info("Creating directory " + indexPath);
            	dir2.mkdir();
            }
            Path path = new File(indexPath).toPath();
            dir = FSDirectory.open(path);
	}
	
	private IndexWriter createWriter() throws IOException {		if (this.analyzer == null) {
			this.analyzer = new StandardAnalyzer(new InputStreamReader(new ByteArrayInputStream("".getBytes())));
		}
		if (this.parser == null) {
			this.parser = new QueryParser("sentence", this.analyzer);
		}
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setWriteLockTimeout(1000);
        
		
		IndexWriter writer = new IndexWriter(this.dir, iwc);
		return writer;
	}
	
	public void addSentence(String sentence) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("sentence", sentence, Store.YES));
		
		this.writer.addDocument(doc);
	}
	
	public void refresh() throws IOException {
		this.manager.maybeRefresh();
	}
	
	public String getSentence(int docID) throws IOException {
		IndexSearcher indexSearcher = null;
		try {
			indexSearcher = this.manager.acquire();
			Document doc = indexSearcher.doc(docID);
			String ret = doc.get("sentence");
			return ret;
		} finally {
			if (indexSearcher != null) this.manager.release(indexSearcher);
		}
	}
	
	public SentenceIndexIterator searchSentencesWithWord(String word, int maxDocs) throws IOException, ParseException {
		Query query = this.parser.parse(word);
		TopDocs tops;
		IndexSearcher indexSearcher = null;
		try {
			indexSearcher = this.manager.acquire();
			tops = indexSearcher.search(query, maxDocs);
			SentenceIndexIterator iter = new SentenceIndexIterator(tops, this);
			return iter;
		} finally {
			if (indexSearcher != null) this.manager.release(indexSearcher);
		}
	}
	
	public void commit() throws IOException {
		this.writer.commit();
	}
	
	public void close() throws IOException {
		this.writer.close();
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		SentencesLuceneIndex index = new SentencesLuceneIndex("/home/srmq/git/dl4jtests/dl4jtests/Holmes-sentence-index", true);
		index.commit();
		/*index.addSentence("the book is on the table");
		index.addSentence("THE BOOK IS ON THE TABLE");
		index.addSentence("the quick fox jumped over the lazy dog");
		index.addSentence("THE QUICK FOX JUMPED OVER THE LAZY DOG");
		index.commit();
		index.refresh();*/
		String[] searchWords = {"\"am not hateful\""};
		for (int i = 0; i < searchWords.length; i++) {
			System.out.println("Searching for " + searchWords[i]);
			SentenceIndexIterator iter = index.searchSentencesWithWord(searchWords[i], 10);
			while (iter.hasNext()) {
				System.out.print("FOUND DOCUMENT: ");
				System.out.println(iter.next());
			}
		}
		

	}
	
}
