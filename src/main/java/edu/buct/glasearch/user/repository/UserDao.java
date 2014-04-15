package edu.buct.glasearch.user.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import edu.buct.glasearch.user.entity.User;

public interface UserDao extends PagingAndSortingRepository<User, String> {
	User findByLoginName(String loginName);
}
