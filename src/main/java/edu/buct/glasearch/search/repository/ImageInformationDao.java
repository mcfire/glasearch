package edu.buct.glasearch.search.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.user.entity.Task;

public interface ImageInformationDao extends PagingAndSortingRepository<ImageInformation, String>, JpaSpecificationExecutor<Task> {

}
