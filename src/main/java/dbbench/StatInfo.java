package dbbench;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StatInfo implements Comparable {

  static SimpleDateFormat dfrm = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");

  String name;
  String host;
  String pid;
  int    count;
  long   lastEvent;

  public boolean equals(StatInfo anOther) {
    return name.equals(anOther.name) &&
        host.equals(anOther.host) &&
        pid.equals(anOther.pid);
  }

  public void clear() {
    name = "";
    host = "";
    pid = "";
  }

  public String toString() {
    return name + ", " + host + ", " + pid + ", " + dfrm.format(new Date(lastEvent));
  }

  @Override
  public int compareTo(Object o) {
    StatInfo si = (StatInfo)o;
    return si.count - count;
  }
}
