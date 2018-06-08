package cc.cmu.edu.minisite;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

public class FollowerServlet extends HttpServlet {
    // hbase configurations
    private final String hbaseTableName = "linktable";
    private final String hbaseMasterIP = "172.31.16.184";
    private Configuration hBaseConfig;
    private HTable hBaseTable;
    // mysql configurations
    private static final String user = "root";
    private static final String password = "15319project";
    private Connection conn = null;

    // setup mysql and hbase connection
    public FollowerServlet() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            System.out.println("Initialization Error");
        }
        
        try {
            // establish local mysql connection
            conn = DriverManager.getConnection("jdbc:mysql://localhost/demo", user, password);
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } 
        //initialize hbase connection, configure hbase
        hBaseConfig = HBaseConfiguration.create(); 
        //hBaseConfig.clear();
        hBaseConfig.set("fs.hdfs.impl", "emr.hbase.fs.BlockableFileSystem");
        hBaseConfig.set("hbase.zookeeper.quorum", hbaseMasterIP);
        hBaseConfig.set("hbase.rootdir", "hdfs://" + hbaseMasterIP + ":9000/hbase");
        //increase threads to 100 since there are only GET queries
        hBaseConfig.set("hbase.client.max.total.tasks", "1000");
        hBaseConfig.set("hbase.client.max.perserver.tasks", "1000");
        hBaseConfig.set("hbase.regionserver.handler.count", "1000");
        hBaseConfig.set("hbase.zookeeper.property.maxClientCnxns", "1000");
        hBaseConfig.set("hbase.cluster.distributed", "true");
        hBaseConfig.set("hbase.master.wait.for.log.splitting", "false");
        hBaseConfig.set("hfile.block.cache.size", "0.9");
        hBaseConfig.set("hbase.hregion.max.filesize", "10737418240");
        hBaseConfig.set("hbase.hregion.memstore.flush.size", "134217728");
        hBaseConfig.set("hbase.hregion.memstore.block.multiplier", "4");
        hBaseConfig.set("hbase.hstore.blockingStoreFiles", "30");
        hBaseConfig.set("hbase.client.scanner.caching", "100");
        hBaseConfig.set("hbase.zookeeper.property.clientPort", "2181");
        
        try {
            hBaseTable = new HTable(hBaseConfig, hbaseTableName);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        // get input
        String id = request.getParameter("id");
        // initialize return type
        JSONObject result = new JSONObject();
        JSONArray followers = new JSONArray();

        // get links from the hbase based on searchKey
        System.out.println(id);
        Get get = new Get(Bytes.toBytes(id));
        Result getResult = null;
        try {
            getResult = hBaseTable.get(get);
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        // convert to string 
        String[] links = null;
        if(!getResult.isEmpty()) {
            String getResultString = new String(getResult.
                getValue(Bytes.toBytes("data"), Bytes.toBytes("response")), StandardCharsets.UTF_8);
            links = getResultString.split(",");
        }

        Statement stmt = null;
        ResultSet rs = null;
        // find follower info from mysql database
        List<String> resultArr = new ArrayList<String>();
        for (int i = 0; i < links.length; i++) {
            String f_id = links[i];
            if (f_id == null || f_id.equals(""))
                continue;
            stmt = null;
            rs = null;
            try {
                // execute SELECT query
                stmt = conn.createStatement();
                // find follower name and image
                rs = stmt.executeQuery("SELECT * FROM userinfo WHERE k = " + f_id);
                if (rs.next()) {
                    resultArr.add(rs.getString("r1") + "15619" + rs.getString("r2"));
                }
            }
            catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }
        
        // sort the result array
        Collections.sort(resultArr);

        // add the sorted followers to result array
        for (int j = 0; j < resultArr.size(); j++) {
            String[] f = resultArr.get(j).split("15619");
            String name = f[0];
            String profile = f[1];
            JSONObject follower = new JSONObject();
            follower.put("name", name);
            follower.put("profile", profile);
            followers.put(follower);
            result.put("followers", followers);
        }
        
        // return query result
        PrintWriter writer = response.getWriter();
        writer.write(String.format("returnRes(%s)", result.toString()));
        writer.close();
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}

