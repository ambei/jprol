/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.prol.easygui;

import com.igormaznitsa.prol.annotations.*;
import com.igormaznitsa.prol.data.*;
import com.igormaznitsa.prol.exceptions.*;
import com.igormaznitsa.prol.io.ProlStreamManager;
import com.igormaznitsa.prol.libraries.*;
import com.igormaznitsa.prol.logic.*;
import com.igormaznitsa.prol.parser.ProlConsult;
import com.igormaznitsa.prol.trace.TraceListener;
import com.igormaznitsa.prol.utils.Utils;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeModel;
import javax.swing.undo.*;

/**
 * The class implements the main frame of the Prol Pad IDE (a small UI utility to edit and run prol scripts) because it is a very specialized auxiliary class, it is not described
 * very precisely
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class MainFrame extends javax.swing.JFrame implements ProlStreamManager, Runnable, UndoableEditListener, WindowListener, DocumentListener, HyperlinkListener, TraceListener {

  private static final long serialVersionUID = -3816861562325125649L;

  /**
   * The version of the IDE
   */
  private final String VERSION = getString(this.getClass().getPackage().getImplementationVersion(), "<Development>");
  /**
   * Inside logger
   */
  private static final Logger LOG = Logger.getLogger("PROL_NOTE_PAD");

  private final ThreadGroup executingScripts = new ThreadGroup("ProlExecutingScripts");
  private final String PROPERTY_PROL_STACK_DEPTH = "prol.stack.depth";

  @Override
  public boolean onProlGoalCall(final Goal goal) {
    traceEditor.addCallText(goal.getGoalTerm().forWrite());
    return true;
  }

  @Override
  public boolean onProlGoalRedo(final Goal goal) {
    traceEditor.addRedoText(goal.getGoalTerm().forWrite());
    return true;
  }

  @Override
  public void onProlGoalFail(final Goal goal) {
    traceEditor.addFailText(goal.getGoalTerm().forWrite());
  }

  @Override
  public void onProlGoalExit(final Goal goal) {
    traceEditor.addExitText(goal.getGoalTerm().forWrite());
  }

  private static String getString(final String str, final String def) {
    return str == null ? def : str;
  }

  /**
   * This class is a helper Prol library to allow a user to print messages at the Message dialog window (and the log too) directly from its scripts
   */
  protected final class LogLibrary extends ProlAbstractLibrary {

    public LogLibrary() {
      super("JProlNotepadLog");
    }

    @Predicate(Signature = "msgerror/1", Reference = "The predicate allows to output information marked as error at the message window.")
    @Determined
    public void predicateMSGERROR(final Goal goal, final TermStruct struct) {
      final Term term = Utils.getTermFromElement(struct.getElement(0));
      final String text = term.forWrite();
      LOG.log(Level.SEVERE, "msgerror/1 : {0}", text);
      messageEditor.addErrorText(text);
    }

    @Predicate(Signature = "msgwarning/1", Reference = "The predicate allows to output information marked as warning at the message window.")
    @Determined
    public void predicateMSGWARNING(final Goal goal, final TermStruct struct) {
      final Term term = Utils.getTermFromElement(struct.getElement(0));
      final String text = term.forWrite();
      LOG.log(Level.WARNING, "msgwarning/1 : {0}", text);
      messageEditor.addWarningText(text);
    }

    @Predicate(Signature = "msginfo/1", Reference = "The predicate allows to output information marked as info at the message window.")
    @Determined
    public void predicateMSGINFO(final Goal goal, final TermStruct struct) {
      final Term term = Utils.getTermFromElement(struct.getElement(0));
      final String text = term.forWrite();
      LOG.log(Level.INFO, "msginfo/1 : {0}", text);
      messageEditor.addInfoText(text);
    }
  }
  protected final LogLibrary logLibrary;
  protected HashMap<String, LookAndFeelInfo> lookAndFeelMap;
  protected volatile Thread currentExecutedScriptThread;
  protected File currentOpenedFile;
  protected File lastOpenedFile;
  protected boolean startInTraceMode;
  protected boolean documentHasBeenChangedFlag;
  static final String[] PROL_LIBRARIES = new String[]{"com.igormaznitsa.prol.libraries.ProlGraphicLibrary", "com.igormaznitsa.prol.libraries.ProlStringLibrary"};
  protected volatile ProlContext lastContext;

  protected static final String PROL_EXTENSION = ".prl";

  private static final FileFilter PROL_FILE_FILTER = new FileFilter() {

    @Override
    public boolean accept(File f) {
      if (f == null) {
        return false;
      }
      if (f.isDirectory()) {
        return true;
      }
      return f.getName().toLowerCase().endsWith(PROL_EXTENSION);
    }

    @Override
    public String getDescription() {
      return "Prol files (*" + PROL_EXTENSION + ')';
    }
  };

  private static final int MAX_RECENT_FILES = 10;
  private final RecentlyOpenedFileFixedList recentFiles = new RecentlyOpenedFileFixedList(MAX_RECENT_FILES);

  /**
   * Creates new form MainFrame
   */
  public MainFrame() {
    initComponents();

    Toolkit dt = Toolkit.getDefaultToolkit();
    Dimension scr = dt.getScreenSize();
    setSize((scr.width * 10) / 12, (scr.height * 10) / 12);

    editorSource.addUndoableEditListener(this);
    editorSource.addDocumentListener(this);
    messageEditor.addHyperlinkListener(this);
    addWindowListener(this);
    panelProgress.setVisible(false);

    logLibrary = new LogLibrary();

    try {
      setIconImage(new ImageIcon(this.getClass().getResource("/com/igormaznitsa/prol/easygui/icons/appico.png")).getImage());
    }
    catch (Exception ex) {
      LOG.throwing(this.getClass().getCanonicalName(), "<init>()", ex);
    }

    fillLAndFeelMenu();

    loadPreferences();

    newFile();

    this.menuItemWordWrapSources.setState(editorSource.isWordWrap());

    final Action action = new AbstractAction("closeFindPanel") {
      private static final long serialVersionUID = 4377386270269629176L;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (panelFindText.isVisible()) {
          panelFindText.setVisible(false);
          textFind.setText("");
        }
      }

    };

    final KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    action.putValue(Action.ACCELERATOR_KEY, escKey);
    this.buttonCloseFind.getActionMap().put("closeFind", action);
    this.buttonCloseFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKey, "closeFind");

    this.panelFindText.setVisible(false);
  }

  private void fillLAndFeelMenu() {
    final LookAndFeelInfo plaf[] = UIManager.getInstalledLookAndFeels();
    this.lookAndFeelMap = new HashMap<String, LookAndFeelInfo>();
    final ButtonGroup lfGroup = new ButtonGroup();

    final ActionListener lfListener = new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {
        final JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
        setSelectedLookAndFeel(item.getText());
      }
    };

    for (int i = 0, n = plaf.length; i < n; i++) {
      final String lfName = plaf[i].getName();

      if (lfName.toLowerCase(Locale.ENGLISH).contains("nimbus")) {
        continue;
      }

      lookAndFeelMap.put(lfName, plaf[i]);
      JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(lfName);

      menuItem.addActionListener(lfListener);

      lfGroup.add(menuItem);
      menuLookAndFeel.add(menuItem);
    }
  }

  private void setSelectedLookAndFeel(String lookAndFeelName) {

    if (lookAndFeelName != null) {
      if (!this.lookAndFeelMap.containsKey(lookAndFeelName)) {
        lookAndFeelName = null;
      }
    }

    if (lookAndFeelName == null) {
      // set the first
      lookAndFeelName = this.menuLookAndFeel.getItem(0).getText();
    }

    final LookAndFeelInfo feelInfo = this.lookAndFeelMap.get(lookAndFeelName);
    final JFrame thisFrame = this;

    for (int li = 0; li < this.menuLookAndFeel.getItemCount(); li++) {
      final JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) this.menuLookAndFeel.getItem(li);
      if (menuItem.getText().equals(lookAndFeelName)) {
        if (!menuItem.isSelected()) {
          menuItem.setSelected(true);
        }
        break;
      }
    }
    try {
      UIManager.setLookAndFeel(feelInfo.getClassName());
    }
    catch (Exception ex) {
      LOG.throwing(thisFrame.getClass().getCanonicalName(), "L&F", ex);
    }

    SwingUtilities.updateComponentTreeUI(thisFrame);
  }

  public MainFrame(final File initFile) {
    this();
    loadFile(initFile, true);
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
   * Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    splitPaneMain = new javax.swing.JSplitPane();
    splitPaneTop = new javax.swing.JSplitPane();
    try {
      dialogEditor = new com.igormaznitsa.prol.easygui.DialogEditor();
    } catch (java.io.IOException e1) {
      e1.printStackTrace();
    }
    editorPanel = new javax.swing.JPanel();
    editorSource = new com.wordpress.tips4java.TextLineNumber();
    panelFindText = new javax.swing.JPanel();
    labelFind = new javax.swing.JLabel();
    textFind = new javax.swing.JTextField();
    buttonCloseFind = new javax.swing.JButton();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    splitPanelDown = new javax.swing.JSplitPane();
    messageEditor = new com.igormaznitsa.prol.easygui.MessageEditor();
    traceEditor = new com.igormaznitsa.prol.easygui.TraceDialog();
    panelProgress = new javax.swing.JPanel();
    progressBarTask = new javax.swing.JProgressBar();
    buttonStopExecuting = new javax.swing.JButton();
    jMenuBar1 = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileNew = new javax.swing.JMenuItem();
    menuFileOpen = new javax.swing.JMenuItem();
    menuFileSaveAs = new javax.swing.JMenuItem();
    menuFileSave = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    menuFileRecentFiles = new javax.swing.JMenu();
    jSeparator4 = new javax.swing.JPopupMenu.Separator();
    menuExit = new javax.swing.JMenuItem();
    menuEdit = new javax.swing.JMenu();
    menuUndo = new javax.swing.JMenuItem();
    menuRedo = new javax.swing.JMenuItem();
    jSeparator2 = new javax.swing.JPopupMenu.Separator();
    menuClearText = new javax.swing.JMenuItem();
    menuEditCommentSelected = new javax.swing.JMenuItem();
    menuEditUncommentSelected = new javax.swing.JMenuItem();
    jSeparator3 = new javax.swing.JPopupMenu.Separator();
    menuitemFindText = new javax.swing.JMenuItem();
    menuItemWordWrapSources = new javax.swing.JCheckBoxMenuItem();
    menuItemFullScreen = new javax.swing.JMenuItem();
    jSeparator5 = new javax.swing.JPopupMenu.Separator();
    menuEditOptions = new javax.swing.JMenuItem();
    menuRun = new javax.swing.JMenu();
    menuRunScript = new javax.swing.JMenuItem();
    menuTraceScript = new javax.swing.JMenuItem();
    menuRunStop = new javax.swing.JMenuItem();
    menuView = new javax.swing.JMenu();
    menuViewKnowledgeBase = new javax.swing.JMenuItem();
    menuItemLibraryInfo = new javax.swing.JMenuItem();
    menuLookAndFeel = new javax.swing.JMenu();
    menuHelp = new javax.swing.JMenu();
    menuHelpHelp = new javax.swing.JMenuItem();
    menuAbout = new javax.swing.JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

    splitPaneMain.setDividerLocation(350);
    splitPaneMain.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    splitPaneMain.setResizeWeight(0.8);
    splitPaneMain.setOneTouchExpandable(true);

    splitPaneTop.setDividerLocation(500);
    splitPaneTop.setResizeWeight(0.9);
    splitPaneTop.setOneTouchExpandable(true);

    dialogEditor.setToolTipText("The window allows to communicate with a user");
    splitPaneTop.setRightComponent(dialogEditor);

    editorPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Editor"));
    editorPanel.setLayout(new java.awt.BorderLayout());

    editorSource.setBorder(null);
    editorSource.setToolTipText("The editor allows to enter and edit text of a program");
    editorSource.setFont(new java.awt.Font("DejaVu Sans", 1, 13)); // NOI18N
    editorPanel.add(editorSource, java.awt.BorderLayout.CENTER);

    panelFindText.setLayout(new java.awt.GridBagLayout());

    labelFind.setText("Find:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    panelFindText.add(labelFind, gridBagConstraints);

    textFind.setToolTipText("Enter text for search (wildcard chars ? and * are supported)");
    textFind.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyReleased(java.awt.event.KeyEvent evt) {
        textFindKeyReleased(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipadx = 300;
    panelFindText.add(textFind, gridBagConstraints);

    buttonCloseFind.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/cross.png"))); // NOI18N
    buttonCloseFind.setToolTipText("Hide the find text panel (ESC)");
    buttonCloseFind.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    buttonCloseFind.setIconTextGap(0);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    panelFindText.add(buttonCloseFind, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1000.0;
    panelFindText.add(filler1, gridBagConstraints);

    editorPanel.add(panelFindText, java.awt.BorderLayout.PAGE_END);

    splitPaneTop.setLeftComponent(editorPanel);

    splitPaneMain.setTopComponent(splitPaneTop);

    splitPanelDown.setDividerLocation(500);
    splitPanelDown.setResizeWeight(0.8);
    splitPanelDown.setOneTouchExpandable(true);

    messageEditor.setToolTipText("The window shows messages during an execution of the script");
    splitPanelDown.setLeftComponent(messageEditor);

    traceEditor.setToolTipText("The window shows trace information if the engine is being started at the trace mode");
    splitPanelDown.setRightComponent(traceEditor);

    splitPaneMain.setBottomComponent(splitPanelDown);

    getContentPane().add(splitPaneMain, java.awt.BorderLayout.CENTER);

    panelProgress.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelProgress.setLayout(new java.awt.GridBagLayout());

    progressBarTask.setMaximumSize(new java.awt.Dimension(100, 20));
    progressBarTask.setPreferredSize(new java.awt.Dimension(40, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1000.0;
    panelProgress.add(progressBarTask, gridBagConstraints);

    buttonStopExecuting.setBackground(new java.awt.Color(255, 156, 156));
    buttonStopExecuting.setFont(new java.awt.Font("DejaVu Sans", 1, 13)); // NOI18N
    buttonStopExecuting.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/flag_red.png"))); // NOI18N
    buttonStopExecuting.setText("STOP EXECUTION");
    buttonStopExecuting.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
    buttonStopExecuting.setMaximumSize(new java.awt.Dimension(100, 23));
    buttonStopExecuting.setMinimumSize(new java.awt.Dimension(60, 23));
    buttonStopExecuting.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonStopExecutingActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    panelProgress.add(buttonStopExecuting, gridBagConstraints);

    getContentPane().add(panelProgress, java.awt.BorderLayout.SOUTH);

    menuFile.setText("File");

    menuFileNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/page.png"))); // NOI18N
    menuFileNew.setText("New");
    menuFileNew.setToolTipText("Create new document");
    menuFileNew.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileNewActionPerformed(evt);
      }
    });
    menuFile.add(menuFileNew);

    menuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
    menuFileOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/page_edit.png"))); // NOI18N
    menuFileOpen.setText("Open");
    menuFileOpen.setToolTipText("Open a saved document");
    menuFileOpen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileOpenActionPerformed(evt);
      }
    });
    menuFile.add(menuFileOpen);

    menuFileSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/page_save.png"))); // NOI18N
    menuFileSaveAs.setText("Save As..");
    menuFileSaveAs.setToolTipText("Save the current document as a file");
    menuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSaveAsActionPerformed(evt);
      }
    });
    menuFile.add(menuFileSaveAs);

    menuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    menuFileSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/page_go.png"))); // NOI18N
    menuFileSave.setText("Save");
    menuFileSave.setToolTipText("Save the current document");
    menuFileSave.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSaveActionPerformed(evt);
      }
    });
    menuFile.add(menuFileSave);
    menuFile.add(jSeparator1);

    menuFileRecentFiles.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/folder.png"))); // NOI18N
    menuFileRecentFiles.setText("Recent files...");
    menuFileRecentFiles.setToolTipText("List of files opened early");
    menuFileRecentFiles.addMenuListener(new javax.swing.event.MenuListener() {
      public void menuSelected(javax.swing.event.MenuEvent evt) {
        menuFileRecentFilesMenuSelected(evt);
      }
      public void menuDeselected(javax.swing.event.MenuEvent evt) {
      }
      public void menuCanceled(javax.swing.event.MenuEvent evt) {
      }
    });
    menuFile.add(menuFileRecentFiles);
    menuFile.add(jSeparator4);

    menuExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
    menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/door_in.png"))); // NOI18N
    menuExit.setText("Exit");
    menuExit.setToolTipText("Close the editor");
    menuExit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuExitActionPerformed(evt);
      }
    });
    menuFile.add(menuExit);

    jMenuBar1.add(menuFile);

    menuEdit.setText("Edit");

    menuUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
    menuUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/book_previous.png"))); // NOI18N
    menuUndo.setText("Undo");
    menuUndo.setToolTipText("Undo last changes in the document");
    menuUndo.setEnabled(false);
    menuUndo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuUndoActionPerformed(evt);
      }
    });
    menuEdit.add(menuUndo);

    menuRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
    menuRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/book_next.png"))); // NOI18N
    menuRedo.setText("Redo");
    menuRedo.setToolTipText("Redo canceled changes in the document");
    menuRedo.setEnabled(false);
    menuRedo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuRedoActionPerformed(evt);
      }
    });
    menuEdit.add(menuRedo);
    menuEdit.add(jSeparator2);

    menuClearText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
    menuClearText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/page_white.png"))); // NOI18N
    menuClearText.setText("Clear");
    menuClearText.setToolTipText("Just clear text in the current document");
    menuClearText.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuClearTextActionPerformed(evt);
      }
    });
    menuEdit.add(menuClearText);

    menuEditCommentSelected.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_5, java.awt.event.InputEvent.CTRL_MASK));
    menuEditCommentSelected.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/comment_add.png"))); // NOI18N
    menuEditCommentSelected.setText("Comment selection");
    menuEditCommentSelected.setToolTipText("Place the commenting symbol as the first one into selected lines");
    menuEditCommentSelected.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditCommentSelectedActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditCommentSelected);

    menuEditUncommentSelected.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
    menuEditUncommentSelected.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/comment_delete.png"))); // NOI18N
    menuEditUncommentSelected.setText("Uncomment selection");
    menuEditUncommentSelected.setToolTipText("Remove the first commenting symbol from selected lines");
    menuEditUncommentSelected.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditUncommentSelectedActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditUncommentSelected);
    menuEdit.add(jSeparator3);

    menuitemFindText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
    menuitemFindText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/zoom.png"))); // NOI18N
    menuitemFindText.setText("Find text");
    menuitemFindText.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuitemFindTextActionPerformed(evt);
      }
    });
    menuEdit.add(menuitemFindText);

    menuItemWordWrapSources.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
    menuItemWordWrapSources.setSelected(true);
    menuItemWordWrapSources.setText("Word wrap");
    menuItemWordWrapSources.setToolTipText("Word-wrap mode for the document editor");
    menuItemWordWrapSources.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/text_align_justify.png"))); // NOI18N
    menuItemWordWrapSources.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuItemWordWrapSourcesActionPerformed(evt);
      }
    });
    menuEdit.add(menuItemWordWrapSources);

    menuItemFullScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuItemFullScreen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/shape_move_forwards.png"))); // NOI18N
    menuItemFullScreen.setText("Full screen");
    menuItemFullScreen.setToolTipText("Turn on the full screen mode if it is supported by the device");
    menuItemFullScreen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuItemFullScreenActionPerformed(evt);
      }
    });
    menuEdit.add(menuItemFullScreen);
    menuEdit.add(jSeparator5);

    menuEditOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/cog.png"))); // NOI18N
    menuEditOptions.setText("Options");
    menuEditOptions.setToolTipText("Open editor options");
    menuEditOptions.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditOptionsActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditOptions);

    jMenuBar1.add(menuEdit);

    menuRun.setText("Run");

    menuRunScript.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
    menuRunScript.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/flag_green.png"))); // NOI18N
    menuRunScript.setText("Start");
    menuRunScript.setToolTipText("Execute the current document");
    menuRunScript.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuRunScriptActionPerformed(evt);
      }
    });
    menuRun.add(menuRunScript);

    menuTraceScript.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/flag_blue.png"))); // NOI18N
    menuTraceScript.setText("Trace");
    menuTraceScript.setToolTipText("Execute the current document with tracing");
    menuTraceScript.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTraceScriptActionPerformed(evt);
      }
    });
    menuRun.add(menuTraceScript);

    menuRunStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/flag_red.png"))); // NOI18N
    menuRunStop.setText("Stop");
    menuRunStop.setToolTipText("Stop the current execution");
    menuRunStop.setEnabled(false);
    menuRunStop.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuRunStopActionPerformed(evt);
      }
    });
    menuRun.add(menuRunStop);

    jMenuBar1.add(menuRun);

    menuView.setText("View");

    menuViewKnowledgeBase.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
    menuViewKnowledgeBase.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/eye.png"))); // NOI18N
    menuViewKnowledgeBase.setText("See the knowledge base");
    menuViewKnowledgeBase.setToolTipText("Take and show the snapshot of the current knowledge base saved in the memory");
    menuViewKnowledgeBase.setEnabled(false);
    menuViewKnowledgeBase.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuViewKnowledgeBaseActionPerformed(evt);
      }
    });
    menuView.add(menuViewKnowledgeBase);

    menuItemLibraryInfo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
    menuItemLibraryInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/table.png"))); // NOI18N
    menuItemLibraryInfo.setText("Library info");
    menuItemLibraryInfo.setToolTipText("Show all predicates found in embedded libraries");
    menuItemLibraryInfo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuItemLibraryInfoActionPerformed(evt);
      }
    });
    menuView.add(menuItemLibraryInfo);

    jMenuBar1.add(menuView);

    menuLookAndFeel.setText("Look&Feel");
    jMenuBar1.add(menuLookAndFeel);

    menuHelp.setText("Help");

    menuHelpHelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/information.png"))); // NOI18N
    menuHelpHelp.setText("Help");
    menuHelpHelp.setToolTipText("Show information about usage of the utility");
    menuHelpHelp.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuHelpHelpActionPerformed(evt);
      }
    });
    menuHelp.add(menuHelpHelp);

    menuAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/emoticon_smile.png"))); // NOI18N
    menuAbout.setText("About");
    menuAbout.setToolTipText("Show the information about the application and license");
    menuAbout.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuAboutActionPerformed(evt);
      }
    });
    menuHelp.add(menuAbout);

    jMenuBar1.add(menuHelp);

    setJMenuBar(jMenuBar1);

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void menuRunScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRunScriptActionPerformed
      if (this.currentExecutedScriptThread != null && this.currentExecutedScriptThread.isAlive()) {
        JOptionPane.showMessageDialog(this, "Script is already executing.", "Can't start", JOptionPane.WARNING_MESSAGE);
      }
      else {
        this.startInTraceMode = false;

        clearTextAtAllWindowsExcludeSource();

        this.dialogEditor.initBeforeSession();

        final long stackSize = extractStackDepth();
        LOG.info("Start execution with the stack depth " + stackSize + " bytes");

        this.currentExecutedScriptThread = new Thread(this.executingScripts, this, "ProlScriptExecutingThread", stackSize);
        currentExecutedScriptThread.setDaemon(false);

        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            showTaskControlPanel();
            currentExecutedScriptThread.start();
          }
        });
      }
    }//GEN-LAST:event_menuRunScriptActionPerformed

    private void menuUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUndoActionPerformed
      try {
        this.editorSource.getUndoManager().undo();
      }
      catch (CannotUndoException ex) {
      }
      UndoManager undo = editorSource.getUndoManager();
      this.menuUndo.setEnabled(undo.canUndo());
      this.menuRedo.setEnabled(undo.canRedo());

    }//GEN-LAST:event_menuUndoActionPerformed

    private void menuRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRedoActionPerformed
      try {
        this.editorSource.getUndoManager().redo();
      }
      catch (CannotRedoException ex) {
      }
      UndoManager undo = this.editorSource.getUndoManager();
      this.menuUndo.setEnabled(undo.canUndo());
      this.menuRedo.setEnabled(undo.canRedo());

    }//GEN-LAST:event_menuRedoActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
      windowClosing(null);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuClearTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuClearTextActionPerformed
      if (this.editorSource.getEditor().getDocument().getLength() > 10) {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to clean?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          this.editorSource.getUndoManager().discardAllEdits();
          this.editorSource.clearText();
        }
      }
    }//GEN-LAST:event_menuClearTextActionPerformed

    private void menuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileOpenActionPerformed
      loadFile(this.lastOpenedFile, false);
    }//GEN-LAST:event_menuFileOpenActionPerformed

    private void menuFileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSaveActionPerformed
      saveFile(false);
    }//GEN-LAST:event_menuFileSaveActionPerformed

    private void menuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSaveAsActionPerformed
      saveFile(true);
    }//GEN-LAST:event_menuFileSaveAsActionPerformed

    private void buttonStopExecutingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopExecutingActionPerformed
      final Thread thr = this.currentExecutedScriptThread;

      SwingUtilities.invokeLater(new Runnable() {

        @Override
        public void run() {
          if (thr != null) {
            try {
              thr.interrupt();
              dialogEditor.cancelRead();
              thr.join();
            }
            catch (Throwable tr) {
              tr.printStackTrace();
            }
            finally {
              hideTaskControlPanel();
            }
            messageEditor.addWarningText("Execution is canceled.");
          }
        }
      });

    }//GEN-LAST:event_buttonStopExecutingActionPerformed

    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
      final JHtmlLabel label = new JHtmlLabel("<html><body><h1>JProl Notepad</h1>Version: " + VERSION + "<br><b>Project page:</b> <a href=\"https://github.com/raydac/jprol\">https://github.com/raydac/jprol</a><br><b>Author:</b> Igor Maznitsa (<a href=\"http://www.igormaznitsa.com\">http://www.igormaznitsa.com</a>)<br><br>(C)2010-2016 Igor A. Maznitsa. <a href=\"https://www.apache.org/licenses/LICENSE-2.0\">Apache 2.0 License</a><br>Icons from the free icon set <a href=\"http://www.famfamfam.com/lab/icons/silk/\">http://www.famfamfam.com/lab/icons/silk/</a></body></html>");
      label.addLinkListener(new JHtmlLabel.LinkListener() {
        @Override
        public void onLinkActivated(final JHtmlLabel source, final String link) {
          try{
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(link));
            }
          }catch(Exception ex){
            LOG.log(Level.SEVERE, "Can't open URL : " + link, ex);
          }
        }
      });
      JOptionPane.showMessageDialog(this, label, "About", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(this.getIconImage()));
    }//GEN-LAST:event_menuAboutActionPerformed

    private void menuViewKnowledgeBaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuViewKnowledgeBaseActionPerformed
      if (lastContext == null) {
        return;
      }
      
      final KnowledgeBaseSnapshotViewDialog dialog = new KnowledgeBaseSnapshotViewDialog(this, lastContext);

      dialog.setSize(600, 400);

      dialog.setLocationRelativeTo(this);

      dialog.setVisible(true);
    }//GEN-LAST:event_menuViewKnowledgeBaseActionPerformed

    private void menuRunStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRunStopActionPerformed
      buttonStopExecutingActionPerformed(evt);
    }//GEN-LAST:event_menuRunStopActionPerformed

    private void menuHelpHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpHelpActionPerformed
      new HelpDialog(this).setVisible(true);
    }//GEN-LAST:event_menuHelpHelpActionPerformed

    private void menuEditOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditOptionsActionPerformed
      OptionsDialog dialog = new OptionsDialog(this, new TreeModel[]{editorSource, dialogEditor, messageEditor, traceEditor});
      dialog.setVisible(true);
    }//GEN-LAST:event_menuEditOptionsActionPerformed

    private void menuItemLibraryInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemLibraryInfoActionPerformed
      final java.util.List<String> list = new ArrayList<String>(PROL_LIBRARIES.length + 2);
      list.add(ProlCoreLibrary.class.getCanonicalName());
      list.addAll(Arrays.asList(PROL_LIBRARIES));
      list.add(MainFrame.class.getCanonicalName() + "$LogLibrary");

      LibraryInfoDialog infoDialog = null;
      try {
        infoDialog = new LibraryInfoDialog(this, list.toArray(new String[list.size()]));
      }
      catch (Exception ex) {
        LOG.throwing(this.getClass().getCanonicalName(), "MenuItemLibraryInfoActionPerformed()", ex);
        this.messageEditor.addErrorText("Can't show library info dialog [" + ex.getMessage() + ']');
        return;
      }

      infoDialog.setSize(512,480);

      infoDialog.setLocationRelativeTo(this);

      infoDialog.setVisible(true);
      infoDialog.dispose();
    }//GEN-LAST:event_menuItemLibraryInfoActionPerformed

    private void menuItemWordWrapSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemWordWrapSourcesActionPerformed
      this.editorSource.setWordWrap(this.menuItemWordWrapSources.isSelected());
    }//GEN-LAST:event_menuItemWordWrapSourcesActionPerformed

    private void menuFileNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileNewActionPerformed
      if (this.currentExecutedScriptThread != null && this.currentExecutedScriptThread.isAlive()) {
        JOptionPane.showMessageDialog(this, "The current script is executing.", "Can't make new", JOptionPane.ERROR_MESSAGE);
      }
      else {
        if (this.documentHasBeenChangedFlag) {
          if (JOptionPane.showConfirmDialog(this, "Document is changed and not saved. Do you really want to make new one?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            newFile();
          }
        }
        else {
          newFile();
        }
      }
    }//GEN-LAST:event_menuFileNewActionPerformed

  private long extractStackDepth() {
    final long MINIMAL_STACK = 5 * 1024 * 1024;

    long stackSize = MINIMAL_STACK;
    final String definedProlStackDepth = System.getProperty(PROPERTY_PROL_STACK_DEPTH);
    if (definedProlStackDepth != null) {
      int scale = 1;
      final String trimmed = definedProlStackDepth.trim().toLowerCase(Locale.ENGLISH);
      String text = trimmed;
      if (trimmed.endsWith("m")) {
        scale = 1024 * 1024;
        text = trimmed.substring(0, trimmed.length() - 1);
      }
      else if (trimmed.endsWith("k")) {
        scale = 1024;
        text = trimmed.substring(0, trimmed.length() - 1);
      }
      try {
        stackSize = Math.max(MINIMAL_STACK, Long.parseLong(text) * scale);
      }
      catch (NumberFormatException ex) {
        LOG.log(Level.SEVERE, "Can't extract stack depth value [" + definedProlStackDepth + ']', ex);
      }
    }
    return stackSize;
  }

    private void menuTraceScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTraceScriptActionPerformed
      if (this.currentExecutedScriptThread != null && this.currentExecutedScriptThread.isAlive()) {
        JOptionPane.showMessageDialog(this, "Script is already executing.", "Can't start", JOptionPane.WARNING_MESSAGE);
      }
      else {
        this.startInTraceMode = true;
        clearTextAtAllWindowsExcludeSource();
        this.dialogEditor.initBeforeSession();

        final long stackSize = extractStackDepth();
        LOG.info("Start traced execution with the stack depth " + stackSize + " bytes");

        this.currentExecutedScriptThread = new Thread(this.executingScripts, this, "ProlScriptTracedExecutingThread", stackSize);
        this.currentExecutedScriptThread.setDaemon(false);

        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            showTaskControlPanel();
            currentExecutedScriptThread.start();
          }
        });
      }
    }//GEN-LAST:event_menuTraceScriptActionPerformed

    private void menuFileRecentFilesMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_menuFileRecentFilesMenuSelected
      JMenu menu = (JMenu) evt.getSource();
      menu.removeAll();
      for (final String path : this.recentFiles.getCollection()) {
        final JMenuItem newItem = new JMenuItem(path);
        newItem.setActionCommand(path);
        newItem.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(final ActionEvent e) {
            final String text = e.getActionCommand();
            if (text != null) {
              try {
                loadFile(new File(text), true);
              }
              catch (Exception ex) {

                LOG.throwing(this.getClass().getCanonicalName(), "MenuFileRecentFilesMenuSelected()", ex);
              }
            }
          }
        });
        menu.add(newItem);
      }
    }//GEN-LAST:event_menuFileRecentFilesMenuSelected

    private void menuEditCommentSelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditCommentSelectedActionPerformed
      // TODO add your handling code here:
      if (this.editorSource.commentSelectedLines()) {
        documentChanged();
      }
    }//GEN-LAST:event_menuEditCommentSelectedActionPerformed

    private void menuEditUncommentSelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditUncommentSelectedActionPerformed
      // TODO add your handling code here:
      if (this.editorSource.uncommentSelectedLines()) {
        documentChanged();
      }
    }//GEN-LAST:event_menuEditUncommentSelectedActionPerformed

  private void menuItemFullScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemFullScreenActionPerformed
    final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    if (gd != null && gd.isFullScreenSupported()) {
      if (gd.getFullScreenWindow() == null) {
        gd.setFullScreenWindow(this);
      }
      else {
        gd.setFullScreenWindow(null);
      }
    }
  }//GEN-LAST:event_menuItemFullScreenActionPerformed

  private void menuitemFindTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitemFindTextActionPerformed
    this.panelFindText.setVisible(true);
    this.textFind.setText("");
    this.textFind.requestFocus();
  }//GEN-LAST:event_menuitemFindTextActionPerformed

  private int searchText(final String text, final Pattern pattern, final int cursorPos) {
    if (cursorPos >= text.length()) {
      return -1;
    }

    final Matcher matcher = pattern.matcher(text);
    if (matcher.find(cursorPos)) {
      return matcher.start();
    }
    return -1;
  }

  private void textFindKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFindKeyReleased
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
      final Pattern patternToFind = UIUtils.makePattern(textFind.getText());

      final String text = this.editorSource.getText();

      int cursorPos = searchText(text, patternToFind, this.editorSource.getCaretPosition() + 1);

      if (cursorPos < 0) {
        cursorPos = searchText(text, patternToFind, 0);
      }

      if (cursorPos >= 0) {
        this.editorSource.setCaretPosition(cursorPos);
      }
    }
  }//GEN-LAST:event_textFindKeyReleased

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonCloseFind;
  private javax.swing.JButton buttonStopExecuting;
  private com.igormaznitsa.prol.easygui.DialogEditor dialogEditor;
  private javax.swing.JPanel editorPanel;
  private com.wordpress.tips4java.TextLineNumber editorSource;
  private javax.swing.Box.Filler filler1;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JPopupMenu.Separator jSeparator2;
  private javax.swing.JPopupMenu.Separator jSeparator3;
  private javax.swing.JPopupMenu.Separator jSeparator4;
  private javax.swing.JPopupMenu.Separator jSeparator5;
  private javax.swing.JLabel labelFind;
  private javax.swing.JMenuItem menuAbout;
  private javax.swing.JMenuItem menuClearText;
  private javax.swing.JMenu menuEdit;
  private javax.swing.JMenuItem menuEditCommentSelected;
  private javax.swing.JMenuItem menuEditOptions;
  private javax.swing.JMenuItem menuEditUncommentSelected;
  private javax.swing.JMenuItem menuExit;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileNew;
  private javax.swing.JMenuItem menuFileOpen;
  private javax.swing.JMenu menuFileRecentFiles;
  private javax.swing.JMenuItem menuFileSave;
  private javax.swing.JMenuItem menuFileSaveAs;
  private javax.swing.JMenu menuHelp;
  private javax.swing.JMenuItem menuHelpHelp;
  private javax.swing.JMenuItem menuItemFullScreen;
  private javax.swing.JMenuItem menuItemLibraryInfo;
  private javax.swing.JCheckBoxMenuItem menuItemWordWrapSources;
  private javax.swing.JMenu menuLookAndFeel;
  private javax.swing.JMenuItem menuRedo;
  private javax.swing.JMenu menuRun;
  private javax.swing.JMenuItem menuRunScript;
  private javax.swing.JMenuItem menuRunStop;
  private javax.swing.JMenuItem menuTraceScript;
  private javax.swing.JMenuItem menuUndo;
  private javax.swing.JMenu menuView;
  private javax.swing.JMenuItem menuViewKnowledgeBase;
  private javax.swing.JMenuItem menuitemFindText;
  private com.igormaznitsa.prol.easygui.MessageEditor messageEditor;
  private javax.swing.JPanel panelFindText;
  private javax.swing.JPanel panelProgress;
  private javax.swing.JProgressBar progressBarTask;
  private javax.swing.JSplitPane splitPaneMain;
  private javax.swing.JSplitPane splitPaneTop;
  private javax.swing.JSplitPane splitPanelDown;
  private javax.swing.JTextField textFind;
  private com.igormaznitsa.prol.easygui.TraceDialog traceEditor;
  // End of variables declaration//GEN-END:variables

  private void setLastContext(ProlContext context) {
    this.lastContext = context;
    this.menuViewKnowledgeBase.setEnabled(lastContext != null);
  }

  @Override
  public Reader getReaderForResource(String resourceName) throws IOException {
    boolean successful = false;
    boolean notTraceable = false;
    try {
      if (resourceName.equals("user")) {
        successful = true;
        notTraceable = true;
        return this.dialogEditor.getInputReader();
      }
      else {
        final FileReader reader = new FileReader(resourceName);
        successful = true;
        notTraceable = false;
        return reader;
      }
    }
    finally {
      if (!notTraceable) {
        if (successful) {
          this.messageEditor.addInfoText("The reader for \'" + resourceName + "\' has been opened.");
        }
        else {
          this.messageEditor.addWarningText("The reader for \'" + resourceName + "\' can't be opened.");
        }
      }
    }
  }

  @Override
  public Writer getWriterForResource(String resourceName, boolean append) throws IOException {
    boolean successful = false;
    boolean notTraceable = false;

    try {
      if (resourceName.equals("user")) {
        successful = true;
        notTraceable = true;
        return this.dialogEditor.getOutputWriter();
      }
      else {
        final Writer writer = new FileWriter(resourceName);
        successful = true;
        notTraceable = false;

        return writer;
      }
    }
    finally {
      if (!notTraceable) {
        if (successful) {
          this.messageEditor.addInfoText("The writer for \'" + resourceName + "\' has been opened.");
        }
        else {
          this.messageEditor.addWarningText("The writer for \'" + resourceName + "\' can't be opened.");
        }
      }
    }
  }

  @Override
  public void run() {
    ProlContext context = null;
    ProlConsult consult = null;
    boolean successfully = false;
    boolean canceled = false;
    ProlHaltExecutionException halted = null;
    ParserException parserException = null;

    long startTime = 0;

    try {
      this.dialogEditor.setEnabled(true);
      this.dialogEditor.requestFocus();

      this.messageEditor.addInfoText("Creating Context...");

      try {
        context = new ProlContext("ProlScript", this);
        if (this.startInTraceMode) {
          context.setDefaultTraceListener(this);
        }

        for (final String str : PROL_LIBRARIES) {
          final ProlAbstractLibrary lib = (ProlAbstractLibrary) Class.forName(str).newInstance();

          context.addLibrary(lib);
          this.messageEditor.addInfoText("Library \'" + lib.getLibraryUID() + "\' has been added...");
        }

        context.addLibrary(logLibrary);
        this.messageEditor.addInfoText("Library \'" + logLibrary.getLibraryUID() + "\' has been added...");

        setLastContext(context);
      }
      catch (Throwable ex) {
        LOG.log(Level.WARNING, "ExecutionThread.run()", ex);
        this.messageEditor.addErrorText("Can't create context for exception [" + ex.getMessage() + ']');
        return;
      }

      this.messageEditor.addInfoText("Consult with the script... ");
      consult = new ProlConsult(editorSource.getText(), context);

      startTime = System.currentTimeMillis();

      try {
        consult.consult();
        // wait for async threads
        context.getContextExecutorService().shutdown();
        context.getContextExecutorService().awaitTermination(60, TimeUnit.SECONDS);
      }
      catch (ParserException ex) {
        LOG.log(Level.WARNING, "ExecutionThread.run()", ex);
        parserException = ex;

        final Throwable cause = ex.getCause();

        if (cause instanceof StackOverflowError) {
          this.messageEditor.addErrorText("Stack Overflow!");
          JOptionPane.showMessageDialog(this, "Stack overflow exception detected!", "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }
        else if (cause instanceof OutOfMemoryError) {
          this.messageEditor.addErrorText("Out of Memory!");
          JOptionPane.showMessageDialog(this, "Out of Memory exception  detected!", "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }

        if (cause instanceof ProlHaltExecutionException) {
          halted = (ProlHaltExecutionException) ex.getCause();
        }
        if (cause instanceof InterruptedException) {
          canceled = true;
        }
        else {
          this.messageEditor.addText("Parser exception [" + ex.getMessage() + ']', MessageEditor.TYPE_ERROR, "source://" + ex.getLine() + ';' + ex.getPos(), "line " + ex.getLine() + ":" + ex.getPos());
          return;
        }
      }
      catch (ThreadDeath death) {
        canceled = true;
      }
      catch (Throwable ex) {
        LOG.log(Level.WARNING, "ExecutionThread.run()", ex);

        if (ex instanceof ProlHaltExecutionException || ex.getCause() instanceof ProlHaltExecutionException) {
          if (ex instanceof ProlHaltExecutionException) {
            halted = (ProlHaltExecutionException) ex;
          }
          else {
            halted = (ProlHaltExecutionException) ex.getCause();
          }
        }
        else {
          this.messageEditor.addErrorText("Can't parse script for exception [" + ex.getMessage() + ']');
          return;
        }
      }

      successfully = true;

    }
    finally {
      try {
        this.messageEditor.addInfoText("Total time " + ((System.currentTimeMillis() - startTime) / 1000f) + " sec.");

        if (halted == null) {
          if (!canceled) {
            if (successfully) {
              this.messageEditor.addInfoText("Completed successfully.");
            }
            else {
              this.messageEditor.addErrorText("Completed with errors or not started.");
            }
          }
        }
        else {
          this.messageEditor.addText("Halted [" + halted.getMessage() + ']', MessageEditor.TYPE_WARNING, parserException != null ? ("source://" + parserException.getLine() + ';' + parserException.getPos()) : null, parserException != null ? ("line " + parserException.getLine() + ":" + parserException.getPos()) : null);
        }
        this.dialogEditor.setEnabled(false);
        this.currentExecutedScriptThread = null;
      }
      finally {
        if (context != null) {
          try {
            context.halt();
          }
          catch (IllegalStateException ex) {
          }
        }
        hideTaskControlPanel();
      }
    }
  }

  @Override
  public void undoableEditHappened(final UndoableEditEvent e) {
    final UndoManager undo = this.editorSource.getUndoManager();
    undo.addEdit(e.getEdit());
    this.menuUndo.setEnabled(undo.canUndo());
    this.menuRedo.setEnabled(undo.canRedo());

  }

  @Override
  public void windowOpened(final WindowEvent e) {
  }

  @Override
  public void windowClosing(final WindowEvent e) {
    if (this.documentHasBeenChangedFlag) {
      if (JOptionPane.showConfirmDialog(this, "Document is changed but not saved. Do you really want to exit?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return;
      }
    }

    if (this.currentExecutedScriptThread != null) {
      if (JOptionPane.showConfirmDialog(this, "Task is under execution. Do you really want to exit?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

        this.dialogEditor.cancelRead();
        this.dialogEditor.close();

        final Thread thrd = this.currentExecutedScriptThread;

        try {
          thrd.interrupt();
          thrd.join();
        }
        catch (Throwable thr) {
        }
      }
      else {
        return;
      }
    }
    else {
      this.dialogEditor.cancelRead();
      this.dialogEditor.close();
    }

    savePreferences();

    try {
      this.dispose();
    }
    catch (Exception ex) {
    }
    finally {
      System.exit(0);
    }
  }

  @Override
  public void windowClosed(final WindowEvent e) {
  }

  @Override
  public void windowIconified(final WindowEvent e) {
  }

  @Override
  public void windowDeiconified(final WindowEvent e) {
  }

  @Override
  public void windowActivated(final WindowEvent e) {
    if (this.currentExecutedScriptThread != null) {
      this.dialogEditor.requestFocus();
    }
    else {
      this.editorSource.requestFocus();
    }

  }

  @Override
  public void windowDeactivated(final WindowEvent e) {
  }

  @Override
  public void insertUpdate(final DocumentEvent e) {
    documentChanged();
  }

  @Override
  public void removeUpdate(final DocumentEvent e) {
    documentChanged();
  }

  @Override
  public void changedUpdate(final DocumentEvent e) {
    documentChanged();
  }

  private void documentChanged() {
    if (!this.documentHasBeenChangedFlag) {
      this.documentHasBeenChangedFlag = true;
      if (this.currentOpenedFile != null) {
        this.menuFileSave.setEnabled(true);
      }
      setTitle("*" + getTitle());
    }
  }

  private void setTextToDocument(final String text) {
    this.editorSource.clearText();
    this.editorSource.getEditor().setText(text);

    if (this.currentOpenedFile != null) {
      this.menuFileSave.setEnabled(true);
    }
    else {
      this.menuFileSave.setEnabled(false);
    }

    this.editorSource.getUndoManager().discardAllEdits();
    this.menuUndo.setEnabled(false);
    this.menuRedo.setEnabled(false);

    this.documentHasBeenChangedFlag = false;
  }

  private void saveFile(final boolean saveAs) {
    File file = this.currentOpenedFile;
    if (saveAs || this.currentOpenedFile == null) {
      JFileChooser fileChooser = new JFileChooser(file);
      fileChooser.addChoosableFileFilter(PROL_FILE_FILTER);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setDragEnabled(false);
      fileChooser.setMultiSelectionEnabled(false);

      if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        file = fileChooser.getSelectedFile();

        if (!file.exists() && fileChooser.getFileFilter().equals(PROL_FILE_FILTER)) {
          // ake auto extension
          if (!file.getName().toLowerCase().endsWith(PROL_EXTENSION)) {
            file = new File(file.getAbsolutePath() + PROL_EXTENSION);
          }
        }

        if (saveAs || !file.equals(this.currentOpenedFile)) {
          if (file.exists()) {
            if (JOptionPane.showConfirmDialog(this, "File \'" + file.getAbsolutePath() + "\' exists, to overwrite it?", "File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
              return;
            }
          }
        }
      }
      else {
        return;
      }
    }

    final String textFromEditor = this.editorSource.getEditor().getText();

    Writer writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));
      writer.write(textFromEditor);
      writer.flush();
      this.recentFiles.put(file.getAbsolutePath());
    }
    catch (Throwable thr) {
      LOG.throwing(this.getClass().getCanonicalName(), "saveFile()", thr);
      JOptionPane.showMessageDialog(this, "Can't save file for error \'" + (thr.getMessage() == null ? thr.getClass().getCanonicalName() : thr.getLocalizedMessage()), "Can't save file", JOptionPane.ERROR_MESSAGE);
      return;
    }
    finally {
      if (writer != null) {
        try {
          writer.close();
        }
        catch (Throwable thr) {
        }
      }
    }

    this.currentOpenedFile = file;
    this.lastOpenedFile = currentOpenedFile;
    setTitle(this.currentOpenedFile.getAbsolutePath());

    this.editorSource.getUndoManager().discardAllEdits();
    this.menuFileSave.setEnabled(true);
    this.documentHasBeenChangedFlag = false;
  }

  private void newFile() {
    // make new

    this.editorSource.clearText();

    clearTextAtAllWindowsExcludeSource();

    this.currentOpenedFile = null;
    this.documentHasBeenChangedFlag = false;

    setTitle("The Prol Notepad utility. Version: " + VERSION);

    repaint();
  }

  private void clearTextAtAllWindowsExcludeSource() {
    this.traceEditor.clearText();
    this.dialogEditor.clearText();
    this.messageEditor.clearText();
  }

  private void loadFile(final File file, final boolean justLoadFile) {
    if (this.documentHasBeenChangedFlag) {
      if (JOptionPane.showConfirmDialog(this, "Document is changed and not saved. To load new one?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
        return;
      }
    }

    JFileChooser fileChooser = new JFileChooser(file);
    if (!justLoadFile) {
      fileChooser.addChoosableFileFilter(PROL_FILE_FILTER);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setDragEnabled(false);
      fileChooser.setMultiSelectionEnabled(false);
      fileChooser.setFileFilter(PROL_FILE_FILTER);
    }

    if (justLoadFile || fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File fileToOpen = justLoadFile ? file : fileChooser.getSelectedFile();

      this.lastOpenedFile = fileToOpen;

      Reader reader = null;

      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToOpen), "UTF-8"));
        final StringBuilder buffer = new StringBuilder((int) fileToOpen.length() < 0 ? 16384 : (int) fileToOpen.length());
        while (true) {
          final int chr = reader.read();
          if (chr < 0) {
            break;
          }
          buffer.append((char) chr);
        }

        setTextToDocument(buffer.toString());
        this.currentOpenedFile = fileToOpen;
        setTitle(this.currentOpenedFile.getCanonicalPath());
        this.repaint();

        this.recentFiles.put(fileToOpen.getAbsolutePath());

      }
      catch (Throwable thr) {
        LOG.throwing(this.getClass().getCanonicalName(), "loadFile()", thr);
        JOptionPane.showMessageDialog(this, "Can't load file " + fileToOpen.getAbsolutePath() + " [" + thr.getMessage() + "]");
        this.recentFiles.remove(fileToOpen.getAbsolutePath());
      }
      finally {
        if (reader != null) {
          try {
            reader.close();
          }
          catch (Throwable thr) {
          }
        }
      }

    }
  }

  private void showTaskControlPanel() {
    this.panelProgress.setVisible(true);
    this.progressBarTask.setIndeterminate(true);
    this.menuRunStop.setEnabled(true);
  }

  private void hideTaskControlPanel() {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        panelProgress.setVisible(false);
        progressBarTask.setIndeterminate(false);
        menuRunStop.setEnabled(false);
      }
    });
  }

  @Override
  public void hyperlinkUpdate(HyperlinkEvent e) {
    if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
      return;
    }

    String path = e.getDescription();
    if (path.startsWith("source://")) {
      path = path.substring(9);
      String[] parsed = path.split(";");
      if (parsed.length == 2) {
        try {
          int line = Integer.parseInt(parsed[0].trim());
          int pos = Integer.parseInt(parsed[1].trim());

          this.editorSource.setCaretPosition(line, pos + 1);
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  private void loadPreferences() {
    final Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    setSelectedLookAndFeel(prefs.get("lookandfeel", menuLookAndFeel.getItem(0).getText()));

    int recentFileIndex = 1;
    this.recentFiles.clear();
    while (true) {
      final String path = prefs.get("RecentFile" + recentFileIndex, null);
      if (path == null) {
        break;
      }
      this.recentFiles.add(path);
      recentFileIndex++;
    }

    if (prefs.getBoolean("maximized", false)) {
      setExtendedState(JFrame.MAXIMIZED_BOTH);
      invalidate();
      doLayout();
    }
    else {
      setSize(prefs.getInt("mainwidth", 640), prefs.getInt("mainheight", 600));
      setLocation(prefs.getInt("mainx", 0), prefs.getInt("mainy", 0));
    }

    this.splitPaneMain.setDividerLocation(prefs.getInt("splitpanemainpos", 400));
    this.splitPaneTop.setDividerLocation(prefs.getInt("splitpanetoppos", 300));

    final String lastFile = prefs.get("lastfile", "");
    if (lastFile.length() > 0) {
      this.lastOpenedFile = new File(lastFile);
    }
    else {
      this.lastOpenedFile = null;
    }

    this.editorSource.loadPreferences(prefs);
    this.messageEditor.loadPreferences(prefs);
    this.dialogEditor.loadPreferences(prefs);
    this.traceEditor.loadPreferences(prefs);
  }

  private void savePreferences() {
    final Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    LookAndFeelInfo currentInfo = null;
    final String landfClass = UIManager.getLookAndFeel().getClass().getName();
    for(final LookAndFeelInfo i : UIManager.getInstalledLookAndFeels()){
      if (i.getClassName().equals(landfClass)){
        currentInfo = i;
        break;
      }
    }
    if (currentInfo != null) {
      prefs.put("lookandfeel", currentInfo.getName());
    }

    int recentFileIndex = 1;
    for (final String recentFile : this.recentFiles.getCollection()) {
      prefs.put("RecentFile" + recentFileIndex, recentFile);
      recentFileIndex++;
    }

    prefs.putBoolean("maximized", (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH);

    prefs.putInt("mainwidth", getWidth());
    prefs.putInt("mainheight", getHeight());

    prefs.putInt("mainx", getX());
    prefs.putInt("mainy", getY());

    prefs.putInt("splitpanemainpos", splitPaneMain.getDividerLocation());
    prefs.putInt("splitpanetoppos", splitPaneTop.getDividerLocation());

    prefs.put("lastfile", this.lastOpenedFile == null ? "" : this.lastOpenedFile.getAbsolutePath());

    this.editorSource.savePreferences(prefs);
    this.messageEditor.savePreferences(prefs);
    this.dialogEditor.savePreferences(prefs);
    this.traceEditor.savePreferences(prefs);
  }
}
