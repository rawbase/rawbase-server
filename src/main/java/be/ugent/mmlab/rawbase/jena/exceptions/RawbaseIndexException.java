/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena.exceptions;

/**
 *
 * @author mielvandersande
 */
public class RawbaseIndexException extends RawbaseException {

    public RawbaseIndexException() {
    }

    public RawbaseIndexException(String message) {
        super(message);
    }

    public RawbaseIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawbaseIndexException(Throwable cause) {
        super(cause);
    }

    public RawbaseIndexException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
