package Shadow_impl.util;

import cn.edu.sustech.cs307.database.SQLDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class executeSQL {
    public static void update(String sql, Object...O){
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for(int i=0;i<O.length;i++) ps.setObject(i+1,O[i]);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Object> query(String sql, String className, Object...O){
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            for(int i=0;i<O.length;i++) ps.setObject(i+1,O[i]);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            Class<?> aClass = Class.forName(className);
            List<Object> L = new ArrayList<>();
            while(rs.next()){
                Object o = aClass.getDeclaredConstructor().newInstance();
                for(int i = 0;i<colCount;i++){
                    Object columnVal = rs.getObject(i+1);
                    String columnName = rsmd.getColumnName(i+1);

                    Field[] fields = aClass.getFields();
                    for(Field fie : fields){
                        if(fie.getName().toLowerCase().equals(columnName)){
                            columnName = fie.getName();
                            break;
                        }
                    }

                    Field declaredField = aClass.getField(columnName);
                    //aClass.getField()
                    declaredField.setAccessible(true);
                    declaredField.set(o, columnVal);
                }
                L.add(o);
            }
            rs.close();
            ps.close();
            return L;
        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}