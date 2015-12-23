package br.ufpe.cin.nlp.sentence.base;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class SentenceIndexIterator implements Iterator<String> {
	private TopDocs docs;
	private int n = 0;
	private SentencesLuceneIndex index;
	
	public SentenceIndexIterator(TopDocs docs, SentencesLuceneIndex index) {
		this.docs = docs;
		this.index = index;
	}

	@Override
	public boolean hasNext() {
		return this.n < this.docs.totalHits;
	}

	@Override
	public String next() {
		String ret = null;
		if (!this.hasNext()) {
			throw new NoSuchElementException("No more sentences");
		}
		final ScoreDoc result = this.docs.scoreDocs[this.n];
		final int docNum = result.doc;
		try {
			ret = this.index.getSentence(docNum);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot get document with docID " + docNum, e);
		}
		this.n++;
		return ret;
	}

}
