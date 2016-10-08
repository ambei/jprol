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

import java.awt.Dialog;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The class implements a small dialog windows to give possibility to custom
 * font because it is a very specialized auxiliary class, it is not described
 * very precisely
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
@SuppressWarnings("rawtypes")
public class FontChooserDialog extends javax.swing.JDialog implements ActionListener, ChangeListener {

  private static final long serialVersionUID = -1141137262767344186L;

  private static final String[] STYLES = new String[]{"PLAIN", "BOLD", "ITALIC", "BOLDI+TALIC"};
  private Font result;

  /**
   * Creates new form FontChooserDialog
   * @param parent
   * @param title
   * @param font
   * @param testText
   */
  @SuppressWarnings({"rawtypes","unchecked"})
  public FontChooserDialog(final Dialog parent, final String title, Font font, String testText) {
    super(parent, true);
    initComponents();

    setTitle(title);

    if (testText == null) {
      testText = "Sample text. <?!;:,.>";
    }

    LabelPreview.setText(testText);

    if (font == null) {
      font = Font.decode(null);
    }

    ComboBoxFont.removeAllItems();

    final String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    for (final String name : fonts) {
      ComboBoxFont.addItem(name);
    }

    SpinnerSize.setModel(new SpinnerNumberModel(12, 4, 100, 1));

    ComboBoxStyle.removeAllItems();
    for (final String style : STYLES) {
      ComboBoxStyle.addItem(style);
    }

    ComboBoxFont.setSelectedItem(font.getFamily());
    SpinnerSize.setValue(font.getSize());

    String style;
    switch (font.getStyle()) {
      case Font.BOLD: {
        style = "BOLD";
      }
      break;
      case Font.ITALIC: {
        style = "ITALIC";
      }
      break;
      case (Font.BOLD | Font.ITALIC): {
        style = "BOLDITALIC";
      }
      break;
      default: {
        style = "PLAIN";
      }
      break;
    }

    ComboBoxStyle.setSelectedItem(style);

    ComboBoxFont.addActionListener(this);
    ComboBoxStyle.addActionListener(this);
    SpinnerSize.addChangeListener(this);

    updateSampleFont();

    result = null;

    this.setLocationRelativeTo(parent);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        LabelPreview = new javax.swing.JTextArea();
        LabelFont = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        ButtonOk = new javax.swing.JButton();
        ButtonCancel = new javax.swing.JButton();
        SpinnerSize = new javax.swing.JSpinner();
        ComboBoxFont = new javax.swing.JComboBox();
        ComboBoxStyle = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Font");
        setLocationByPlatform(true);
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Preview", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 12))); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        LabelPreview.setColumns(20);
        LabelPreview.setRows(5);
        jScrollPane1.setViewportView(LabelPreview);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        LabelFont.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        LabelFont.setText("Font");

        jLabel2.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel2.setText("Style");

        jLabel3.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel3.setText("Size");

        ButtonOk.setText("Ok");
        ButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonOkActionPerformed(evt);
            }
        });

        ButtonCancel.setText("Cancel");
        ButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonCancelActionPerformed(evt);
            }
        });

        ComboBoxFont.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        ComboBoxStyle.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(LabelFont)
                                .addGap(212, 212, 212))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(ComboBoxFont, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ComboBoxStyle, 0, 160, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(118, 118, 118)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(SpinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(ButtonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ButtonCancel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {ButtonCancel, ButtonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(LabelFont)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SpinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ComboBoxStyle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ComboBoxFont, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ButtonOk)
                    .addComponent(ButtonCancel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {ComboBoxFont, ComboBoxStyle, SpinnerSize});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonOkActionPerformed
      result = LabelPreview.getFont();
      dispose();
    }//GEN-LAST:event_ButtonOkActionPerformed

    private void ButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonCancelActionPerformed
      result = null;
      dispose();
    }//GEN-LAST:event_ButtonCancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ButtonCancel;
    private javax.swing.JButton ButtonOk;
    private javax.swing.JComboBox ComboBoxFont;
    private javax.swing.JComboBox ComboBoxStyle;
    private javax.swing.JLabel LabelFont;
    private javax.swing.JTextArea LabelPreview;
    private javax.swing.JSpinner SpinnerSize;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

  public Font getResult() {
    return result;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    updateSampleFont();
  }

  @Override
  public void stateChanged(final ChangeEvent e) {
    updateSampleFont();
  }

  private synchronized void updateSampleFont() {
    final String fontName = (String) ComboBoxFont.getSelectedItem();
    final String fontStyle = (String) ComboBoxStyle.getSelectedItem();
    final int fontSize = (Integer) SpinnerSize.getValue();

    LabelPreview.setFont(Font.decode(fontName + ' ' + fontStyle + ' ' + fontSize));
    LabelPreview.invalidate();
    LabelPreview.repaint();
  }
}
