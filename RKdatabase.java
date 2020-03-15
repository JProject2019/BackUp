package RKserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RKdatabase {
	
	static List<String> ids = new ArrayList<String>();

	static String date = null;
	static String time = null;
	
	public static void insert(String userid, String coordinates) {
		Connection conn = null;
		PreparedStatement pstmt = null;
	
	try {
		
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
	 	Date time1 = new Date();
	 	String timeT = format1.format(time1);
	 	String[] time_array = timeT.split(",");
	 	date = time_array[0];
	 	time = time_array[1];	//db에 날짜, 시간 넣기 위해
		
		Class.forName("com.mysql.cj.jdbc.Driver");
		
		String url = "jdbc:mysql://localhost/rkdatabase?useUnicode=true&useJDBCCompliantTimezoneShift"
				+ "=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		
		conn = DriverManager.getConnection(url,"yewon","yewon1234");
		
		System.out.println("연결 성공");
		
		String sql = "INSERT INTO gpsinfo VALUES(?,?,?,?)";
		pstmt = conn.prepareStatement(sql);
		
		pstmt.setString(1, userid);
		pstmt.setString(2, date);
		pstmt.setString(3,  time);
		pstmt.setString(4, coordinates);
	
		
		int count = pstmt.executeUpdate();
		if(count==0) {
		
		System.out.println("데이터 입력 실패");
		}
		else {
			System.out.println("데이터 입력 성공");
		}
		
		
	}catch(ClassNotFoundException e) {
		
	}catch(SQLException e) {
		System.out.println("에러: "+e);
		
	}
	finally {
		try {
			if(conn != null && !conn.isClosed()) {
			conn.close();
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	}
	public static void calldb() {
		Connection conn = null;
		Statement stmt = null;
	
		try {
			
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			String url = "jdbc:mysql://localhost/rkdatabase?useUnicode=true&useJDBCCompliantTimezoneShift"
					+ "=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
			
			conn = DriverManager.getConnection(url,"yewon","yewon1234");
			
			//System.out.println("연결 성공");
			
			String sql = "SELECT id FROM idtable";
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				ids.add(rs.getString("id"));
			}
			
			
			System.out.print("First id called:" + ids.get(0) + "\n");
			System.out.print("Second id called:" + ids.get(1)+ "\n");
			System.out.print("Third id called:" + ids.get(2)+ "\n");
			
			
		
	}catch(ClassNotFoundException e) {
		
	}catch(SQLException e) {
		System.out.println("에러: "+e);
		
	}finally {
		try {
			if(conn != null && !conn.isClosed()) {
			conn.close();
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	}

}
