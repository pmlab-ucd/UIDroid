/**
 *	Union-Find
 * @author hao 
 */
package uiDroid.depressed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("hiding")
public class UnionFind<Object> {
	private Map<Object, Object> map = new HashMap<>();
	
	public void union(Object elem1, Object elem2) {
		Object ceo1 = find(elem1);
		Object ceo2 = find(elem2);
		if (!ceo1.equals(ceo2)) {
			map.put(ceo2, ceo1);
		}
	}
	
	public Object find(Object elem) {
		if (!map.containsKey(elem)) {
			map.put(elem, elem);
		}
		while (map.get(elem) != elem) {
			elem = map.get(elem);
		}
		
		return elem;
	}
	
	public Set<Object> keySet() {
		return map.keySet();
	}

}
