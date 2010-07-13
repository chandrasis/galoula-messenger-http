package com.galoula.messenger;

public class CTException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
			 
		 CTException() {
			 super();
		 }

		 CTException(String message, Throwable cause) {
			  super(message, cause);
		 }
		 
		 CTException(Throwable cause) {
			  super(cause);
		 }

		 CTException(String message) {
			  super(message);
		 }
}

