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

import com.igormaznitsa.prol.easygui.AbstractProlEditor.PropertyLink;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * The class implements a small dialog window contains the tree which allows
 * user to change values of different IDE parts because it is a very specialized
 * auxiliary class, it is not described very precisely
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public class OptionsDialog extends javax.swing.JDialog implements TreeSelectionListener, TreeModel {

    private static final long serialVersionUID = 6189268173338687096L;

    private final ArrayList<TreeModel> items;
    private final ArrayList<TreeModelListener> treeModelListeners;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCloseDialog;
    private javax.swing.JButton buttonEditOption;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree optionsTree;

    /**
     * Creates new form OptionsDialog
     *
     * @param parent the parent frame, can be null
     * @param items the tree model items, must not be null
     */
    public OptionsDialog(final java.awt.Frame parent, final TreeModel[] items) {
        super(parent, true);
        initComponents();

        treeModelListeners = new ArrayList<>();

        this.items = new ArrayList<>();
        for (final TreeModel model : items) {
            this.items.add(model);
        }

        optionsTree.setModel(this);
        optionsTree.setCellRenderer(new TreeRenderer());
        optionsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        optionsTree.addTreeSelectionListener(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        optionsTree = new javax.swing.JTree();
        buttonEditOption = new javax.swing.JButton();
        buttonCloseDialog = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Options");
        setLocationByPlatform(true);

        optionsTree.setRootVisible(false);
        optionsTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                optionsTreeMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(optionsTree);

        buttonEditOption.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/cog_edit.png"))); // NOI18N
        buttonEditOption.setText("Edit");
        buttonEditOption.setToolTipText("Edit or change the selected option");
        buttonEditOption.setEnabled(false);
        buttonEditOption.addActionListener((java.awt.event.ActionEvent evt) -> {
          buttonEditOptionActionPerformed(evt);
        });

        buttonCloseDialog.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/prol/easygui/icons/cross.png"))); // NOI18N
        buttonCloseDialog.setText("Close");
        buttonCloseDialog.setToolTipText("Close the dialog");
        buttonCloseDialog.addActionListener((java.awt.event.ActionEvent evt) -> {
          buttonCloseDialogActionPerformed(evt);
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(buttonEditOption, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(buttonCloseDialog, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{buttonCloseDialog, buttonEditOption});

        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(buttonEditOption)
                                        .addComponent(buttonCloseDialog))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCloseDialogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseDialogActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCloseDialogActionPerformed

    private void buttonEditOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEditOptionActionPerformed
        final TreePath path = optionsTree.getSelectionPath();
        if (path == null) {
            return;
        }
        final AbstractProlEditor.PropertyLink prop = (PropertyLink) path.getLastPathComponent();
        final Object val = prop.getProperty();
        if (val instanceof Font) {
            final FontChooserDialog dialog = new FontChooserDialog(this, "Tune the font for \'" + prop.toString() + "\'", (Font) val, "?-repeat,write('Hello world'),nl,fail.\r\n:-X is 2*2,write(X).");
            dialog.setVisible(true);
            final Font font = dialog.getResult();
            if (font != null) {
                prop.setProperty(font);
            }
        } else if (val instanceof Color) {
            final Color color = (Color) val;
            JColorChooser chooser = new JColorChooser(color);

            final boolean[] array = new boolean[1];

            ActionListener actionListenerOk = (ActionEvent e) -> {
              array[0] = true;
            };

            JDialog colorChooser = JColorChooser.createDialog(this, "Choose color for \'" + prop.toString() + "\'", true, chooser, actionListenerOk, null);
            colorChooser.setVisible(true);
            if (array[0]) {
                prop.setProperty(chooser.getColor());
            }
        } else if (val instanceof Boolean) {
            prop.setProperty((Boolean) (!(Boolean) val));
        }

        firePropertyChanged(path);

    }//GEN-LAST:event_buttonEditOptionActionPerformed

    private void optionsTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_optionsTreeMouseClicked
        final JTree tree = (JTree) evt.getSource();
        int selRow = tree.getRowForLocation(evt.getX(), evt.getY());
        final TreePath selPath = tree.getPathForLocation(evt.getX(), evt.getY());
        if (selRow != -1) {
            if (evt.getClickCount() == 2) {
                if (buttonEditOption.isEnabled()) {
                    buttonEditOption.doClick();
                }
            }
        }
    }//GEN-LAST:event_optionsTreeMouseClicked

    @Override
    public void valueChanged(final TreeSelectionEvent e) {
        TreePath path = optionsTree.getSelectionPath();
        if (path != null) {

            buttonEditOption.setEnabled(path.getLastPathComponent() instanceof AbstractProlEditor.PropertyLink);
            return;
        }

        buttonEditOption.setEnabled(false);
    }
    // End of variables declaration//GEN-END:variables

    @Override
    public Object getRoot() {
        return this;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (this.equals(parent)) {
            return items.get(index);
        } else {
            return ((TreeModel) parent).getChild(parent, index);
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (this.equals(parent)) {
            return items.size();
        } else {
            return ((TreeModel) parent).getChildCount(parent);
        }
    }

    @Override
    public boolean isLeaf(final Object node) {
        return node instanceof AbstractProlEditor.PropertyLink;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (this.equals(parent)) {
            return items.indexOf(child);
        } else {
            return ((TreeModel) parent).getIndexOfChild(parent, child);
        }

    }

    private void firePropertyChanged(TreePath path) {
        for (TreeModelListener listener : treeModelListeners) {
            listener.treeNodesChanged(new TreeModelEvent(this, path));
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    private static final class TreeRenderer extends DefaultTreeCellRenderer {

        private static final long serialVersionUID = 4008482488500265205L;

        private static final Icon ICON_CLOSE = UIUtils.loadIcon("folder_wrench");
        private static final Icon ICON_OPEN = UIUtils.loadIcon("folder");
        private static final Icon ICON_LEAF = UIUtils.loadIcon("wrench");

        @Override
        public Icon getLeafIcon() {
            return ICON_LEAF;
        }

        @Override
        public Icon getClosedIcon() {
            return ICON_CLOSE;
        }

        @Override
        public Icon getOpenIcon() {
            return ICON_OPEN;
        }
    }

}
