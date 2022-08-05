package Shadow_impl.util;

import cn.edu.sustech.cs307.database.SQLDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class check {
    public static boolean checkPrere(int studentId, String courseId){
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select isPassedPrere(?,?)");
            ps.setInt(1,studentId);
            ps.setString(2,courseId);
            ResultSet rs = ps.executeQuery();
            boolean flg = false;
            if(rs.next()){
                flg = rs.getBoolean(1);
            }
            rs.close();
            ps.close();
            conn.close();
            return flg;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean checkConf(int studentId, int sectionId){
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement("select isconflict(?,?)");
            ps.setInt(1,studentId);
            ps.setInt(2,sectionId);
            ResultSet rs = ps.executeQuery();
            boolean flg = false;
            if(rs.next()){
                flg = rs.getBoolean(1);
            }
            rs.close();
            ps.close();
            conn.close();
            return flg;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
