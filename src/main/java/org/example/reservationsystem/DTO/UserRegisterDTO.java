package org.example.reservationsystem.DTO;

public class UserRegisterDTO {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;

    public String getUsername() { return username; }  public void setUsername(String v) { this.username = v; }
    public String getPassword() { return password; }  public void setPassword(String v) { this.password = v; }
    public String getFullName() { return fullName; }  public void setFullName(String v) { this.fullName = v; }
    public String getEmail()    { return email; }     public void setEmail(String v)    { this.email = v; }
    public String getPhone()    { return phone; }     public void setPhone(String v)    { this.phone = v; }
}