package uiDroid.depressed;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ParseManifestXML implements ParseXML {

	@Override
	public Map<String, String> parseXML(InputStream inputStream, String encode) {
		Map<String, String> res = new HashMap<>();
		
		//创建一个xml解析的工厂
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			//获得xml解析类的引用
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(inputStream,encode);
			//获得事件的类型
			int eventType = parser.getEventType();
			while(eventType!=XmlPullParser.END_DOCUMENT){
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					//String name = parser.getName();
					//System.out.println(name);
					if(parser.getName().equals("activity")){
						String label = parser.getAttributeValue(null, "android:label");
						String name = parser.getAttributeValue(null, "android:name");
						res.put(name, label);
					}
					break;
				case XmlPullParser.END_TAG:					
					break;
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return res;
	}

}
