/* File:   Parser.java
**
** Author(s): Paul Fodor
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2014 - 2016.
** All rights reserved.
**
*/

package com.coherentknowledge.nlp.stanford;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

/** Miguel Calejo changes: 
- refactored into Parser + Parse inner class, to reuse a Stanford parser loaded instance
- some local vars changed places
- testing: javaMessage('com.coherentknowledge.nlp.stanford.Parser',P,'Parser').
*/
public class Parser {
	private int count1 = 1; // counter for x vars
	private int count2 = 1; // ... y ...
	private int count3 = 1; // ... z ...
	StanfordCoreNLP pipeline;
	static final String PIPELINE_ITEMS = "tokenize, ssplit, pos, lemma, ner, parse, dcoref";
	
	public Parser(){
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.put("annotators",PIPELINE_ITEMS);
		pipeline = new StanfordCoreNLP(props);
	}
		
	class Parse {
		private String wordsPOS[][]; // sentences (in same string), words in each
		private String wordsLabel[][];
		/** Word, variable ? */
		private HashMap<String, String> map;
		/** these are all the sentences in this document
			a CoreMap is essentially a Map that uses class objects as keys and
			has values with custom types */
		List<CoreMap> sentences;
		Annotation document;
		
		Parse(String text){
			// create an empty Annotation just with the given text
			document = new Annotation(text);
			// run all Annotators on this text
			pipeline.annotate(document);
			sentences = document.get(SentencesAnnotation.class);
			// get pos tag of each sentence for each word
			wordsPOS = new String[sentences.size() + 1][100]; // Max number of words
			wordsLabel = new String[sentences.size() + 1][100];
			map = new HashMap<String, String>();
		}
		
		void processSentence(String originalText,String filepath){
			PrintStream oldOut = System.out;
			// Hacky; prints should be cleaned up elsewhere
			try {
				System.setOut(new PrintStream(filepath));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("/* " + originalText + " */");
			// assign vars
			for (int i = 0; i < sentences.size(); i++) {
				getTokenPOS(i + 1, sentences.get(i));
				System.out.println();
			}
			// get parser & dependency tree
			for (int i = 0; i < sentences.size(); i++) {
				getDependency(sentences.get(i));
				System.out.println();
			}

			// get coreference
			getCoref(document);
			System.setOut(oldOut);
		}

		private void getTokenPOS(int sentNum, CoreMap sentence) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			// System.out.println("Sentence "+sentNum+":");
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the index of the token
				int index = token.index();
				// System.out.println("  "+index+","+word+","+pos);
				wordsPOS[sentNum][index] = pos;
				wordsLabel[sentNum][index] = word.toLowerCase();
				if (pos.contains("NN") || pos.contains("VB") || pos.contains("JJ")) {
					if (map.containsKey(word.toLowerCase()) == false) {
						map.put(word.toLowerCase(), "x" + count1);
						System.out.println("nvar(" + word.toLowerCase() + ","
								+ index + "," + pos.toLowerCase() + ",x" + count1
								+ ").");
						count1++;
					} else {
						System.out.println("nvar(" + word.toLowerCase() + ","
								+ index + "," + pos.toLowerCase() + ","
								+ map.get(word.toLowerCase()) + ").");
					}
				} else {
					if (map.containsKey(word.toLowerCase()) == false) {
						map.put(word.toLowerCase(), "y" + count2);
						System.out.println("nvar(" + word.toLowerCase() + ","
								+ index + "," + pos.toLowerCase() + ",y" + count2
								+ ").");
						count2++;
					} else {
						System.out.println("nvar(" + word.toLowerCase() + ","
								+ index + "," + pos.toLowerCase() + ","
								+ map.get(word.toLowerCase()) + ").");
					}

				}
			}
		}

		private void getCoref(Annotation document) {
			/** No need to have this as instance var */
			boolean hascoref = false;
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			for (Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
				CorefChain c = (CorefChain) entry.getValue();
				CorefMention cm = c.getRepresentativeMention();
				// System.out.println(c.toString());
				List<CorefChain.CorefMention> list = c.getMentionsInTextualOrder();
				if (list.size() == 1) {
					continue;
				} else {
					for (int i = 0; i < list.size(); i++) {
						if ((list.get(i).endIndex - list.get(i).startIndex == 1)
								&& wordsPOS[list.get(i).sentNum][list.get(i).headIndex] // why
																						// only
																						// prp
																						// is
																						// used
										.contains("PRP")) {
							hascoref = true;
							System.out
									.println("coref("
											// + list.get(i).headIndex
											+ map.get(wordsLabel[list.get(i).sentNum][list
													.get(i).headIndex])
											// + "," + cm.headIndex
											+ ","
											+ map.get(wordsLabel[list.get(i).sentNum][cm.headIndex])
											+ ").");
						}
					}
				}
			}
			if (hascoref == false) {
				System.out.println("coref(na,na).");
			}
		}

		private void getDependency(CoreMap sentence) {
			/** no need to have this as instance var */
			boolean hasprep = false; 
			TreebankLanguagePack tlp = new PennTreebankLanguagePack();
			GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
			Tree tree = sentence.get(TreeAnnotation.class);
			// tree.pennPrint();
			GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
			Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
			Object[] list = tdl.toArray();
			// System.out.println(list.length);
			TypedDependency typedDependency;
			for (Object object : list) {
				typedDependency = (TypedDependency) object;
				if (typedDependency.reln().getShortName().equals("prep")) {
					hasprep = true;
					System.out.println("edge(prep,"
							+ typedDependency.reln().getSpecific()
							// + "," +
							// typedDependency.gov().label().value().toLowerCase()
							// + "," + typedDependency.gov().index()
							+ ","
							+ map.get(typedDependency.gov().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.gov().parent().label().value())
							// + "," +
							// typedDependency.dep().label().value().toLowerCase()
							// + "," + typedDependency.dep().index()
							+ ","
							+ map.get(typedDependency.dep().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.dep().parent().label().value())
							+ ").");
				} else if (typedDependency.reln().getShortName().equals("root")) {
					System.out.println("edge("
							+ typedDependency.reln().toString()
							// + "," +
							// typedDependency.gov().label().value().toLowerCase()
							// + "," + typedDependency.gov().index()
							+ ","
							+ map.get(typedDependency.gov().label().value()
									.toLowerCase())
							// + "," +
							// typedDependency.dep().label().value().toLowerCase()
							// + "," + typedDependency.dep().index()
							+ ","
							+ map.get(typedDependency.dep().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.dep().parent().label().value())
							+ ").");
				} else if (typedDependency.reln().getShortName().equals("conj")) {
					System.out.println("nvar(union,"
					// + typedDependency.gov().label().value().toLowerCase()
					// + "," + typedDependency.gov().index()
							+ map.get(typedDependency.gov().label().value()
									.toLowerCase()) + ",z" + count3 + ").");
					count3++;
					System.out.println("edge("
							+ typedDependency.reln().toString()
							// + "," +
							// typedDependency.gov().label().value().toLowerCase()
							// + "," + typedDependency.gov().index()
							+ ","
							+ map.get(typedDependency.gov().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.gov().parent().label().value())
							// + "," +
							// typedDependency.dep().label().value().toLowerCase()
							// + "," + typedDependency.dep().index()
							+ ","
							+ map.get(typedDependency.dep().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.dep().parent().label().value())
							+ ").");
				} else {
					System.out.println("edge("
							+ typedDependency.reln().toString()
							// + "," +
							// typedDependency.gov().label().value().toLowerCase()
							// + "," + typedDependency.gov().index()
							+ ","
							+ map.get(typedDependency.gov().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.gov().parent().label().value())
							// + "," +
							// typedDependency.dep().label().value().toLowerCase()
							// + "," + typedDependency.dep().index()
							+ ","
							+ map.get(typedDependency.dep().label().value()
									.toLowerCase())
							// + "," +
							// getSimplifiedPos(typedDependency.dep().parent().label().value())
							+ ").");
				}
			}
			if (hasprep == false) {
				// System.out.println("edge(prep,na,na,na,na,na,na,na).");
				System.out.println("edge(prep,na,na).");
			}
		}
	}

	public void parseSentence(String text) {
		parseSentence(text,"output.txt");
	}
	
	public void parseSentence(String text,String filePath) {
		Parse parse = new Parse(text);
		parse.processSentence(text,filePath);
	}	

	public static void main(String[] args) {
		Parser parser = new Parser();
		Scanner sc = new Scanner(System.in);
		String sentence = sc.nextLine();
		sc.close();
		parser.parseSentence(sentence);
	}

	static String getSimplifiedPos(String val) {
		if (val.contains("JJ") == true) {
			return "adj";
		} else if (val.contains("VB") == true) {
			return "vb";
		} else if (val.contains("NN") == true) {
			return "nn";
		} else if (val.contains("WDT") == true) {
			return "wp";
		} else if (val.equals("WP") == true) {
			return "wp";
		} else if (val.equals("WP$") == true) {
			return "wps";
		} else if (val.contains("WRB") == true) {
			return "wp";
		} else if (val.equals("PRP$") == true) {
			return "prps";
		} else {
			return val.toLowerCase();
		}
	}

}
