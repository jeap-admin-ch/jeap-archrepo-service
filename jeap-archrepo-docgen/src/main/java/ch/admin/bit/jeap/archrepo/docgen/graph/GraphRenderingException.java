package ch.admin.bit.jeap.archrepo.docgen.graph;

/**
 * A stored reaction graph could not be rendered.
 */
public class GraphRenderingException extends RuntimeException {

    public GraphRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
