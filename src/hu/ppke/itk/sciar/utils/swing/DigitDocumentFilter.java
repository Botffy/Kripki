package hu.ppke.itk.sciar.utils.swing;

import javax.swing.*;
import javax.swing.text.*;


// straight from http://stackoverflow.com/q/5662651/477453
public class DigitDocumentFilter extends DocumentFilter {
	@Override public void insertString(FilterBypass fb, int off, String str, AttributeSet attr) throws BadLocationException {
		fb.insertString(off, str.replaceAll("\\D++", ""), attr);
	}
	@Override public void replace(FilterBypass fb, int off, int len, String str, AttributeSet attr) throws BadLocationException {
		fb.replace(off, len, str.replaceAll("\\D++", ""), attr);  // remove non-digits
	}
}
