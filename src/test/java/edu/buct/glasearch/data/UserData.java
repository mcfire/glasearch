package edu.buct.glasearch.data;

import org.springside.modules.test.data.RandomData;

import edu.buct.glasearch.user.entity.User;

public class UserData {

	public static User randomNewUser() {
		User user = new User();
		user.setLoginName(RandomData.randomName("user"));
		user.setName(RandomData.randomName("User"));
		user.setPlainPassword(RandomData.randomName("password"));

		return user;
	}
}
