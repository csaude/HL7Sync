package mz.org.csaude.hl7sync;

public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

}
