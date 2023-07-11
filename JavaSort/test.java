class test {
  public static void main(String[] args) {
    LongArray a = new LongArray(0);
    a.push(0L);
    a.push(255L);
    byte[] bytes = a.ExporttoBytes();
    System.out.println(bytes);
  }
}
