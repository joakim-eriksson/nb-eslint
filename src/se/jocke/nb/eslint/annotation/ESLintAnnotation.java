package se.jocke.nb.eslint.annotation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import org.openide.text.Annotatable;
import org.openide.text.Line;

/**
 *
 * @author jocke
 */
public final class ESLintAnnotation extends org.openide.text.Annotation implements PropertyChangeListener {

    public static final String ATTACHED = "attached";
    private final Type type;
    private final String reason;
    private final int line;
    private final int col;
    private final Line.Part part;

    public enum Type {
        ERROR, WARNING
    }

    private boolean attached = false;

    private ESLintAnnotation(Type type, String reason, int line, int col, Line.Part part) {
        this.type = type;
        this.reason = reason;
        this.line = line;
        this.col = col;
        this.part = part;
    }

    public static ESLintAnnotation create(Type type, String reason, int line, int col, Line.Part part) {
        final ESLintAnnotation annotation = new ESLintAnnotation(type, reason, line, col, part);
        annotation.attach(part);
        part.addPropertyChangeListener(annotation);
        return annotation;
    }

    @Override
    public String getAnnotationType() {
        switch (type) {
            case ERROR:
                return "se-jocke-nb-eslint-jslinterrorannotation";
            case WARNING:
                return "se-jocke-nb-eslint-jslintwarnannotation";
            default:
                throw new AssertionError();
        }
    }

    @Override
    public String getShortDescription() {
        return reason + " (" + "Column: " + col + ")";
    }

    @Override
    protected void notifyDetached(Annotatable fromAnno) {
        part.removePropertyChangeListener(this);
        this.attached = false;
        super.firePropertyChange(ESLintAnnotation.ATTACHED, true, this.attached);
        super.notifyDetached(fromAnno);
    }

    @Override
    protected void notifyAttached(Annotatable toAnno) {
        this.attached = true;
        super.firePropertyChange(ESLintAnnotation.ATTACHED, false, this.attached);
        super.notifyAttached(toAnno);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.reason);
        hash = 97 * hash + this.line;
        hash = 97 * hash + this.col;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ESLintAnnotation other = (ESLintAnnotation) obj;
        if (this.line != other.line) {
            return false;
        }
        if (this.col != other.col) {
            return false;
        }
        return Objects.equals(this.reason, other.reason);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (propName == null || propName.equals(Annotatable.PROP_TEXT)) {
            this.detach();
        }
    }

    @Override
    public String toString() {
        return getShortDescription();
    }
    
    

}
