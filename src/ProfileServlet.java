package cc.cmu.edu.minisite;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;

public class ProfileServlet extends HttpServlet {
    // credentials for mysql
    private static final String user = "";
    private static final String password = "";
    private Connection conn = null;

    // setup mysql
    public ProfileServlet() {
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
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) 
            throws ServletException, IOException {
        JSONObject result = new JSONObject();
        // get input
        String id = request.getParameter("id");
        String pwd = request.getParameter("pwd");

        String name = "Unauthorized";
        String profile = "#";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // execute SELECT query
            stmt = conn.createStatement();
            // find password
            rs = stmt.executeQuery("SELECT * FROM users WHERE k = " + id);
            if (rs.next()) {
                String pwd2 = rs.getString("r");
                // check password
                if (pwd2.equals(pwd)) {
                    // find user information
                    rs = stmt.executeQuery("SELECT * FROM userinfo WHERE k = " + id);
                    if (rs.next()) {
                        name = rs.getString("r1");
                        profile = rs.getString("r2");
                    }
                } 
            }
        }
        catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        result.put("name", name);
        result.put("profile", profile);
    
        // return the query result
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
