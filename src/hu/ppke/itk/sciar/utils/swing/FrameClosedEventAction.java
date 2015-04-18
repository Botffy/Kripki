package hu.ppke.itk.sciar.utils.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class FrameClosedEventAction extends WindowAdapter {	//sorry about it
	private final Action action;
	public FrameClosedEventAction(Action action) {
		this.action = action;
	}

	@Override public void windowClosing(WindowEvent ev) {
		action.actionPerformed(new ActionEvent(ev.getSource(), ActionEvent.ACTION_PERFORMED, "window closed"));
	}
}
