package com.galoula.messenger;

public class ErrorGettingPage extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		 
	 ErrorGettingPage() {
		 super();
	 }

	 ErrorGettingPage(String message, Throwable cause) {
		  super(message, cause);
	 }
	 
	 ErrorGettingPage(Throwable cause) {
		  super(cause);
	 }

	ErrorGettingPage(String message) {
		  super(message);
	}
}
