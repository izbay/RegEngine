package com.github.izbay.regengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CheckUpdate
{
  private final int projectID;
  private final Plugin plugin;
  private final String apiKey;
  
  public CheckUpdate(Plugin plugin, int projectID)
  {
    this(plugin, projectID, null);
  }
  
  public CheckUpdate(Plugin plugin, int projectID, String apiKey)
  {
    this.projectID = projectID;
    this.apiKey = apiKey;
    this.plugin = plugin;
    
    query();
  }
  
  public void query()
  {
    URL url = null;
    try
    {
      url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + this.projectID);
    }
    catch (MalformedURLException e)
    {
      e.printStackTrace();
      return;
    }
    try
    {
      URLConnection conn = url.openConnection();
      if (this.apiKey != null) {
        conn.addRequestProperty("X-API-Key", this.apiKey);
      }
      conn.addRequestProperty("User-Agent", 
        "ServerModsAPI-Example (by Gravity)");
      



      BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream()));
      String response = reader.readLine();
      

      JSONArray array = (JSONArray)JSONValue.parse(response);
      if (array.size() > 0)
      {
        JSONObject latest = (JSONObject)array.get(array.size() - 1);
        

        String versionName = (String)latest.get("name");
        double newVersion = Double.parseDouble(versionName.substring(versionName.length() - 4));
        

        double thisVersion = Double.parseDouble(this.plugin.getDescription().getVersion());
        if (thisVersion >= newVersion)
        {
          System.out.println("This version is up to date. Good job!");
        }
        else
        {
          System.out.println("*****************************************");
          System.out.println("YOUR VERSION IS NO LONGER THE MOST RECENT");
          System.out.println("GET THE LATEST BUILD, v" + newVersion + ", ON BUKKITDEV!");
          //System.out.println("bit.ly/stablemaster");
          System.out.println("*****************************************");
        }
      }
      else
      {
        System.out.println("There are no files for this project");
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return;
    }
  }
}
