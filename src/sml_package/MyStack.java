package sml_package;

import java.util.LinkedList;

public class MyStack<T> {
	private final LinkedList<T> data;
	
	public MyStack() {data = new LinkedList<T>();}
	
	public void push(T obj) {data.push(obj);}
	public T pop() {return data.pop();}
	public T peek() {return data.peek();}
	public void empty() {data.clear();}
	public boolean isEmpty() {return data.isEmpty();}
	public void print() {
		System.out.print("[");
		for (T ob : data) {
			System.out.print(ob.toString() + " ");
		}
		System.out.println("]");
	}
	public String toString() {
		String s = "[";
		for (T ob : data) {
			s += ob.toString() + " ";
		}
		s += "]";
		return s;
	}
}
