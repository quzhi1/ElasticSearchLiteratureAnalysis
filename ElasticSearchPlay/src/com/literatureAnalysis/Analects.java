package com.literatureAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.json.JSONObject;

public class Analects {
	
	private Analects(){	} // Utility class
	
	public static void bookToJSONFile(String path, String destPath) throws FileNotFoundException, IOException{
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		PrintWriter writer = new PrintWriter(destPath, "UTF-8");
		
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		    	InputStreamReader read = new InputStreamReader(new FileInputStream(file),"gbk");   
				
				BufferedReader br = new BufferedReader(read);
				
				String chapterNamePatternStr = "(.*)第.*";
				// Example: 堯曰第二十
				String chapterContentPatternStr = "(\\S+)";
				// Example: 『２』子曰：“《诗》三百，一言以蔽之，曰：思无邪。”
				
				Pattern chapterNamePattern = Pattern.compile(chapterNamePatternStr);
				Pattern chapterContentPattern = Pattern.compile(chapterContentPatternStr);
				
				String chapterName = null;
				
				// Read every file
				String line = br.readLine();
				while (line != null){
		        	
		        	System.out.println(line);
		        	Matcher chapterNameMatcher = chapterNamePattern.matcher(line);
		        	Matcher chapterContentMatcher = chapterContentPattern.matcher(line);
		        	if ( chapterNameMatcher.find() ){
		        		// Update chapterName
		        		System.out.println("Find Name!");
		        		chapterName = chapterNameMatcher.group(1);
		        	} else if ( chapterContentMatcher.find() ){
		        		System.out.println("Find Content!");
		        		String chapterContent = chapterContentMatcher.group(1);
		        		String jsonContent = strToJSON(chapterContent, chapterName).toString();
		        		
		        		// Write to file
		        		writer.println(jsonContent);
		        		
		        	}
		        	line = br.readLine();
		        }
		        
		        br.close();
		    }
		}
		writer.close();
	}
	
	private static JSONObject strToJSON(String content, String chapterName){
		JSONObject contentJSON = new JSONObject();
		
		contentJSON.put("chapter", chapterName);
		contentJSON.put("content", content);
		return contentJSON;
	}
	
	public static void JSONToES(String path) throws IOException, URISyntaxException{
		
		putIndex();
		
		putMapping();
		
		indexAll(path);
	}
	
	public static void delete(){
		Node node = NodeBuilder.nodeBuilder().client(true).node();
		Client client = node.client();
		DeleteIndexRequest deleteIndexRequest=new DeleteIndexRequest("lunyu");
	    client.admin().indices().delete(deleteIndexRequest).actionGet();
		node.close();
	}
	
	private static void putIndex() throws URISyntaxException, ClientProtocolException, IOException{
		// Create a index
		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder()
	        .setScheme("http")
	        .setHost("localhost")
	        .setPath("/lunyu")
	        .setPort(9200)
	        .build();
		HttpPut putIndex = new HttpPut(uri);
		CloseableHttpResponse response = httpclient.execute(putIndex);
		String statusString = response.getStatusLine().toString();
		response.close();
		// Check if the index is created successfully
		if ( !statusString.equals("HTTP/1.1 200 OK")) {
			System.out.println(statusString);
			System.exit(1);
		}
		httpclient.close();
	}
	
	private static void putMapping() throws URISyntaxException, ClientProtocolException, IOException{
		// Configure the analyzer
				CloseableHttpClient httpclient = HttpClients.createDefault();
				// Mapping json:
				//	{"lunyu": 
				//		{"properties": 
				// 			{"chapter": 
				// 				{"type": "string", "index": "not_analyzed"}
				// 			}
				// 		}
				//	}
				String query = "{\"lunyu\": {\"properties\": {\"chapter\": {\"type\": \"string\", \"index\": \"not_analyzed\"}}}}";
				URI uri = new URIBuilder()
		        .setScheme("http")
		        .setHost("localhost")
		        .setPath("/lunyu/_mapping/lunyu")
		        .setPort(9200)
		        .build();
				HttpPut putMapping = new HttpPut(uri);
				putMapping.setEntity(new StringEntity(query));
				CloseableHttpResponse response = httpclient.execute(putMapping);
				String statusString = response.getStatusLine().toString();
				response.close();
				// Check if the mappint is created successfully
				if ( !statusString.equals("HTTP/1.1 200 OK")) {
					System.out.println(statusString);
					System.exit(1);
				}
				httpclient.close();
	}
	
	private static void indexAll(String path) throws IOException, FileNotFoundException {
		Node node = NodeBuilder.nodeBuilder().client(true).node();
		Client client = node.client();
		
		// Index data
		InputStreamReader read = new InputStreamReader(new FileInputStream(new File(path)),"UTF-8");   
		BufferedReader br = new BufferedReader(read);
		String line = br.readLine();
		while (line != null){
			client.prepareIndex("lunyu", "lunyu")
	        	.setSource(line)
	        	.execute()
	        	.actionGet();
			line = br.readLine();
		}
		br.close();
		node.close();
	}
}
