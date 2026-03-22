package vn.com.m1tech.cnn.sor.appEtax.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LandingZoneDao {
	Connection conn;
	public Connection LandingZoneConnect(String driver, String host, String user, String pass) throws SQLException{
		
		try {
			Class.forName(driver);
			DriverManager.setLoginTimeout(300);
			System.out.println("paramater 1: " + host + "|" + user + "|" + pass);
			conn = DriverManager.getConnection(host, user, pass);			
		} catch (Exception ex) {
			ex.printStackTrace(); 
			System.out.println(ex.getMessage());
			if(!conn.isClosed()){ 
				conn.close();
			}
		}
		return conn;
	}  

	public void CloseConnect() throws Exception {
		try {
			conn.close();
		} catch (Exception ex) { throw ex; }
	}
	
}