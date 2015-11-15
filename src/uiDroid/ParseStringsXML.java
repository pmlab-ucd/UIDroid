package uiDroid;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ParseStringsXML implements ParseXML {
	/**
	 * @param inputStream 获取xml文件，以流的形式返回
	 * @param encode 编码格式
	 * @return
	 */
	public Map<String, String> parseXML(InputStream inputStream, String encode){
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
					if(parser.getName().equals("string")){
						//获取该节点的内容
						String strName = parser.getAttributeValue(null, "name");
						String value = parser.nextText();
						res.put(strName, value);
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
