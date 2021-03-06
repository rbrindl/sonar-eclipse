/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.console;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.jobs.SonarRunnerLogListener;
import org.sonar.ide.eclipse.ui.internal.ISonarConsole;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;

import java.io.IOException;

public class SonarConsole extends IOConsole implements SonarRunnerLogListener, ISonarConsole {

  static final String P_DEBUG_OUTPUT = "debugOutput"; //$NON-NLS-1$
  static final String P_SHOW_CONSOLE = "showConsole"; //$NON-NLS-1$
  static final String P_SHOW_CONSOLE_NEVER = "never"; //$NON-NLS-1$
  static final String P_SHOW_CONSOLE_ON_OUTPUT = "onOutput"; //$NON-NLS-1$
  static final String P_SHOW_CONSOLE_ON_ERROR = "onError"; //$NON-NLS-1$

  private static final String TITLE = Messages.SonarConsole_title;

  private IOConsoleOutputStream infoStream;
  private IOConsoleOutputStream warnStream;

  // Colors must be disposed
  private Color warnColor;

  public SonarConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
    initStreams(Display.getDefault());
  }

  private void initStreams(Display display) {
    this.infoStream = newOutputStream();
    this.warnStream = newOutputStream();

    // TODO make colors configurable
    warnColor = new Color(display, new RGB(255, 0, 0));

    getWarnStream().setColor(warnColor);
  }

  @Override
  protected void dispose() {
    super.dispose();

    warnColor.dispose();
  }

  public void info(String msg) {
    if (isShowConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getInfoStream(), msg);
  }

  public void error(String msg) {
    if (isShowConsoleOnOutput() || isShowConsoleOnError()) {
      bringConsoleToFront();
    }
    write(getWarnStream(), msg);
  }

  private void write(IOConsoleOutputStream stream, String msg) {
    try {
      stream.write(msg);
    } catch (IOException e) {
      throw new SonarEclipseException("Unable to write in console", e);
    }
  }

  public void bringConsoleToFront() {
    showConsole();
  }

  public void showConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole console : manager.getConsoles()) {
      if (this == console) {
        manager.showConsoleView(this);
        return;
      }
    }
    // not found - create a new one
    manager.addConsoles(new IConsole[] {this});
  }

  public void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.removeConsoles(new IConsole[] {this});
  }

  private IOConsoleOutputStream getInfoStream() {
    return infoStream;
  }

  private IOConsoleOutputStream getWarnStream() {
    return warnStream;
  }

  private String getShowConsolePreference() {
    return Platform.getPreferencesService().getString(ISonarConstants.PLUGIN_ID, P_SHOW_CONSOLE, P_SHOW_CONSOLE_ON_OUTPUT, null);
  }

  private boolean isShowConsoleOnOutput() {
    return StringUtils.equals(getShowConsolePreference(), P_SHOW_CONSOLE_ON_OUTPUT);
  }

  private boolean isShowConsoleOnError() {
    return StringUtils.equals(getShowConsolePreference(), P_SHOW_CONSOLE_ON_ERROR);
  }

  public static boolean isDebugEnabled() {
    return SonarUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarConsole.P_DEBUG_OUTPUT);
  }

}
