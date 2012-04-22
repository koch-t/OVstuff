import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
* Hacked together code to grab all the line keys from the KV78 demo and check if each stop on a line has a stopname and stoptown
* Needs the Jackson JSON jars version 2.0+
* WTFPL 2.0 licensed
*/

public class KV78Tester {

	public static void main (String[] args){
		new KV78Tester().testLines();
	}

	public void testLines(){
		try{
			System.out.println("Start checking lines");
			checkLines(getLines());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void checkStop(JsonParser jp, String line,int userStopOrder) throws JsonParseException, IOException{
		boolean hasTown = false, hasName = false;
		String timingPoint = "";
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String namefield = jp.getCurrentName();
			jp.nextToken();
			if ("Town".equals(namefield)){
				hasTown = true;
			}
			if ("Name".equals(namefield)){
				hasName = true;
			}
			if ("TimingPointCode".equals(namefield)){
				timingPoint = jp.getText();
			}
		}
		if (!hasTown && !hasName)
			System.out.println(String.format("Line %s misses townname and stopname for stopnumer %d, TPC %s", line,userStopOrder, timingPoint));
		else if (!hasTown)
			System.out.println(String.format("Line %s misses townname for stopnumer %d, TPC %s", line,userStopOrder, timingPoint));
		else if (!hasName)
			System.out.println(String.format("Line %s misses stopname for stopnumer %d, TPC %s", line,userStopOrder, timingPoint));
	}

	public void checkLines(BufferedReader in) throws JsonParseException, IOException{
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);
		String line = "";
		jp.nextToken();
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String namefield = jp.getCurrentName();
			if (namefield != null && namefield.contains("_")){
				line = namefield;
			}
			jp.nextToken();
			if ("Actuals".equals(namefield)){
				while (jp.nextToken() != JsonToken.END_OBJECT) {
					jp.getCurrentName();
					jp.nextToken();
					while (jp.nextToken() != JsonToken.END_OBJECT) {
						jp.nextToken();
					}
				}
			}else if ("Network".equals(namefield)){
				while (jp.nextToken() != JsonToken.END_OBJECT) {
					int userstop = Integer.parseInt(jp.getCurrentName());
					jp.nextToken();
					checkStop(jp,line,userstop);
				}
			}
		}
	}

	public void checkLines(String[] lines) throws IOException{
		System.out.println("Checking " + lines.length + " lines");
		for (int i = 0; i < lines.length; i++){
			String uri = "http://openov.nl:5078/line/" + lines[i];
			URL url = new URL(uri);
			HttpURLConnection uc = (HttpURLConnection) url.openConnection();
			uc.setRequestProperty("User-Agent", "KV78Turbo-Test");
			uc.setConnectTimeout(60000);
			uc.setReadTimeout(60000);
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));		
			try{
				checkLines(in);
			}finally{
				uc.disconnect();
			}
		}
	}

	public String[] parseLines(BufferedReader in) throws Exception{
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);
		ArrayList<String> lines = new ArrayList<String>();
		jp.nextToken();
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String line = jp.getCurrentName();
			lines.add(line);
			jp.nextToken();
		}
		jp.close();
		in.close();
		return lines.toArray(new String[lines.size()]);
	}

	public String[] getLines() throws Exception{
		String uri = "http://openov.nl:5078/line/";
		URL url = new URL(uri);
		HttpURLConnection uc = (HttpURLConnection) url.openConnection();
		uc.setRequestProperty("User-Agent", "KV78Turbo-Test");
		uc.setConnectTimeout(60000);
		uc.setReadTimeout(60000);
		BufferedReader in;
		in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));		
		try{
			return parseLines(in);
		}finally{
			uc.disconnect();
		}
	}

}
