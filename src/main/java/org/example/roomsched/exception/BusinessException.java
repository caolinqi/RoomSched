package org.example.roomsched.exception;

public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
