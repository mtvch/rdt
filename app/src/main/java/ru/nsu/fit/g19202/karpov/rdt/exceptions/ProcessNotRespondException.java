package ru.nsu.fit.g19202.karpov.rdt.exceptions;

public class ProcessNotRespondException extends Exception {
    private static final long serialVersionUID = -3868867472342L;
    public ProcessNotRespondException() {
		super();
	}
	public ProcessNotRespondException(String message) {
		super(message);
	}
}
