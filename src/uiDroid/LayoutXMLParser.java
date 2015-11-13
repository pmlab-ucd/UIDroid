/*
 * Parse layout Activity xml and store relevant widgets
 */

package uiDroid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LayoutXMLParser {
	/**
	 * @param inputStream 获取xml文件，以流的形式返回
	 * @param encode 编码格式
	 * @return
	 */
	public static List<Widget> parseXML(InputStream inputStream, String encode){
		List<Widget> list = null;
		Widget widget = null;//装载解析每一个Widget节点的内容
		
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
					list = new ArrayList<Widget>();
					break;
				case XmlPullParser.START_TAG:
					String name = parser.getName();
					System.out.println(name);
					if(parser.getName().equals("Button")){
						widget = new Widget();
						widget.setType(1);
						//获取该节点的内容
						String sid = parser.getAttributeValue(0);
						widget.setSid(sid);
						String text = parser.getAttributeValue(null, "android:text");
						widget.setText(text);
						list.add(widget);
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
		
		return list;
	}

}
