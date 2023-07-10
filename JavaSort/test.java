class test {
    private static char[] converttostring(int ID_Bucket, long Low_Data_Str) {
        char[] tmp_string = new char[16];
        tmp_string[0] = (char) ('a' + ID_Bucket / 26);
        tmp_string[1] = (char) ('a' + ID_Bucket % 26);
        for (int i = 14; i > 1; i--) {
          tmp_string[i] = (char) ('a' + Low_Data_Str % 26);
          Low_Data_Str /= 26;
        }
        tmp_string[15] = '\n';
        return tmp_string;
      }
    public static void main(String[] args) {
        System.out.println(converttostring(26,1));
    }
}