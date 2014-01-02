/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena.exceptions;

/**
 *
 * @author mielvandersande
 */
public class RawbaseException extends Exception {

    public RawbaseException() {
    }

    public RawbaseException(String message) {
        super(message);
    }

    public RawbaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawbaseException(Throwable cause) {
        super(cause);
    }

    public RawbaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    
    
}
