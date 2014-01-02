/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena.exceptions;

/**
 *
 * @author mielvandersande
 */
public class RawbaseCommitException extends RawbaseException {

    public RawbaseCommitException() {
    }

    public RawbaseCommitException(String message) {
        super(message);
    }

    public RawbaseCommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawbaseCommitException(Throwable cause) {
        super(cause);
    }

    public RawbaseCommitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
