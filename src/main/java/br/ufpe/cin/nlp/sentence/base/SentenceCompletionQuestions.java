package br.ufpe.cin.nlp.sentence.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;

public class SentenceCompletionQuestions {
	public static class Question {
		private List<String> tokensBefore;
		private List<String> tokensAfter;
		private List<String> options;
		private int correctIndex = -1;
		private char correctLetter;
		private String correctWord;
		
		private Question(int capacity) {
			this.tokensBefore = new ArrayList<String>(capacity);
			this.tokensAfter = new ArrayList<String>(capacity);
			this.options = new ArrayList<String>(5);
		}
		
		public Question(List<String> tokensBefore, List<String> tokensAfter, List<String> options, int correctIndex) {
			this.tokensBefore = tokensBefore;
			this.tokensAfter = tokensAfter;
			this.options = options;
			this.correctIndex = correctIndex;
			this.correctLetter = (char)('a' + correctIndex);
			this.correctWord = options.get(correctIndex);
		}

		public List<String> getTokensBefore() {
			return tokensBefore;
		}

		public List<String> getTokensAfter() {
			return tokensAfter;
		}

		public List<String> getOptions() {
			return options;
		}

		public int getCorrectIndex() {
			return correctIndex;
		}

		public char getCorrectLetter() {
			return correctLetter;
		}

		public String getCorrectWord() {
			return correctWord;
		}
	}
	
	private List<Question> questions;
	
	public SentenceCompletionQuestions() {
		readQuestions();
		readAnswers();
		
	}

	private void readAnswers() {
		ClassPathResource resource = new ClassPathResource("/answers.txt");
		InputStream fStream;
		try {
			fStream = resource.getInputStream();
			final BufferedReader bufFile = new BufferedReader(new InputStreamReader(fStream, "UTF-8"));
			String line;
			for (int i = 0; (line = bufFile.readLine()) != null && line.trim().length() > 0; i++) {
				final String[] tokens = line.split(" ");
				assert question(tokens[0]) == (i+1);
				final Question q = questions.get(i);
				q.correctLetter = letter(tokens[0]);
				assert q.correctLetter >= 'a' && q.correctLetter <= 'e';
				q.correctIndex = q.correctLetter - 'a';
				boolean found = false;
				for (int j = 1; j < tokens.length && !found; j++) {
					final String tokenAnswer = tokens[j];
					if (tokens[j].charAt(0) == '[') {
						q.correctWord = tokenAnswer.substring(1, tokenAnswer.length()-1).toLowerCase(Locale.US);
						found = true;
						assert q.options.contains(q.correctWord);
					}
				}
				assert found;
			}
			bufFile.close();
		} catch (IOException e) {
			throw new IllegalStateException("Error processing question file in classpath (answers.txt)", e);		
		}
		
	}

	private char letter(String string) {
		final char ret = string.charAt(string.indexOf(')') - 1);
		return ret;
	}

	private void readQuestions() {
		ClassPathResource resource = new ClassPathResource("/questions.txt");
		InputStream fStream;
		this.questions = new ArrayList<Question>(1040);
		try {
			fStream = resource.getInputStream();
			final BufferedReader bufFile = new BufferedReader(new InputStreamReader(fStream, "UTF-8"));
			String line;
			int lastQuestion = 0;
			Question q = null;
			while((line = bufFile.readLine()) != null) {
				final String[] tokens = line.split(" ");
				final int currQuestion = question(tokens[0]);
				if (currQuestion != lastQuestion) {
					q = new Question(tokens.length);
					this.questions.add(q);
				}
				boolean firstPart = true;
				for (int i = 1; i < tokens.length; i++) {
					if (tokens[i].charAt(0) != '[' && (currQuestion != lastQuestion)) {
						final String word = tokens[i].toLowerCase(Locale.US);
						if (firstPart) q.tokensBefore.add(word);
						else q.tokensAfter.add(word);
					} else if (tokens[i].charAt(0) == '[') {
						q.options.add(tokens[i].substring(1, tokens[i].length()-1).toLowerCase(Locale.US));
						firstPart = false;
						if (lastQuestion == currQuestion) break;
					}
				}
				lastQuestion = currQuestion;
			}
			bufFile.close();
		} catch (IOException e) {
			throw new IllegalStateException("Error processing question file in classpath (questions.txt)", e);		
		}
	}

	private int question(String string) {
		StringBuffer stb = new StringBuffer(5);
		char c;
		for (int i = 0; i < string.length() && Character.isDigit((c = string.charAt(i))); i++) {
			stb.append(c);
		}
		return Integer.parseInt(stb.toString());
	}

	public List<Question> getQuestions() {
		return questions;
	}
	
	public static void main(String[] args) {
		SentenceCompletionQuestions questions = new SentenceCompletionQuestions();
		for (int i = 0; i < questions.getQuestions().size(); i++) {
			Question q = questions.getQuestions().get(i);
			System.out.println("QUESTION " + (i+1));
			System.out.println("Tokens before: " + q.getTokensBefore().toString());
			System.out.println("Tokens after: " + q.getTokensAfter().toString());			
			System.out.println("Options: " + q.getOptions().toString());
			System.out.println("Answer: " + (q.getCorrectIndex() + 1) + q.getCorrectLetter() + ") " + q.getCorrectWord());
			System.out.println("");
		}
	}
}
