/*
 * @(#)AbortException.java  
 *
 * Copyright (c) 1999-2012 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

/**
 * This exception is thrown when the production of an image
 * has been aborted.
 *
 * @author  Werner Randelshofer, Hausmatt 10, CH-6405 Immensee, Switzerland
 *
 * @version  $Id: AbortException.java 186 2012-03-28 11:18:42Z werner $
 */
public class AbortException extends Exception {

    public static final long serialVersionUID = 1L;

    /**
    Creates a new exception.
     */
    public AbortException() {
        super();
    }

    /**
    Creates a new exception.
    
     */
    public AbortException(String message) {
        super(message);
    }
}
