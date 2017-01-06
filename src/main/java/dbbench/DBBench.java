package dbbench;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.swing.*;

import fr.esrf.tangoatk.widget.util.ATKGraphicsUtils;
import fr.esrf.tangoatk.widget.util.chart.*;
import fr.esrf.tangoatk.widget.util.ErrorPane;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.Tango.DevFailed;

public class DBBench extends JFrame {

  static SimpleDateFormat dfr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

  private JLChart chart = new JLChart();
  private JLDataView[] dv;
  private String[] dvNames;
  private DeviceProxy ds;
  private double oldTime = -1.0;
  private double[] oldValues;
  private int nbSignal;
  private int refreshPeriod=1000;
  private ArrayList<StatInfo> allCalls;
  private StatFrame statFrame = null;
  private boolean runningFromShell;
  static final Color[] defaultColor = {
    Color.red,
    Color.blue,
    Color.cyan,
    Color.green,
    Color.magenta,
    Color.orange,
    Color.pink,
    Color.yellow,
    Color.black};

  // Release number
  public static final String DEFAULT_VERSION = "-.-";
  public static final String VERSION = getVersion();
  private static final int minPeriod = 100;

  public DBBench(String dbName,boolean runningFromShell) {

    this.runningFromShell = runningFromShell;
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        exitFrame();
      }
    });

    // Menu Bar
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);
    final JMenuItem exitMenu = new JMenuItem("Exit");
    exitMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exitFrame();
      }
    });
    fileMenu.add(exitMenu);

    JMenu viewMenu = new JMenu("View");
    menuBar.add(viewMenu);
    JMenuItem statMenu = new JMenuItem("Stats...");
    statMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        viewStats();
      }
    });
    viewMenu.add(statMenu);

    JMenuItem periodMenu = new JMenuItem("Refresh period");
    periodMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editRefreshPeriod();
      }
    });
    viewMenu.add(periodMenu);

    setJMenuBar(menuBar);

    // Initialise chart properties
    chart.setHeaderFont(new Font("Times", Font.BOLD, 18));
    chart.setHeader("DBBench on " + dbName);
    chart.setDisplayDuration(300000);

    // Initialise axis properties
    chart.getY1Axis().setName("calls/sec");
    chart.getY1Axis().setAutoScale(true);

    chart.getXAxis().setAutoScale(true);
    chart.getXAxis().setName("Time");

    chart.setLabelPlacement(JLChart.LABEL_RIGHT);
    chart.setPaintAxisFirst(true);

    // Build dataviews
    try {
      ds = new DeviceProxy(dbName);
      DeviceAttribute da = ds.read_attribute("Timing_index");
      dvNames = da.extractStringArray();
    } catch (DevFailed e) {
      ErrorPane.showErrorMessage(null,dbName,e);
      exitFrame();
    }

    nbSignal = dvNames.length;
    dv = new JLDataView[nbSignal];
    for(int i=0;i<nbSignal;i++) {
      dv[i] = new JLDataView();
      dv[i].setName(dvNames[i]);
      dv[i].setUnit("calls/sec");
      dv[i].setColor(defaultColor[i % defaultColor.length]);
      chart.getY1Axis().addDataView(dv[i]);
    }
    oldValues = new double[nbSignal];

    allCalls = new ArrayList<StatInfo>();

    // Refresher thread
    new Thread() {
      public void run() {
        while (true) {
          if( isVisible() )
            refresh();
          try {
            Thread.sleep(refreshPeriod);
          } catch (InterruptedException e) {
          }
        }
      }
    }.start();

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(chart, BorderLayout.CENTER);

    getContentPane().setPreferredSize(new Dimension(800, 480));
    setTitle("DBBench " + VERSION);
  }

  private void editRefreshPeriod() {
    String newPeriod = JOptionPane.showInputDialog(this,"Enter refresh period (ms)",refreshPeriod);
    if(newPeriod!=null) {
      int period = Integer.parseInt(newPeriod);
      if (period<minPeriod) {
        ErrorPane.showErrorMessage(this,
                "Period to short", new Exception("Period must be equal or higher than "+minPeriod));
      }
      else {
        refreshPeriod = Integer.parseInt(newPeriod);
      }
    }
  }

  private void refresh() {

    int i;
    double[] values = null;
    double time = (double) (System.currentTimeMillis());

    // Read timing info
    try {

      DeviceAttribute da = ds.read_attribute("Timing_calls");
      values = da.extractDoubleArray();
      if(oldTime>0.0) {
        double t = (time - oldTime)/1000.0;
        for(i=0;i<nbSignal;i++) {
          chart.addData(dv[i],time,(values[i]-oldValues[i])/t);
        }
      }

      // Got Device info
      String[] histo = ds.black_box(50);
      synchronized (allCalls) {
        for(i=0;i<histo.length;i++) {
          StatInfo si = GetInfo(histo[i]);
          if(si!=null) addCall(si);
        }
        Collections.sort(allCalls);
      }

    } catch (DevFailed e) {
      System.out.println("Tango Error " + e.errors[0].desc);
    }

    // Save last value
    if(values!=null) {
      oldTime = time;
      for(i=0;i<nbSignal;i++)
        oldValues[i] = values[i];
    } else {
      // Invalid data
      oldTime = -1.0;
    }

    if(statFrame!=null)
      statFrame.refresh();

  }

  private void exitFrame() {
    if(runningFromShell) {
      System.exit(0);
    } else {
      setVisible(false);
      dispose();
    }
  }

  private void viewStats() {

    if(statFrame==null) {
      statFrame = new StatFrame(allCalls);
      statFrame.refresh();
      ATKGraphicsUtils.centerFrameOnScreen(statFrame);
    }
    statFrame.setVisible(true);

  }

  private void addCall(StatInfo si) {

    boolean found = false;
    int i = 0;

    while(!found && i<allCalls.size()) {
      StatInfo it = allCalls.get(i);
      found = it.equals(si);
      if(!found) i++;
    }

    if(!found) {
      // New item
      si.count = 1;
      allCalls.add(si);
    } else {
      if(si.lastEvent > allCalls.get(i).lastEvent) {
        allCalls.get(i).count++;
      }
    }

  }

  private StatInfo GetInfo(String v) {

    StatInfo si = new StatInfo();

    int p1Idx = v.lastIndexOf('(');
    int p2Idx = v.lastIndexOf(')');
    if(p1Idx<0 || p2Idx<0) {
      //System.out.println("Non handled " + v);
      return null;
    }
    si.pid = v.substring(p1Idx+1,p2Idx);
    v = v.substring(0,p1Idx-1);
    int sIdx = v.lastIndexOf(' ');
    if( sIdx<0 ) {
      //System.out.println("Non handled " + v);
      return null;
    }
    si.host = v.substring(sIdx+1);
    p1Idx = v.lastIndexOf('(');
    p2Idx = v.lastIndexOf(')');
    if(p1Idx<0 || p2Idx<0) {
      //System.out.println("Non handled " + v);
      return null;
    }
    si.name = v.substring(p1Idx + 1, p2Idx);
    si.count = 0;
    v = v.substring(0,p1Idx-1);
    sIdx = v.lastIndexOf(':');
    if( sIdx<0 ) {
      //System.out.println("Non handled " + v);
      return null;
    }
    String date = v.substring(0,sIdx-4);
    String ms = v.substring(sIdx-3,sIdx-1);

    try {
      Date d = dfr.parse(date);
      long millis = Long.parseLong(ms);
      si.lastEvent = d.getTime() + millis*10;
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      return null;
    }
    return si;

  }

  private static String getVersion(){
    Package p = DBBench.class.getPackage();

    //if version is set in MANIFEST.mf
    if(p.getImplementationVersion() != null) return p.getImplementationVersion();

    return DEFAULT_VERSION;
  }

  public static void main(String[] args) {

    if(args.length!=1) {
      System.out.println("Usage: dbbench database_name.");
      System.exit(0);
    }
    final DBBench f = new DBBench(args[0],true);
    ATKGraphicsUtils.centerFrameOnScreen(f);
    f.setVisible(true);

  }


}

