/*
 * @(#)ParseException.java  
 *
 * Copyright (c) 1999-2012 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

/**
 * Exception thrown by IFFParse.
 * 
 * @author  Werner Randelshofer, Hausmatt 10, CH-6405 Immensee, Switzerland
 * @version  $Id: ParseException.java 186 2012-03-28 11:18:42Z werner $
 */
public class ParseException extends Exception {

    public static final long serialVersionUID = 1L;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
