package dbbench;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class StatInfo implements Comparable {

  static SimpleDateFormat dfrm = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
  static private JSch jsch=new JSch();

  String name;
  String host;
  String pid;
  String cmdLine = null;
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

  public String extractPID() {

    int idx = pid.length()-1;

    while(idx>=0 && pid.charAt(idx)>='0' && pid.charAt(idx)<='9')
      idx--;
    idx++;

    if(idx<0)
      return null;
    else
      return pid.substring(idx);

  }

  public String getCmdLine() {

    if(cmdLine==null) {
      String PID = extractPID();
      cmdLine = PID + "  " + execCommand(host, "xargs -0 < /proc/"+extractPID()+"/cmdline");
    }
    return cmdLine;

  }


  private String execCommand(String host, String command) {

    try {
      Session session = jsch.getSession(StatFrame.defaultUser, host, 22);
      session.setPassword(StatFrame.defaultPassword);
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.connect();

      ChannelExec channel = (ChannelExec) session.openChannel("exec");
      BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
      channel.setCommand(command + ";");
      channel.connect();

      String msg = null;
      StringBuffer ret = new StringBuffer();
      while ((msg = in.readLine()) != null) {
        ret.append(msg);
        ret.append("\n");
      }

      channel.disconnect();
      session.disconnect();
      return ret.toString();

    } catch (IOException e1) {
      return "Cannot get process name";
    } catch (JSchException e2) {
      return "Cannot get process name";
    }

  }
}
