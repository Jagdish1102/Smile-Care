package model;

public class PatientBillingInfo {
    private final int id;
    private final int age;
    private final String gender;

    public PatientBillingInfo(int id, int age, String gender) {
        this.id = id;
        this.age = age;
        this.gender = gender == null ? "" : gender;
    }

    public int getId() {
        return id;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }
}
