package info.bliki.extensions.scribunto;

public final class ScribuntoException extends Exception {
    public ScribuntoException(Exception e) {
        super(e);
    }

    public ScribuntoException(String s) {
        super(s);
    }
}
