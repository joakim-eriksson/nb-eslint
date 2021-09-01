package se.jocke.nb.eslint.annotation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.openide.text.Annotatable;
import org.openide.text.Annotation;
import org.openide.text.Line;

/**
 *
 * @author jocke
 */
public final class ESLintAnnotation extends Annotation implements PropertyChangeListener {

    public static final String ATTACHED = "attached";
    private final String type;
    private final String reason;
    private final int line;
    private final int startCol;
    private final int endCol;
    private final Line.Part part;

    public static Map<Integer, String> Type = new HashMap<Integer, String>() {
        {
            put(1, "WARNING");
            put(2, "ERROR");
        }
    };

    private boolean attached = false;

    private ESLintAnnotation(String type, String reason, int line, int startCol, int endCol, Line.Part part) {
        super();

        this.type = type;
        this.reason = reason;
        this.line = line;
        this.startCol = startCol;
        this.endCol = endCol;
        this.part = part;
    }

    public static ESLintAnnotation create(String type, String reason, int line, int startCol, int endCol, Line.Part part) {
        final ESLintAnnotation annotation = new ESLintAnnotation(type, reason, line, startCol, endCol, part);
        annotation.attach(part);
        part.addPropertyChangeListener(annotation);

        return annotation;
    }

    @Override
    public String getAnnotationType() {
        switch (type) {
            case "WARNING":
                return "se-jocke-nb-eslint-eslintwarnannotation";
            case "ERROR":
                return "se-jocke-nb-eslint-eslinterrorannotation";
            default:
                throw new AssertionError();
        }
    }

    @Override
    public String getShortDescription() {
        return reason + " (" + "Column: " + startCol + ")";
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
        hash = 97 * hash + this.startCol;
        hash = 97 * hash + this.endCol;

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
        if (this.startCol != other.startCol) {
            return false;
        }

        if (this.endCol != other.endCol) {
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

//    public long getStartOffest() {
//        return startOffest;
//    }
//
//    public int getLength() {
//        return length;
//    }
//
//    public String getRuleKey() {
//        return ruleKey;
//    }
//
//    public String getRuleName() {
//        return ruleName;
//    }
//
//    public String getSeverity() {
//        return severity;
//    }
}
