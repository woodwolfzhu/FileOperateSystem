import java.util.regex.Pattern;

public class test {
    public static void main(String[] args){

//        String a= "rgsdgshkldjvflahreruv123.sdf";
//        String str = "[sdg]";
//        Pattern pattern =
//        boolean isMatch = a.matches(str);
//        if(isMatch){
//            System.out.println("我成功了");
//        }
//        else{
//            System.out.println("哎");
//        }

        String content = "ac.txt";

        String pattern = "a.\\.txt";

        boolean isMatch = Pattern.matches(pattern, content);
        System.out.println("字符串中是否包含了 'runoob' 子字符串? " + isMatch);
    }
}
