package org.wso2.apim.sample.app.sub.migrator.exception;

public class DAOException extends Exception {
    public DAOException(String msg) {
        super(msg);
    }

    public DAOException(String msg, Throwable e) {
        super(msg, e);
    }
}
