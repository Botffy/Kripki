package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public interface WorkerOrigin {
	public Component getComponent();
	public void workerStarted();
	public void workerSuccess();
	public void workerFailure();
}
