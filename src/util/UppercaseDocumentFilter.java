package util;

import javax.swing.text.*;

public class UppercaseDocumentFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
            throws BadLocationException {
        if (text != null) {
            text = text.toUpperCase();
        }
        super.insertString(fb, offset, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text != null) {
            text = text.toUpperCase();
        }
        super.replace(fb, offset, length, text, attrs);
    }
}