/**
 * Copyright 2015 Hao Fu. All rights Reserved 
 * @Title: 	DFSPathQueue.java 
 * @Package playAppContext 
 * @Description: Helper data structure to traverse the icfg
 * @author:	Hao Fu 
 * @date:	Dec 31, 2015 10:47:10 AM 
 * @version	V1.0   
 */
package playAppContext;

import java.util.ArrayList;

/**
 * @ClassName: DFSPathQueue
 * @Description: Store a single dfs path from sensitive method to dummyMain
 * @author: Hao Fu
 * @date: Dec 31, 2015 10:47:10 AM
 */
public class DFSPathQueue<T> {
	ArrayList<T> queue;
	ArrayList<Boolean> existed;

	public DFSPathQueue() {
		queue = new ArrayList<>();
		existed = new ArrayList<>();
	}

	public void push(T element) {
		queue.add(element);
		existed.add(new Boolean(true));
	}

	public T pop() {
		int length = queue.size() - 1;
		for (int i = length; i >= 0; i--) {
			if (((Boolean) existed.get(i)).booleanValue()) {
				T element = queue.get(i);
				existed.set(i, new Boolean(false));
				return element;
			}
		}
		return null;
	}

	public T lastRemoved() {
		for (int i = queue.size() - 1; i >= 0; i--) {
			if (((Boolean) existed.get(i)).booleanValue()) {
				if (i == queue.size() - 1)
					return null;
				return (T) queue.get(i + 1);
			}
		}
		return null;
	}

	public boolean isEmpty() {
		for (int i = queue.size() - 1; i >= 0; i--) {
			if (((Boolean) existed.get(i)).booleanValue()) {
				return false;
			}
		}
		return true;
	}
}
