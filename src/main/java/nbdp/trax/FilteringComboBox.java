package nbdp.trax;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class FilteringComboBox<T> extends JComboBox<T> {
  private final List<T> allItems = new ArrayList<>();
  private final Function<T, String> displayText;
  private boolean filtering = false;
  private boolean navigating = false;
  private T confirmedSelection = null;

  FilteringComboBox(Function<T, String> displayText) {
    this.displayText = displayText;
    setEditor(new BasicComboBoxEditor() {
      @Override
      public void setItem(Object item) {
        if (item == null) {
          super.setItem(null);
        } else {
          @SuppressWarnings("unchecked")
          T typed = (T) item;
          editor.setText(displayText.apply(typed));
        }
      }
    });
    setEditable(true);
    JTextField editor = (JTextField) getEditor().getEditorComponent();
    editor.setBorder(new EmptyBorder(0, 4, 0, 0));

    editor.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        filterLater();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        filterLater();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        filterLater();
      }
    });

    editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          confirmSelection();
          hidePopup();
          e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          resetToConfirmed();
          hidePopup();
          e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
          navigating = true;
          int current = getSelectedIndex();
          if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            if (current < getItemCount() - 1) {
              filtering = true;
              setSelectedIndex(current + 1);
              filtering = false;
            }
          } else {
            if (current > 0) {
              filtering = true;
              setSelectedIndex(current - 1);
              filtering = false;
            }
          }
          e.consume();
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
          navigating = false;
          e.consume();
        }
      }
    });

    editor.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        confirmSelection();
      }
    });

    addActionListener(e -> {
      if (!filtering) {
        confirmSelection();
      }
    });
  }

  @Override
  public void addItem(T item) {
    allItems.add(item);
    filtering = true;
    try {
      super.addItem(item);
    } finally {
      filtering = false;
    }
  }

  @Override
  public void removeAllItems() {
    allItems.clear();
    filtering = true;
    try {
      super.removeAllItems();
    } finally {
      filtering = false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setSelectedItem(Object item) {
    filtering = true;
    try {
      super.setSelectedItem(item);
      if (item != null) {
        confirmedSelection = (T) item;
        JTextField editor = (JTextField) getEditor().getEditorComponent();
        editor.setText(displayText.apply(confirmedSelection));
      }
    } finally {
      filtering = false;
    }
  }

  T getConfirmedSelection() {
    return confirmedSelection;
  }

  private void confirmSelection() {
    int idx = getSelectedIndex();
    if (idx >= 0) {
      confirmedSelection = getItemAt(idx);
    } else if (getItemCount() > 0) {
      confirmedSelection = getItemAt(0);
    }
    resetToConfirmed();
  }

  private void resetToConfirmed() {
    if (confirmedSelection == null) return;
    filtering = true;
    try {
      super.removeAllItems();
      for (T item : allItems) {
        super.addItem(item);
      }
      super.setSelectedItem(confirmedSelection);
      JTextField editor = (JTextField) getEditor().getEditorComponent();
      editor.setText(displayText.apply(confirmedSelection));
    } finally {
      filtering = false;
    }
  }

  private void filterLater() {
    if (filtering || navigating) return;
    SwingUtilities.invokeLater(this::applyFilter);
  }

  private void applyFilter() {
    JTextField editor = (JTextField) getEditor().getEditorComponent();
    String text = editor.getText();
    String lower = text.toLowerCase();

    filtering = true;
    try {
      super.removeAllItems();
      for (T item : allItems) {
        if (lower.isEmpty() || displayText.apply(item).toLowerCase().contains(lower)) {
          super.addItem(item);
        }
      }
      if (getItemCount() > 0) {
        setPopupVisible(true);
      }
      editor.setText(text);
      editor.setCaretPosition(text.length());
    } finally {
      filtering = false;
    }
  }
}