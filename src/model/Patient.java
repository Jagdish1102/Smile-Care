package model;

public class Patient {

	private int id;
	private String name;
	private int age;
	private String gender;
	private String phone;
	private String address;
	private String disease;
	private String date;
	

	public Patient(int id, String name, int age, String gender, String phone, String address, String disease,
			String date) {
		this.id = id;
		this.name = name;
		this.age = age;
		this.gender = gender;
		this.phone = phone;
		this.address = address;
		this.disease = disease;
		this.date = date;
	
	}

	public Patient(String name, int age, String gender, String phone, String address, String disease, String date) {
		this(0, name, age, gender, phone, address, disease, date);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}

	public String getGender() {
		return gender;
	}

	public String getPhone() {
		return phone;
	}

	public String getAddress() {
		return address;
	}

	public String getDisease() {
		return disease;
	}

	public String getDate() {
		return date;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setDisease(String disease) {
		this.disease = disease;
	}

	public void setDate(String date) {
		this.date = date;
	}
	
}