package dbbench;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.tangoatk.widget.util.ATKConstant;
import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class StatFrame extends JFrame implements MouseListener {

  private JTable              table;
  private DefaultTableModel   model;
  private JCheckBox           update;
  private ArrayList<StatInfo> calls;
  private StatInfo selected = new StatInfo();
  private Object[][] data=null;


  class HostTableRenderer extends JPanel implements TableCellRenderer
  {

    private JButton hostBtn;
    private JLabel hostLabel;

    public HostTableRenderer()
    {
      setOpaque(true);
      setLayout(new BorderLayout());
      hostBtn = new JButton("...");
      hostBtn.setMargin(new Insets(0,0,0,0));
      hostLabel = new JLabel();
      hostBtn.setFont(ATKConstant.labelFont);
      hostLabel.setFont(ATKConstant.labelFont);
      hostLabel.setOpaque(true);
      add(hostBtn, BorderLayout.EAST);
      add(hostLabel, BorderLayout.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      hostLabel.setText((value == null) ? "" : value.toString());
      if(isSelected)
        hostLabel.setBackground(table.getSelectionBackground());
      else
        hostLabel.setBackground(table.getBackground());
      return this;
    }

  }

  StatFrame(ArrayList<StatInfo> calls) {

    this.calls = calls;
    // -- Change event table -------------------------------
    model = new DefaultTableModel() {

      public Class getColumnClass(int columnIndex) {
        if(columnIndex==2)
          return Button.class;
        else
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
    table.setDefaultRenderer(Button.class, new HostTableRenderer());
    JScrollPane view = new JScrollPane(table);

    JPanel btnPanel = new JPanel();
    btnPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    btnPanel.setBorder(BorderFactory.createEtchedBorder());

    update = new JCheckBox("Refresh");
    update.setSelected(true);
    btnPanel.add(update);

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

  public void launchTerminal(String host) {

    JSSHTerminal.MainPanel terminal;
    String defaultUser = null;
    String defaultPassword = null;
    try {
      Database db = ApiUtil.get_db_obj();
      DbDatum dd = db.get_property("Astor","RloginUser");
      if(!dd.is_empty())
        defaultUser = dd.extractString();
      dd = db.get_property("Astor","RloginPassword");
      if(!dd.is_empty())
        defaultPassword = dd.extractString();
    } catch (DevFailed e) {}

    if(defaultUser!=null) {
      terminal = new JSSHTerminal.MainPanel(host,defaultUser,defaultPassword,80,24,500);
      terminal.setX11Forwarding(true);
      terminal.setExitOnClose(false);
      ATKGraphicsUtils.centerFrameOnScreen(terminal);
      terminal.setVisible(true);
    } else {
      JOptionPane.showMessageDialog(this, "No username !\nAStor/RloginUser free property not defined.", "Error", JOptionPane.ERROR_MESSAGE);
    }

  }

  public void refresh() {

    if(!update.isSelected())
      return;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String colName[] = {"Name", "ID", "Host" , "Count"};
        int toSelect = -1;
        synchronized (calls) {
          data = new Object[calls.size()][5];
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
        if (toSelect >= 0)
          table.setRowSelectionInterval(toSelect, toSelect);
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
        selected.clear();
        int r = getRowForLocation(e.getY());
        if(r>=0) {
          selected.name = (String)data[r][0];
          selected.pid = (String)data[r][1];
          selected.host = (String)data[r][2];
          int[] c = getColForLocation(e.getX());
          if(c != null && c[0]==2) {
            if((c[2]-c[1])<30)
              launchTerminal(selected.host);
          }
        }
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

  private int[] getColForLocation(int x) {

    boolean found = false;
    int i = 0;
    int h = 0;
    int cw = 0;

    while(i<table.getColumnCount() && !found) {
      cw = table.getColumnModel().getColumn(i).getWidth();
      found = (x>=h && x<=h+cw);
      if(!found) {
        h+=cw;
        i++;
      }
    }

    if(found) {
      return new int[]{i,x-h,cw};
    } else {
      return null;
    }

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
