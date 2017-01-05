package dbbench;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class StatFrame extends JFrame implements MouseListener {

  private JTable              table;
  private DefaultTableModel   model;
  private ArrayList<StatInfo> calls;
  private StatInfo selected = new StatInfo();
  private Object[][] data=null;

  StatFrame(ArrayList<StatInfo> calls) {

    this.calls = calls;
    // -- Change event table -------------------------------
    model = new DefaultTableModel() {

      public Class getColumnClass(int columnIndex) {
        return String.class;
      }

      public boolean isCellEditable(int row, int column) {
        return false;
      }

      public void setValueAt(Object aValue, int row, int column) {
      }

    };
    table = new JTable(model);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.addMouseListener(this);
    JScrollPane view = new JScrollPane(table);

    JPanel btnPanel = new JPanel();
    btnPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    btnPanel.setBorder(BorderFactory.createEtchedBorder());

    JButton clearStats = new JButton("Clear Stats");
    clearStats.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearStats();
      }
    });
    btnPanel.add(clearStats);

    JButton dismissBtn = new JButton("Dismiss");
    dismissBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    });
    btnPanel.add(dismissBtn);

    JPanel innerPanel = new JPanel();
    innerPanel.setLayout(new BorderLayout());
    innerPanel.add(view, BorderLayout.CENTER);
    innerPanel.add(btnPanel,BorderLayout.SOUTH);
    innerPanel.setPreferredSize(new Dimension(1000,600));
    setContentPane(innerPanel);
    setTitle("Database calls");

  }

  public void clearStats() {
    synchronized (calls) {
      calls.clear();
    }
    selected.clear();
    refresh();
  }

  public void refresh() {

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String colName[] = {"Name", "ID", "Host", "Count"};
        int toSelect = -1;
        synchronized (calls) {
          data = new Object[calls.size()][4];
          for (int i = 0; i < calls.size(); i++) {
            StatInfo si = calls.get(i);
            data[i][0] = si.name;
            data[i][1] = si.pid;
            data[i][2] = si.host;
            data[i][3] = si.count;
            if (si.equals(selected))
              toSelect = i;
          }
        }
        table.clearSelection();
        model.setDataVector(data, colName);
        if (toSelect >= 0) {

          table.setRowSelectionInterval(toSelect, toSelect);
        }
        table.getColumnModel().getColumn(0).setMinWidth(200);
        table.getColumnModel().getColumn(1).setMinWidth(300);
        table.getColumnModel().getColumn(2).setMinWidth(200);
        table.getColumnModel().getColumn(3).setMinWidth(100);
      }
    });

  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if(SwingUtilities.isLeftMouseButton(e)) {
      synchronized (calls) {
        int r = getRowForLocation(e.getY());
        selected.name = (String)data[r][0];
        selected.pid = (String)data[r][1];
        selected.host = (String)data[r][2];
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {

  }

  @Override
  public void mouseReleased(MouseEvent e) {

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  private int getRowForLocation(int y) {

    boolean found = false;
    int i = 0;
    int h = 0;

    while(i<table.getRowCount() && !found) {
      found = (y>=h && y<=h+table.getRowHeight(i));
      if(!found) {
        h+=table.getRowHeight(i);
        i++;
      }
    }

    if(found) {
      return i;
    } else {
      return -1;
    }

  }

}
