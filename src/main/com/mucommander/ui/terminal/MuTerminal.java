/*
 * This file is part of trolCommander, http://www.trolsoft.ru/soft/trolcommander
 * Copyright (C) 2013-2014 Oleg Trifonov
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mucommander.ui.terminal;

import ch.qos.logback.classic.BasicConfigurator;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalPanelListener;
import com.jediterm.terminal.ui.TerminalSession;
import com.jediterm.terminal.ui.TerminalWidget;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import com.mucommander.cache.WindowsStorage;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.ui.main.MainFrame;
import com.pty4j.PtyProcess;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;

/**
 * @author Oleg Trifonov
 * Created on 24/10/14.
 */
public class MuTerminal {

    private final MainFrame mainFrame;
    private final TerminalWidget termWidget;
    private final SettingsProvider settingsProvider;
    private final MuTerminalTtyConnector ttyConnector;

    private static final String STORAGE_KEY = "TerminalPanel";

    public MuTerminal(final MainFrame mainFrame) {
        super();
        this.mainFrame = mainFrame;
        this.settingsProvider = new TerminalSettingsProvider();

        this.ttyConnector = createTtyConnector(getCurrentFolder());

        BasicConfigurator.configureDefaultContext();

        termWidget = new JediTermWidget(settingsProvider) {
            @Override
            protected com.jediterm.terminal.ui.TerminalPanel createTerminalPanel(SettingsProvider settingsProvider, StyleState styleState, TerminalTextBuffer textBuffer) {
                return new JediTerminalPanelEx(settingsProvider, textBuffer, styleState, mainFrame);
            }
        };


        termWidget.setTerminalPanelListener(new TerminalPanelListener() {
            public void onPanelResize(final Dimension pixelDimension, final RequestOrigin origin) {
            }

            @Override
            public void onSessionChanged(final TerminalSession currentSession) {
                updateTitle();
            }

            @Override
            public void onTitleChanged(String title) {
                updateTitle();//mainFrame.setTitle(termWidget.getCurrentSession().getSessionName());
            }
        });

        termWidget.getComponent().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                updateTitle();
            }

            @Override
            public void focusLost(FocusEvent e) {
                mainFrame.updateWindowTitle();
            }
        });

        if (termWidget.canOpenSession()) {
            openSession(termWidget, ttyConnector);
        }
    }


    private MuTerminalTtyConnector createTtyConnector(String directory) {
        try {
            return new MuTerminalTtyConnector(directory) {
                @Override
                public void close() {
                    super.close();
                    mainFrame.closeTerminalSession();
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
            return null;
        }
    }

    public void openSession(TerminalWidget terminal, TtyConnector ttyConnector) {
        TerminalSession session = terminal.createTerminalSession(ttyConnector);
        session.start();
    }


    public void storeHeight(int height) {
        WindowsStorage.getInstance().put(STORAGE_KEY, new WindowsStorage.Record(0, 0, 0, height));
    }

    public int loadHeight() {
        WindowsStorage.Record rec = WindowsStorage.getInstance().get(STORAGE_KEY);
        return rec != null ? rec.height : -1;
    }


    public JComponent getComponent() {
        return termWidget.getComponent();
    }

    public void show(boolean show) {
        termWidget.getComponent().setVisible(show);
        if (!show) {
            return;
        }
//        try {
//            termWidget.getCurrentSession().getTtyConnector().write("cd " + getCurrentFolder() + "\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
System.out.println(getCurrentFolder());
            PtyProcess.exec(new String[]{"cd", getCurrentFolder()});
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
        }

    }


    private String getCurrentFolder() {
        String currentFolder = mainFrame.getActivePanel().getCurrentFolder().getAbsolutePath();
        return currentFolder.contains("://") ? null : currentFolder;
    }


    public void updateTitle() {
        mainFrame.setTitle(termWidget.getCurrentSession().getSessionName());
    }

}