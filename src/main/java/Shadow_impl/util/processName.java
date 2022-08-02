package Shadow_impl.util;

public class processName {
    public static String getName(String f_name, String s_name){
        if(f_name == null) return s_name;
        if(f_name.equals("")) return s_name;
        if(allAlpha(f_name)){
            return f_name.trim() + ' ' + s_name.trim();
        } else return f_name.trim() + s_name.trim();
    }

    private static boolean allAlpha(String f_name) {
        for(int i = 0; i<f_name.length(); i++){
            char ch = f_name.charAt(i);
            if(ch!=' ' && (ch<'a' || 'z' < ch) && (ch < 'A' || 'Z' < ch))
                return false;
        }
        return true;
    }
}
